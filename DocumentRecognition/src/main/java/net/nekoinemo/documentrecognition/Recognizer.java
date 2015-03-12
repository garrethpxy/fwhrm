package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.DocumentData;
import net.nekoinemo.documentrecognition.document.DocumentType;
import net.nekoinemo.documentrecognition.event.RecognitionResultEvent;
import net.nekoinemo.documentrecognition.event.RecognitionResultEventListener;
import net.sourceforge.tess4j.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Recognizer implements Runnable {

	private static final FileFilter SUPPORTED_FILES_FILTER = new FileFilter() {

		private final ArrayList SUPPORTED_EXTENSIONS = new ArrayList() {{
			add(".pdf");
			add(".jpg");
			add(".jpeg");
			add(".png");
		}};

		@Override
		public boolean accept(File pathname) {

			if (!pathname.isFile()) return false;

			String extension = pathname.getName().substring(pathname.getName().toLowerCase().lastIndexOf('.'));
			return SUPPORTED_EXTENSIONS.contains(extension);
		}
	};

	private static Recognizer instance = null;

	private Thread thread;
	private boolean isRunning = false;

	private final LinkedBlockingQueue<RecognitionTarget> targets;
	private RecognitionSettings[] recognitionSettings = RecognitionSettings.DEFAULT;
	private final Tesseract tesseract;

	private File debugOutputDirectory = null;

	public static Recognizer getInstance() {

		if (instance == null) instance = new Recognizer();
		return instance;
	}

	private Recognizer() {

		tesseract = Tesseract.getInstance();
		tesseract.setLanguage("eng");

		targets = new LinkedBlockingQueue<>();
	}

	public synchronized void Init() throws RecognizerException {

		Init(true);
	}
	public synchronized void Init(boolean testRecognition) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Recognizer is running!");

		if (debugOutputDirectory != null) {
			if (!debugOutputDirectory.exists() || !debugOutputDirectory.isDirectory())
				if (!debugOutputDirectory.mkdirs()) {
					//todo event failed to create debug directory
					debugOutputDirectory = null;
				}
		}

		BufferedImage testImage;
		try {
			testImage = ImageIO.read(Recognizer.class.getResourceAsStream("testText.png"));
		} catch (IOException e) {
			throw new RecognizerException("Can't load test image.", e);
		}

		tesseract.setOcrEngineMode(TessAPI.TessOcrEngineMode.OEM_DEFAULT);
		tesseract.setPageSegMode(TessAPI.TessPageSegMode.PSM_SINGLE_WORD);
		tesseract.setHocr(false);

		String result;
		try {
			result = tesseract.doOCR(testImage);
		} catch (TesseractException e) {
			throw new RecognizerException("Can't do recognition on a test image. Check if tessdata path is specified correctly and proper language files are present", e);
		}
		if (testRecognition && !result.trim().equalsIgnoreCase("test"))
			throw new RecognizerException("Tesseract failed to recognize test image. Correct language/trained data may be missing from tessdata folder. Continuing with current settings may yeld low quality recognition results.");

		tesseract.setHocr(true);
	}

	public boolean isRunning() {

		return isRunning;
	}
	public int getQueueSize() {

		return targets.size();
	}

	public void setTessDataPath(String value) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Recognizer is running!");

		tesseract.setDatapath(value);
	}
	public void setDebugOutputDirectory(File debugOutputDirectory) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Recognizer is running!");

		this.debugOutputDirectory = debugOutputDirectory;
	}
	public RecognitionSettings[] getRecognitionSettings() {

		return recognitionSettings;
	}
	public void setRecognitionSettings(RecognitionSettings[] recognitionSettings) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Recognizer is running!");

		this.recognitionSettings = recognitionSettings;
	}
	public void PushAllFiles(File directory, RecognitionResultEventListener eventListener) throws InterruptedException {

		for (File file : directory.listFiles(SUPPORTED_FILES_FILTER)) {
			PushFile(file, eventListener);
		}
	}
	public void PushFile(File file, RecognitionResultEventListener eventListener) throws InterruptedException {

		targets.put(new RecognitionTarget(file.getName(), file, eventListener));
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
				Recognize(targets.take(), recognitionSettings);
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

		OutputStreamWriter debugWriter = null;
		if (debugOutputDirectory != null) {
			try {
				debugWriter = new OutputStreamWriter(new FileOutputStream(new File(debugOutputDirectory, target.getId() + ".txt")));
			} catch (IOException e) {
				//todo failed to create debug file;
			}
		}

		for (int i = 0; i < recognitionSettings.length; i++) {
			RecognitionResult result = new RecognitionResult(recognitionSettings[i]);
			recognitionResults.add(i, result);

			try {
				result.sethOCR(Jsoup.parse(RecognizeTarget(target, result.settings)));
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

			// Debug output
			if (debugWriter != null) {
				try {
					debugWriter.write("iteration: " + i + "\tcompleteness: " + result.getDocumentDataBuilder().getCompleteness() + '\n');
					debugWriter.write(result.getSettings().toString() + '\n');
					debugWriter.write(result.getDocumentDataBuilder().getDocumentData().toString(true) + '\n');
					debugWriter.write('\n');
					debugWriter.write(result.gethOCR().outerHtml() + '\n');
					debugWriter.write('\n');
					debugWriter.write(result.getRawText() + '\n');
					debugWriter.write('\n');
					debugWriter.flush();

					if (i == recognitionSettings.length - 1) debugWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

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

		tesseract.setOcrEngineMode(recognitionSettings.getEngineMode());
		tesseract.setPageSegMode(recognitionSettings.getPageSegMode());

		try {
			File file = target.getFile();
			return tesseract.doOCR(file);
		} catch (TesseractException e) {
			throw new RecognizerException(e);
		}
	}

	private class RecognitionResult {

		private RecognitionSettings settings;
		private Document hOCR = null;
		private String rawText = null;
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
		public Document gethOCR() {

			return hOCR;
		}
		public void sethOCR(Document hOCR) {

			this.hOCR = hOCR;

			StringBuilder stringBuilder = new StringBuilder();
			for (Element ocr_par : hOCR.getElementsByClass("ocr_line")) {
				stringBuilder.append(ocr_par.text() + '\n');
			}
			rawText = stringBuilder.toString();
		}
		public DocumentData.DocumentDataBuilder getDocumentDataBuilder() {

			return documentDataBuilder;
		}
		public void setDocumentDataBuilder(DocumentData.DocumentDataBuilder documentDataBuilder) {

			this.documentDataBuilder = documentDataBuilder;
		}
	}
	private class RecognitionTarget {

		private final String id;
		private final File file;
		private final RecognitionResultEventListener eventListener;

		public RecognitionTarget(String id, File file, RecognitionResultEventListener eventListener) {

			this.id = id;
			this.file = file;
			this.eventListener = eventListener;
		}

		public String getId() {

			return id;
		}
		public File getFile() throws RecognizerException {

			return file;
		}
		public RecognitionResultEventListener getEventListener() {

			return eventListener;
		}
	}
}
