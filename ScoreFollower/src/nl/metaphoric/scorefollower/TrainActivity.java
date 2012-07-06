package nl.metaphoric.scorefollower;

import java.io.File;
import java.io.IOException;

import nl.metaphoric.scorefollower.file_dialog.FileDialog;
import nl.metaphoric.scorefollower.file_dialog.SelectionMode;
import nl.metaphoric.scorefollower.lib.AnalyzeListener;
import nl.metaphoric.scorefollower.lib.AudioAnalyzer;
import nl.metaphoric.scorefollower.lib.feature.FrameVector;
import nl.metaphoric.scorefollower.lib.file.PositionRecorder;
import nl.metaphoric.scorefollower.lib.file.ScoreReader;
import nl.metaphoric.scorefollower.utils.AndroidLogger;
import nl.metaphoric.scorefollower.utils.AudioInput;
import nl.metaphoric.scorefollower.utils.MicrophoneReader;
import nl.metaphoric.scorefollower.utils.PageManager;
import nl.metaphoric.scorefollower.utils.WavReader;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * The TrainActivity allows a user to train one or more
 * image files with microphone input.
 * 
 * @author Elte Hupkes
 *
 */
public class TrainActivity extends Activity implements AnalyzeListener {
	/**
	 * Log tag
	 */
	private static final String TAG = "SF_TrainActivity";
	
	/**
	 * Activity flag to load an existing training file
	 */
	public static final String LOAD_EXISTING = "LOAD_EXISTING";
	
	/**
	 * File dialog intent codes
	 */
	private static final int REQUEST_SAVE_FILENAME = 0,
							 REQUEST_ADD_PAGE = 1, 
							 REQUEST_PICK_WAV = 2;
	
	/**
	 * Mic/wav, active audio input instances
	 */
	private AudioInput audioInput, micInput, wavInput;
	
	/**
	 * Analyzes microphone data, called
	 * by MicrophoneReader.
	 */
	private AudioAnalyzer analyzer;
	
	/**
	 * Records FrameVector references, saves them
	 * to file.
	 */
	private PositionRecorder recorder;
	
	/**
	 * The marker touch listener / manager
	 */
	private PageManager pages;
	
	/**
	 * Buttons for easy showing/hiding
	 */
	private View startButton, pauseButton, stopButton;
	
	/**
	 * Sample rate (retrieved from config file)
	 */
	private int sampleRate;
	
	/**
	 * An active sound indicator
	 */
	View startedIndicator;	
	
	/**
	 * If "stopped" is set to true, the analyzer,
	 * recorder and pager will be reset before
	 * playing.
	 */
	private boolean stopped = false;
	
	/**
	 * The last used path for FileDialog
	 */
	private String lastPath = Environment.getExternalStorageDirectory().getPath();
	
