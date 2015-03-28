package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.event.RecognitionManagerEventListener;
import net.nekoinemo.documentrecognition.event.RecognitionResultEventListener;

import java.io.File;

/**
 * Main recognition process class. Operates as a singleton on its own thread.
 * Responsible for the managing the recognition process through keeping a queue of recognition targets and notifying the corresponding event listeners about target being processed.
 */
public interface IRecognitionManager extends Runnable {

	/**
	 * Puts all supported files in the recognition queue.
	 *
	 * @param directory     Directory containing the files to be processed. Only files of the supported types will be added to the queue.
	 * @param eventListener Event listener that should process the recognition result.
	 *                      This event listener will be set for every found file. Files can e differentiated based on their ID (matches the file name).
	 *
	 * @throws InterruptedException
	 */
	void pushAllFiles(File directory, RecognitionResultEventListener eventListener) throws InterruptedException;
	/**
	 * Puts a file in the recognition queue. File type is not checked (can cause and exception if unsupported file is provided).
	 *
	 * @param file          File to be recognized.
	 * @param eventListener Event listener that should process the recognition result.
	 *
	 * @throws InterruptedException
	 */
	void pushFile(File file, RecognitionResultEventListener eventListener) throws InterruptedException;
	/**
	 * Get the size of the current recognition queue.
	 *
	 * @return
	 */
	int getQueueSize();
	/**
	 * Initialize the RecognitionManager. Should be done before executing the start().
	 * Performs test recognition.
	 *
	 * @throws net.nekoinemo.documentrecognition.RecognitionManagerException
	 */
	void init() throws RecognitionManagerException;
	/**
	 * Initialize the RecognitionManager. Should be done before executing the start().
	 *
	 * @param testRecognition Set to false if test recognition step should be skipped. Test recognition will throw an error if Recognized isn't initialized properly.
	 *
	 * @throws net.nekoinemo.documentrecognition.RecognitionManagerException
	 */
	void init(boolean testRecognition) throws RecognitionManagerException;
	/**
	 * Returns the current state of the RecognitionManager
	 *
	 * @return
	 */
	boolean isRunning();
	/**
	 * Set an event listener for the RecognitionManager events
	 *
	 * @param eventListener
	 */
	void setEventListener(RecognitionManagerEventListener eventListener);
	/**
	 * Disconnect the event listener
	 */
	void removeEventListener();
	/**
	 * Sets whenever debug output should be created. Output is stored in the "debug" folder inside the temporary directory.
	 *
	 * @param debugOutput
	 */
	void setDebugOutput(boolean debugOutput) throws RecognitionManagerException;
	/**
	 * Specifies recognition setting that should be used.
	 *
	 * @param recognitionSettings Array of the ordered RecognitionSettings.
	 *
	 * @throws net.nekoinemo.documentrecognition.RecognitionManagerException if RecognitionManager is currently running.
	 */
	void setRecognitionSettings(RecognitionSettings[] recognitionSettings) throws RecognitionManagerException;
	/**
	 * Sets the location of the temporary folder.
	 *
	 * @param temporaryDirectoriesLocation Location where the folder(s) for the temporary files will be created.
	 */
	void setTemporaryDirectoriesLocation(File temporaryDirectoriesLocation) throws RecognitionManagerException;
	/**
	 * Specifies the path to the tessdata directory. Directory should contain language and training data for the used languages.
	 *
	 * @param value Path to the tessdata directory.
	 *
	 * @throws net.nekoinemo.documentrecognition.RecognitionManagerException if RecognitionManager is currently running.
	 */
	void setTessDataPath(String value) throws RecognitionManagerException;
	/**
	 * Starts RecognitionManager in a standby mode, awaiting for the files to process. If files were already put into the queue - begins processing them immediately.
	 * For the RecognitionManager to function correctly it has to be initialized with init() first otherwise this method will throw an error.
	 *
	 * @throws net.nekoinemo.documentrecognition.RecognitionManagerException if RecognitionManager wasn't properly initialized
	 */
	void start() throws RecognitionManagerException;
	/**
	 * Stops RecognitionManager, aborting any pending task in the queue. Queue is cleared uppon stopping.
	 * Doing so will deinitialize the RecognitionManager
	 *
	 * @throws InterruptedException
	 */
	void stop() throws InterruptedException;
}
