package nl.metaphoric.scorefollower.view;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.TextView;

/**
 * Defines a view for markers
 * @author Elte Hupkes
 *
 */
public class MarkerView extends TextView implements Checkable {
	public MarkerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MarkerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MarkerView(Context context) {
		super(context);
	}

	/**
	 * Current checked state
	 */
	private boolean checked = false;
	
	public void setChecked(boolean checked) {
		if (checked != this.checked) {
			this.checked = checked;
			refreshDrawableState();
		}
	}

	public boolean isChecked() {
		return checked;
	}

	public void toggle() {
		setChecked(!checked);
	}
	
	/**
	 * @see http://stackoverflow.com/questions/3742979/
	 */
	private static final int[] CheckedStateSet = {
	    R.attr.state_checked
	};
	@Override
	public int[] onCreateDrawableState(int extraSpace) {
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
	    if (isChecked()) {
	        mergeDrawableStates(drawableState, CheckedStateSet);
	    }
	    return drawableState;
	}
}
