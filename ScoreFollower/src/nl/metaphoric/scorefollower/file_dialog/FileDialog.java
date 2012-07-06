/**
 * This is a modified version of http://code.google.com/p/android-file-dialog/
 * which supposedly is distributed using a new BSD License, but the details
 * (name/organization) aren't actually included anywhere.
 */
package nl.metaphoric.scorefollower.file_dialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import nl.metaphoric.scorefollower.R;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/**
 * Activity used to pick files / directories
 * 
 */
public class FileDialog extends ListActivity {

	/**
	 * Log tag
	 */
	private static final String TAG = "FileDialog";
	
	/**
	 * A key item in the path list
	 */
	private static final String ITEM_KEY = "key";

	/**
	 * An image item in the path list
	 */
	private static final String ITEM_IMAGE = "image";

	/**
	 * Root directory
	 */
	private static final String ROOT = "/";

	/**
	 * Activity input parameter for the initial path
	 */
	public static final String START_PATH = "START_PATH";

	/**
	 * Input parameter for file formats filter. Defaults to NULL.
	 */
	public static final String FORMAT_FILTER = "FORMAT_FILTER";

	/**
	 * Output parameter of the activity; the chosen path. Defaults to NULL.
	 */
	public static final String RESULT_PATH = "RESULT_PATH";

	/**
	 * Input parameter for the activity.
	 * The maximum number of items that can be selected. If more
	 * items are selected, the last one is replaced.
	 * Defaults to 1.
	 */
	public static final String MAX_ITEMS = "MAX_ITEMS";
	private int maxItems = 1;
	
	/**
	 * Input parameter for this activity, sets the selection mode to
	 * only existing files / new files.
	 * 
	 * @see {@link SelectionMode}
	 */
	public static final String SELECTION_MODE = "SELECTION_MODE";

	/**
	 * Input parameter of the activity, determines if selecting a directory
	 * is allowed.
	 */
	public static final String CAN_SELECT_DIR = "CAN_SELECT_DIR";

	private List<String> path = null;
	private TextView myPath;
	private EditText mFileName;
	private ArrayList<HashMap<String, Object>> mList;

	private Button selectButton;

	private LinearLayout layoutSelect;
	private LinearLayout layoutCreate;
	
	private InputMethodManager inputManager;
	private String parentPath;
	private String currentPath = ROOT;

	private int selectionMode = SelectionMode.MODE_CREATE;

	private String[] formatFilter = null;

	private boolean canSelectDir = false;
	
	/**
	 * List of selected files
	 */
	private List<Integer> selectedFiles = new ArrayList<Integer>();
	
	private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

	/**
	 * Called when the activity is first created. Sets input parameters
	 * and views.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED, getIntent());

		setContentView(R.layout.file_dialog_main);
		myPath = (TextView) findViewById(R.id.path);
		mFileName = (EditText) findViewById(R.id.fdEditTextFile);
		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		// Set the click listener for the select button
		selectButton = (Button) findViewById(R.id.fdButtonSelect);
		selectButton.setEnabled(false);
		selectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (selectedFiles.size() > 0) {						
					getIntent().putExtra(RESULT_PATH, getFileNames());
					setResult(RESULT_OK, getIntent());
					finish();
				} else if (canSelectDir) {
					getIntent().putExtra(RESULT_PATH, new String[] {currentPath});
					setResult(RESULT_OK, getIntent());
					finish();
				}
			}
		});

		// Set the click listener for the create button
		final Button newButton = (Button) findViewById(R.id.fdButtonNew);
		newButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setCreateVisible(v);

				mFileName.setText("");
				mFileName.requestFocus();
			}
		});

		// Get input parameters
		selectionMode = getIntent().getIntExtra(SELECTION_MODE, SelectionMode.MODE_CREATE);
		formatFilter = getIntent().getStringArrayExtra(FORMAT_FILTER);
		canSelectDir = getIntent().getBooleanExtra(CAN_SELECT_DIR, false);
		maxItems = getIntent().getIntExtra(MAX_ITEMS, maxItems);
		
		if (selectionMode == SelectionMode.MODE_OPEN) {
			newButton.setEnabled(false);
		}

		layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
		layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);
		layoutCreate.setVisibility(View.GONE);

		// Set options for the create layout
		final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setSelectVisible(v);
			}

		});
		final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
		createButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mFileName.getText().length() > 0) {
					getIntent().putExtra(RESULT_PATH, 
							new String[] {currentPath + "/" + mFileName.getText()});
					setResult(RESULT_OK, getIntent());
					finish();
				}
			}
		});

		String startPath = getIntent().getStringExtra(START_PATH);
		startPath = startPath != null ? startPath : ROOT;
		getDir(startPath);
	}

	/**
	 * 
	 * @param dirPath
	 */
	private void getDir(String dirPath) {
		boolean useAutoSelection = dirPath.length() < currentPath.length();
		Integer position = lastPositions.get(parentPath);
		getDirImpl(dirPath);
		if (position != null && useAutoSelection) {
			getListView().setSelection(position);
		}
		
		// Always clear selected files when a new directory is opened
		selectedFiles.clear();
		setSelectState();
	}
	
	/**
	 * Sets the correct enabled state for the
	 * select button.
	 */
	private void setSelectState() {
		selectButton.setEnabled(selectedFiles.size() > 0 || canSelectDir);
		Log.d(TAG, "Selected files:" + selectedFiles.size());
	}
	
