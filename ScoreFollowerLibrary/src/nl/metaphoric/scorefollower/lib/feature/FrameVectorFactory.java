package nl.metaphoric.scorefollower.lib.feature;

import nl.metaphoric.scorefollower.lib.AudioBuffer;
import nl.metaphoric.scorefollower.lib.Parameters;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Produces FrameVectors depending on the current FrameVector settings.
 * Singleton class.
 * @author Elte Hupkes
 *
 */
public class FrameVectorFactory {
	/**
	 * FrameVector types.
	 */
	public static final int TYPE_LINEAR_CHROMA = 0,
							TYPE_LOG_CHROMA = 1,
							TYPE_LINEAR_SUM_CHROMA = 2,
							TYPE_LOG_SUM_CHROMA = 3,
							TYPE_STRAIN = 4;
	
	/**
	 * Enforces singleton
	 */
	private FrameVectorFactory() {}
	
	/**
	 * Returns a FrameVector of the current type.
	 * @return
	 */
	public static FrameVector getVector(double[] parts) {
		switch (Parameters.frameVectorType) {
		case TYPE_LOG_CHROMA:
			return new LogChromaVector(parts);
		case TYPE_LINEAR_SUM_CHROMA:
			return new LinearSumChromaVector(parts);
		case TYPE_LOG_SUM_CHROMA:
			return new LogSumChromaVector(parts);
		case TYPE_STRAIN:
			return new StrainVector(parts);
		default:
			return new LinearChromaVector(parts);
		}
	}
	
	/**
	 * Returns a FrameVector of the current type.
	 * @return
	 */
	public static FrameVector getVector(AudioBuffer buffer, float sampleRate, DoubleFFT_1D transformer) {
		switch (Parameters.frameVectorType) {
		case TYPE_LOG_CHROMA:
			return new LogChromaVector(buffer, sampleRate, transformer);
		case TYPE_LINEAR_SUM_CHROMA:
			return new LinearSumChromaVector(buffer, sampleRate, transformer);
		case TYPE_LOG_SUM_CHROMA:
			return new LogSumChromaVector(buffer, sampleRate, transformer);
		case TYPE_STRAIN:
			return new StrainVector(buffer, sampleRate, transformer);
		default:
			return new LinearChromaVector(buffer, sampleRate, transformer);
		}
	}
}
