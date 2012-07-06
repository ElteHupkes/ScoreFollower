package nl.metaphoric.scorefollower.lib;

/**
 * Circular buffer implementation to store
 * audio data.
 * 
 * In terms of what is described at the top in the
 * class description, this works as follows (^ denotes
 * the current position pointer):
 * [x x x x] + [a] = [a x x x]
 *  ^                   ^
 * [a x x x] + [b] = [a b x x]
 *  ^                     ^
 * etc.
 * 
 * @author Elte Hupkes
 */
public class AudioBuffer {
	/**
	 * The data array
	 */
	private short[] data;
	
	/**
	 * The size and head of the buffer
	 */
	private int size, pos = 0;
	
	/**
	 * The length of the current buffer contents,
	 * maximum value is the buffer size.
	 */
	private int len = 0;
	
	/**
	 * Creates a new buffer of the given size
	 * @param size
	 */
	public AudioBuffer(int size) {
		data = new short[size];
		this.size = size;
	}
	
	/**
	 * Puts a set of shorts
	 * @param in
	 */
	public void put(short[] in) {
		for (int i = 0; i < in.length; i++) {
			data[pos % size] = in[i];
			pos++;
		}
		len = (in.length + len);
		if (len > size) {
			len = size;
		}
		pos %= size;
	}

	
	/**
	 * Returns the item at the given index
	 * @param index
	 * @return
	 */
	public short get(int index) {
		return data[(pos + index) % size];
	}
	
	/**
	 * @return True if the buffer has wrapped at least once.
	 */
	public boolean full() {
		return len >= size;
	}
	
	/**
	 * Get the size of this buffer
	 * @return
	 */
	public int size() {
		return size;
	}
	
	/**
	 * Clears the buffer
	 */
	public void clear() {
		pos = 0;
		len = 0;
	}
	
	public String toString() {
		String s = "["+get(0);
		for (int i = 1; i < size; i++) {
			s += " "+get(i);
		}
		return s +"]";
	}
}
