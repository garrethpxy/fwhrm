package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.RecognizerException;

public interface DocumentData {

	/**
	 * Returns the type of the document.
	 *
	 * @return
	 */
	DocumentType getDocumentType();
	/**
	 * Returns the percentage of the fields which contains recognized data.
	 *
	 * @return
	 */
	int getCompleteness();

	String toString();
	/**
	 *
	 *
	 * @param full If true - lists all of the fields, even ones that are empty.
	 * @return
	 */
	String toString(boolean full);

	public interface DocumentDataBuilder{

		public DocumentData getDocumentData();
		public void ProcessText(String text);
		public int getCompleteness();
		public void FillEmptyFields(DocumentData value) throws RecognizerException;
		public void FillFields(DocumentData value) throws RecognizerException;
	}
}
