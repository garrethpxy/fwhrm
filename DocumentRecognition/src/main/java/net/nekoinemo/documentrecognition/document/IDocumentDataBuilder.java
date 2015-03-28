package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.RecognitionSettings;
import net.nekoinemo.documentrecognition.RecognitionManagerException;

import java.io.File;

public interface IDocumentDataBuilder {

	public IDocumentData getDocumentData();
	public void processImage(File target, RecognitionSettings settings) throws RecognitionManagerException;
	public int getCompleteness();
	public void fillEmptyFields(IDocumentData value) throws RecognitionManagerException;
	public void fillFields(IDocumentData value) throws RecognitionManagerException;
}
