package nl.metaphoric.scorefollower.lib.window;

/**
 * Hamming Window function.
 * @author Elte Hupkes
 */
public class HammingWindow implements WindowFunction {
	@Override
	public double window(int index, int frameSize) {
		return (0.54 - 0.46 * Math.cos(2 * Math.PI * index / (frameSize - 1.0)));
	}	
}
