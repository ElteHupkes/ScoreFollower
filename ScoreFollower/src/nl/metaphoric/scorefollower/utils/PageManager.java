package nl.metaphoric.scorefollower.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import nl.metaphoric.scorefollower.R;
import nl.metaphoric.scorefollower.TrainActivity;
import nl.metaphoric.scorefollower.lib.Position;
import nl.metaphoric.scorefollower.lib.file.PositionRecorder;
import nl.metaphoric.scorefollower.view.MarkerView;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * The PageManager holds a list of draw pages and calculates
 * corresponding positions.
 * @author Elte Hupkes
 */
public class PageManager implements OnTouchListener {
	/**
	 * Log tag
	 */
	private static final String TAG = "SF_PageManager";	
	
	/**
	 * Internal class used to represent a page
	 */
	public class Page {
		/**
		 * The ImageView object corresponding
		 * to this page.
		 */
		private ImageView image;
		private RelativeLayout layout;
		
		/**
		 * Width and height parameters, possibly calculated.
		 */
		private int width, height;
		
		/**
		 * Absolute path to the filename for this score
		 */
		private String filePath;
		
		/**
		 * Creates a new page from a file. This also does the bitmap decoding,
		 * using memory saving techniques described at these urls:
		 * http://stackoverflow.com/questions/3331527/
		 * http://stackoverflow.com/questions/2641726/
		 * 
		 * @param filename
		 */
		public Page(String filename) {
			Log.d(TAG, "Adding page with file name "+filename);
			Bitmap bm;

			File file = new File(filename);
			filePath= file.getAbsolutePath();
			
			// First, decode the boundaries of the image to get its width / height			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			
			// Determine the scale factor; scale up to the screen width
			double scale = maxWidth / (double)options.outWidth;
			
			// Set the new width and height according to this scale factor
			int originalWidth = options.outWidth;
			width = (int)(options.outWidth * scale);
			height = (int)(options.outHeight * scale);			
			
			Log.d(TAG, "Page scale factor: "+scale);
			
			options.inJustDecodeBounds = false;
			if (scale < 1) {
				// Set the sample size to the smallest power of two that would 
				// create a larger image than required.
				options.inSampleSize = (int)(1/scale);
				Log.d(TAG, "Setting sample size to "+options.inSampleSize);
			}
			
			// Now load the actual data using the new sample size
			bm = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			
			Log.d(TAG, "Actual sample size: "+((double)originalWidth / bm.getWidth()));
			
			// If this file is still larger than the texture limits, we have to resize it.
			if (bm.getWidth() > 2048 || bm.getHeight() > 2048) {
				// Resize the bitmap
				Bitmap nbm = Bitmap.createScaledBitmap(bm, width, height, false);
				
				// Clear the old bitmap data
				bm.recycle();
				bm = nbm;
			}
			
			layout = (RelativeLayout)inflater.inflate(R.layout.page, null);
			image = (ImageView)layout.findViewById(R.id.page_image);
			// Set width and height for correct scaling
			image.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
			image.setImageBitmap(bm);
			layout.setOnTouchListener(PageManager.this);
			layout.setTag(R.id.id_context_type, TYPE_PAGE);
			image.setScaleType(ImageView.ScaleType.FIT_XY);
		}
		
		/**
		 * Returns the coordinates in this page corresponding to
		 * the position p.
		 * @param p
		 * @return
		 */
		public int[] getCoords(Position p) {
			return new int[] {(int)(p.xFrac() * width), (int)(p.yFrac() * height)};
		}
		
		/**
		 * Draws this page. _Always_ add the page to the pages array
		 * before drawing it.
		 */
		public void draw() {
			int pageIndex = pages.indexOf(this);
			layout.setTag(R.id.id_context_index, pageIndex);
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(width, height);
			pageLayout.addView(layout, pageIndex, params);
		}
		
		/**
		 * Removes this page and all markers
		 * on it.
		 */
		public void remove() {
			int pageIndex = pages.indexOf(this);
			
			// Remove all markers on this page
			Iterator<Marker> values = markers.values().iterator();
			while (values.hasNext()) {
				Marker marker = values.next();
				if (marker.pageRemoved(pageIndex)) {
					values.remove();
				}
			}
			
			pageLayout.removeView(layout);
		}
		
		/**
		 * Moves a page up, if possible
		 */
		public void moveUp() {
			int pageIndex = pages.indexOf(this);
			if (pageIndex > 0) {
				swap(pageIndex, pageIndex - 1);
			}
		}

		/**
		 * Moves a page up, if possible
		 */
		public void moveDown() {
			int pageIndex = pages.indexOf(this);
			if (pageIndex < (pages.size() - 1)) {
				swap(pageIndex, pageIndex + 1);
			}
		}		
		
