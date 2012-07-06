package nl.metaphoric.scorefollower.utils;

import android.util.Log;
import nl.metaphoric.scorefollower.lib.Log.Logger;

/**
 * Android Logger implementation for Logger
 * @author Elte Hupkes
 */
public class AndroidLogger implements Logger {
	public void d(String tag, String msg) { Log.d(tag, msg); }
	public void w(String tag, String msg) { Log.w(tag, msg); }
	public void e(String tag, String msg) { Log.e(tag, msg); }
	public void i(String tag, String msg) { Log.i(tag, msg); }
}
