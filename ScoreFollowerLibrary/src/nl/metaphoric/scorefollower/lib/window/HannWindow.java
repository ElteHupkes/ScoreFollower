package nl.metaphoric.scorefollower.lib.window;

/**
 * Hann window function
 * @author Elte Hupkes
 */
public class HannWindow implements WindowFunction {
	@Override
	public double window(int index, int frameSize) {
		return 0.5 * (1 - Math.cos(2 * Math.PI * index / (frameSize - 1)));
	}
}