		/**
		 * Move a page to a different position, swapping it
		 * with the one there.
		 * @param from
		 * @param to
		 */
		private void swap(int from, int to) {
			Page swap = pages.get(to);
			
			pages.set(from, swap);
			pages.set(to, this);
			pageLayout.removeView(swap.getLayout());
			pageLayout.removeView(layout);
			Log.d(TAG, "Current parent: "+layout.getParent());
			Log.d(TAG, "Swap parent: "+swap.getLayout().getParent());
			if (from > to) {
				// Moving down, draw this one first
				this.draw();
				swap.draw();
			} else {
				swap.draw();
				this.draw();
			}
			
			updateMarkerPages(from, to);
		}
		
		/**
		 * Updates marker values
		 * @param from
		 * @param to
		 */
		private void updateMarkerPages(int from, int to) {
			for (Marker m : markers.values()) {
				m.pageChanged(from, to);
			}
		}
		
		// Getter methods
		public int width() { return width; }
		public int height() { return height; }
		public int index() { return pages.indexOf(this); }
		public String getFilePath() { return filePath; }
		public RelativeLayout getLayout() { return layout; }
	}
	
	/**
	 * Class to represent a marker
	 */
	private class Marker {
		private MarkerView im;
		private Position p;
		private boolean drawn = false;
		
		/**
		 * Creates a new marker from an
		 * ImageView and a position.
		 * @param im
		 * @param p
		 */
		public Marker(Position p, int index, int count) {
			im = (MarkerView)inflater.inflate(R.layout.marker, null);
			im.setTag(R.id.id_context_type, TYPE_MARKER);
			im.setTag(R.id.id_context_index, index);
			setCount(count);
						
			im.setOnTouchListener(PageManager.this);
			this.p = p;
		}
		
		/**
		 * Draws the marker
		 */
		public void draw() {
			if (drawn) {
				return;
			}
			Page page = pages.get(p.page());
			int coords[] = page.getCoords(p);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(20, 20);
			params.leftMargin = coords[0] - 10;
			params.topMargin = coords[1] - 10;
			page.getLayout().addView(im, params);
			drawn = true;
		}
		
		/**
		 * Removes the marker from the layout
		 * @param fromList
		 */
		public void remove() {
			pages.get(p.page()).getLayout().removeView(im);
			drawn = false;
		}
		
		/**
		 * Call this method when a page has been removed. This marker
		 * will then remove itself is necessary. Due to concurrency issues,
		 * this marker cannot remove itself from the marker list, so you'll
		 * have to do that yourself if this method returns true.
		 * @param page
		 * @return True if this marker was removed, false if it wasn't.
		 */
		public boolean pageRemoved(int page) {
			if (p.page() == page) {
				// Remove this marker
				remove();
				return true;
			} else if (p.page() > page) {
				// Set lower page index
				p.page(p.page() - 1);
			}
			return false;
		}
		
		/**
		 * Called if a page number is changed.
		 * @param from
		 * @param to
		 * @return
		 */
		public void pageChanged(int from, int to) {
			if (p.page() == from) {
				p.page(to);
			} else if (p.page() == to) {
				p.page(from);
			}
		}
		
		/**
		 * Sets the checked state of this marker
		 */
		public void setChecked(boolean checked) {
			im.setChecked(checked);
		}
		
		/**
		 * Sets the activated state of this marker
		 * @param activated
		 */
		public void setActivated(boolean activated) {
			im.setActivated(activated);
		}
		
		/**
		 * Shows/hides the marker
		 * @param show
		 */
		public void setVisible(boolean show) {
			if (show) {
				im.setVisibility(View.VISIBLE);
			} else {
				im.setVisibility(View.INVISIBLE);
			}
		}
		
		/**
		 * Sets the counter in the MarkerView
		 * @param count
		 */
		public void setCount(int count) {
			im.setText(""+count);
		}
	}
	
	/**
	 * Interface for marker click events
	 * as used by the PlayActivity.
	 */
	public interface OnMarkerClickListener {
		public void onMarkerClick(int position);
	}
	
	/**
	 * Type identifiers used in View tags
	 */
	private static final int TYPE_PAGE = 0,
							 TYPE_MARKER = 1;
	