	/**
	 * The save button / play along button in the action bar
	 */
	Menu activityMenu;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.train);
	    
		// Set logger for library files
		nl.metaphoric.scorefollower.lib.Log.setLogger(new AndroidLogger());
	    
	    sampleRate = Integer.parseInt(getString(R.string.sample_rate)); 
	    
	    analyzer = new AudioAnalyzer(this, sampleRate,
	    	Double.parseDouble(getString(R.string.window_size)),
	    	Double.parseDouble(getString(R.string.hop_size))
	    );
	    
	    recorder = new PositionRecorder();
	    micInput = new MicrophoneReader(analyzer);
	    
		// Set a touch marker to add position markers
	    ViewGroup markerLayout = (ViewGroup)findViewById(R.id.train_markers); 
		pages = new PageManager(this, (ViewGroup)findViewById(R.id.train_pages),
				markerLayout, recorder);
		pages.setMode(PageManager.MODE_EDITING);
		pages.init();
		
		startButton = findViewById(R.id.train_start);
		stopButton = findViewById(R.id.train_stop);
		pauseButton = findViewById(R.id.train_pause);
		
		startedIndicator = findViewById(R.id.started_status);
		
		String existing = getIntent().getStringExtra(LOAD_EXISTING);
		if (existing != null) {
			loadTrainingFile(existing);
		}
	}
	
	/**
	 * Loads the specified score follower reference file
	 * @param filename
	 */
	public void loadTrainingFile(String filename) {
		try {
			lastPath = filename;
			
			ScoreReader file = new ScoreReader(filename);
			for (String s : file.getPages()) {
				pages.addPage(s);
			}
			
			// Draw visual markers
			pages.addVisualMarkers(file.getPager().getPositions());
			
			if (activityMenu != null) {
				checkPages();
			}
		} catch (IOException io) {
			Toast.makeText(this, "Could not open the specified file.", Toast.LENGTH_LONG).show();
			Log.e(TAG, "Could not open the specified file.");
		}
	}
	
	/**
	 * Called when new microphone data has been analyzed into
	 * a FrameVector. This will _not_ take place on the UI-thread.
	 */
	@Override
	public void onNewAnalysisData(FrameVector v) {
		recorder.addData(v);
		
		// Create new boolean for thread safety
		final boolean playing = recorder.playing;
		runOnUiThread(new Runnable() {
			public void run() {
				startedIndicator.setActivated(playing);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.train_bar, menu);
		activityMenu = menu;
		checkPages();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_page:
			pickImage();
			return true;
		case R.id.load_wav:
			pickWav();
			return true;
		case R.id.play_along:
			finish();
			return true;
		case R.id.train_save:
			setSaveFilename();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Starts an activity to pick an image from the file system.
	 */
	private void pickImage() {
		pickFile(REQUEST_ADD_PAGE);
	}	
	
	private void pickWav() {
		pickFile(REQUEST_PICK_WAV);
	}
	
	/**
	 * Select files as pages or input audio
	 * @param type
	 */
	private void pickFile(int type) {
		String[] ext;
		int maxItems;
		if (type == REQUEST_ADD_PAGE) {
			ext = new String[] {"jpg", "jpeg", "gif", "png"};
			maxItems = 10;
		} else {
			ext = new String[] {"wav"};
			maxItems = 1;
		}
		
		Intent intent = new Intent(getBaseContext(), FileDialog.class);
		intent.putExtra(FileDialog.START_PATH, lastPath);
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
		intent.putExtra(FileDialog.FORMAT_FILTER, ext);
		intent.putExtra(FileDialog.MAX_ITEMS, maxItems);
		
		startActivityForResult(intent, type);
	}
	
	/**
	 * Called when the file pick activity returns.
	 */
	@Override
	public synchronized void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (resultCode == RESULT_OK) {
			String[] filePath = data.getStringArrayExtra(FileDialog.RESULT_PATH);
			lastPath = (new File(filePath[0])).getParent();
			switch (requestCode) {
			case REQUEST_SAVE_FILENAME:
				// Force the .sft extension
				if (!filePath[0].endsWith(".sft")) {
					filePath[0] = filePath[0] + ".sft";
				}
				writeToFile(filePath[0]);
				break;
			case REQUEST_ADD_PAGE:
				for (String path : filePath) {
					pages.addPage(path);
				}
				checkPages();
				break;
			case REQUEST_PICK_WAV:
				loadWavFile(filePath[0]);
				activityMenu.findItem(R.id.use_mic).setVisible(true);
				break;
			}
		}
	}	
	
	/**
	 * Loads a demo WAV file.
	 * @param filename
	 */
	public void loadWavFile(String filename) {
		try {
			wavInput = new WavReader(filename, analyzer);	
		} catch (Exception io) {
			Toast.makeText(this, "Could not open the specified file.", Toast.LENGTH_LONG).show();
			Log.e(TAG, "Could not open the specified file.");
		}
	}	
	
	/**
	 * Set the display state of the play / pause / stop buttons
	 * @param start
	 * @param pause
	 * @param stop
	 */
	private void showButtons(boolean start, boolean pause, boolean stop) {
		startButton.setVisibility(start ? View.VISIBLE : View.GONE);
		pauseButton.setVisibility(pause ? View.VISIBLE : View.GONE);
		stopButton.setVisibility(stop ? View.VISIBLE : View.GONE);
		
		startedIndicator.setVisibility(!start && (pause || stop) ? View.VISIBLE : View.GONE);
	}	
	
	/**
	 * Starts training mode.
	 */
	private void start() {
		boolean wavMode = wavInput != null && !activityMenu.findItem(R.id.use_mic).isChecked();
		if (audioInput == null && !wavMode) {			
			// Load the Microphone Reader if it wasn't loaded yet.
			micInput = new MicrophoneReader(analyzer);
		}
		
		audioInput = wavMode ? wavInput : micInput;
		if (!audioInput.isActive()) {
			// Clear any old data in the audio analyzer to prevent the first
			// frame from being garbage.
			analyzer.reset();
			if (stopped) {
				pages.clearMarkers();
				recorder.reset();
			}
			stopped = false;
			pages.setMode(PageManager.MODE_RECORDING);
			audioInput.startRecorder();
		}
	}
	
	/**
	 * Click handler for the playback controls
	 * @param v
	 */
	public void clickHandler(View v) {
		switch (v.getId()) {
		case R.id.train_start:
			showButtons(false, true, true);
			start();
			break;
		case R.id.train_pause:
			showButtons(true, false, true);
			if (audioInput.isActive()) {
				audioInput.stopRecorder();
			}
			break;
		case R.id.train_stop:
			showButtons(true, false, false);
			if (audioInput.isActive()) {
				pages.setMode(PageManager.MODE_EDITING);
				audioInput.stopRecorder();
				stopped = true;
			}
			break;
		}
	}
	
	/**
	 * Picks an image from the gallery, adds it as
	 * new score image.
	 */
	private void setSaveFilename() {
		Intent intent = new Intent(getBaseContext(), FileDialog.class);
		intent.putExtra(FileDialog.START_PATH, 
				Environment.getExternalStorageDirectory().getAbsolutePath());
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_CREATE);
		intent.putExtra(FileDialog.FORMAT_FILTER, new String[] {"sft"});
		
		startActivityForResult(intent, REQUEST_SAVE_FILENAME);
	}
	
	/**
	 * Writes the score to file
	 * @param filename
	 */
	private void writeToFile(String filename) {
		try {					
			recorder.write(filename, pages.getFilenames(), analyzer);
			Toast.makeText(this, "Successfully saved.", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(this, "Failed to save to the specified file: "+e.getMessage(), 
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "Failed to write to file: "+e.getMessage());
		}
	}
	
	/**
	 * Checks if any pages are loaded, and shows / hides buttons accordingly.
	 */
	public void checkPages() {
		if (pages.getCount() > 0) {
			activityMenu.findItem(R.id.train_save).setVisible(true);
			showButtons(true, false, false);
		} else {
			showButtons(false, false, false);
			activityMenu.findItem(R.id.train_save).setVisible(false);
		}
	}
	
	/**
	 * Stops recording when the activity is paused
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (audioInput != null && audioInput.isActive()) {
			showButtons(true, false, false);
			audioInput.stopRecorder();
		}
	}
}
