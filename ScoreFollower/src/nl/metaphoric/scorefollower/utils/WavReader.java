package nl.metaphoric.scorefollower.utils;

import java.io.File;
import java.io.IOException;

import nl.metaphoric.scorefollower.lib.AudioAnalyzer;
import nl.metaphoric.scorefollower.lib.Log;
import nl.metaphoric.scorefollower.utils.wav.WavFile;
import nl.metaphoric.scorefollower.utils.wav.WavFileException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;

/**
 * The WavReader is an audio reader for demo mode, it reads audio data
 * from a Wave file as well as playing it through the device's microphone.
 * 
 * AudioTrack has event callbacks for when a certain amount of data has
 * been played. These callbacks are used to send data to the AudioAnalyzer
 * at the same moment this would normally happen.
 * 
 * @author Elte Hupkes
 */
public class WavReader implements AudioTrack.OnPlaybackPositionUpdateListener, AudioInput {
	/**
	 * Log tag
	 */
	private static final String TAG = "SF_WavReader";
	
	/**
	 * Input WavFile instance.
	 */
	private WavFile file;
	
	/**
	 * The AudioAnalyzer used to pass
	 * the data to.
	 */
	private AudioAnalyzer analyzer;
	
	/**
	 * The active playback state
	 */
	private boolean active = false, paused = false;
	
	/**
	 * The recorder runner thread
	 */
	private Thread player;
	private HandlerThread reader;
	private Handler readerHandler;
	
	/**
	 * Buffers for reading / writing
	 */
	private short[][] buffers;
	private static final int NBUFFERS = 3;
	
	// Indices of the active reading / writing buffers
	private int readBuf = 0, writeBuf = 0;
	
	/**
	 * Loaded file name
	 */
	private String filename;
	
	/**
	 * Creates a new WavReader using the given filename
	 * as the input file.
	 * @param filename
	 * @throws WavFileException 
	 * @throws IOException 
	 */
	public WavReader(String filename, AudioAnalyzer analyzer) throws IOException, WavFileException {
		this.analyzer = analyzer;
		this.filename = filename;
	}
	
	/**
	 * Starts the recorder
	 */
	@Override
	public void startRecorder() {
		synchronized(this) {			
			// Do not start an active recorder
			if (active) return;
			
			if (file == null) {
				// Only load file if there isn't a current one present
				try {
					file = WavFile.openWavFile(new File(filename));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// Start reader before player to make sure its available.
			reader = new HandlerThread("Analyze worker.");
			reader.start();
			readerHandler = new Handler(reader.getLooper());			
			
			player = new Thread(new Runnable() {
				@Override
				public void run() {
					runRecorder();
				}
			});
			player.start();
		}
	}
	
	/**
	 * Stops and joins the player and reader threads.
	 */
	private void joinThreads() {
		if (player != null) {
			try {
				player.join();
			} catch(InterruptedException e) {}
		}
		
		if (reader != null) {
			reader.quit();
			try {
				reader.join();
			} catch (InterruptedException e) {}
		}
		
		player = reader = null;
	}
	
	/**
	 * Stops the recorder
	 */
	@Override
	public void stopRecorder() {
		synchronized(this) {
			active = false;
		}
		
		joinThreads();
	}
	
	/**
	 * Pauses the recorder.
	 */
	@Override
	public void pauseRecorder() {
		synchronized (this) {
			active = false;
			paused = true;
		}
		
		joinThreads();
	}
	
	/**
	 * Runner method for the thread
	 */
	private void runRecorder() {
		final int sampleRate = (int)file.getSampleRate();
		int minDataSize = 2 * analyzer.getDataSize();
		int minSize = AudioTrack.getMinBufferSize(sampleRate, 
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		
		final int bufferSize = minSize > minDataSize ? minSize : minDataSize;
		
		/*
		 * The AudioTrack instance MUST be created in this
		 * method (= thread), otherwise the callbacks will occur in
		 * the UI thread and block it. 
		 */
		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 
				bufferSize, AudioTrack.MODE_STREAM);			
		
		active = true;
		paused = false;
		
		// Wait for the recorder to initialize
        int timeout = 200;
        try {
			while (timeout > 0 && track.getState() != AudioTrack.STATE_INITIALIZED) {
				Thread.sleep(50);
				timeout -= 50;
			}
        } catch (InterruptedException e) { }		
		
        if (track.getState() != AudioTrack.STATE_INITIALIZED) {
        	// Cannot start reading
        	stopError();
        	return;
        }
        
		int channels = file.getNumChannels();
		
		int dataSize = analyzer.getDataSize();
		double[] inputBuffer = new double[channels * dataSize];
		
		// Buffer to send to AudioTrack
		buffers = new short[NBUFFERS][dataSize];
		int read, position;
		
		try {
			track.play();
			
			// Add a notification for the playback of each dataSize, to send data
			// to the AudioAnalyzer.
			// These posts are handled on a separate worker thread.
			track.setPositionNotificationPeriod(dataSize);
			track.setPlaybackPositionUpdateListener(this, readerHandler);
			
			while(active) {
				/*
				 * Read samples
				 */
				read = file.readFrames(inputBuffer, dataSize);
				short[] outputBuffer = buffers[writeBuf];
				
				synchronized(outputBuffer) {					
					int index = 0, max = channels * read;
					// Convert input buffer to output buffer
					for (int i = 0; i < max; i += channels) {
						double sum = 0;
						for (int j = 0; j < channels; j++) {
							sum += inputBuffer[i+j];
						}
						outputBuffer[index] = (short)Math.round((Short.MAX_VALUE * sum / channels));
						index++;
					}
					
					position = 0;
					do {
						position += track.write(outputBuffer, position, read - position);
					} while (position < read);
				}
				
				writeBuf = (writeBuf + 1) % NBUFFERS;
				
				if (read < dataSize) {
					// Out of frames, break it off
					active = false;
				}
			}
		} catch(Exception e) {
			Log.e(TAG, "An error occured:" + e);
			e.printStackTrace();
		} finally {
			track.stop();
			track.release();
			
			if (!paused) {				
				try {
					file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				file = null;
			}
		}
		
	}
	
	/**
	 * Stops while throwing an error.
	 */
	private void stopError() {
		active = false;
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.e(TAG, "ERROR: Could not start recorder.");
	}

	@Override
	public void onMarkerReached(AudioTrack track) {}

	@Override
	public void onPeriodicNotification(AudioTrack track) {
		short[] readBuffer = buffers[readBuf];
		synchronized(readBuffer) {
			analyzer.onNewData(readBuffer);
		}
		readBuf = (readBuf + 1) % NBUFFERS;
	}
	
	/**
	 * Checks whether the wav reader is active.
	 * @return
	 */
	@Override
	public boolean isActive() {
		synchronized(this) {
			return active;
		}
	}
}
