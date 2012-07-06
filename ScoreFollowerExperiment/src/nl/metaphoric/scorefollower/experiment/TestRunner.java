package nl.metaphoric.scorefollower.experiment;

import java.io.FileOutputStream;
import java.io.IOException;

import nl.metaphoric.scorefollower.lib.Parameters;
import nl.metaphoric.scorefollower.lib.window.WindowFunction;

public class TestRunner {
	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			return;
		}
		
		
		String outDir = args[1];
		ExperimentLogger logger = new ExperimentLogger();
		logger.debug = false;
		nl.metaphoric.scorefollower.lib.Log.setLogger(logger);		
		
		/*
		 * Tests are read from the test directory (TD). Each entry of this array
		 * can have four items (three of which are mandatory):
		 * - The reference file name (TD/reference/...mp3)
		 * - The input file name (TD/performance/...mp3)
		 * - The annotation file name (TD/annotations/...)
		 * - The output data file name
		 */
		String[][] tests = {
			{
				"cMajorScale.mp3",
				"cMajorScale.mp3",
				"cMajorScale",
				"cMajorScale.data"
			},
			{
				"cMajorArpeggio.mp3",
				"cMajorArpeggio.mp3",
				"cMajorArpeggio",
				"cMajorArpeggio.data"
			},
			{
				"maryHadALittleLamb.mp3",
				"maryHadALittleLambPiano.mp3",
				"maryHadALittleLambPiano",
				"maryHadALittleLambPiano.data"
			},
			{
				"maryHadALittleLamb.mp3",
				"maryHadALittleLambGuitar.mp3",
				"maryHadALittleLambGuitar",
				"maryHadALittleLambGuitar.data"
			},
			{
				"amsterdam.mp3",
				"amsterdam.mp3",
				"amsterdam",
				"amsterdam.data"
			},
			{
				"somebodyThatIUsedToKnow.mp3",
				"somebodyThatIUsedToKnow.mp3",
				"somebodyThatIUsedToKnow",
				"somebodyThatIUsedToKnow.data"
			},
			{
				"allDayLong.mp3",
				"allDayLong.mp3",
				"allDayLong",
				"allDayLong.data"
			},
			{
				"everything.mp3",
				"everything.mp3",
				"everything",
				"everything.data"
			},
			// Match to random songs, to get.. deliberate failure.
//			{
//				"everything.mp3",
//				"allDayLong.mp3",
//				"somebodyThatIUsedToKnow",
//				"phony.data"
//			}
			{
				"somebodyThatIUsedToKnow.mp3",
				"stiutkFast.mp3",
				"stiutkFast",
				"stiutkFast.data"
			}
		};
		
		/*
		 * These arrays can be used to brute force different
		 * kinds of parameters. Currently just uses the configured
		 * values.
		 */
		int[] vectors = {
			//FrameVectorFactory.TYPE_PHONY,
			//FrameVectorFactory.TYPE_LINEAR_CHROMA,
			//FrameVectorFactory.TYPE_LOG_CHROMA,
			//FrameVectorFactory.TYPE_LINEAR_SUM_CHROMA,
			//FrameVectorFactory.TYPE_LOG_SUM_CHROMA
			Parameters.frameVectorType
		};
		
		// All possible window combinations
		double[][] windowSizes = {
//			{0.30, 0.30},
//			{0.25, 0.25},
//			{0.20, 0.20},
//			{0.30, 0.20},
//			{0.30, 0.15},
//			{0.25, 0.20},
//			{0.25, 0.15}
			{Parameters.windowSize, Parameters.hopSize}
		};
		
		double[] dBTresholds = {
//			5, 
//			8, 
//			10
			Parameters.dBTreshold
		};
		double[] startStdDevs = {
//			0.5, 
//			1.0, 
//			2.0
			Parameters.startStdDev
		};
		double[] minStdDevs = {
//			1.0, 
//			2.0
			Parameters.minStdDev
		};
		float[] searchWindows = {
//			3.0f, 
//			4.0f, 
//			6.0f
			Parameters.searchWindow
		};
		
		WindowFunction[] windowFunctions = {
//			new RectangularWindow(),
//			new HammingWindow(),
//			new HannWindow(),
//			new BlackmanHarrisWindow()
			Parameters.window
		};
		
		// Bruteforce through all the parameters and run the tests
		// This is some of the ugliest code I've written, ever :). 
		// Does the job quite easily though.
		for (int vector : vectors) {			
			for (double[] windowSize : windowSizes) {
				for (double dBTreshold : dBTresholds) {
					for (double startStdDev : startStdDevs) {
						for (double minStdDev : minStdDevs) {
							for (float searchWindow : searchWindows) {
								for (WindowFunction windowFunction : windowFunctions) {
									System.out.println("FrameVector type: "+vector);
									System.out.println("Parameters: ");
									System.out.println("windowFunction: "+windowFunction);
									System.out.println("dBTreshold: "+dBTreshold);
									System.out.println("startStdDev: "+startStdDev);
									System.out.println("minStdDev: "+minStdDev);
									System.out.println("searchWindow: "+searchWindow);
									System.out.println();
									
									for (String[] test : tests) {
										String outFile = test.length > 3 ? outDir + test[3] : null;
										runTest(args[0], vector, test, windowSize, dBTreshold, 
												startStdDev, minStdDev, 
												searchWindow, windowFunction, outFile);
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Prints a usage message
	 */
	public static void usage() {
		System.out.println("Usage:");
		System.out.println("TestRunner test_directory output_directory");
	}
	
	/**
	 * Runs a test and outputs the results
	 * @param test
	 * @param windowSize
	 * @param dBTreshold
	 * @param startStdDev
	 * @param minStdDev
	 * @param searchWindow
	 * @param windowFunction
	 */
	private static void runTest(String baseDir, int vector, String[] test, double[] windowSize, 
			double dBTreshold, double startStdDev, double minStdDev, float searchWindow,
			WindowFunction windowFunction, String outFile) {
		
		System.out.println(test[0]+":"+test[1]);

		Parameters.frameVectorType = vector;
		Parameters.window = windowFunction;
		Parameters.dBTreshold = dBTreshold;
		Parameters.startStdDev = startStdDev;
		Parameters.minStdDev = minStdDev;
		Parameters.searchWindow = searchWindow;
		
		FileOutputStream out = null;
		if (outFile != null) {
			try {
				out = new FileOutputStream(outFile);
			} catch (IOException e) {
				e.printStackTrace();
				outFile = null;
			}
		}
		
		new FollowTester(baseDir, test[0], test[1], test[2], windowSize[0], windowSize[1], out);
		
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println();
	}
}