	/**
	 * Selects a certain file.
	 * @param path
	 */
	private void select(Integer pos) {
		if (selectedFiles.size() >= maxItems) {
			Integer r = selectedFiles.remove(selectedFiles.size() - 1);
			((CheckableRow)getListView().getChildAt(r)).setChecked(false);
		}
		selectedFiles.add(pos);
		setSelectState();
	}
	
	/**
	 * Deselects a file path
	 */
	private void deselect(Integer pos) {
		selectedFiles.remove(pos);
		((CheckableRow)getListView().getChildAt(pos)).setChecked(false);
		setSelectState();
	}
	
	/**
	 * Returns the filenames of the selected items
	 * @return
	 */
	private String[] getFileNames() {
		String[] res = new String[selectedFiles.size()];
		Collections.sort(selectedFiles);
		for (int i = 0; i < selectedFiles.size(); i++) {
			res[i] = path.get(selectedFiles.get(i));
			Log.d(TAG, "Returning path: "+res[i]);
		}
		return res;
	}
	
	/**
	 * Create the file / directory structure of the provided path.
	 * 
	 * @param dirPath
	 */
	private void getDirImpl(final String dirPath) {
		currentPath = dirPath;

		final List<String> item = new ArrayList<String>();
		path = new ArrayList<String>();
		mList = new ArrayList<HashMap<String, Object>>();

		File f = new File(currentPath);
		File[] files = f.listFiles();
		if (files == null) {
			currentPath = ROOT;
			f = new File(currentPath);
			files = f.listFiles();
		}
		myPath.setText(getText(R.string.location) + ": " + currentPath);

		if (!currentPath.equals(ROOT)) {
			item.add(ROOT);
			addItem(ROOT, R.drawable.ic_folder);
			path.add(ROOT);

			item.add("../");
			addItem("../", R.drawable.ic_folder);
			path.add(f.getParent());
			parentPath = f.getParent();

		}

		TreeMap<String, String> dirsMap = new TreeMap<String, String>(),
								dirsPathMap = new TreeMap<String, String>(),
								filesMap = new TreeMap<String, String>(),
								filesPathMap = new TreeMap<String, String>();
		for (File file : files) {
			if (file.isDirectory()) {
				String dirName = file.getName();
				dirsMap.put(dirName, dirName);
				dirsPathMap.put(dirName, file.getPath());
			} else {
				final String fileName = file.getName();
				final String fileNameLwr = fileName.toLowerCase();
				boolean contains = true;
				
				// Use the filter if supplied
				if (formatFilter != null) {
					contains = false;
					for (int i = 0; i < formatFilter.length; i++) {
						final String formatLwr = formatFilter[i].toLowerCase();
						if (fileNameLwr.endsWith(formatLwr)) {
							contains = true;
							break;
						}
					}
				}
				
				// Add the file if contains is true.
				if (contains) {
					filesMap.put(fileName, fileName);
					filesPathMap.put(fileName, file.getPath());
				}
			}
		}
		
		item.addAll(dirsMap.tailMap("").values());
		item.addAll(filesMap.tailMap("").values());
		path.addAll(dirsPathMap.tailMap("").values());
		path.addAll(filesPathMap.tailMap("").values());

		SimpleAdapter fileList = new SimpleAdapter(this, mList, R.layout.file_dialog_row, 
				new String[] {ITEM_KEY, ITEM_IMAGE }, 
				new int[] { R.id.fdrowtext, R.id.fdrowimage });

		for (String dir : dirsMap.tailMap("").values()) {
			addItem(dir, R.drawable.ic_folder);
		}

		for (String file : filesMap.tailMap("").values()) {
			addItem(file, R.drawable.ic_file);
		}

		fileList.notifyDataSetChanged();
		setListAdapter(fileList);
	}

	/**
	 * Adds an item to the adapter list
	 * @param fileName
	 * @param imageId
	 */
	private void addItem(String fileName, int imageId) {
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put(ITEM_KEY, fileName);
		item.put(ITEM_IMAGE, imageId);
		mList.add(item);
	}

	/**
	 * When a list item is clicked, either:
	 * - Open its files
	 * - If canSelectDir is true and it is a directory, add it to the file list
	 * - If it is a file, add it to the active list of files
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(path.get(position));
		setSelectVisible(v);

		if (file.isDirectory()) {
			if (file.canRead()) {
				lastPositions.put(currentPath, position);
				getDir(path.get(position));
			} else {
				new AlertDialog.Builder(this).setIcon(R.drawable.ic_folder)
					.setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {}
					}).show();
			}
		} else {
			CheckableRow r = (CheckableRow)v;
			r.toggle();
			if (r.isChecked()) {				
				select(position);
			} else {
				deselect(position);
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			selectButton.setEnabled(false);

			if (layoutCreate.getVisibility() == View.VISIBLE) {
				layoutCreate.setVisibility(View.GONE);
				layoutSelect.setVisibility(View.VISIBLE);
			} else {
				if (!currentPath.equals(ROOT)) {
					getDir(parentPath);
				} else {
					return super.onKeyDown(keyCode, event);
				}
			}

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * Hides the select layout, shows the create layout
	 * 
	 * @param v
	 */
	private void setCreateVisible(View v) {
		layoutCreate.setVisibility(View.VISIBLE);
		layoutSelect.setVisibility(View.GONE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);
	}

	/**
	 * Hides the create layout, shows the select layout
	 * 
	 * @param v
	 */
	private void setSelectVisible(View v) {
		layoutCreate.setVisibility(View.GONE);
		layoutSelect.setVisibility(View.VISIBLE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);
	}
}