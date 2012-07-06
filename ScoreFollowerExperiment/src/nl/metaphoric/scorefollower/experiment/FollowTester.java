package nl.metaphoric.scorefollower.experiment;

import java.io.IOException;
import java.io.OutputStream;

import nl.metaphoric.scorefollower.lib.AnalyzeListener;
import nl.metaphoric.scorefollower.lib.RunningAverage;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.file.ScoreReader;
import nl.metaphoric.scorefollower.lib.matcher.PositionMatcher;

/**
 * The FollowTester opens a reference file, and "follows"
 * it using an input wave file. This allows following a lot
 * faster than real-time, which is very useful for
 * experimentation.
 * 
 * The follow tester creates the required reference
 * SFT, feeds it to a ScoreReader, and then performs
 * the testing.
 * 
 * @author Elte Hupkes
 *
 */
public class FollowTester implements AnalyzeListener {
	public static void main(String[] args) {
		if (args.length < 5) {
			usage();
			return;
		}
		
		ExperimentLogger logger = new ExperimentLogger();
		logger.debug = false;
		nl.metaphoric.scorefollower.lib.Log.setLogger(logger);		
		
		String reference = args[1], performance = args[2], 
				annotation = args.length > 5 ? args[5] : null;
		double windowSize = Double.parseDouble(args[3]),
				hopSize = Double.parseDouble(args[4]);
		new FollowTester(args[0], 
				reference, performance, annotation, windowSize, hopSize, System.out);
	}
	
	public static void usage() {
		System.out.println("Usage: ");
		System.out.println("FollowTester testDataDir name window_size hop_size[ annotation]");
	}
	
	/**
	 * The error threshold in seconds under which we consider
	 * a following successful.
	 */
	private static final double FOLLOW_THRESHOLD = 0.5;
	
	/**
	 * The PositionMatcher used for the comparison
	 */
	private PositionMatcher matcher;
	
	/**
	 * Expected value getter
	 */
	private Annotator annotator;
	
	/**
	 * The file reader
	 */
	private ScoreReader reader;
	
	/**
	 * Running average for keeping track of the
	 * error in position.
	 */
	private RunningAverage error, ooError;
	private double maxError = 0, minError = 99999;
	
	/**
	 * Number of frames received
	 */
	private int nData = 0, nCounted = 0, nFollowed = 0;
	
	/**
	 * A stream (could be stdout, could be a file)
	 * to write the follower's analysis data to.
	 */
	private OutputStream out = null;
	
	/**
	 * Performs the follow test
	 */
	public FollowTester(String baseDir, String reference, String performance, String annotation, 
			double windowSize, double hopSize, OutputStream detailWriter) {
		String tmpFile = baseDir + "last_test.sft";
		
		out = detailWriter;
		
		// Create the reference file
		new CreateReference(baseDir+"reference/"+reference, tmpFile, windowSize, hopSize);
		
		String input = baseDir+"performance/"+performance;
		error = new RunningAverage();
		ooError = new RunningAverage();
		try {
			if (annotation != null) {
				annotator = new Annotator(baseDir+"annotations/"+annotation);
			} else {
				annotator = new Annotator(1d);
			}
			
			reader = new ScoreReader(tmpFile, true);
			matcher = reader.getMatcher();
			AudioFileAnalyzer w = new AudioFileAnalyzer(input, matcher.windowSize(), 
					matcher.hopSize(), this);
			
//			if (out != null) {				
//				out.write(String.format("%10s \t\t %10s \t\t %10s \t\t %10s \t\t %10s\n",
//						"Time", "Pos.", "Est.", "Exp.", "Err.").getBytes());
//			}
			w.start();
			
			if (out != null) {				
				out.write(new byte[]{'\n'});
			}
			
			System.out.println("Percentage followed: "+100 * nFollowed / (double)nCounted+"%");
			System.out.println("Average error when following: "+error.getMean()+"s");
			System.out.println("Standard deviation:"+error.getStd());
			System.out.println("Max error: "+maxError);
			System.out.println("Min error: "+minError);
			System.out.println("[Avg 1:1 error: "+ooError.getMean()+"]");
		} catch (Exception e) {
			System.out.println("ERROR: "+e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void onNewAnalysisData(FrameVector v) {
		// Get the estimated position
		int position = matcher.getPosition(v);
		
		/*
		 * Current time: Number of frames * frame hop in time
		 * Estimated time: time from getTimes(position) [*]
		 * Expected time: From annotator, with current time
		 * 
		 * [*] This time is given as the number of seconds passed
		 * at the _start_ of this frame, which corresponds to
		 * increasing nData after this calculation.
		 * 
		 * ooErr calculates the error that would've occurred had
		 * distance vectors been ignored (in that case the calculation
		 * would've run 1:1 in time, oo = one-one).
		 */
		double current	   = nData * matcher.hopSize(),
			   estimate	   = reader.getTimes().get(position),
			   expected	   = annotator.getExpectedPosition(current),
			   err		   = Math.abs(estimate - expected),
			   ooErr	   = Math.abs(current - expected);
		if (position > 0 && expected > -1) {
			if (out != null) {			
				try {
//					out.write(String.format("%10.4f \t\t %10d \t\t %10.4f \t\t %10.4f \t\t %10.4f\n", 
//							current, position, estimate, expected, err).getBytes());
					out.write(String.format("%4f \t\t %d \t\t %4f \t\t %4f \t\t %4f\n", 
							current, position, estimate, expected, err).getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (err > maxError) {
				maxError = err;
			}
			if (err < minError) {
				minError = err;
			}
			
			if (err < FOLLOW_THRESHOLD) {
				nFollowed++;
				error.add(err);
			}
			
			ooError.add(ooErr);
			nCounted++;
		}
		nData++;
	}
}
