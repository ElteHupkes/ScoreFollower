package nl.metaphoric.scorefollower.lib;

import nl.metaphoric.scorefollower.lib.feature.FrameVector;

/**
 * The AnalyzeListener defines a callback listener interface for an
 * AudioAnalyzer.
 * 
 * @author Elte Hupkes
 */
public interface AnalyzeListener {
	public void onNewAnalysisData(FrameVector a);
}
