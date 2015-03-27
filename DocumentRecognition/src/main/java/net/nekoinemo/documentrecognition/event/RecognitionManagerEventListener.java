package net.nekoinemo.documentrecognition.event;

import java.util.EventListener;

public interface RecognitionManagerEventListener extends EventListener {

	void SystemExceptionOccurred(RecognitionManagerEvent event);
	void RecognitionExceptionOccurred(RecognitionManagerEvent event);
	void MiscellaneousExceptionOccurred(RecognitionManagerEvent event);
}
