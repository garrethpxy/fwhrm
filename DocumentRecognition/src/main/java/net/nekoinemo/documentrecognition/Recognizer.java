package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.DocumentData;
import net.nekoinemo.documentrecognition.document.DocumentType;
import net.nekoinemo.documentrecognition.event.RecognitionResultEvent;
import net.sourceforge.tess4j.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Recognizer implements Runnable {

	private static final FileFilter SUPPORTED_FILES_FILTER = new FileFilter() {

		private final ArrayList SUPPORTED_EXTENSIONS = new ArrayList() {{
			add(".pdf");
		}};

		@Override
		public boolean accept(File pathname) {

			if (!pathname.isFile()) return false;

			String extension = pathname.getName().substring(pathname.getName().lastIndexOf('.'));
			return SUPPORTED_EXTENSIONS.contains(extension);
		}
	};

	private static Recognizer instance = null;

	private Thread thread;
	private boolean isRunning = false;

	private final LinkedBlockingQueue<RecognitionTarget> targets;
	private final Tesseract tesseract;

	public static Recognizer getInstance() {

		if (instance == null) instance = new Recognizer();
		return instance;
	}

	private Recognizer() {

		tesseract = Tesseract.getInstance();
		targets = new LinkedBlockingQueue<>();
	}

	public synchronized void Init() throws RecognizerException {

		Init(true);
	}
	public synchronized void Init(boolean testRecognition) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Can't initialize an active Recognizer!");

		BufferedImage testImage;
		try {
			testImage = ImageIO.read(Recognizer.class.getResourceAsStream("testText.png"));
		} catch (IOException e) {
			throw new RecognizerException("Can't load test image.", e);
		}

		tesseract.setOcrEngineMode(TessAPI.TessOcrEngineMode.OEM_DEFAULT);
		tesseract.setPageSegMode(TessAPI.TessPageSegMode.PSM_SINGLE_WORD);

		String result;
		try {
			result = tesseract.doOCR(testImage);
		} catch (TesseractException e) {
			throw new RecognizerException("Can't do recognition on a test image. Check if tessdata path is specified correctly and proper language files are present", e);
		}
		if (testRecognition && !result.trim().equalsIgnoreCase("test"))
			throw new RecognizerException("Tesseract failed to recognize test image. Correct language/trained data may be missing from tessdata folder. Continuing with current settings may yeld low quality recognition results.");
	}

	public boolean isRunning() {

		return isRunning;
	}
	public int getQueueSize() {

		return targets.size();
	}

	public void setTessDataPath(String value) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Can't change tessdata path of an active Recognizer!");

		tesseract.setDatapath(value);
	}

	public void PushAllFiles(File directory, RecognitionResultEventListener eventListener) throws InterruptedException {

		for (File file : directory.listFiles(SUPPORTED_FILES_FILTER)) {
			PushFile(file, eventListener);
		}
	}
	public void PushFile(File file, RecognitionResultEventListener eventListener) throws InterruptedException {

		targets.put(new RecognitionTarget(file.getName(), file, false, eventListener));
	}
	public void PushImage(String id, BufferedImage image, RecognitionResultEventListener eventListener) throws InterruptedException {

		targets.put(new RecognitionTarget(id, image, eventListener));
	}

	public synchronized void Start() throws RecognizerException {

		if (isRunning) return;

		isRunning = true;
		thread = new Thread(this, "Recognizer thread");
		thread.start();
	}
	public synchronized void Stop() throws InterruptedException {

		if (!isRunning) return;

		isRunning = false;
		thread.join();
	}

	@Override
	public void run() {

		while (isRunning) {
			try {
				Recognize(targets.take(), RecognitionSettings.DEFAULT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (RecognizerException e) {
				System.out.println(e);
				//todo notify recognizer listener
			}
		}

		if (targets.size() > 0) {
			targets.forEach(target -> target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId(), new RecognizerException("Recognition process was aborted")).getEvent()));
			targets.clear();
		}
	}

	private void Recognize(RecognitionTarget target, RecognitionSettings[] recognitionSettings) throws RecognizerException {

		ArrayList<RecognitionResult> recognitionResults = new ArrayList<>(recognitionSettings.length);
		DocumentType documentType = null;

		for (int i = 0; i < recognitionSettings.length; i++) {
			RecognitionResult result = new RecognitionResult(recognitionSettings[i]);
			recognitionResults.add(i, result);

			tesseract.setOcrEngineMode(result.getSettings().getEngineMode());
			tesseract.setPageSegMode(result.getSettings().getPageSegMode());

			String rawText;
			try {
				result.setRawText(RecognizeTarget(target, result.settings));
			} catch (RecognizerException e) {
				target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId(), e).getEvent());
				throw new RecognizerException("Error recognizing image \"" + target.getId() + '\"', e);
			}

			if (documentType == null) {
				float bestMatch = 0f;

				for (DocumentType type : DocumentType.values()) {
					float match = type.MatchText(result.getRawText());
					if (match > bestMatch) {
						documentType = type;
						bestMatch = match;
					}
				}

				if (documentType == null) continue;
			}

			result.setDocumentDataBuilder(documentType.getBuilder());
			result.getDocumentDataBuilder().ProcessText(result.getRawText());
			if (i > 0) try {
				result.getDocumentDataBuilder().FillEmptyFields(recognitionResults.get(i - 1).getDocumentDataBuilder().getDocumentData());
			} catch (RecognizerException e) {}

			if (result.getDocumentDataBuilder().getCompleteness() >= result.getSettings().getPassingCompliteness())
				break;
		}

		RecognitionResultEvent.RecognitionResultEventBuilder eventBuilder = new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(documentType);
		if (documentType != null) {
			DocumentData documentData = recognitionResults.get(recognitionResults.size() - 1).getDocumentDataBuilder().getDocumentData();
			eventBuilder.setDocumentData(documentData);
			eventBuilder.setRecognitionPercentage(documentData.getCompleteness());
		}
		target.getEventListener().RecognitionFinished(eventBuilder.getEvent());
	}
	private String RecognizeTarget(RecognitionTarget target, RecognitionSettings recognitionSettings) throws RecognizerException {

		if (target.isImage()) {
			try {
				BufferedImage image = target.getImage();
				return tesseract.doOCR(image);
			} catch (TesseractException e) {
				throw new RecognizerException(e);
			}
		} else {
			try {
				File file = target.getFile();
				return tesseract.doOCR(file);
			} catch (TesseractException e) {
				throw new RecognizerException(e);
			}
		}
	}

	private class RecognitionResult {

		private RecognitionSettings settings;
		private String rawText;
		private DocumentData.DocumentDataBuilder documentDataBuilder = null;

		public RecognitionResult(RecognitionSettings settings) {

			this.settings = settings;
		}

		public RecognitionSettings getSettings() {

			return settings;
		}
		public String getRawText() {

			return rawText;
		}
		public void setRawText(String rawText) {

			this.rawText = rawText;
		}
		public DocumentData.DocumentDataBuilder getDocumentDataBuilder() {

			return documentDataBuilder;
		}
		public void setDocumentDataBuilder(DocumentData.DocumentDataBuilder documentDataBuilder) {

			this.documentDataBuilder = documentDataBuilder;
		}
	}
	private class RecognitionTarget {

		private String id;
		private BufferedImage image = null;
		private File file = null;
		private RecognitionResultEventListener eventListener;
		private boolean isImage;

		public RecognitionTarget(String id, BufferedImage image, RecognitionResultEventListener eventListener) {

			this.id = id;
			this.image = image;
			this.eventListener = eventListener;
		}
		public RecognitionTarget(String id, File file, boolean treatAsImage, RecognitionResultEventListener eventListener) {

			this.id = id;
			this.file = file;
			this.eventListener = eventListener;
			this.isImage = treatAsImage;
		}

		public String getId() {

			return id;
		}
		public BufferedImage getImage() throws RecognizerException {

			if (!isImage) throw new RecognizerException("Target is not an image.  " + file.getName());

			if (image == null) {
				try {
					image = ImageIO.read(file);
					if (image == null) throw new RecognizerException("Couldn't load image from file " + file.getName());
				} catch (IOException e) {
					throw new RecognizerException("Couldn't load image from file " + file.getName(), e);
				}
			}

			return image;
		}
		public File getFile() throws RecognizerException {

			if (file == null) throw new RecognizerException("Target is not a file.  " + file.getName());

			return file;
		}
		public RecognitionResultEventListener getEventListener() {

			return eventListener;
		}
		public boolean isImage() {

			return isImage;
		}
	}
}
