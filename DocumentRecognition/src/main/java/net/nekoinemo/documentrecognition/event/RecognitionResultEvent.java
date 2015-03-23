package net.nekoinemo.documentrecognition.event;

import net.nekoinemo.documentrecognition.Recognizer;
import net.nekoinemo.documentrecognition.document.DocumentData;
import net.nekoinemo.documentrecognition.document.DocumentType;

import java.util.EventObject;

public class RecognitionResultEvent extends EventObject {

	private final String documentID;
	private DocumentData documentData = null;
	private DocumentType documentType = null;
	private int recognitionPercentage = 0;
	private Throwable cause = null;

	private RecognitionResultEvent(String documentID) {

		super(Recognizer.getInstance());

		this.documentID = documentID;
	}

	/**
	 * Returns the Throwable cause of the event being fired (Only set if event was fired because of an recognition error).
	 *
	 * @return
	 */
	public Throwable getCause() {

		return cause;
	}
	/**
	 * Returns an ID of the target.
	 *
	 * @return Target ID. For the files it matches the file name with the extension.
	 */
	public String getDocumentID() {

		return documentID;
	}
	/**
	 * Returns the recognized data of the document.
	 * To access the fields result should be casted to the appropriate class based on type which can be obtained via event.getDocumentType().
	 *
	 * @return Class implementing the DocumentData interface.
	 */
	public DocumentData getDocumentData() {

		return documentData;
	}
	/**
	 * Returns the Type of a recognized document.
	 *
	 * @return Type of a recognized document or null if type was not recognized.
	 */
	public DocumentType getDocumentType() {

		return documentType;
	}
	/**
	 * Returns the percentage of the fields which contains recognized data.
	 *
	 * @return
	 */
	public int getRecognitionPercentage() {

		return recognitionPercentage;
	}

	public static class RecognitionResultEventBuilder{

		private RecognitionResultEvent event;

		public RecognitionResultEventBuilder(String documentID) {

			event = new RecognitionResultEvent(documentID);
		}
		public RecognitionResultEventBuilder(String documentID, Throwable cause) {

			event = new RecognitionResultEvent(documentID);
			event.cause = cause;
		}

		public RecognitionResultEvent getEvent(){

			return event;
		}

		public RecognitionResultEventBuilder setDocumentData(DocumentData documentData) {

			event.documentData = documentData;
			return this;
		}
		public RecognitionResultEventBuilder setDocumentType(DocumentType documentType) {

			event.documentType = documentType;
			return this;
		}
		public RecognitionResultEventBuilder setRecognitionPercentage(int recognitionPercentage) {

			event.recognitionPercentage = recognitionPercentage;
			return this;
		}
		public RecognitionResultEventBuilder setCause(Throwable cause) {

			event.cause = cause;
			return this;
		}
	}
}
