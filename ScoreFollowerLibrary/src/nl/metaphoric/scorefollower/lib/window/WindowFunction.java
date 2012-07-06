package nl.metaphoric.scorefollower.lib.window;

/**
 * Interface for a class that implements a window function
 * @author Elte Hupkes
 */
public interface WindowFunction {
	/**
	 * 
	 * @param value The input value
	 * @param index The index in the frame
	 * @param frameSize The total size of the frame
	 * @return The value transformed using this window
	 */
	public double window(int index, int frameSize);
}
