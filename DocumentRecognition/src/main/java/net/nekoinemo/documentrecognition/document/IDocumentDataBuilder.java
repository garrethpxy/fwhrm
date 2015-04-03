package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.*;

public interface IDocumentDataBuilder {

	IDocumentData getDocumentData();
	int getCompleteness();

	void processImage(RecognitionManager.RecognitionTarget target, RecognitionSettings[] settings) throws RecognitionManagerException;
	String getDebugText();
}
