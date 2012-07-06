package nl.metaphoric.scorefollower.lib.feature;

/**
 * An abstract base FrameVector. 
 * @author Elte Hupkes
 *
 */
public abstract class FrameVector {
	/**
	 * The mean and standard deviation of this vector.
	 * Used so commonly that they're defined here.
	 */
	public double mean, std;
	
	/**
	 * The lower and upper boundaries of frequencies
	 * that are used for the chroma vector.
	 */
	protected static final double MIN_FREQUENCY = 20,
								MAX_FREQUENCY = 2000;	
	
	/**
	 * Log tag
	 */
	protected static final String tag = "SF_FrameVector";
	
	/**
	 * We're going to need this fixed value over and over
	 */
	protected static final double logBase = Math.log(2);	
	
	/**
	 * The chroma vector, with chroma bins starting at A.
	 * Implementation may vary.
	 */
	protected double[] chroma;	
	
	/**
	 * The amplitude of this FrameVector. 
	 * 
	 * his is the average intensity of the supplied data as it was measured. 
	 * Note that this value is meaningless without context, 
	 * since amplitude is heavily dependent on environmental conditions. 
	 * 
	 * The amplitude value is always positive.
	 */
	public double rms;	
	
	/**
	 * String representation of the vector, can be used
	 * to write to file.
	 */
	public String toString() {
		StringBuffer n = new StringBuffer();
		for (int i = 0; i < 11; i++) {
			n.append(chroma[i]).append(" ");
		}
		n.append(chroma[11]).append(" ").append(rms);
		return n.toString();
	}
	
	/**
	 * Returns the match probability of this vector
	 * with another FrameVector.
	 * @param b
	 * @return
	 */
	public abstract double matchProbability(FrameVector b);
	
	/**
	 * @param frequency The frequency in Hz
	 * @return The frequency bin (as an equal-tempered distance from A0(440Hz), between 0 and 12
	 */
	public static int frequencyBin(double frequency) {
		/**
		 * Calculating a frequency from a note (assuming 12 step equal-temperament)
		 * uses the formula:
		 * f = 440 * 2^(n/12)
		 * We already have the frequency though, so we need to invert that:
		 * n = log2(f/440) * 12
		 * 
		 * Java returns the modulo with the sign of the dividend, meaning it can be negative.
		 * In order to always get a number 0-12, add 12 and take the modulo again.
		 */
		return ((12 + (int) Math.round(12 * Math.log(frequency / 440.0) / logBase) % 12) % 12);
	}
	
	/**
	 * Returns the chroma array of this vector
	 * @return
	 */
	public double[] getChroma() {
		return chroma;
	}
}