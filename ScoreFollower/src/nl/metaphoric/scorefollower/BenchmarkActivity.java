package nl.metaphoric.scorefollower;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import nl.metaphoric.scorefollower.lib.AnalyzeListener;
import nl.metaphoric.scorefollower.lib.AudioAnalyzer;
import nl.metaphoric.scorefollower.lib.Parameters;
import nl.metaphoric.scorefollower.lib.RunningAverage;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.feature.FrameVectorFactory;
import nl.metaphoric.scorefollower.lib.file.ScoreReader;
import nl.metaphoric.scorefollower.lib.matcher.PositionMatcher;
import nl.metaphoric.scorefollower.utils.MicrophoneReader;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * The BenchmarkActivity is used to put
 * maximum strain on the application, thereby
 * testing its performance. It uses a fixed
 * reference file.
 * 
 * This activity has some dependencies,
 * such as paths that need to exist.
 * I've hidden the button to it, as
 * it's not really required for anything
 * but the thesis results which were already
 * generated.
 * 
 * @author Elte Hupkes
 *
 */
public class BenchmarkActivity extends Activity implements AnalyzeListener {
	private static final String TAG = "SF_BenchmarkActivity";
	private PositionMatcher matcher;
	private MicrophoneReader micReader;
	private AudioAnalyzer analyzer;
	private int sampleRate;
	
	/**
	 * Number of received samples
	 */
	private int nSamples = 0,
				nRuns = 0;
	
	/**
	 * Average of times
	 */
	private RunningAverage times;
	
	/**
	 * Stores all times
	 */
	ArrayList<Long> timeList, featureTimeList;
	
	/**
	 * The number of samples after which benchmarking
	 * is stopped.
	 */
	private static final int maxSamples = 2400, maxRuns = 3;
	
	private TextView resultText;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.benchmark);
		Parameters.dBTreshold = 0.0;
		Parameters.frameVectorType = FrameVectorFactory.TYPE_STRAIN;
		sampleRate = Integer.parseInt(getString(R.string.sample_rate));
		resultText = (TextView)findViewById(R.id.bench_result);
	}
	
	public void startBenchmark() {
		if (micReader != null) {
			// Already started
			return;
		}
		
		timeList = new ArrayList<Long>();
		featureTimeList = new ArrayList<Long>();
		times = new RunningAverage();
		nSamples = 0;
		
		nl.metaphoric.scorefollower.lib.Log.setLogger(null);
		InputStream input;
		ScoreReader score;
		try {
			input = getAssets().open("benchmark.sft");
			score = new ScoreReader(new InputStreamReader(input), false);
		} catch (IOException e) {
			Log.e(TAG, "Could not start benchmark!");
			e.printStackTrace();
			return;
		}
		// Get a position matcher
		matcher = score.getMatcher();
		
		// Load the AudioAnalyzer
		analyzer = new AudioAnalyzer(this, sampleRate, matcher.windowSize(), matcher.hopSize());
		
		// Load the Microphone Reader
		micReader = new MicrophoneReader(analyzer);
		micReader.startRecorder();
	}

	/**
	 * Stops the benchmark
	 */
	public void stopBenchmark() {
		/**
		 * The benchmark can be stopped on any thread, to prevent
		 * thread-joining weirdness, force the actual stop on the UI-thread.
		 */
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (micReader != null && micReader.isActive()) {
					micReader.stopRecorder();
					micReader = null;
				}
				showResults();
			}
		});
	}
	
	public void showResults() {
		long max = Collections.max(timeList),
				 min = Collections.min(timeList);
		String text = "Average calculation time: "+times.getMean()+"\n"+
					  "Standard deviation: "+times.getStd()+"\n"+
					  "Nr. of samples: "+nSamples+"\n"+
					  "Min/max: "+min+"/"+max;
		
		Log.i(TAG, text);
		setResultText(text);
		
		try {			
			BufferedWriter out = new BufferedWriter(
					new FileWriter("/sdcard/ScoreFollower/benchmark_feature"+nRuns+".result"));
			out.write(text);
			out.newLine();
			for (int i = 0; i < timeList.size(); i++) {
				// Write away as microseconds
				out.write(Math.round(timeList.get(i) / 1000.0) + " " 
							+ Math.round(featureTimeList.get(i) / 1000.0));
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Stops the benchmark on destroy
	 */
	@Override
	public void onDestroy() {
		stopBenchmark();
		super.onDestroy();
	}
	
	@Override
	public void onPause() {
		stopBenchmark();
		super.onPause();
	}
	
	@Override
	public void onNewAnalysisData(FrameVector a) {
		if (nSamples >= maxSamples) {
			Log.i(TAG, "Enough samples, stop benchmark.");
			nRuns++;
			stopBenchmark();
			if (nRuns <= maxRuns) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						startBenchmark();
					}
				});				
			}
			return;
		}
		nSamples++;
		// Add time needed for feature calculation
		featureTimeList.add(System.nanoTime() - micReader.lastDataReady);
		final int position = matcher.getPosition(a);
		long t = System.nanoTime() - micReader.lastDataReady; 
		times.add(t);
		timeList.add(t);
		setResultText("Samples: "+nSamples+"/"+maxSamples+"\nPosition: "+position);
	}

	/**
	 * Sets the result text from any thread
	 * @param text
	 */
	private void setResultText(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				resultText.setText(text);
			}
		});		
	}
	
	/**
	 * Handles click events for the action bar
	 * @param view
	 */
	public void clickHandler(View view) {
		switch (view.getId()) {
		case R.id.bench_start:
			nRuns = 0;
			startBenchmark();
		break;
		case R.id.bench_stop:
			stopBenchmark();
		break;
		}
	}
}
