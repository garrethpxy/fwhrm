package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.*;
import net.nekoinemo.documentrecognition.event.RecognitionResultEvent;
import net.nekoinemo.documentrecognition.event.RecognitionResultEventListener;
import net.sourceforge.tess4j.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import java.awt.*;
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
			add(".gif");
			add(".bmp");
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

	/**
	 * Returns an instance of a class.
	 *
	 * @return
	 */
	public static Recognizer getInstance() {

		if (instance == null) instance = new Recognizer();
		return instance;
	}

	private Recognizer() {

		tesseract = Tesseract.getInstance();
		tesseract.setLanguage("eng");

		targets = new LinkedBlockingQueue<>();
	}

	/**
	 * Initialize the Recognizer. Should be done before executing the Start().
	 * Performs test recognition.
	 *
	 * @throws RecognizerException
	 */
	public synchronized void Init() throws RecognizerException {

		Init(true);
	}
	/**
	 * Initialize the Recognizer. Should be done before executing the Start().
	 *
	 * @param testRecognition Set to false if test recognition step should be skipped. Test recognition will throw an error if Recognized isn't initialized properly.
	 *
	 * @throws RecognizerException
	 */
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

	/**
	 * Specifies the path to the tessdata directory. Directory should contain language and training data for the used languages.
	 *
	 * @param value Path to the tessdata directory.
	 *
	 * @throws RecognizerException if Recognizer is currently running.
	 */
	public void setTessDataPath(String value) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Recognizer is running!");

		tesseract.setDatapath(value);
	}
	/**
	 * Specifies the directory for the debug output. if none specified - there will be no debug output.
	 * Debug output file has file name of target ID + ".txt" and contains results (found data, raw text, hOCR text) of each recognition iteration for this target.
	 * Debug output shouldn't be active in a normal circumstantials as it performs a lot of (unnecessary) write operations to the hard drive.
	 *
	 * @param debugOutputDirectory Path to the debug output directory.
	 *
	 * @throws RecognizerException if Recognizer is currently running.
	 */
	public void setDebugOutputDirectory(File debugOutputDirectory) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Recognizer is running!");

		this.debugOutputDirectory = debugOutputDirectory;
	}
	/**
	 * Returns current recognition settings.
	 *
	 * @return
	 */
	public RecognitionSettings[] getRecognitionSettings() {

		return recognitionSettings;
	}
	/**
	 * Specifies recognition setting that should be used.
	 *
	 * @param recognitionSettings Array of the ordered RecognitionSettings.
	 *
	 * @throws RecognizerException if Recognizer is currently running.
	 */
	public void setRecognitionSettings(RecognitionSettings[] recognitionSettings) throws RecognizerException {

		if (isRunning) throw new RecognizerException("Recognizer is running!");

		this.recognitionSettings = recognitionSettings;
	}
	/**
	 * Puts all supported files in the recognition queue.
	 *
	 * @param directory     Directory containing the files to be processed. Only files of the supported types will be added to the queue.
	 * @param eventListener Event listener that should process the recognition result.
	 *                      This event listener will be set for every found file. Files can e differentiated based on their ID (matches the file name).
	 *
	 * @throws InterruptedException
	 */
	public void PushAllFiles(File directory, RecognitionResultEventListener eventListener) throws InterruptedException {

		for (File file : directory.listFiles(SUPPORTED_FILES_FILTER)) {
			PushFile(file, eventListener);
		}
	}
	/**
	 * Puts a file in the recognition queue. File type is not checked (can cause and exception if unsupported file is provided).
	 *
	 * @param file          File to be recognized.
	 * @param eventListener Event listener that should process the recognition result.
	 *
	 * @throws InterruptedException
	 */
	public void PushFile(File file, RecognitionResultEventListener eventListener) throws InterruptedException {

		targets.put(new RecognitionTarget(file.getName(), file, eventListener));
	}

	/**
	 * Starts Recognizer in a standby mode, awaiting for the files to process. If files were already put into the queue - begins processing them immediately.
	 * For the recognizer to function correctly it has to be initialized with Init() first.
	 *
	 * @throws RecognizerException
	 */
	public synchronized void Start() throws RecognizerException {

		if (isRunning) return;

		isRunning = true;
		thread = new Thread(this, "Recognizer thread");
		thread.start();
	}
	/**
	 * Stops Recognizer, aborting any pending task in the queue. Queue is cleared uppon stopping.
	 *
	 * @throws InterruptedException
	 */
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

		// Set up debug output
		OutputStreamWriter debugWriter = null;
		if (debugOutputDirectory != null) {
			try {
				debugWriter = new OutputStreamWriter(new FileOutputStream(new File(debugOutputDirectory, target.getId() + ".txt")));
			} catch (IOException e) {
				//todo failed to create debug file;
			}
		}

		// Get document type
		DocumentType documentType = null;
		try {
			documentType = GetDocumentType(target);
		} catch (RecognizerException e) {
			target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId(), e).getEvent());
			throw e;
		}
		if (documentType == null) {
			target.getEventListener().RecognitionFinished(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(documentType).getEvent());
		}

		// Do recognition
		ArrayList<RecognitionResult> recognitionResults = new ArrayList<>(recognitionSettings.length);
		for (int i = 0; i < recognitionSettings.length; i++) {
			RecognitionResult result = new RecognitionResult(recognitionSettings[i]);
			recognitionResults.add(i, result);

			DocumentDataBuilder builder = documentType.getBuilder();
			result.setDocumentDataBuilder(builder);

			try {
				builder.ProcessImage(target.getFile(), result.settings);
			} catch (RecognizerException e) {
				target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId(), e).getEvent());
				throw new RecognizerException("Error recognizing image \"" + target.getId() + '\"', e);
			}

			// Debug output
			if (debugWriter != null) {
				try {
					debugWriter.write("iteration: " + i + "\tcompleteness: " + result.getDocumentDataBuilder().getCompleteness() + '\n');
					debugWriter.write(result.getSettings().toString() + '\n');
					debugWriter.write(result.getDocumentDataBuilder().getDocumentData().toString(true) + '\n');
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

		// Send recognition results
		DocumentData documentData = recognitionResults.get(recognitionResults.size() - 1).getDocumentDataBuilder().getDocumentData();
		RecognitionResultEvent.RecognitionResultEventBuilder eventBuilder = new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(documentType).setDocumentData(documentData).setRecognitionPercentage(documentData.getCompleteness());
		target.getEventListener().RecognitionFinished(eventBuilder.getEvent());
	}
	public String RecognizeFile(File target, Rectangle area, RecognitionSettings recognitionSettings) throws RecognizerException {

		tesseract.setOcrEngineMode(recognitionSettings.getEngineMode());
		tesseract.setPageSegMode(recognitionSettings.getPageSegMode());

		try {
			return tesseract.doOCR(target, area);
		} catch (TesseractException e) {
			throw new RecognizerException(e);
		}
	}
	private DocumentType GetDocumentType(RecognitionTarget target) throws RecognizerException {

		DocumentType documentType = null;
		final Document hOCRText;
		float bestMatch = 0f;

		try {
			hOCRText = Jsoup.parse(RecognizeFile(target.getFile(), null, new RecognitionSettings(0, RecognitionSettings.ENGINE_MODE_BASIC, RecognitionSettings.PAGESEG_MODE_SINGLE_BLOCK)));
		} catch (RecognizerException e) {
			throw new RecognizerException("Error recognizing image \"" + target.getId() + '\"', e);
		}

		for (DocumentType type : DocumentType.values()) {
			float match = type.MatchText(hOCRText.text());
			if (match > bestMatch) {
				documentType = type;
				bestMatch = match;
			}
		}

		return documentType;
	}

	private class RecognitionResult {

		private RecognitionSettings settings;
		private Document hOCR = null;
		private String rawText = null;
		private DocumentDataBuilder documentDataBuilder = null;

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
		public DocumentDataBuilder getDocumentDataBuilder() {

			return documentDataBuilder;
		}
		public void setDocumentDataBuilder(DocumentDataBuilder documentDataBuilder) {

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
