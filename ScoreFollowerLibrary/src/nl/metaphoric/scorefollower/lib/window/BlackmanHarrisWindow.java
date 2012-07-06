package nl.metaphoric.scorefollower.lib.window;

/**
 * Implements the Blackman-Harris window
 * @author Elte Hupkes
 *
 */
public class BlackmanHarrisWindow implements WindowFunction {
	/**
	 * Blackman-Harris window parameters
	 */
	public static final double A0 = 0.35875,
							   A1 = 0.48829,
							   A2 = 0.14128,
							   A3 = 0.01168;
	
	@Override
	public double window(int index, int frameSize) {
		double f = 2 * Math.PI * index / (frameSize - 1);
		return A0 - A1 * Math.cos(f) + A2 * Math.cos(2 * f) + A3 * Math.cos(3 * f);
	}

}
