package nl.metaphoric.scorefollower.lib;

import nl.metaphoric.scorefollower.lib.feature.FrameVectorFactory;
import nl.metaphoric.scorefollower.lib.window.HannWindow;
import nl.metaphoric.scorefollower.lib.window.WindowFunction;

/**
 * The Parameters class stores all the algorithm parameters
 * that can be varied as to have one central place in which
 * to modify them for the entire application.
 * @author Elte Hupkes
 *
 */
public class Parameters {
	/**
	 * The size in seconds of one audio data window
	 */
	public static double windowSize = 0.25;
	
	/**
	 * The size in seconds of the jump in time from
	 * the start of one window to the next. If this
	 * is smaller than windowSize the windows overlap.
	 * Constraint:
	 * 0 < hopSize <= windowSize
	 */
	public static double hopSize = 0.25;
	
	/**
	 * The recorder sample rate
	 */
	public static float sampleRate = 44100;
	
	/**
	 * The size in seconds of the window that is searched
	 * for a matching position around each candidate data point,
	 * both backward and forward.
	 */
	public static float searchWindow = 6.0f;
	
	/**
	 * The distance in which data windows are discarded
	 */
	public static float distWindow = 10.0f;
	
	/**
	 * The initial time transition standard deviation in seconds
	 */
	public static double startStdDev = 2.0;
	
	/**
	 * The minimum time transition standard deviation in
	 * seconds.
	 */
	public static double minStdDev = 1.0;
	
	/**
	 * The minimum value (in detected decibel) that is considered
	 * active sound by the PlaybackAnalyzer.
	 */
	public static double dBTreshold = 8;
	
	/**
	 * The window function actively used
	 */
	public static WindowFunction window = new HannWindow();
	
	/**
	 * The type of FrameVector used.
	 */
	public static int frameVectorType = FrameVectorFactory.TYPE_LOG_SUM_CHROMA;
}
