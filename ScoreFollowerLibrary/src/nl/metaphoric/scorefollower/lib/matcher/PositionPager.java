
package nl.metaphoric.scorefollower.lib.matcher;

import java.util.Map;
import java.util.SortedMap;

import nl.metaphoric.scorefollower.lib.Position;
import nl.metaphoric.scorefollower.lib.file.ScoreReader.FileSettings;

/**
 * The position pager matches a position to
 * a page and a position on that page, telling
 * the PlayActivity where to move to.
 * 
 * @author Elte Hupkes
 */
public class PositionPager {
	/**
	 * The positions map. This has to be a SortedMap
	 * to force ascending key order.
	 */
	private SortedMap<Integer, Position> positions;
	
	/**
	 * Position trackers
	 */
	private int position = -1, lastPosition = -1;
	
	/**
	 * Used by ScoreReader to create a new PositionPager
	 * @param positions
	 * @param settings
	 */
	public PositionPager(SortedMap<Integer, Position> positions, FileSettings settings) {
		this.positions = positions;
	}
	
	/**
	 * Sets the latest estimated position
	 * @param index
	 */
	public void setEstimate(int index) {
		// Locate the first index smaller than this position, scroll to that
		SortedMap<Integer, Position> s = positions.headMap(index);
		if (s.size() > 0) {
			int newIndex = s.lastKey();
			
			/*
			 * When moving back, require that index
			 * is closer to maxIndex than to the current
			 * position. 
			 */
			boolean validChange = (newIndex >= position) ||
								  (position - index) > (index - newIndex);
							
			if (validChange) {
				lastPosition = position;
				position = newIndex;	
			}
		}
	}
	
	/**
	 * Returns whether the location has changed,
	 * and thus a scroll needs to happen. 
	 * @return
	 */
	public boolean hasChanged() {
		return position != lastPosition;
	}
	
	/**
	 * Returns the position to scroll to.
	 * @return
	 */
	public Position getPosition() {
		if (positions.containsKey(position)) {
			return positions.get(position);
		} else {			
			return new Position(0, 0, 0);
		}
	}
	
	/**
	 * Returns the index of the active position
	 * @return
	 */
	public int getPositionIndex() {
		return position;
	}
	
	/**
	 * Returns a collection view of all available
	 * positions.
	 * @return
	 */
	public Map<Integer, Position> getPositions() {
		return positions;
	}
}
