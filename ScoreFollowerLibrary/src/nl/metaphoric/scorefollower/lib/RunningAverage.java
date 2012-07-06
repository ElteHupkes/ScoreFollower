/**
 * Implements a running average for doubles
 * 
 * @author Elte Hupkes
 * 
 * For the calculations used see:
 * http://www.johndcook.com/standard_deviation.html
 * and also:
 * http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Compute_running_.28continuous.29_variance
 */
package nl.metaphoric.scorefollower.lib;

public class RunningAverage {
	/**
	 * Average counter
	 */
	private int count;
	
	/**
	 * The M and S values, for details see URLs above
	 */
	private double vM, vS, var;
	
	/**
	 * Creates a new running average
	 */
	public RunningAverage() {
		reset();
	}
	
	/**
	 * Creates a new running average with initial values.
	 */
	public RunningAverage(double m, double s) {
		reset(m, s);
	}
	
	/**
	 * Resets the running average
	 */
	public void reset() {
		vM = vS = count = 0;
	}
	
	/**
	 * Resets the running average with initial
	 * values.
	 */
	public void reset(double m, double s) {
		count = 1;
		vM = m;
		vS = s;
	}
	
	/**
	 * Adds a value to the running average
	 * @param v
	 */
	public void add(double v) {
		double diff = (v - vM);
		count++;
		vM += diff / (double)count;
		
		// Note that vM has changed, so v - vM is a different value here.
		vS += diff * (v - vM);
		var = vS / (count - 1);
	}
	
	/**
	 * Returns the mean
	 * @return
	 */
	public double getMean() {
		return vM;
	}
	
	/**
	 * Returns the variance
	 * @return
	 */
	public double getVariance() {
		return var;
	}
	
	/**
	 * Returns the standard deviation
	 * @return
	 */
	public double getStd() {
		return Math.sqrt(var);
	}
	
	/**
	 * @return The number of values
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * Returns a string representation of
	 * the current average.
	 */
	public String toString() {
		return getMean()+"";
	}
}
