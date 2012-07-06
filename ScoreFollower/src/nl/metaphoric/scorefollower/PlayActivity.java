/**
 * The PlayActivity is used to play along with existing
 * reference data.
 * @author Elte Hupkes
 */
package nl.metaphoric.scorefollower;

import java.io.File;
import java.io.IOException;

import nl.metaphoric.scorefollower.file_dialog.FileDialog;
import nl.metaphoric.scorefollower.file_dialog.SelectionMode;
import nl.metaphoric.scorefollower.lib.AnalyzeListener;
import nl.metaphoric.scorefollower.lib.AudioAnalyzer;
import nl.metaphoric.scorefollower.lib.Position;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.file.ScoreReader;
import nl.metaphoric.scorefollower.lib.matcher.PositionMatcher;
import nl.metaphoric.scorefollower.lib.matcher.PositionPager;
import nl.metaphoric.scorefollower.utils.AndroidLogger;
import nl.metaphoric.scorefollower.utils.AudioInput;
import nl.metaphoric.scorefollower.utils.MicrophoneReader;
import nl.metaphoric.scorefollower.utils.PageManager;
import nl.metaphoric.scorefollower.utils.WavReader;
import nl.metaphoric.scorefollower.view.PositionGraphView;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

public class PlayActivity extends Activity implements AnalyzeListener {
	/**
	 * Log tag
	 */
	public static final String TAG = "SF_PlayActivity";
	
	/**
	 * Name for the Shared Preferences file
	 */
	public static final String PREFERENCES = "SF_Preferences",
							   PREF_LAST_FILE = "lastFile";
	
	/**
	 * Activity request codes
	 */
	public static final int REQUEST_PICK_TRAINING = 0,
							REQUEST_SAVE_SCORE = 1,
							REQUEST_PICK_DEMO = 2;
	
	/**
	 * The active audio input
	 */
	private AudioInput audioInput;
	
	/**
	 * Holds instances of microphone / wav file input
	 * to switch between.
	 */
	private AudioInput micInput, wavInput;
	
	/**
	 * Analyzes microphone data, called
	 * by MicrophoneReader.
	 */
	private AudioAnalyzer analyzer;
	
	/**
	 * Matches new audio signal.
	 */
	private PositionMatcher matcher = null;
	
	/**
	 * The position pager, matches estimated
	 * position to location on screen.
	 */
	private PositionPager pager = null;
	
	/**
	 * Draws image pages and handles events on them
	 */
	private PageManager pages = null;
	
	/**
	 * The sample rate
	 */
	int sampleRate;
	
	/**
	 * Holds the scroller layout, used for paging.
	 */
	private ScrollView scrollLayout;
	
	/**
	 * Holds the layout that contains the pages
	 */
	private ViewGroup pageLayout;
	
	/**
	 * Button views for ease of access.
	 */
	private View playButton, pauseButton, stopButton;
	
	/**
	 * The filename of the currently loaded file
	 */
	private String activeTraining;
	
	/**
	 * An active sound indicator
	 */
	private View startedIndicator;
	
	/**
	 * Activity bar instance so we can
	 * show / hide items.
	 */
	Menu activityMenu;
	
	/**
	 * Shared preferences object
	 */
	SharedPreferences prefs;
	
