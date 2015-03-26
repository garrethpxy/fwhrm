package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.RecognitionSettings;
import net.nekoinemo.documentrecognition.RecognizerException;

import java.io.File;

public interface DocumentDataBuilder {

	public DocumentData getDocumentData();
	public void ProcessImage(File target, RecognitionSettings settings) throws RecognizerException;
	public int getCompleteness();
	public void FillEmptyFields(DocumentData value) throws RecognizerException;
	public void FillFields(DocumentData value) throws RecognizerException;
}
