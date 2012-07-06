package nl.metaphoric.scorefollower.lib;

import nl.metaphoric.scorefollower.lib.feature.FrameVector;

/**
 * Analyzes incoming FrameVectors / amplitudes, to see
 * if there is active microphone input. 
 * 
 * It keeps track of the minimum rms value recorded,
 * and calculates a sound power level relative to
 * that. 
 * 
 * @author Elte Hupkes
 *
 */
public class PlaybackAnalyzer {
	private final String TAG = "SF_PlaybackAnalyzer";
	
	/**
	 * Playback states
	 * TODO: Change to final ints, which 
	 * apparently has better performance.
	 */
	public enum Status {WAITING, ACTIVE}	
	
	/**
	 * The minimum amplitude registered,
	 * used to calculate a sound power level.
	 */
	private double min = 99999999;
	
	/**
	 * Cool off period for audio spikes
	 */
	private static final int COOL_DOWN_INIT = 1, WARM_UP_INIT = 2;
	
	/**
	 * Cool off value for audio spikes. This is used
	 * to account for short silences in songs.
	 */
	private int coolDown = 0;

	/**
	 * The started / warm up variables
	 */
	private int warmUp = WARM_UP_INIT;
	
	/**
	 * The average of the silent amplitudes
	 */
	private RunningAverage amplitudes = new RunningAverage();	
	
	/**
	 * The last calculated status
	 */
	private Status status = Status.WAITING;
	
	/**
	 * Resets the analyzer
	 */
	public void reset() {
		amplitudes.reset();
		status = Status.WAITING;
		coolDown = 0;
		min = 9999999;
	}
	
	/**
	 * Adds data to the analyzer and calculates
	 * a new status.
	 * @param v
	 */
	public void addData(FrameVector v) {
		Log.d(TAG, "RMS: "+v.rms);
		
		if (v.rms < min && v.rms > 0.0) {
			min = v.rms;
			Log.d(TAG, "Min: "+min);
		}
		
		if (amplitudes.getCount() < 2) {
			// Wait for more data
			amplitudes.add(v.rms);
			return;
		}
		
		double dB = v.rms < min ? 0.0 : 10 * Math.log10(v.rms / min);
		Log.d(TAG, "dB: "+dB);
		
		boolean amplLow  = dB < Parameters.dBTreshold;
		
		if (amplLow) {
			if (coolDown > 0) {
				coolDown--;
			} else {
				status = Status.WAITING;
			}
		} else {
			// Only use a cool down after the startup phase.
			// This way we can deal with initial spikes.
			status = Status.ACTIVE;
			if (warmUp <= 0) {
				coolDown = COOL_DOWN_INIT;
			} else {
				warmUp--;
			}
		}
	}
	
	/**
	 * "Hacky" method to force a playback analyzer
	 * start. Involves setting the min value to
	 * something very low, and adding some
	 * arbitrary amplitude data.
	 */
	public void forceStart() {
		min = 0.00000001;
		if (amplitudes.getCount() < 2) {
			amplitudes.add(0.00000001);
			amplitudes.add(0.00000001);
		}
	}
	
	/**
	 * Returns the last calculated status
	 * @return
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Adds new data and returns the newly calculated
	 * status.
	 * @param v
	 * @return
	 */
	public Status getStatus(FrameVector v) {
		addData(v);
		return status;
	}
}
