package nl.metaphoric.scorefollower.experiment;

import nl.metaphoric.scorefollower.lib.AnalyzeListener;
import nl.metaphoric.scorefollower.lib.PlaybackAnalyzer;
import nl.metaphoric.scorefollower.lib.PlaybackAnalyzer.Status;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;

/**
 * Checks a wav file, shows an overview of the sound level
 * as determined by the AmplitudeAnalyzer.
 * @author Elte Hupkes
 */
public class PlaybackTester implements AnalyzeListener {
	/**
	 * Creates a silence tester
	 * @param args
	 */
	public static void main(String[] args) {
		ExperimentLogger e = new ExperimentLogger();
		//e.debug = false;
		//e.silent = true;
		nl.metaphoric.scorefollower.lib.Log.setLogger(e);
		if (args.length < 3) {
			usage();
			return;
		}
		
		new PlaybackTester(args);
	}
	
	/**
	 * Prints usage info
	 */
	public static void usage() {
		System.out.println("Usage: SilenceTester input_file window_size hop_size");
	}
	
	/*
	 * Instance variables 
	 */
	private int position = 0;
	private PlaybackAnalyzer status;
	double windowSize, hopSize;
	
	/**
	 * Creates and starts the silence test
	 */
	private PlaybackTester(String[] args) {
		String input = args[0];
		windowSize = Double.parseDouble(args[1]);
		hopSize = Double.parseDouble(args[2]);
		status = new PlaybackAnalyzer();
		
		try {			
			AudioFileAnalyzer a = new AudioFileAnalyzer(input, windowSize, hopSize, this);
			a.start();
		} catch(Exception e) {
			System.out.println("Something went wrong: "+e.getMessage());
		}
	}

	@Override
	public void onNewAnalysisData(FrameVector v) {
		Status s = status.getStatus(v);
		// Convert amplitude values in the range -2 (Unknown) -> 2 (Loud)
		int value = -1;
		
		switch (s) {
		case WAITING: value = 0; break;
		case ACTIVE: value = 1; break;
		}
		
		System.out.println(String.format("%d \t %.2f \t %d", position, 
				position * hopSize + windowSize, value));
		
		position++;
	}
}
