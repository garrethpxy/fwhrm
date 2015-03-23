package net.nekoinemo.documentrecognition.event;

import java.util.EventListener;

public interface RecognitionResultEventListener extends EventListener {

	/**
	 * Fired upon successful recognition.
	 *
	 * @param event Event, containing all of the recognized data.
	 */
	public void RecognitionFinished(RecognitionResultEvent event);
	/**
	 * Fired upon the error in the recognition process.
	 *
	 * @param event Event, containing the exception cause.
	 */
	public void RecognitionError(RecognitionResultEvent event);
}
