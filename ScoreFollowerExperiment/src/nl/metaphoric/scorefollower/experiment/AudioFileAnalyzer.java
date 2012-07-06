package nl.metaphoric.scorefollower.experiment;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import nl.metaphoric.scorefollower.lib.AnalyzeListener;
import nl.metaphoric.scorefollower.lib.AudioAnalyzer;

/**
 * Reads any supported audio file as PCM-16 data, for analysis
 * purposes. 
 * Requires mp3plugin.jar, the MP3 plugin from Sun's Java Media Framework,
 * to read MP3 data.
 * @author Elte Hupkes
 */
public class AudioFileAnalyzer {
	/**
	 * The input stream and the decoder
	 * input stream
	 */
	private AudioInputStream in, din;
	
	/**
	 * The format of the input file, and the decoder format
	 */
	private AudioFormat base, decode;
	
	/**
	 * Audio Analyzer to create FrameVectors
	 */
	private AudioAnalyzer analyzer;
	
	
	public AudioFileAnalyzer(String input, double windowSize, double hopSize, AnalyzeListener listener) 
			throws UnsupportedAudioFileException, IOException {
		File file = new File(input);
		in = AudioSystem.getAudioInputStream(file);
		base = in.getFormat();
		
		decode = new AudioFormat(
			// The encoding we want to use
			AudioFormat.Encoding.PCM_SIGNED,
			// The sample rate
			base.getSampleRate(),
			// The sample size in bits
			16, 
			// The number of channels
			base.getChannels(), 
			// The number of bytes in each frame. 2 bytes for each channel.
			base.getChannels() * 2,
			// The frame rate
			base.getSampleRate(), 
			// Use big endian (true) or little endian(false)
			false
		);
		
		din = AudioSystem.getAudioInputStream(decode, in);
		analyzer = new AudioAnalyzer(listener, base.getSampleRate(), windowSize, hopSize);
	}
	
	/**
	 * Reads MP3 data, analyzes it and passes the FrameVectors
	 * to the listener.
	 * @param listener
	 * @throws IOException 
	 */
	public void start() throws IOException {
		
		// Read data as if it's coming from the microphone, but average
		// over the number of channels.
		int bytesRead = 0, framesToRead = analyzer.getDataSize(),
				frameSize = decode.getFrameSize(), 
				nchannels = decode.getChannels(),
				bufSize = framesToRead * frameSize;
		
		// Need two bytes for each channel
		byte[] dataIn = new byte[bufSize];
		short[] data = new short[framesToRead];
		
		/*
		 * Read data from the buffer until we cannot fill it anymore.
		 * The last few frames will usually not fill the buffer completely
		 * and will thus be discarded, but this is also how it would go
		 * in real life.
		 */
		while (din.read(dataIn) == bufSize) {
			// Convert bytes to integers and create frame
			// averages.
			int index = 0;
			for (int i = 0; i < bufSize; i += frameSize) {
				int avg = 0;
				
				for (int offset = 0; offset < nchannels; offset += 2) {
					avg += toShort(dataIn, i + offset);
				}
				data[index] = (short)Math.round(avg / (double)nchannels);
				index++;
			}
			
			// The frame is filled; pass on to the analyzer
			analyzer.onNewData(data);
		}
		
		do {				
			bytesRead = din.read(dataIn);
			
			if (bytesRead < bufSize) {
				// Last frame not long enough, discard it.
				break;
			}
		} while (bytesRead > 0);
		
		din.close();
		in.close();
	}
	
	/**
	 * Shows info about the opened file. 
	 */
	public void showInfo() {
		System.out.println("In: "+base.toString());
		System.out.println("Out: "+decode.toString());
	}
	
	/**
	 * Converts two bytes from a byte array to signed short.
	 * The bytes are expected to be in little endian order.
	 * 
	 * This actually returns unsigned short values, but since
	 * there's no way of representing those in Java, it uses
	 * integers.
	 * @param b
	 * @return
	 */
	public static short toShort(byte[] b, int pos) {
		// Take the most significant byte, shift 8
		// positions to the left, and add the least significant byte.
		return (short)((((b[pos + 1] & 0xFF) << 8) + (b[pos] & 0xFF)));
	}
	
	/**
	 * @return
	 */
	public AudioAnalyzer getAnalyzer() {
		return analyzer;
	}
}
