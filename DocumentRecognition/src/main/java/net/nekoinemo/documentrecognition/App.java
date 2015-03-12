package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.event.RecognitionResultEvent;
import net.nekoinemo.documentrecognition.event.RecognitionResultEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App {

	public static Path AppLocation;

	public static void main(String[] args) throws URISyntaxException, FileNotFoundException {

		AppLocation = Paths.get(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

		if (args.length != 2)
			System.out.println("Command line mode. Specify tessdata path as first argument and scans directory as second");

		RecognitionResultEventListener eventListener = new RecognitionResultEventListener() {
			@Override
			public void RecognitionFinished(RecognitionResultEvent event) {

				System.out.println(event.getDocumentID() + '\t' + event.getRecognitionPercentage() + "%\t" + event.getDocumentType());
				if (event.getDocumentType() == null) System.out.println("Document type wasn't recognized!");
				else System.out.println(event.getDocumentData().toString());
				System.out.flush();
			}
			@Override
			public void RecognitionError(RecognitionResultEvent event) {

				System.out.println("[Error] " + event.getDocumentID());
				event.getCause().printStackTrace();
				System.out.flush();
			}
		};

		Recognizer recognizer = Recognizer.getInstance();
		try {
			recognizer.setTessDataPath(args[0]);
			recognizer.setDebugOutputDirectory(new File(args[1]));
			recognizer.Init();

			recognizer.Start();
			recognizer.PushAllFiles(new File(args[1]), eventListener);
			while (recognizer.getQueueSize() > 0) Thread.sleep(1000);

			recognizer.Stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
