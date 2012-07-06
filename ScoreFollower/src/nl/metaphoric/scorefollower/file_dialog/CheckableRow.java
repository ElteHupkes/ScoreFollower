package nl.metaphoric.scorefollower.file_dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * File dialog row that is checkable, so multiple
 * items can be visualized to be selected.
 * @author Elte Hupkes
 *
 */
public class CheckableRow extends RelativeLayout implements Checkable {
	private boolean isChecked;
	
	/**
	 * Required default constructor
	 * @param context
	 */
	public CheckableRow(Context context) {
		super(context);
	}
	
	public CheckableRow(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public CheckableRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public void setChecked(boolean checked) {
		if (checked != isChecked) {			
			isChecked = checked;
			refreshDrawableState();
		}
	}

	public boolean isChecked() {
		return isChecked;
	}

	public void toggle() {
		// TODO Auto-generated method stub
		setChecked(!isChecked);
	}

	/**
	 * @see http://stackoverflow.com/questions/3742979/
	 */
	private static final int[] CheckedStateSet = {
	    android.R.attr.state_checked
	};
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
	    if (isChecked()) {
	        mergeDrawableStates(drawableState, CheckedStateSet);
	    }
	    return drawableState;
	}
}
