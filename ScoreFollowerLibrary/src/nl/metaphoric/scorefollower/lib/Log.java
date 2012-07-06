package nl.metaphoric.scorefollower.lib;

/**
 * By using the lib code both as a Java project and as an
 * Android project, logging using the Android Log class has
 * become impossible.
 * To overcome this problem I created this Log class instead,
 * which can be fed with a platform specific logger, but mimics
 * the Android class' behavior.  
 * @author Elte Hupkes
 *
 */
public class Log {
	/**
	 * Interface that needs to be implemented
	 * by logging implementations.
	 */
	public interface Logger {
		public void d(String tag, String message);
		public void i(String tag, String message);
		public void w(String tag, String message);
		public void e(String tag, String message);
	}
	
	/**
	 * The active logger instance
	 */
	private static Logger l = null;
	
	/**
	 * Sets the logger to use
	 * @param l
	 */
	public static void setLogger(Logger l) {
		Log.l = l;
	}
	
	/**
	 * Debug log entry
	 * @param tag
	 * @param message
	 */
	public static void d(String tag, String message) {
		if (l != null) {
			l.d(tag, message);
		}
	}
	
	/**
	 * Info log entry
	 * @param tag
	 * @param message
	 */
	public static void i(String tag, String message) {
		if (l != null) {
			l.i(tag, message);
		}
	}
	
	/**
	 * Warning log entry
	 * @param tag
	 * @param message
	 */
	public static void w(String tag, String message) {
		if (l != null) {
			l.w(tag, message);
		}
	}
	
	/**
	 * Error log entry
	 * @param tag
	 * @param message
	 */
	public static void e(String tag, String message) {
		if (l != null) {
			l.e(tag, message);
		}
	}
}
