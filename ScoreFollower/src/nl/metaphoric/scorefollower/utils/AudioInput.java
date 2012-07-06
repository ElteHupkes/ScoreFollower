package nl.metaphoric.scorefollower.utils;

/**
 * Interface for Audio / Microphone readers
 * @author Elte Hupkes
 */
public interface AudioInput {
	public void startRecorder();
	public void stopRecorder();
	public void pauseRecorder();
	public boolean isActive();
}
