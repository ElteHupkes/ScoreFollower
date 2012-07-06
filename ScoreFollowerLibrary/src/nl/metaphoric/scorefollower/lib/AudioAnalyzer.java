package nl.metaphoric.scorefollower.lib;

import nl.metaphoric.scorefollower.lib.feature.FrameVectorFactory;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


/**
 * The AudioAnalyzer reads Audio data (in a different thread),
 * and creates FrameVector instances from this information.
 * 
 * The analyzer uses both a window- and a hop size. The window
 * size determines the amount of data that is included in every
 * window that is analyzed. The hop size determines the overlap
 * in this data, and is defined as the amount of frames that
 * is hopped every time to create a new window. So for example,
 * if you have a window size of 20ms and a hop size of 10ms,
 * the used data windows are 0ms-20ms, 10ms-30ms, 20ms-40ms, etc.
 * 
 * The AudioAnalyzer requests data from the microphone in frames
 * the size of the hop size, in order to always be able to use
 * the most recent data. This means however that, at the start,
 * data might be discarded if the hopSize is inconveniently chosen.
 * 
 * The following examples illustrate how the analysis buffer works
 * (x denotes invalid / unknown data):
 * 
 * Example one, window = 4, hop = 1:
 * [a b c d], new data [e]
 * [b c d e], new data [f]
 * [c d e f], enz
 * Start solution:
 * [x x x x], new data [a]
 * [x x x a] (discard), + [b]
 * [x x a b] (discard), + [c]
 * ...
 * [a b c d] -> valid, process (no data discarded)
 * 
 * 
 * Example two, window = 4, hop = 3:
 * [a b c d], new data [e f g]
 * [d e f g], new data [h i j]
 * [g h i j]
 * Start solution:
 * [x x x x], new data [a b c]
 * [x a b c] (discard) + [d e f]
 * [c d e f] -> valid, process ([a b] discarded)
 * 
 * @author Elte Hupkes
 */
public class AudioAnalyzer {
	/**
	 * The log tag
	 */
	private static final String TAG = "SF_AudioAnalyzer";

	/**
	 * Fast Fourier transformer
	 */
	private DoubleFFT_1D transformer;
	
	/**
	 * The window and hop size in seconds.
	 * Restraints: 0 < hopSize <= windowSize
	 */
	private double windowSize = 0.25, hopSize = windowSize;
	
	/**
	 * The sample rate
	 */
	private float sampleRate;
	
	/**
	 * The frame size, buffer size, buffer length
	 * and buffer position.
	 */
	private int frameSize, hopFrameSize;
	
	/**
	 * Class instance that listens to 
	 */
	private AnalyzeListener listener = null;
	
	/**
	 * When was the last data submitted?
	 */
	private long lastData;
	
	/**
	 * The audio buffer
	 */
	private AudioBuffer buf;
	
	/**
	 * 
	 * @param listener Callback class for new data
	 * @param sampleRate 
	 */
	public AudioAnalyzer(AnalyzeListener listener, float sampleRate, double windowSize, double hopSize) {
		this.listener = listener;
		this.sampleRate = sampleRate;
		this.windowSize = windowSize;
		this.hopSize = hopSize;
		lastData = 0;
		
		if (hopSize > windowSize || hopSize <= 0) {
			throw new IllegalArgumentException("0 < hopSize < windowSize prerequisite not met.");
		}
		
		/*
		 * I used to define the frame size as the nearest larger power of two, as such:
		 * 
		 * (int)Math.pow(2, Math.ceil(Math.log(windowSize * sampleRate) / Math.log(2)));
		 * 
		 * However, this resulted in a far too large buffer (around 0.37 seconds) for a 44100Hz
		 * sample rate. Since our FFT actually works just fine with non 2^ buffers, let's just
		 * pick the nearest number.
		 */
		frameSize = (int)Math.round(windowSize * sampleRate);
		hopFrameSize = (int)Math.round(hopSize * sampleRate);
		buf = new AudioBuffer(frameSize);
		
		transformer = new DoubleFFT_1D(frameSize);
		Log.d(TAG, "Analyzer frame size: "+frameSize);
		Log.d(TAG, "Analyzer input size: "+hopFrameSize);
	}
	
	/**
	 * Resets the audio analyzer
	 */
	public void reset() {
		buf.clear();
	}
	
	/**
	 * Called whenever new audio data is available. The supplied buffer should
	 * have the same length as getBufferSize().
	 * 
	 * Note that this method blocks progress of the audio reader, so if this
	 * gets too computationally intensive it shows a warning.
	 * 
	 * @param buffer
	 */
	public void onNewData(short[] buffer) {		
		//Log.d(TAG, "Last analyzer update: "+(System.currentTimeMillis() - lastData)+"ms ago.");
		lastData = System.nanoTime();
		
		buf.put(buffer);
		if (buf.full()) {
			listener.onNewAnalysisData(FrameVectorFactory.getVector(buf, sampleRate, transformer));
		}
		
		if ((lastData - System.nanoTime()) > (windowSize * 800000000)) {
			Log.w(TAG, "WARNING: Analysis taking up more than 80% of window size");
		}
	}
	
	/**
	 * Returns the size of the data buffers that
	 * this analyzer wishes to receive.
	 * @return The buffer size
	 */
	public int getDataSize() {
		return hopFrameSize;
	}
	
	/**
	 * Returns the active hop size (in seconds).
	 * Due to the integer rounding of the frame sizes this
	 * might not be entirely the same as the hopsize
	 * that was passed in.
	 * @return
	 */
	public double hopSize() {
		return hopFrameSize / (double)sampleRate;
	}
	
	/**
	 * Returns the active window size (in seconds)
	 * Due to the integer rounding of the frame sizes this
	 * might not be entirely the same as the hopsize
	 * that was passed in.
	 * @return
	 */
	public double windowSize() {
		return frameSize / (double)sampleRate;
	}
	
	/**
	 * Returns the sample rate (samples / second) this
	 * analyzer was configured with.
	 * @return
	 */
	public float getSampleRate() {
		return sampleRate;
	}
}
