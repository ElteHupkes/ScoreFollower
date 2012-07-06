package nl.metaphoric.scorefollower.lib.feature;

import nl.metaphoric.scorefollower.lib.AudioBuffer;
import nl.metaphoric.scorefollower.lib.Parameters;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * FrameVector similar to the LinearChromaVector,
 * but sums intensities instead of averaging them.
 * 
 * Normalizes vector values to [0, 1]. Match probability
 * is calculated using the distance and a spreading value.
 * 
 * @author Elte Hupkes
 */
public class LinearSumChromaVector extends FrameVector {

	/**
	 * Creates an empty vector, doing nothing.
	 * This is a workaround to satisfy the Java compiler when extending
	 * this class.
	 */
	public LinearSumChromaVector() {}
	
	/**
	 * Creates a frame vector from a chroma array and an
	 * amplitude value.
	 * @param chroma The chroma double array. The FrameVector
	 *  will use the existing reference, so you cannot reuse
	 *  the supplied data.
	 */
	public LinearSumChromaVector(double[] parts) {
		// Calculate mean and standard deviation
		chroma = parts;
		mean = 0;
		for (int i = 0; i < 12; i++) {
			chroma[i] = parts[i];
			mean += parts[i];
		}
		mean /= 12;
		
		std = 0;
		for (int i = 0; i < 12; i++) {
			std += Math.pow(chroma[i] - mean, 2);
		}
		std = Math.sqrt(std / 12.0);
		
		if (parts.length > 12) {			
			this.rms = parts[12];
		} else {
			this.rms = 0;
		}
	}
	
	/**
	 * Creates a new frame vector using the given audio intensities.
	 * @param buffer Input samples
	 * @param sampleRate The sample rate of the incoming samples
	 * @param transformer FFT transformer, for efficiency only one is used and passed here.
	 */
	public LinearSumChromaVector(AudioBuffer buffer, float sampleRate, DoubleFFT_1D transformer) {
		chroma = new double[12];
		double frequency, intensity;
		int bin, i;
		mean = rms = 0.0;
		
		// Create a new buffer to hold double data
		int frameSize = buffer.size();
		double[] data = new double[frameSize];
		double v;
		for (i = 0; i < frameSize; i++) {
			// Create double value from short value
			v = buffer.get(i) / 32768.0;

			data[i] = v * Parameters.window.window(i, frameSize);
			
			// Add to real mean square
			rms += v * v;
		}
		
		// Calculate the root of the mean of the squares to get the RMS
		rms = Math.sqrt(rms / frameSize);
		
		// Now transform the data using FFT
		transformer.realForward(data);
		
		/**
		 * Generate the chroma vector. We do this according to the "to catch a chorus"
		 * paper; take the logarithmic magnitude of each frequency, classify them
		 * into bins, and take the average of each bin. After this, the vector is
		 * normalized to zero mean.
		 * 
		 * I used to normalize to unit variance as well, but the results appear to be
		 * much better using just zero mean.
		 */
		int[] nvalues = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		int imag;
		for (i = 2; i < data.length; i += 2) {
			// Calculate frequency in Hz using cycles / total cycles
			// Note: this could be made faster by doing the division once and
			// using a summation. But, let's first make it work & beautiful :).
			frequency = sampleRate * i * 0.5 / (double)data.length;
			
			if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) {
				// Ignore this frequency
				// TODO: this could actually be worked into the loop to make it faster
				continue;
			}
			
			bin = frequencyBin(frequency);
			
			// Use the absolute value of the magnitude, ignoring phase.
			imag = ((i+1) < data.length) ? (i+1) : 1;
			intensity = intensity(data[i], data[imag]);
			
			chroma[bin] += intensity;
			nvalues[bin]++;
		}
		
		/**
		 * Add the final frequency in data[1] for even data lengths, for the n/2
		 * frequency. The FFT returns only an intensity for this value (as opposed
		 * to the 0 frequency, which has no phase).
		 * 
		 * Usually the frequency calculation is sampleRate * k / n, in the loop i = 2k.
		 * If n = even, k = n/2, f = sampleRate * (n/2) / n = sampleRate * 0.5.
		 */
		if (data.length % 2 == 0) {			
			frequency = sampleRate * 0.5;
			if (frequency <= MAX_FREQUENCY && frequency >= MIN_FREQUENCY) {				
				bin = frequencyBin(frequency);
				intensity = intensity(data[1], 0);
				chroma[bin] += intensity;
				nvalues[bin]++;
			}
		}
		
		// Find the minimum and maximum bin content value to
		// normalize to.
		int minN = 999999999, maxN = 0;
		for (i = 0; i < 12; i++) {
			if (nvalues[i] > maxN) {
				maxN = nvalues[i];
			}
			if (nvalues[i] < minN) {
				minN = nvalues[i];
			}
		}
		normalize(nvalues, minN, maxN);
	}	
	
	/**
	 * Normalizes vector so all items are in 0 ... 1 range.
	 */
	public void normalize(int[] nvalues, int minN, int maxN) {
		
		double min = 9999999999.0, max = 0;
		for (int i = 0; i < 12; i++) {
			chroma[i] *= (double)maxN / (double)nvalues[i];
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
	
	/**
	 * 
	 * @param real
	 * @param imag
	 * @return
	 */
	public double intensity(double real, double imag) {
		//return Math.abs(imag);
		return Math.sqrt(real * real + imag * imag);
	}
	
	/**
	 * Calculates the Euclidean difference between this vector
	 * and the given vector. This difference is based
	 * solely on the chroma vector, not the amplitude.
	 * @param v
	 * @return The difference as a sum of absolute differences
	 */
	public double distance(FrameVector v) {
		double[] diffChroma = v.getChroma();
		double diff = 0.0;
		for (int i = 0; i < 12; i++) {
			diff += Math.pow(diffChroma[i] - chroma[i], 2);
		}
		return Math.sqrt(diff);
	}
	
	/**
	 * Calculates the matching probability of two FrameVectors.
	 * This is proportional to the Euclidean distance between
	 * their chroma vectors.
	 * The maximum distance occurs when one vector is at (0, 0, ...)
	 * and the other at (1, 1, ....), which has a distance
	 * of sqrt(12)
	 * @param a
	 * @param b
	 * @return The match probability of the two vectors.
	 */
	@Override
	public double matchProbability(FrameVector b) {
		//return 1.0;
		return 1 - (distance(b) / 20);
	}

}