	/**
	 * Action Mode callback for pages 
	 */
	private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			int contextMenu;
			actionMode = mode;
			if (activePage >= 0) {
				contextMenu = R.menu.train_context_page;
			} else {
				markers.get(activeMarker).setChecked(true);
				contextMenu = R.menu.train_context_marker;
			}
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(contextMenu, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Page p;
			switch (item.getItemId()) {
			case R.id.move_up:
				p = pages.get(activePage);
				p.moveUp();
				updateMarkerCounts();
				return true;
			case R.id.move_down:
				p = pages.get(activePage);
				p.moveDown();
				updateMarkerCounts();
				return true;
			case R.id.remove_page:
				p = pages.get(activePage);
				p.remove();
				pages.remove(activePage);
				((TrainActivity)context).checkPages();
				updateMarkerCounts();
				activePage = -1;
				actionMode.finish();
				return true;
			case R.id.remove_marker:
				Marker m = markers.get(activeMarker);
				m.remove();
				markers.remove(activeMarker);
				updateMarkerCounts();
				activeMarker = -1;
				actionMode.finish();
				return true;
			default:
				return false;
			}
		}

		public void onDestroyActionMode(ActionMode mode) {
			if (activeMarker >= 0) {
				markers.get(activeMarker).setChecked(false);
			}
			activePage = -1;
			activeMarker = -1;
			actionMode = null;
		}
	};	
	
	/**
	 * 
	 */
	private OnMarkerClickListener mcl = null;
	
	/**
	 * The active ActionMode
	 */
	private ActionMode actionMode = null;
	
	/**
	 * Active page when ActionMode is used
	 */
	private int activePage = -1;
	
	/**
	 * Active marker when ActionMode is used.
	 */
	private int activeMarker = -1;
	
	/**
	 * Page modes
	 */
	public static final int MODE_EDITING = 0,
							MODE_RECORDING = 1,
							MODE_PLAYING = 2;
	
	/**
	 * The active page mode
	 */
	private int mode = MODE_PLAYING;	
	
	/**
	 * Internal list of pages, usually
	 * a LinkedList implementation.
	 */
	private List<Page> pages = new ArrayList<Page>();
	
	/**
	 * Map with markers
	 */
	private Map<Integer, Marker> markers = new HashMap<Integer, Marker>();		
	
	/**
	 * The layout that holds the pages and the markers
	 */
	private ViewGroup pageLayout;
	
	/**
	 * The Activity
	 */
	private Activity context;
	
	/**
	 * A PositionRecorder object, if any
	 */
	private PositionRecorder recorder;
	
	/**
	 * The maximum display width; determined from
	 * device resolution.
	 */
	private int maxWidth = 0;
	
	private LayoutInflater inflater;
	
	/**
	 * Initializes a new page manager
	 */
	public PageManager(Activity context, ViewGroup pageLayout, ViewGroup markerLayout) {
		this(context, pageLayout, markerLayout, null);
	}
	
	/**
	 * Initializes a new page manager with a recorder
	 */
	public PageManager(Activity context, ViewGroup pageLayout, 
			ViewGroup markerLayout, PositionRecorder recorder) {
		this.context = context;
		this.pageLayout = pageLayout;
		this.recorder = recorder;
	}
	
	/**
	 * Fetches window size etc
	 */
	public void init() {
		Point size = new Point();
		context.getWindowManager().getDefaultDisplay().getSize(size);
		maxWidth = size.x;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	/**
	 * Sets the visibility state of the markers
	 * @param visible
	 */
	public void setMarkersVisible(boolean visible) {
		for (Marker m : markers.values()) {
			m.setVisible(visible);
		}
	}
	
	/**
	 * Returns the number of pages loaded
	 * @return
	 */
	public int getCount() {
		return pages.size();
	}
	
	/**
	 * 
	 * @param mode
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}
	
	/**
	 * Returns the current mode, one of the MODE_ constants.
	 * @return
	 */
	public int getMode() {
		return mode;
	}
	
	/**
	 * Removes (and undraws) all pages and markers from this
	 * PageManager
	 */
	public void clear() {
		for (Marker m : markers.values()) {
			m.remove();
		}
		markers.clear();
		
		for (Page p : pages) {
			p.remove();
		}
		pages.clear();
	}
	
	/**
	 * Clears the markers but leaves the pages in tact.
	 * If you do this, you should probably clear the
	 * PositionRecorder too. 
	 */
	public void clearMarkers() {
		for (Marker m : markers.values()) {
			m.remove();
		}
		markers.clear();
	}
	
	/**
	 * Updates marker text counters
	 */
	public void updateMarkerCounts() {
		int i = 1;
		TreeSet<Integer> keys = new TreeSet<Integer>(markers.keySet());
		Log.d(TAG, "Number of markers: "+keys.size());
		for (Integer k : keys) {
			markers.get(k).setCount(i);
			i++;
		}
	}
	
	/**
	 * Sets the active state of the marker
	 * at the given position index.
	 * @param p
	 */
	private Marker activatedMarker = null;
	public void setActivedMarker(int positionIndex) {
		if (activatedMarker != null) {
			activatedMarker.setActivated(false);
		}
		if (markers.containsKey(positionIndex)) {
			activatedMarker = markers.get(positionIndex); 
			activatedMarker.setActivated(true);
		}
	}
	
	/**
	 * Adds the page at the given filename
	 * @param filename
	 */
	public void addPage(String filename) {
		Page p = new Page(filename);
		pages.add(p);
		p.draw();
	}
	
	/**
	 * Returns a Position object from the
	 * given coordinates, or null if the coordinates
	 * are outside of the page position bounds.
	 * @return
	 */
	public Position getPositionFromCoords(double x, double y, int page) {
		Page p = pages.get(page);
		double yFrac = y / p.height(),
				xFrac = x / p.width();
		return new Position(page, xFrac, yFrac);
	}
	
	/**
	 * Returns the x, y coordinates corresponding to the given position p.
	 * Returns [0,0] coordinates if the position cannot be determined.
	 * @param p
	 * @return
	 */
	public int[] getCoordsFromPosition(Position p) {
		double total = 0;
		for (int i = 0; i < pages.size(); i++) {
			Page page = pages.get(i);
			if (i < p.page()) {
				total += page.height();
			} else {
				return new int[] {(int)(page.width() * p.xFrac()), 
						(int)(total + page.height() * p.yFrac())};
			}
		}
		Log.w(TAG, "Cannot successfully determine position coordinates.");
		return new int[] {0, 0};
	}	
	
	/**
	 * Adds a new position marker with a touch context
	 * menu, corresponding to the current position in the
	 * recorder.
	 * @param x
	 * @param y
	 * @param i The page
	 */
	public void addRecorderMarker(double x, double y, int i) {
		Position np = getPositionFromCoords(x, y, i);
		if (np != null) {			
			int p = recorder.addPosition(np);
			putMarker(p, np);
		}
	}
	
	/**
	 * Draws a list of markers, intended for playing mode.
	 * There is a mild complexity here, as all pages need to
	 * have been drawn to screen before this can work (otherwise they
	 * will not have layout).
	 */
	public void addVisualMarkers(Map<Integer, Position> m) {
		for (int i : m.keySet()) {
			putMarker(i, m.get(i));
		}
	}
	
	/**
	 * Creates a new marker, represented by a dot on screen.
	 */
	public Marker putMarker(int index, Position p) {	
		if (markers.containsKey(index)) {
			// Remove the existing Marker
			markers.get(index).remove();
			markers.remove(index);
		}
		
		Marker m = new Marker(p, index, markers.size() + 1);
		markers.put(index, m);	
		m.draw();
		return m;
	}
	
	/**
	 * Touch listener for marker add events, if enabled. ScrollView cancels
	 * all events that scroll, meaning we'll only ever get ACTION_UP on click,
	 * which is exactly what we need to know.
	 * @param v
	 * @param event
	 * @return
	 */
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// Just return true to let the system know we're capturing this event.
			// ScrollView will take over if we start moving.
			return true;
		case MotionEvent.ACTION_UP:
			int type = (Integer)v.getTag(R.id.id_context_type),
				index = (Integer)v.getTag(R.id.id_context_index);
			
			Log.d(TAG, "Touch up at "+event.getX(0)+"/"+event.getY(0));
			if (mode == MODE_RECORDING && type == TYPE_PAGE) {
				// This touch event is now bound to pages, so we will get
				// the coordinates _within_ the page. 
				addRecorderMarker(event.getX(0), event.getY(0), index);
			} else if (mode == MODE_EDITING) {
				// Open action context menu's
				boolean alreadyActive = 
						(type == TYPE_MARKER && activeMarker == index) ||
						(type == TYPE_PAGE && activePage == index);
				
				if (!alreadyActive) {						
					if (actionMode != null) {
						actionMode.finish();
					}
					
					if (type == TYPE_MARKER) {
						activeMarker = index;
					} else {
						// Must be a page
						activePage = index;
					}
					
					context.startActionMode(actionModeCallback);				
				} else {
					return false;
				}
			} else if (mode == MODE_PLAYING && type == TYPE_MARKER && mcl != null) {
				// Call the marker click listener with this index
				mcl.onMarkerClick(index);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Sets the listener for marker click events in play mode
	 * @param m
	 */
	public void setOnMarkerClickListener(OnMarkerClickListener m) {
		mcl = m;
	}
	
	/**
	 * Returns the filenames of the currently loaded
	 * pages.
	 * @return
	 */
	public String[] getFilenames() {
		String[] filenames = new String[pages.size()];
		
		int i = 0;
		for (Page p : pages) {
			filenames[i] = p.getFilePath();
			i++;
		}
		return filenames;
	}
}