	/**
	 * A graph displaying position probabilities
	 */
	private PositionGraphView positionGraph;
	
	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.play);
		
		// Set logger for library
		nl.metaphoric.scorefollower.lib.Log.setLogger(new AndroidLogger());
		
		sampleRate = Integer.parseInt(getString(R.string.sample_rate));
		scrollLayout = (ScrollView)findViewById(R.id.play_scroll);
		pageLayout = ((ViewGroup)findViewById(R.id.play_pages));
		startedIndicator = findViewById(R.id.started_status);
		
		pages = new PageManager(this, pageLayout,
				((ViewGroup)findViewById(R.id.play_markers)));
		
		playButton = findViewById(R.id.play_start);
		pauseButton = findViewById(R.id.play_pause);
		stopButton = findViewById(R.id.play_stop);
		positionGraph = (PositionGraphView)findViewById(R.id.position_graph);
		
		prefs = getSharedPreferences(PREFERENCES, 0);
		String lastFile = prefs.getString(PREF_LAST_FILE, null);
		
		if (lastFile != null) {
			File test = new File(lastFile);
			if (test.exists()) {				
				loadTrainingFile(lastFile);
			}
		}
	}

	/**
	 * Loads the specified score follower reference file
	 * @param filename
	 */
	private void loadTrainingFile(String filename) {
		try {
			// Stop everything if active
			stop();
			pages.clear();
			pages.init();
			
			activeTraining = filename;
			
			ScoreReader file = new ScoreReader(filename);
			matcher = file.getMatcher();
			pager = file.getPager();
			
			Log.d(TAG, "Number of pages: "+file.getPages().size());
			for (String s : file.getPages()) {
				pages.addPage(s);
			}
			
			// Set listener for when a marker is touched
			pages.setOnMarkerClickListener(new PageManager.OnMarkerClickListener() {	
				@Override
				public void onMarkerClick(int position) {
					matcher.restart(position);
					pages.setActivedMarker(position);
				}
			});			
			
			// Draw visual markers
			pages.addVisualMarkers(pager.getPositions());
			
			// Load the AudioAnalyzer
			analyzer = new AudioAnalyzer(this, sampleRate, matcher.windowSize(), matcher.hopSize());
			
			if (activityMenu != null) {
				activityMenu.findItem(R.id.edit_train).setVisible(true);
				activityMenu.findItem(R.id.play_reload).setVisible(true);
			}
			
			SharedPreferences.Editor edit = prefs.edit();
			edit.putString(PREF_LAST_FILE, filename);
			edit.commit();
			
			showButtons(true, false, false);
		} catch (Exception io) {
			Toast.makeText(this, "Could not open the specified file.", Toast.LENGTH_LONG).show();
			Log.e(TAG, "Could not open the specified file.");
		}
	}
	
	/**
	 * Loads a demo WAV file.
	 * @param filename
	 */
	private void loadDemoFile(String filename) {
		try {
			wavInput = new WavReader(filename, analyzer);	
		} catch (Exception io) {
			Toast.makeText(this, "Could not open the specified file.", Toast.LENGTH_LONG).show();
			Log.e(TAG, "Could not open the specified file.");
		}
	}
	
	/**
	 * Starts an activity to pick a training filename
	 */
	private void pickTraining() {
		pickFile(REQUEST_PICK_TRAINING);
	}
	
	/**
	 * Starts an activity to open a demo WAV file.
	 */
	private void pickDemo() {
		pickFile(REQUEST_PICK_DEMO);
	}
	
	/**
	 * Starts an activity to open either a training or
	 * a demo file.
	 * @param type
	 */
	private void pickFile(int type) {
		String[] ext;
		if (type == REQUEST_PICK_TRAINING) {
			ext = new String[] {"sft"};
		} else {
			ext = new String[] {"wav"};
		}
		
		Intent intent = new Intent(getBaseContext(), FileDialog.class);
		intent.putExtra(FileDialog.START_PATH, 
				Environment.getExternalStorageDirectory().getAbsolutePath());
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
		intent.putExtra(FileDialog.FORMAT_FILTER, ext);
		
		startActivityForResult(intent, type);		
	}
	
	/**
	 * Called when the image pick activity returns.
	 */
	@Override
	public synchronized void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_PICK_TRAINING) {				
				String filePath = data.getStringArrayExtra(FileDialog.RESULT_PATH)[0];
				loadTrainingFile(filePath);
			} else if (requestCode == REQUEST_PICK_DEMO) {
				String filePath = data.getStringArrayExtra(FileDialog.RESULT_PATH)[0];
				loadDemoFile(filePath);
				activityMenu.findItem(R.id.toggle_demo).setVisible(true);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.play_bar, menu);
		activityMenu = menu;
		if (activeTraining != null) {
			menu.findItem(R.id.edit_train).setVisible(true);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.train_tab:
			intent = new Intent(this, TrainActivity.class);
			startActivity(intent);
			return true;
		case R.id.edit_train:
			// Tell the train activity to load an existing training
			intent = new Intent(this, TrainActivity.class);
			intent.putExtra(TrainActivity.LOAD_EXISTING, activeTraining);
			startActivity(intent);
			return true;
		case R.id.play_reload:
			if (activeTraining != null) {				
				loadTrainingFile(activeTraining);
			}
			return true;
		case R.id.toggle_markers:
			pages.setMarkersVisible(!item.isChecked());
			item.setChecked(!item.isChecked());
			break;
		case R.id.toggle_graph:
			positionGraph.setVisibility(item.isChecked() ? View.GONE : View.VISIBLE);
			item.setChecked(!item.isChecked());
			break;
		case R.id.toggle_demo:
			
			break;
		case R.id.select_train:
			pickTraining();
			return true;
		case R.id.load_demo:
			pickDemo();
			return true;
		case R.id.goto_benchmark:
			intent = new Intent(this, BenchmarkActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Callback for new analysis data.
	 * Note that this method will not be called by the UI-thread,
	 * so we'll have to use special methods to modify the UI.
	 * @param v
	 */
	@Override
	public void onNewAnalysisData(final FrameVector v) {
		/*
		 * Run new position getter on microphone thread for now,
		 * this prevents stacking up of events on the UI-thread.
		 * I might even allocate yet another thread for this
		 * in the future.
		 */
		int position = matcher.getPosition(v);
		Log.d(TAG, "New position: "+position);
		
		// Update the position graph with the new data, only if its visible.
		if (positionGraph.getVisibility() == View.VISIBLE) {			
			positionGraph.setData(matcher.getTransitionProbabilities(), position);
		}
		
		pager.setEstimate(position);
		
		if (pager.hasChanged()) {
			final Position p = pager.getPosition();
			final int positionIndex = pager.getPositionIndex();
			final int[] coords = pages.getCoordsFromPosition(p);
			
			// We'll try to put the reference point in the center of the screen,
			// so subtract half the containing view height.
			final int scrollHeight = (int)(coords[1] - (0.5 * scrollLayout.getHeight()));
			Log.d(TAG, "Scrolling to "+coords[0]+"/"+coords[1]+" : "+scrollHeight);
			runOnUiThread(new Runnable() {
				public void run() {
					pages.setActivedMarker(positionIndex);
					scrollLayout.smoothScrollTo(coords[0], scrollHeight);
				}
			});
		}
		
		// Create new boolean for thread safety
		final boolean playing = matcher.playing;
		runOnUiThread(new Runnable() {
			public void run() {
				startedIndicator.setActivated(playing);
			}
		});
	}
	
	/**
	 * Set the display state of the play / pause / stop buttons
	 * @param play
	 * @param pause
	 * @param stop
	 */
	private void showButtons(boolean play, boolean pause, boolean stop) {
		playButton.setVisibility(play ? View.VISIBLE : View.GONE);
		pauseButton.setVisibility(pause ? View.VISIBLE : View.GONE);
		stopButton.setVisibility(stop ? View.VISIBLE : View.GONE);
		startedIndicator.setVisibility(!play ? View.VISIBLE : View.GONE);
	}
	
	
	/**
	 * General fail-safe stop function, 
	 * stops pretty much everything.
	 */
	private void stop() {
		if (audioInput != null && audioInput.isActive()) {				
			audioInput.stopRecorder();
		}
		if (matcher != null) {			
			matcher.restart();
		}
		pages.setActivedMarker(-1);
		showButtons(true, false, false);
	}
	
	/**
	 * Starts playing mode.
	 */
	private void start() {
		boolean demoMode = wavInput != null && activityMenu.findItem(R.id.toggle_demo).isChecked();
		if (audioInput == null && !demoMode) {			
			// Load the Microphone Reader if it wasn't loaded yet.
			micInput = new MicrophoneReader(analyzer);
		}
		
		audioInput = demoMode ? wavInput : micInput;
		
		if (!audioInput.isActive()) {
			audioInput.startRecorder();
		}
	}
	
	/**
	 * Handles click events for the action bar
	 * @param view
	 */
	public void clickHandler(View view) {
		switch (view.getId()) {
		case R.id.play_start:
			showButtons(false, true, true);
			start();
			break;
		case R.id.play_stop:
			stop();
			break;
		case R.id.play_pause:				
			showButtons(true, false, true);
			if (audioInput.isActive()) {				
				audioInput.pauseRecorder();
			}
			break;
		}
	}
	
	/**
	 * This should be improved in the future; but for now just
	 * stop when the activity is paused. This at least allows screen
	 * orientation changes.
	 */
	@Override
	public void onPause() {
		stop();
		super.onPause();
	}
}
