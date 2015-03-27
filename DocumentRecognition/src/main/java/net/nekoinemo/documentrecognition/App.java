package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.IDocumentData;
import net.nekoinemo.documentrecognition.event.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class App {

	public static Path AppLocation;

	public static void main(String[] args) throws URISyntaxException, FileNotFoundException {

		AppLocation = Paths.get(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

		if (args.length != 2) {
			System.out.println("Command line mode. Specify tessdata path as first argument and scans directory as second");
			return;
		}

		ArrayList<IDocumentData> results = new ArrayList<>();

		RecognitionManagerEventListener recognitionManagerEventListener = new RecognitionManagerEventListener() {
			@Override
			public void SystemExceptionOccurred(RecognitionManagerEvent event) {
				System.out.println(new StringBuilder("[System] Exception occurred in RecognitionManager: ").append(event.getMessage()).append('\n').append("Reason: ").append(event.getCause().getMessage()).toString());
				System.out.flush();
			}
			@Override
			public void RecognitionExceptionOccurred(RecognitionManagerEvent event) {
				// No reason to use it in this example
			}
			@Override
			public void MiscellaneousExceptionOccurred(RecognitionManagerEvent event) {
				System.out.println(new StringBuilder("[Misc] Exception occurred in RecognitionManager: ").append(event.getMessage()).append('\n').append("Reason: ").append(event.getCause().getMessage()).toString());
				System.out.flush();
			}
		};
		RecognitionResultEventListener recognitionResultEventListener = new RecognitionResultEventListener() {
			@Override
			public void RecognitionFinished(RecognitionResultEvent event) {

				System.out.println(event.getDocumentID() + '\t' + event.getRecognitionPercentage() + "%\t" + event.getDocumentType());
//				if (event.getDocumentType() == null) System.out.println("Document type wasn't recognized!");
//				else System.out.println(event.getDocumentData().toString());
				synchronized (results){
					results.add(event.getDocumentData());
				}
				System.out.flush();
			}
			@Override
			public void RecognitionError(RecognitionResultEvent event) {

				System.out.println("[Error] " + event.getDocumentID());
				event.getCause().printStackTrace();
				System.out.flush();
			}
		};

		RecognitionManager recognitionManager = RecognitionManager.getInstance();
		try {
			// Initialization of the RecognitionManager. Need to be done once in the initialization of the system
			recognitionManager.setEventListener(recognitionManagerEventListener);
			recognitionManager.setTessDataPath(args[0]);
			recognitionManager.setRecognitionSettings(RecognitionSettings.DEFAULT);
			recognitionManager.setTemporaryDirectoriesLocation(new File(args[1]));
			recognitionManager.setDebugOutput(true);
			recognitionManager.init();

			// Puts recognitionManager in a standby mode, awaiting for the files to process
			recognitionManager.start();

			recognitionManager.PushAllFiles(new File(args[1]), recognitionResultEventListener); // All *supported* files in folder. Supported file extensions are defined in the RecognitionManager class
			int queueSize = recognitionManager.getQueueSize();
			//recognitionManager.PushFile(new File(args[1]), recognitionResultEventListener); // Single file
			//while (recognitionManager.getQueueSize() > 0) Thread.sleep(1000);
			while (results.size()<queueSize) Thread.sleep(1000);

			// Stops the recognitionManager, aborting any queued tasks. Should be done on the system shutdown
			recognitionManager.stop();

			results.forEach(o -> System.out.println(o));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
