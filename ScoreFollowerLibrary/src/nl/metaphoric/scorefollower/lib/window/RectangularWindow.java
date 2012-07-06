package nl.metaphoric.scorefollower.lib.window;

/**
 * Rectangular window function, returns the value passed in.
 * @author Elte Hupkes
 */
public class RectangularWindow implements WindowFunction {
	@Override
	public double window(int index, int frameSize) {
		return 1.0;
	}

}
