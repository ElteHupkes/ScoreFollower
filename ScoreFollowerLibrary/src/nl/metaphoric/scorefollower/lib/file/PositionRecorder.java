package nl.metaphoric.scorefollower.lib.file;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import nl.metaphoric.scorefollower.lib.AudioAnalyzer;
import nl.metaphoric.scorefollower.lib.Log;
import nl.metaphoric.scorefollower.lib.PlaybackAnalyzer;
import nl.metaphoric.scorefollower.lib.PlaybackAnalyzer.Status;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.Position;

/**
 * Records position times and writes / markers,
 * writes them to file.
 * 
 * @author Elte Hupkes
 */
public class PositionRecorder {
	/**
	 * Log tag
	 */
	private static String TAG = "SF_PositionRecorder";
	
	/**
	 * Reference data for the position recorder
	 */
	private Vector<FrameVector> ref;
	
	/**
	 * Stores set times, aligns with the
	 * "positions" array.
	 */
	private Map<Integer, Position> positions;
	
	/**
	 * An amplitude analyzer to detect silences
	 */
	private PlaybackAnalyzer status;
	
	/**
	 * The active playing status (basically tells you
	 * if the performer is making a sound).
	 */
	public boolean playing = false;
	
	/**
	 * A list with time data, stored as the
	 * number of received frames (even the
	 * ones ignored) at the point that each
	 * vector is stored.
	 */
	private List<Integer> times;
	private int receivedFrames = 0;
	
	/**
	 * Initializes a new PositionRecorder
	 */
	public PositionRecorder() {
		ref = new Vector<FrameVector>();
		positions = new HashMap<Integer, Position>();
		status = new PlaybackAnalyzer();
	}
	
	/**
	 * Call this method once _before_ you add
	 * any positions to record associated time
	 * data as well. Useful for benchmarking. 
	 */
	public void recordTimes() {
		times = new ArrayList<Integer>();
	}
	
	public void reset() {
		ref.clear();
		positions.clear();
		status.reset();
		playing = false;
		times = null;
		receivedFrames = 0;
	}
	
	/**
	 * Add a new FrameVector to the reference data
	 * @param v
	 */
	public void addData(FrameVector v) {
		Status s = status.getStatus(v);
		receivedFrames++;
		
		if (s == Status.WAITING) {
			Log.d(TAG, "No active audio input...");
			playing = false;
			return;
		}
		
		playing = true;
		
		if (times != null) {
			times.add(receivedFrames);
		}
		
		// Vector is synchronized by itself, so this shouldn't be a problem
		ref.add(v);
	}
	
	/**
	 * Adds a new position at the current time (after
	 * the last inserted FrameVector).
	 * @param x
	 * @param y
	 */
	public int addPosition(Position p) {
		// Store position at the last recorded vector index
		int pos = ref.size() > 0 ? ref.size() - 1 : 0;
		Log.d(TAG, "Adding new position at position index "+pos);
		positions.put(pos, p);
		return pos;
	}
	
	/**
	 * Writes this reference data to the specified file.
	 * @param filename
	 * @param pages
	 * @param windowSize
	 * @param hopSize
	 * @param framerate
	 * @throws IOException 
	 */
	public void write(String filename, String[] pages, AudioAnalyzer analyzer) 
			throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
		out.write("windowSize="+analyzer.windowSize());
		out.newLine();
		out.write("hopSize="+analyzer.hopSize());
		out.newLine();
		out.write("framerate="+analyzer.getSampleRate());
		out.newLine();
		out.newLine();
		
		// Write page filenames
		for (String page : pages) {
			out.write("page="+page);
			out.newLine();
		}
		
		out.newLine();
		out.newLine();
		
		int i = 0;
		for (FrameVector v : ref) {
			if (times != null) {
				out.write(((times.get(i) - 1) * analyzer.hopSize()) +":");
			}
			out.write(v.toString());
			if (positions.containsKey(i)) {
				out.write(" "+positions.get(i).toString());
			}
			out.newLine();
			i++;
		}
		
		out.close();
		
		Log.d(TAG, "Successfully wrote position data to file @ "+filename);
	}
	
	/**
	 * Returns the playback analyzer used by this matcher
	 * @return
	 */
	public PlaybackAnalyzer getPlaybackAnalyzer() {
		return status;
	}
}
