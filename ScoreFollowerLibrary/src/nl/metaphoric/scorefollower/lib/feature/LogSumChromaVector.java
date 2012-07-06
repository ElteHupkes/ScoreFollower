package nl.metaphoric.scorefollower.lib.feature;

import nl.metaphoric.scorefollower.lib.AudioBuffer;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Similar to linear sum but uses logarithmic amplitudes.
 * 
 * @author Elte Hupkes
 */
public class LogSumChromaVector extends LinearSumChromaVector {
	/**
	 * Does the same as the linear chroma vector
	 * @param parts
	 */
	public LogSumChromaVector(double[] parts) {
		super(parts);
	}
	
	/**
	 * Simply calls parent, which uses correct intensity through
	 * the intensity() method.
	 * @param buffer
	 * @param sampleRate
	 * @param transformer
	 */
	public LogSumChromaVector(AudioBuffer buffer, float sampleRate, DoubleFFT_1D transformer) {
		super(buffer, sampleRate, transformer);
	}
	
	/**
	 * Normalizes vector so all items are in 0 ... 1 range.
	 */
	public void normalize(int[] nvalues, int minN, int maxN) {
		double min = 9999999999.0, max = 0;
		
		for (int i = 0; i < 12; i++) {
			chroma[i] = Math.log10(chroma[i] * (maxN / nvalues[i]));
			if (chroma[i] < min) {
				min = chroma[i];
			}
			if (chroma[i] > max) {
				max = chroma[i];
			}
		}

		/*
		 * New max value will be minus the min value.
		 * If min = max this will make max 0, which is alright since
		 * min = max essentially means noise.
		 */
		max = max - min;
		for (int i = 0; i < 12; i++) {
			chroma[i] = max == 0 ? 0 : (chroma[i] - min) / max;
		}
	}
}
