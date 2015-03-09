package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.event.RecognitionResultEvent;

import java.util.EventListener;

public interface RecognitionResultEventListener extends EventListener {

	public void RecognitionFinished(RecognitionResultEvent event);
	public void RecognitionError(RecognitionResultEvent event);
}
