package nl.metaphoric.scorefollower.experiment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Annotator returns expected positions from given positions.
 * To do this it takes an input file and interpolates between
 * the given and expected values.
 * @author Elte Hupkes
 *
 */
public class Annotator {
	/**
	 * Annotation factor
	 */
	private double factor = -1;
	
	/**
	 * The k/v pair map
	 */
	private TreeMap<Double, Double> pairs;
	
	/**
	 * Creates an annotator using only a factor,
	 * so that input * factor = output.
	 * @param factor
	 */
	public Annotator(double factor) {
		this.factor = factor;
	}
	
	/**
	 * Creates an annotator using input/output pairs.
	 * @param filename
	 * @throws IOException 
	 */
	public Annotator(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		pairs = new TreeMap<Double, Double>();
		
		pairs.put(0d, 0d);
		while ((line = reader.readLine()) != null) {
			String[] kv = line.split("\\s+");
			if (kv.length < 2) {
				continue;
			}
			pairs.put(Double.parseDouble(kv[0]), Double.parseDouble(kv[1]));
		}
	}
	
	/**
	 * Returns the expected position of a given input time,
	 * or -1 if it can't be found. Linearly interpolates
	 * between known annotated points.
	 * @param time
	 * @return
	 */
	public double getExpectedPosition(double time) {
		if (factor > 0) {
			return time * factor;
		} else if (pairs.containsKey(time)) {
			return pairs.get(time);
		} else {
			// Find the first lower value and the first higher value,
			// and interpolate linearly.
			Map.Entry<Double, Double> lower = pairs.lowerEntry(time), 
									  higher = pairs.higherEntry(time);
			
			if (lower == null || higher == null) {
				// Can't determine the correct value
				return -1;
			}
			
			// Interpolate between two set values
			double frac = (time - lower.getKey()) / (higher.getKey() - lower.getKey());
			return lower.getValue() + (higher.getValue() - lower.getValue()) * frac;
		}
	}
}
