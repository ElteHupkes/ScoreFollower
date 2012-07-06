package nl.metaphoric.scorefollower.view;

import java.util.ArrayList;
import java.util.SortedMap;

import nl.metaphoric.scorefollower.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * View used to draw a graph of positions and their
 * likelihood, around the current position.
 * @author Elte Hupkes
 */
public class PositionGraphView extends View {
	/**
	 * Log tag
	 */
	private static String TAG = "SF_PositionGraphView";
	
	/**
	 * List of points to draw, generated from the data.
	 */
	private ArrayList<Float> points = new ArrayList<Float>();
	private int min, max, current;
	
	public PositionGraphView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPaint();
	}

	public PositionGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPaint();
	}

	public PositionGraphView(Context context) {
		super(context);
		initPaint();
	}
	
	/**
	 * Initializes a paint instance for drawing.
	 */
	private Paint paint;
	private void initPaint() {
		paint = new Paint();
		paint.setColor(getResources().getColor(R.color.blue));
		paint.setAntiAlias(true);
	}
	
	/**
	 * Receives data to display on the position graph.
	 * This will _not_ be called on the UI-thread, and thus
	 * has to be synchronized (and use postInvalidate() instead
	 * of invalidate()).
	 * @param map
	 */
	public void setData(SortedMap<Integer, Double> map, int current) {
		/*
		 * Parse the data into a list of points
		 * What we need to draw:
		 * - The minimum and maximum point
		 * - The list of points, and their probabilities
		 * 
		 * Luckily we get passed a TreeMap, meaning I can
		 * get all this data at once. I'll copy this
		 * map for later use in the onDraw() method.
		 */
		synchronized(this) {
			this.current = current;
			points.clear();
			min = 99999999; max = 0;
			if (map.size() > 1) {
				for (int k : map.keySet()) {
					if (k < min) {
						min = k;
					}
					if (k > max) {
						max = k;
					}
					
					points.add((float)k);
					points.add(map.get(k).floatValue());
				}
				
				postInvalidate();
			}
		}
	}
	
	/**
	 * Draws the graph to the canvas. This happens on the UI-thread,
	 * as opposed to the setData method. Since we're not sure when
	 * this will be called, synchronize on this instance.
	 */
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		int pad = 5;
		int width = this.getWidth() - 2 * pad, height = this.getHeight() - pad;
		synchronized(this) {
			if (this.getVisibility() == View.VISIBLE && points.size() > 2) {
				/**
				 * We'll draw the points in the visible range, with
				 * the current position in the center. To calculate
				 */
				int xStart = pad + (int)Math.round((0.5 * width)),
					yStart = height;
				int range = Math.max(current - min, max - current);
				double xStep = (0.5 * width) / range;
				int size = points.size();
				
				for (int i = 0; i < size; i += 2) {
					float x = (float)((points.get(i).intValue() - current) * xStep) + xStart,
						  y = yStart - (float)(points.get(i + 1) * height);
					Log.d(TAG, "Drawing point at xpos "+x);
					canvas.drawRect(x - 3, yStart, x + 3, y, paint);
				}
			}
		}
	}
}
