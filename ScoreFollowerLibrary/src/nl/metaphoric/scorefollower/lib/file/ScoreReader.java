package nl.metaphoric.scorefollower.lib.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import nl.metaphoric.scorefollower.lib.Position;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.feature.FrameVectorFactory;
import nl.metaphoric.scorefollower.lib.matcher.PositionMatcher;
import nl.metaphoric.scorefollower.lib.matcher.PositionPager;

/**
 * Class that reads a score from file into a PositionMatcher
 * and a PositionPager.
 * 
 * @author Elte Hupkes
 */
public class ScoreReader {
	/**
	 * The matcher object
	 */
	private PositionMatcher matcher;
	
	/**
	 * The pager object
	 */
	private PositionPager pager;
	
	/**
	 * Page filenames
	 */
	private List<String> filenames;
	
	/**
	 * Timings for each recorded position
	 */
	private List<Double> times = null;
	
	/**
	 * Stores settings
	 */
	private FileSettings settings;
	
	/**
	 * Simple class to store / retrieve settings from the
	 * loaded file.
	 * @author Elte Hupkes
	 */
	public class FileSettings {
		private Map<String, String> settings = new HashMap<String, String>();
		
		public void put(String key, String value) {
			settings.put(key, value);
		}
		
		public String getString(String key) {
			return settings.get(key);
		}
		
		public double getDouble(String key) {
			return Double.parseDouble(settings.get(key));
		}
	}
	
	/**
	 * Creates a new Score Reader
	 * @param reader
	 * @param saveTimes If true, creates a list of time data associated
	 * 					with each vector (if the file specifies it). This
	 * 					is useful for benchmarking if you're not sure
	 * 					when silences were detected.
	 */
	public ScoreReader(Reader file, boolean saveTimes) throws IOException {
		settings = new FileSettings();
		Vector<FrameVector> reference = new Vector<FrameVector>();
		TreeMap<Integer, Position> positions = new TreeMap<Integer, Position>();
		filenames = new LinkedList<String>();
		
		if (saveTimes) {
			times = new ArrayList<Double>();
		}
		
		BufferedReader reader = new BufferedReader(file);
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0 || line.charAt(0) == '#') {
				continue;
			}
			
			// Detect settings (> 0 because setting cannot be empty)
			if (line.indexOf('=') > 0) {
				String[] kv = line.split("=");
				if (kv[0].equals("page")) {
					filenames.add(kv[1]);
				} else {					
					settings.put(kv[0], kv[1]);
				}
			} else {
				// Must be a FrameVector
				
				// If available, get timing data (used mostly for benchmarking)
				double time;
				if (line.indexOf(':') > 0) {
					String[] tv = line.split(":");
					line = tv[1].trim();
					time = Double.parseDouble(tv[0]);
				} else {
					time = -1;
				}
				
				String[] items = line.split("\\s+");
				
				// Only continue on "valid" vectors, this check isn't very strict right now.
				if (items.length >= 13) {
					double[] chroma = new double[items.length];
					for (int i = 0; i < items.length; i++) {
						chroma[i] = Double.parseDouble(items[i]);
					}
					reference.add(FrameVectorFactory.getVector(chroma));
					
					if (items.length >= 16) {
						// Includes x/y position coordinates, add them to position reference array
						positions.put(count, new Position((int)chroma[13], chroma[14], chroma[15]));
					}
					count++;
					
					if (times != null) {						
						times.add(time);
					}
				}
			}
		}
		
		matcher = new PositionMatcher(reference, settings);
		pager = new PositionPager(positions, settings);
	}
	
	/**
	 * Default score reader that doesn't create a times object
	 * @param filename
	 * @throws IOException
	 */
	public ScoreReader(String filename) throws IOException {
		this(filename, false);
	}
	
	/**
	 * Creates a new ScoreReader from a filename instead
	 * of a reader.
	 * @param filename
	 * @param saveTimes
	 * @throws IOException
	 */
	public ScoreReader(String filename, boolean saveTimes) throws IOException {
		this(new FileReader(filename), saveTimes);
	}
	
	/**
	 * Returns the settings object
	 * @return
	 */
	public FileSettings getSettings() {
		return settings;
	}
	
	/**
	 * Return the generated PositionMatcher
	 * @return
	 */
	public PositionMatcher getMatcher() { 
		return matcher; 
	}
	
	/**
	 * Returns the generated position pager
	 */
	public PositionPager getPager() {
		return pager;
	}
	
	/**
	 * Returns page filenames
	 * @return
	 */
	public List<String> getPages() {
		return filenames;
	}
	
	/**
	 * Returns the times object, which is only available
	 * if the reader was initialized with saveTimes = true.
	 * @return
	 */
	public List<Double> getTimes() {
		return times;
	}
}
