package nl.metaphoric.scorefollower.lib.feature;

import nl.metaphoric.scorefollower.lib.AudioBuffer;
import nl.metaphoric.scorefollower.lib.Parameters;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * The Linear Chroma Vector implements a chroma vector as used in
 * "Polyphonic Audio Matching and Alignment for Music Retrieval".
 * 
 * It is a 12-item vector with zero mean and unit variance.
 * 
 * The match probability is given on a scale from 0 to 1 using 
 * the maximum distance between two vectors (which I believe is 24
 * for normalized-unit-variance vectors).
 * 
 * @author Elte Hupkes
 *
 */
public class LinearChromaVector extends FrameVector {
	/**
	 * Creates a frame vector from a chroma array and an
	 * amplitude value.
	 * @param chroma The chroma double array. The FrameVector
	 *  will use the existing reference, so you cannot reuse
	 *  the supplied data.
	 */
	public LinearChromaVector(double[] parts) {
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
	public LinearChromaVector(AudioBuffer buffer, float sampleRate, DoubleFFT_1D transformer) {
		chroma = new double[12];
		double frequency, intensity;
		int bin, i;
		rms = 0.0;
		
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
		
		// Set each bin's value to its mean
		for (i = 0; i < 12; i++) {
			chroma[i] /= nvalues[i];
			mean += chroma[i];
		}
		mean /= 12.0;
		
		normalize();
	}
	
	/**
	 * Creates an empty vector, doing nothing.
	 * This is a workaround to satisfy the Java compiler when extending
	 * this class.
	 */
	public LinearChromaVector() {}
	
	/**
	 * Normalizes this vector. Assumes mean is calculated,
	 * calculates STD.
	 */
	protected void normalize() {
		// Subtract mean for zero mean
		int i;
		for (i = 0; i < 12; i++) {
			std += Math.pow(mean - chroma[i], 2);
			chroma[i] = chroma[i] - mean;
		}
		std = Math.sqrt(std / 12.0);
		
		// Divide by standard deviation for unit variance
		for (i = 0; i < 12; i++) {
			chroma[i] /= std;
		}
	}
	
	/**
	 * Returns the intensity value, is overwritten by
	 * LogarithmicChromaVector to keep code duplication to a minimum.
	 * @param value
	 * @return
	 */
	protected double intensity(double real, double imaginary) {
		//return Math.abs(real);
		return Math.sqrt(real * real + imaginary * imaginary);
	}
	
	/**
	 * Calculates the difference between this vector
	 * and the given vector. This difference is based
	 * solely on the chroma vector, not the amplitude.
	 * @param v
	 * @return The difference as a sum of absolute differences
	 */
	public double difference(FrameVector v) {
		double[] diffChroma = v.getChroma();
		double diff = 0.0;
		for (int i = 0; i < 12; i++) {
			diff += Math.abs(diffChroma[i] - chroma[i]);
		}
		
		return diff;
	}
	
	/**
	 * Calculates the matching probability of two FrameVectors.
	 * This is proportional to the sum of squared distances
	 * of their chroma vector.
	 * @param a
	 * @param b
	 * @return The match probability of the two vectors.
	 */
	public double matchProbability(FrameVector b) {		
		return 1 - (difference(b) / 24.0);
	}
}
