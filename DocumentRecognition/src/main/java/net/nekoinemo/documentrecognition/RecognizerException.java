package net.nekoinemo.documentrecognition;

/**
 * Created by krdm on 04.03.2015.
 */
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
