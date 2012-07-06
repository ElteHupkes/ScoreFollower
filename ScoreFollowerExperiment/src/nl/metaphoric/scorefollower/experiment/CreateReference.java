package nl.metaphoric.scorefollower.experiment;

import nl.metaphoric.scorefollower.lib.AnalyzeListener;
import nl.metaphoric.scorefollower.lib.Log;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.file.PositionRecorder;

/**
 * Creates a ScoreReader readable file from a wav file.
 * 
 * @author Elte Hupkes
 */
public class CreateReference implements AnalyzeListener {
	public static final String TAG = "SF_CreateReference";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ExperimentLogger logger = new ExperimentLogger();
		logger.debug = false;
		nl.metaphoric.scorefollower.lib.Log.setLogger(logger);
		if (args.length < 4) {
			usage();
			return;
		}
		
		String input = args[0], output = args[1];
		double windowSize = Double.parseDouble(args[2]),
				hopSize = Double.parseDouble(args[3]);
		
		new CreateReference(input, output, windowSize, hopSize);
	}

	/**
	 * Prints how to use this script.
	 */
	public static void usage() {
		System.out.println("Usage: ");
		System.out.println("java CreateReference input output_sft window_size hop_size");
	}	
	
	/**
	 * A PositionRecorder to write to file
	 */
	private PositionRecorder recorder;	
	
	/**
	 * Creates a new reference file using the given parameters.
	 * @param input
	 * @param output
	 * @param windowSize
	 * @param hopSize
	 */
	public CreateReference(String input, String output, double windowSize, double hopSize) {
		Log.d(TAG, "Creating reference file from file "+input);
		recorder = new PositionRecorder();
		
		try {
			// Create the wave analyzer, and start it.
			AudioFileAnalyzer w = new AudioFileAnalyzer(input, windowSize, hopSize, this);
			recorder.recordTimes();
			//recorder.getPlaybackAnalyzer().forceStart();
			w.start();
			
			Log.d(TAG, "Writing to output file "+output+"...");
			recorder.write(output, new String[] {}, w.getAnalyzer());
			Log.d(TAG, "Write successful.");
		} catch (Exception e) {
			Log.e(TAG, "Something went wrong: "+e.getMessage());
		}
	}
	
	@Override
	public void onNewAnalysisData(FrameVector v) {
		recorder.addData(v);
	}
}
