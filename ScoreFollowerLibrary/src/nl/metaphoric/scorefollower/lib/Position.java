package nl.metaphoric.scorefollower.lib;

/**
 * Class that stores item positions
 * @author Elte Hupkes
 */
public class Position {
	private double x, y;
	private int page;
	
	public Position(int page, double xFrac, double yFrac) {
		this.page = page;
		x = xFrac;
		y = yFrac;
	}
	
	public double xFrac() { return x; }
	public double yFrac() { return y; }
	public int page() { return page; }
	public void page(int page) { this.page = page; }
	
	public String toString() {
		return page + " " + x + " " + y;
	}
}
