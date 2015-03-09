package net.nekoinemo.documentrecognition;

public class RecognizerException extends Exception {

	public RecognizerException(String message) {
		super(message);
	}
	public RecognizerException(String message, Throwable cause) {
		super(message, cause);
	}
	public RecognizerException(Throwable cause) {
		super(cause);
	}
}
