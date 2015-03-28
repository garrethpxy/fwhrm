package net.nekoinemo.documentrecognition.event;

import java.util.EventListener;

public interface RecognitionManagerEventListener extends EventListener {

	void systemExceptionOccurred(RecognitionManagerEvent event);
	void recognitionExceptionOccurred(RecognitionManagerEvent event);
	void miscellaneousExceptionOccurred(RecognitionManagerEvent event);
}
