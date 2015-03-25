package net.nekoinemo.documentrecognition.document;

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
}
