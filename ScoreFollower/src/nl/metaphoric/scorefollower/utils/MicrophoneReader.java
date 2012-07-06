/**
 * The MicrophoneReader class reads audio from
 * the device microphone, and passes it to the
 * analyzer.
 * 
 * The MicrophoneReader is passed an AudioAnalyzer instance,
 * which converts the data to a format usable for analysis.
 * This AudioAnalyzer in turn sends its data back to
 * the controlling activity.
 * 
 *  @author Elte Hupkes
 */
package nl.metaphoric.scorefollower.utils;

import nl.metaphoric.scorefollower.lib.AudioAnalyzer;
import nl.metaphoric.scorefollower.lib.Log;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class MicrophoneReader implements AudioInput {
	/**
	 * Analyzer object to pass vectors to
	 */
	private AudioAnalyzer analyzer;
	
	/**
	 * The input buffer. We'll use multiple input buffers
	 * so that one can be used for analysis while others
	 * are filled.
	 */
	private short[][] buffers;
	private int activeBuffer = 0;
	private int bufferIndex = 0;
	
	/**
	 * The number of input buffers
	 */
	public static final int NBUFFERS = 3;	
	
	/**
	 * The size of the block to read
	 */
	private int blockSize;
	
	/**
	 * The recorder instance
	 */
	private AudioRecord recorder = null;
	
	/**
	 * The recording state of the reader
	 */
	private boolean recording = false;
	
	/**
	 * The active thread
	 */
	private Thread runner = null;
	
	/**
	 * Log tag
	 */
	public static final String TAG = "SF_MicReader";
	
	/**
	 * The time (System.currentTimeMillis) when the last data
	 * was read.
	 */
	public long lastUpdate = 0, lastDataReady = 0;
	
	/**
	 * Creates a new MicrophoneReader with the given analyzer
	 * @param analyzer
	 */
	public MicrophoneReader(AudioAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	/**
	 * Starts the microphone reader.
	 * 
	 * @param sampleRate The sample rate
	 * @param blockSize The size of the block to read each time
	 */
	@Override
	public void startRecorder() {
		int blockSize = analyzer.getDataSize();
		float sampleRate = analyzer.getSampleRate();
		
		synchronized (this) {
			if (recording) {
				// Do not start already running recorder
				return;
			}
			// Create several buffers of the requested block size
			buffers = new short[NBUFFERS][blockSize];
			
			this.blockSize = blockSize;
			
			// Use twice the minimum buffer size as the buffer size
			int bufferSize = AudioRecord.getMinBufferSize((int)sampleRate, 
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT) * 2;
			Log.d(TAG, "Recorder buffer size: "+bufferSize);
			
			// Set up the audio input.
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, (int)sampleRate,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSize);
			
			activeBuffer = 0;
			bufferIndex = 0;
			
			recording = true;
			
			// Start the thread
			runner = new Thread(new Runnable() {
				public void run() { runRecorder(); }
			}, "Microphone Reader");
			runner.start();
		}
	}
	
	/**
	 * Returns whether or not the microphone is
	 * currently capturing audio.
	 * @return
	 */
	@Override
	public boolean isActive() {
		synchronized(this) {
			return recording;
		}
	}
	
	/**
	 * This is the method central to the reader thread.
	 */
	private void runRecorder() {
		// Reference to the active buffer
        short[] buffer;
        
        // Number of bytes read, and number of bytes to read
        int nread, toRead;
        
		// Wait for the recorder to initialize
        int timeout = 200;
        try {
			while (timeout > 0 && recorder.getState() != AudioRecord.STATE_INITIALIZED) {
				Thread.sleep(50);
				timeout -= 50;
			}
        } catch (InterruptedException e) { }
        
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
        	// Cannot start reading
        	// TODO error handling
        	stopError();
        	return;
        }
        
        try {
        	recorder.startRecording();
        	lastUpdate = System.nanoTime();
        	while (recording) {
        		// Read shorts into the buffer until its full, then pass it to the analyzer
        		buffer = buffers[activeBuffer];
        		toRead = blockSize - bufferIndex;
        		
        		// Make sure only one process is working in this buffer
        		synchronized (buffer) {
					//Log.d(TAG, "Last microphone update: "+(System.currentTimeMillis() - lastUpdate)+"ms ago.");
					nread = recorder.read(buffer, bufferIndex, toRead);
					//Log.d(TAG, "Read "+nread+" shorts (from requested "+toRead+") from microphone.");
					lastUpdate = System.nanoTime();
					
					if (nread < 0) {
						stopError();
						return;
					}
					
					bufferIndex += nread;
					if (bufferIndex >= blockSize) {
						// Process the buffer and switch to a new buffer
						long t = System.nanoTime();
						Log.d(TAG, "Last data ready: "+(t - lastDataReady)+"ms ago.");
						lastDataReady = t;
						bufferIndex = 0;
						activeBuffer = (activeBuffer + 1) % NBUFFERS;
						process(buffer);
					}
				}
        	}
        } finally {
        	if (recorder.getState() == AudioRecord.RECORDSTATE_RECORDING) {
        		recorder.stop();
        		recorder.release();
        	}
        }
	}
	
	/**
	 * 
	 * @param buffer The buffer to process
	 */
	private void process(short[] buffer) {
		analyzer.onNewData(buffer);
	}
	
	/**
	 * Stops the recording process with an error
	 */
	private void stopError() {
		Log.e(TAG, "ERROR: Could not start recorder.");
		recording = false;
		runner = null;
	}
	
	/**
	 * Stops the recorder, if active.
	 */
	@Override
	public void stopRecorder() {
		synchronized (this) {
			// Set recording to false to stop runRecorder
			recording = false;
		}
		
		if (runner != null) {
			try {
				runner.join();
			} catch(InterruptedException e) {}
		}
		
		if (recorder != null) {
			recorder.release();
			recorder = null;
		}
		
		runner = null;
	}
	
	/**
	 * The easiest way to pause this is just to stop
	 */
	@Override
	public void pauseRecorder() {
		stopRecorder();
	}
}
