package nl.metaphoric.scorefollower.lib.window;

/**
 * Implements the Blackman window
 * @author Elte Hupkes
 */
public class BlackmanWindow implements WindowFunction {
	/**
	 * Alpha parameter for the Blackman window
	 */
	public static final double A = 0.16,
							   A0 = (1 - A) / 2.0,
							   A1 = (0.5),
							   A2 = A / 2.0;
	
	@Override
	public double window(int index, int frameSize) {
		double f = 2 * Math.PI * index / (frameSize - 1);
		return A0 - A1 * Math.cos(f) + A2 * Math.cos(2 * f);
	}

}
