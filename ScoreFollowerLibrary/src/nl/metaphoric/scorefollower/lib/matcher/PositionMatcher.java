/**
 * The PositionMatcher estimates position in an audio
 * file from reference data. This uses a Hidden Markov
 * Model around frame vectors. 
 * 
 * @author Elte Hupkes
 */
package nl.metaphoric.scorefollower.lib.matcher;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import nl.metaphoric.scorefollower.lib.Log;
import nl.metaphoric.scorefollower.lib.Parameters;
import nl.metaphoric.scorefollower.lib.PlaybackAnalyzer;
import nl.metaphoric.scorefollower.lib.PlaybackAnalyzer.Status;
import nl.metaphoric.scorefollower.lib.RunningAverage;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.file.ScoreReader.FileSettings;

public class PositionMatcher {
	/**
	 * Debug logging tag
	 */
	private static final String TAG = "SF_PosMatcher";
	
	/**
	 * The probability under which new paths will
	 * be discarded to save computation.
	 * This discarding happens after normalization.
	 */
	public static final double EPSILON = 0.000001;
	
	/**
	 * The window in which we're trying to maintain
	 * matching positions, in seconds. Any path
	 * outside this window is automatically discarded
	 * in the "previous" array.
	 */
	private double posWindow;
	
	/**
	 * Holds a list of previous positions.
	 * The "next" map is to fill the replacement array.
	 */
	private SortedMap<Integer, Double> previous, next;
	
	/**
	 * The current position, -1 is "not started"
	 * 
	 * This stores the index of the last matched position
	 * vector. Since matching windows are obviously always
	 * back in time, the actual matching position, in time,
	 * is always one place _after_ this position.
	 * 
	 * The reference file stores "tapped" positions with
	 * the last recorded FrameVector, so this back-in-time
	 * thing won't actually matter.
	 */
	private int position;
	
	/**
	 * Vector holding previous differences
	 * @see setPosition
	 */
	private RunningAverage diffs = new RunningAverage();
	
	/**
	 * The mean and variance used to determine the transition
	 * probability from the current position.
	 */
	private double mean, std, stdMin;
	
	/**
	 * Reference FrameVector array.
	 */
	private Vector<FrameVector> reference;
	
	/**
	 * The search window size in seconds
	 */
	private int search;
	
	/**
	 * The difference, in seconds, between two
	 * reference vectors, and the duration
	 * of one window.
	 */
	private double hopSize, windowSize;
	
	/**
	 * The started amplitude analyzer
	 */
	private PlaybackAnalyzer status = new PlaybackAnalyzer();
	
	/**
	 * The active playing status (basically tells you
	 * if the performer is making a sound).
	 */
	public boolean playing = false;
	
	/**
	 * Creates a new position matcher from the given
	 * reference data. 
	 */
	public PositionMatcher(Vector<FrameVector> ref, FileSettings settings) {
		windowSize = settings.getDouble("windowSize");
		hopSize = settings.getDouble("hopSize");
		reference = ref;
		
		restart();
	}
	
	/**
	 * (re)starts the matcher.
	 */
	public void restart() {
		restart(-1);
	}
	
	/**
	 * Restarts the matcher at the specified
	 * initial position.
	 * @param position
	 */
	public void restart(int position) {
		diffs.reset();
		
		// Create maps for difference probabilities
		previous = new TreeMap<Integer, Double>();
		next = new TreeMap<Integer, Double>();
		previous.put(position, 1.0);
		
		// Set default search window, in seconds
		search = (int)(0.5 * (Parameters.searchWindow / hopSize));
		posWindow = Parameters.distWindow / hopSize;
		
		// Set default mean and standard deviation
		mean = 1;
		std = Parameters.startStdDev / hopSize;
		stdMin = Parameters.minStdDev / hopSize;
		
		status.reset();
		this.position = position;
	}
	
	/**
	 * Returns the currently set hop size
	 * @return
	 */
	public double hopSize() {
		return hopSize;
	}
	
	/**
	 * Returns the currently set window size
	 * @return
	 */
	public double windowSize() {
		return windowSize;
	}
	
