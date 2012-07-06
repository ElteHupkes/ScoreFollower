package nl.metaphoric.scorefollower.lib.feature;

import nl.metaphoric.scorefollower.lib.AudioBuffer;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Fake vector used for benchmarking, always
 * returns the same probability, thereby creating
 * equal-probability paths that will never be discarded.
 * 
 * @author Elte Hupkes
 */
public class StrainVector extends LogSumChromaVector {

	public StrainVector(AudioBuffer buffer, float sampleRate,
			DoubleFFT_1D transformer) {
		super(buffer, sampleRate, transformer);
	}

	public StrainVector(double[] parts) {
		super(parts);
	}

	/**
	 * Return constant match probability to confuse
	 * the matcher.
	 */
	public double matchProbability(FrameVector v) {
		return 1.0;
	}
}
