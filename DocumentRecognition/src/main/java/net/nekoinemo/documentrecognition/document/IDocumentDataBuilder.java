package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.RecognitionSettings;
import net.nekoinemo.documentrecognition.RecognitionManagerException;

import java.io.File;

public interface IDocumentDataBuilder {

	public IDocumentData getDocumentData();
	public void ProcessImage(File target, RecognitionSettings settings) throws RecognitionManagerException;
	public int getCompleteness();
	public void FillEmptyFields(IDocumentData value) throws RecognitionManagerException;
	public void FillFields(IDocumentData value) throws RecognitionManagerException;
}
