package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.*;
import net.nekoinemo.documentrecognition.event.RecognitionResultEvent;
import net.nekoinemo.documentrecognition.event.RecognitionResultEventListener;
import net.sourceforge.tess4j.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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

	private File temporaryDirectoriesLocation = null;
	private File debugOutputDirectory = null;
	private File workingImagesDirectory = null;

	private boolean debugOutput = false;
	private boolean isInitialized = false;

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

		// Temporary folder checking
		if (temporaryDirectoriesLocation == null)
			throw new RecognizerException("Temporary directory location not specified");
		if (!temporaryDirectoriesLocation.exists() || !temporaryDirectoriesLocation.isDirectory()) try {
			if (!temporaryDirectoriesLocation.mkdirs()) {
				throw new RecognizerException("Can't create temporary directory");
			}
		} catch (SecurityException e) {
			throw new RecognizerException("Can't create temporary directory", e);
		}
		workingImagesDirectory = new File(temporaryDirectoriesLocation, "workingImages");
		workingImagesDirectory.mkdir();

		// Debug folder
		if (debugOutput) {
			debugOutputDirectory = new File(temporaryDirectoriesLocation, "debug");
			debugOutputDirectory.mkdir();
		}

		// Recognition test
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
		isInitialized = true;
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
	public boolean isDebugOutput() {

		return debugOutput;
	}
	/**
	 * Sets whenever debug output should be created. Output is stored in the "debug" folder inside the temporary directory.
	 *
	 * @param debugOutput
	 */
	public void setDebugOutput(boolean debugOutput) {

		this.debugOutput = debugOutput;
	}
	public File getDebugOutputDirectory() {

		return debugOutputDirectory;
	}
	public File getWorkingImagesDirectory() {

		return workingImagesDirectory;
	}
	public File getTemporaryDirectoriesLocation() {

		return temporaryDirectoriesLocation;
	}
	/**
	 * Sets the location of the temporary folder.
	 *
	 * @param temporaryDirectoriesLocation Location where the folder(s) for the temporary files will be created.
	 */
	public void setTemporaryDirectoriesLocation(File temporaryDirectoriesLocation) {

		this.temporaryDirectoriesLocation = temporaryDirectoriesLocation;
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
	 * For the recognizer to function correctly it has to be initialized with Init() first otherwise this method will throw an error.
	 *
	 * @throws RecognizerException if Recognizer wasn't properly initialized
	 */
	public synchronized void Start() throws RecognizerException {

		if (isRunning) return;
		if (!isInitialized) throw new RecognizerException("Recognizer wasn't initialized");

		isRunning = true;
		thread = new Thread(this, "Recognizer thread");
		thread.start();
	}
	/**
	 * Stops Recognizer, aborting any pending task in the queue. Queue is cleared uppon stopping.
	 * Doing so will deinitialize the Recognizer
	 *
	 * @throws InterruptedException
	 */
	public synchronized void Stop() throws InterruptedException {

		if (!isRunning) return;

		isRunning = false;
		isInitialized = false;
		thread.join();
	}

	@Override
	public void run() {

		while (isRunning) {
			try {
				RecognitionTarget target = targets.poll(1000, TimeUnit.MILLISECONDS);
				if (target != null) {
					Recognize(target, recognitionSettings);
					CleanWorkingDirectory();
				}
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
		if (debugOutput) {
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
			if (documentType == null) {
				target.getEventListener().RecognitionFinished(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(documentType).getEvent());
				return;
			}
		} catch (RecognizerException e) {
			target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId(), e).getEvent());
			throw e;
		}

		// Do recognition
		ArrayList<RecognitionResult> recognitionResults = new ArrayList<>(recognitionSettings.length);
		ArrayList<File> images = Helper.GetImagesFromFile(target.getFile());

		for (int i = 0; i < recognitionSettings.length; i++) {
			RecognitionResult result = new RecognitionResult(recognitionSettings[i]);
			recognitionResults.add(i, result);

			DocumentDataBuilder builder = documentType.getBuilder();
			result.setDocumentDataBuilder(builder);

			if (i > 0) try {
				result.getDocumentDataBuilder().FillEmptyFields(recognitionResults.get(i - 1).getDocumentDataBuilder().getDocumentData());
			} catch (RecognizerException e) {}

			// Get all images from the file (pages of PDF or the file itself if it's single image file)
			for (File imageFile : images) {
				try {
					builder.ProcessImage(imageFile, result.settings);
				} catch (RecognizerException e) {
					target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId(), e).getEvent());
					throw new RecognizerException("Error recognizing image \"" + target.getId() + '\"', e);
				}
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
			float match = type.MatchText(Helper.GetProperTextFromJSoupDoc(hOCRText));
			if (match > bestMatch) {
				documentType = type;
				bestMatch = match;
			}
		}

		return documentType;
	}

	private void CleanWorkingDirectory() {

		for (File file : workingImagesDirectory.listFiles()) {
			file.delete();
		}
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
			rawText = Helper.GetProperTextFromJSoupDoc(hOCR);
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
