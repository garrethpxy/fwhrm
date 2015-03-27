package net.nekoinemo.documentrecognition.event;

import net.nekoinemo.documentrecognition.RecognitionManager;

import java.util.EventObject;

public class RecognitionManagerEvent extends EventObject{

	private Throwable cause = null;
	private String message = "";
	private String fileCause = null;

	private RecognitionManagerEvent() {

		super(RecognitionManager.getInstance());
	}

	/**
	 * Returns the Throwable cause of the event being fired (Only set if event was fired because of an error).
	 *
	 * @return
	 */
	public Throwable getCause() {

		return cause;
	}
	/**
	 * Get message accompanying the event.
	 *
	 * @return
	 */
	public String getMessage() {

		return message;
	}
	/**
	 * Get the file processing which caused the event
	 *
	 * @return
	 */
	public String getFileCause() {

		return fileCause;
	}

	public static class RecognitionManagerEventBuilder {

		private RecognitionManagerEvent event;

		public RecognitionManagerEventBuilder() {

			event = new RecognitionManagerEvent();
		}

		public RecognitionManagerEvent getEvent(){

			return event;
		}

		public RecognitionManagerEventBuilder setCause(Throwable cause) {

			event.cause = cause;
			return this;
		}
		public RecognitionManagerEventBuilder setMessage(String message) {

			event.message = message;
			return this;
		}
		public RecognitionManagerEventBuilder setFileCause(String fileCause) {

			event.fileCause = fileCause;
			return this;
		}
	}
}
