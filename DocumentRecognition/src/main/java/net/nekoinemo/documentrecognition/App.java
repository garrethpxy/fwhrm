package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.IDocumentData;
import net.nekoinemo.documentrecognition.event.*;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class App {

	public static Path AppLocation;

	public static void main(String[] args) throws URISyntaxException, FileNotFoundException {

		if (args.length == 1){
			try {
				File file = new File(args[0]);
				BufferedImage image = ImageIO.read(file);
				image = ImageHelper.deskewImage(image);
				ImageIO.write(image, "png", new File(file.getParentFile(), FilenameUtils.removeExtension(file.getName()) + "_deskewed.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}

			return;
		}

		AppLocation = Paths.get(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

		if (args.length != 2) {
			System.out.println("Command line mode. Specify tessdata path as first argument and scans directory as second");
			return;
		}

		HashMap<String, IDocumentData> results = new HashMap<>();

		// Event listener for the RecognitionManager itself. Will notify about exceptions occurred
		RecognitionManagerEventListener recognitionManagerEventListener = new RecognitionManagerEventListener() {
			@Override
			public void systemExceptionOccurred(RecognitionManagerEvent event) {

				System.out.println(new StringBuilder("[System] Exception occurred in RecognitionManager: ").append(event.getMessage()).append('\n').append("Reason: ").append(event.getCause().getMessage()).toString());
				System.out.flush();
			}
			@Override
			public void recognitionExceptionOccurred(RecognitionManagerEvent event) {
				// No reason to use it in this example
			}
			@Override
			public void miscellaneousExceptionOccurred(RecognitionManagerEvent event) {

				System.out.println(new StringBuilder("[Misc] Exception occurred in RecognitionManager: ").append(event.getMessage()).append('\n').append("Reason: ").append(event.getCause().getMessage()).toString());
				System.out.flush();
			}
		};
		// Event listener for the batch of files pushed to the RecognitionManager
		RecognitionResultEventListener recognitionResultEventListener = new RecognitionResultEventListener() {
			@Override
			public void recognitionFinished(RecognitionResultEvent event) {

				System.out.println(event.getDocumentID() + '\t' + event.getRecognitionPercentage() + "%\t" + event.getDocumentType());
				//				if (event.getDocumentType() == null) System.out.println("Document type wasn't recognized!");
				//				else System.out.println(event.getDocumentData().toString());
				synchronized (results) {
					results.put(event.getDocumentID(), event.getDocumentData());
				}
				System.out.flush();
			}
			@Override
			public void recognitionError(RecognitionResultEvent event) {

				System.out.println("[Error] " + event.getDocumentID());
				event.getCause().printStackTrace();
				System.out.flush();
			}
		};

		// RecognitionManager instance stored in the interface reference to hide the auxiliary methods not related to the user API
		IRecognitionManager recognitionManager = RecognitionManager.INSTANCE;
		try {
			// Initialization of the RecognitionManager. Need to be done once in the initialization of the system
			recognitionManager.setEventListener(recognitionManagerEventListener); // Setting the event listener for the RecognitionManager class. Can be skipped
			recognitionManager.setTessDataPath(args[0]); // Setting the path to the directory, containing language data for the Tesseract
			recognitionManager.setRecognitionSettings(RecognitionSettings.DEFAULT); // Setting the recognition settings set, defining the desired recognition quality. Can be skipped (RecognitionSettings.DEFAULT will be used by default)
			recognitionManager.setTemporaryDirectoriesLocation(new File(args[1])); // Setting the directory which would be used for the temporary files of RecognitionManager
			recognitionManager.setDebugOutput(true); // Setting whenever debug data should be outputted in the \debug folder in the temporary directory

			recognitionManager.init(); // Initializes RecognitionManager (will check if everything is ready and works correctly)
			recognitionManager.start(); // Puts recognitionManager in a standby mode, awaiting for the files to process

			recognitionManager.pushAllFiles(new File(args[1]), recognitionResultEventListener); // All *supported* files in folder. Supported file extensions are defined in the RecognitionManager class

			// Wait till all pushed files are processed
			int queueSize = recognitionManager.getQueueSize();
			while (results.size() < queueSize) Thread.sleep(1000);

			recognitionManager.stop(); // Stops the recognitionManager, aborting any queued tasks. Should be done on the system shutdown

			//results.forEach((docID, docData) -> System.out.println(docID + " " + docData)); // In this example all returned data was stored in the map, now it's printed
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
