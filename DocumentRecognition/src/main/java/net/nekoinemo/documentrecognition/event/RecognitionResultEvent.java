package net.nekoinemo.documentrecognition.event;

import net.nekoinemo.documentrecognition.Recognizer;
import net.nekoinemo.documentrecognition.document.DocumentData;
import net.nekoinemo.documentrecognition.document.DocumentType;

import java.util.EventObject;

/**
 * Created by krdm on 04.03.2015.
 */
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

	public Throwable getCause() {

		return cause;
	}
	public String getDocumentID() {

		return documentID;
	}
	public DocumentData getDocumentData() {

		return documentData;
	}
	public DocumentType getDocumentType() {

		return documentType;
	}
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