	/**
	 * Returns the position index in the reference data best corresponding to the given
	 * FrameVector. How well this matches with the real-time position depends on the
	 * duration of the calculation
	 * @param v
	 * @return
	 */
	public int getPosition(FrameVector v) {
		Status s = status.getStatus(v);
		
		if (s == Status.WAITING) {
			// Assume the player has paused
			Log.d(TAG, "Performance waiting at "+this.position);
			playing = false;
			return this.position > 0 ? this.position : 0;
		}
		
		playing = true;
		
		// The best estimate index and its probability
		int best = 0;
		double pBest = 0.0;
		double[] transitions = new double[2 * search];
		
		// Search window positions
		int start, end;
		
		// New position iterator
		int ln;
		
		// Path probability, transition probability
		double pPath, pn;
		
		for (int l : previous.keySet()) {
			// Determine search window boundaries
			start = Math.max(0, (int)Math.round(l + mean - search));
			end = Math.min(reference.size(), (int)Math.round(l + mean + search));
			
			// Get new transitions in the transitions array
			getTransitions(l, v, start, end, transitions);
			pPath = previous.get(l);
			for (ln = start; ln < end; ln++) {
				pn = transitions[ln - start] * pPath;
				
				//if (pn < 0) {
				//	Log.d(TAG, "Probability < 0, is your vector working correctly?");
				//}
				
				if (!next.containsKey(ln) || next.get(ln) < pn) {
					next.put(ln, pn);
				}
				
				if (pn > pBest) {
					// Store the best known value
					best = ln;
					pBest = pn;
				}
			}
		}
		
		// Swap previous and next maps
		SortedMap<Integer, Double> swap = previous;
		previous = next;
		next = swap;
		
		//Log.d(TAG, "pBest: "+pBest);
		
		// Clear for the next iteration
		next.clear();
		normalizeTransitions(pBest);
		setPosition(best);
		return best;
	}
	
	/**
	 * Sets the internal position to the given position,
	 * and updates mean / diff / variance values.
	 * @param position
	 */
	private void setPosition(int position) {
		/**
		 * Calculate new mean and stdDev.
		 * 
		 * http://www.johndcook.com/standard_deviation.html
		 * and also:
		 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Compute_running_.28continuous.29_variance
		 */
		diffs.add(position - this.position);
		if (diffs.getCount() > 20) {
			/*
			 * If enough data is available, use past information
			 * to determine mean and standard deviation.
			 * 
			 * To allow mistakes, multiply the standard deviation
			 * by some value (currently its 2, but I'll have to
			 * experiment with that). 
			 */
			mean = diffs.getMean();
			std = Math.max(diffs.getStd() * 2, stdMin);
			
			//Log.d(TAG, "New position mean: "+mean);
			//Log.d(TAG, "New position std: "+std);
		}
		
		this.position = position;
	}
	
	/**
	 * Get all the transitions from position l, and their probabilities
	 * given the specified FrameVector v.
	 * @param l The position (index) from which the transitions must be calculated. This determines
	 * 			the transition probabilities.
	 * @param v The FrameVector of the current input signal
	 * @param start The start of the search window, including this index
	 * @param end The end of the search window, NOT including this index
	 * @param transitions Return array for the probabilities. Should be at least
	 * 						start - end elements long.
	 */
	private void getTransitions(int l, FrameVector v, int start, int end, double[] transitions) {
		double pTrans, mean = l + this.mean;
		for (int i = start; i < end; i++) {
			// Calculate transition probability as a normal distribution
			pTrans = 1.0 / (std * Math.sqrt(2 * Math.PI)) * 
					 Math.exp(-Math.pow(i - mean, 2) / (2 * std * std));
			//pTrans = 1.0;
			transitions[i - start] = v.matchProbability(reference.get(i)) * pTrans;
		}
	}
	
	/**
	 * Normalizes all current transition probabilities to one
	 * so they won't simply converge to zero.
	 * @param best
	 */
	private void normalizeTransitions(double best) {
		int probDisc = 0, posDisc = 0, total = previous.size();
		Iterator<Integer> it = previous.keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			double nw = previous.get(i) / best;
			if (nw < EPSILON) {
				probDisc++;
				it.remove();
			} else if (Math.abs(i - position) > posWindow) {
				it.remove();
				posDisc++;
			} else {				
				previous.put(i, nw);
			}
		}
		//Log.d(TAG, "Discarded "+probDisc+"/"+total+" elements due to low probability.");
		//Log.d(TAG, "Discarded "+posDisc+"/"+total+" elements due to distance.");
	}
	
	
	
	/**
	 * Returns a map with the latest transition probabilities.
	 * Note that modifying this map will mess up the position
	 * matching.
	 * @return
	 */
	public SortedMap<Integer, Double> getTransitionProbabilities() {
		return previous;
	}
	
	/**
	 * Returns the playback analyzer used by this matcher
	 * @return
	 */
	public PlaybackAnalyzer getPlaybackAnalyzer() {
		return status;
	}
}
