package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.RecognizerException;

public interface DocumentData {

	DocumentType getDocumentType();
	int getCompleteness();

	String toString();
	String toString(boolean full);

	public interface DocumentDataBuilder{

		public DocumentData getDocumentData();
		public void ProcessText(String text);
		public int getCompleteness();
		public void FillEmptyFields(DocumentData value) throws RecognizerException;
		public void FillFields(DocumentData value) throws RecognizerException;
	}
}
