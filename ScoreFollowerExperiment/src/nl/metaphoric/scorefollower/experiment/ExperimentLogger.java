package nl.metaphoric.scorefollower.experiment;

import nl.metaphoric.scorefollower.lib.Log.Logger;

/**
 * Logger implementation that mimics Android's Log class
 * so it can be used in shared libraries.
 * @author Elte Hupkes
 *
 */
public class ExperimentLogger implements Logger {
	public boolean debug = true;
	public boolean silent = false;
	

	@Override
	public void d(String tag, String message) {
		if (debug && !silent) {			
			System.out.println("D ["+tag+"]: "+message);
		}
	}

	@Override
	public void i(String tag, String message) {
		if (!silent) {			
			System.out.println("I ["+tag+"]: "+message);
		}
	}

	@Override
	public void w(String tag, String message) {
		System.out.println("W ["+tag+"]: "+message);
	}

	@Override
	public void e(String tag, String message) {
		System.out.println("E ["+tag+"]: "+message);
	}

}
