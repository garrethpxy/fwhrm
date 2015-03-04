package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.RecognizerException;

public interface DocumentData {

	DocumentType getDocumentType();
	int getCompleteness();

	public interface DocumentDataBuilder{

		public DocumentData getDocumentData();
		public void ProcessText(String text);
		public int getCompleteness();
		public void FillEmptyFields(DocumentData value) throws RecognizerException;
	}
}
