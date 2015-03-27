package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.*;
import net.nekoinemo.documentrecognition.event.*;
import net.sourceforge.tess4j.*;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RecognitionManager implements IRecognitionManager {

	public static final RecognitionManager INSTANCE;
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

	private static RecognitionManager instance = null;

	private RecognitionManagerEventListener eventListener = null;
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

	static {
		INSTANCE = getInstance();
	}

	/**
	 * Returns an instance of a class.
	 *
	 * @return
	 */
	public static RecognitionManager getInstance() {

		if (instance == null) instance = new RecognitionManager();
		return instance;
	}

	private RecognitionManager() {

		tesseract = Tesseract.getInstance();
		tesseract.setLanguage("eng");

		targets = new LinkedBlockingQueue<>();
	}

	@Override
	public synchronized void init() throws RecognitionManagerException {

		init(true);
	}
	@Override
	public synchronized void init(boolean testRecognition) throws RecognitionManagerException {

		try {
			if (isRunning) throw new RecognitionManagerException("RecognitionManager is running!");

			// Temporary folder checking
			if (temporaryDirectoriesLocation == null)
				throw new RecognitionManagerException("Temporary directory location not specified");
			if (!temporaryDirectoriesLocation.exists() || !temporaryDirectoriesLocation.isDirectory()) try {
				if (!temporaryDirectoriesLocation.mkdirs()) {
					throw new RecognitionManagerException("Can't create temporary directory");
				}
			} catch (SecurityException e) {
				throw new RecognitionManagerException("Can't create temporary directory", e);
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
				testImage = ImageIO.read(RecognitionManager.class.getResourceAsStream("testText.png"));
			} catch (IOException e) {
				throw new RecognitionManagerException("Can't load test image.", e);
			}

			tesseract.setOcrEngineMode(TessAPI.TessOcrEngineMode.OEM_DEFAULT);
			tesseract.setPageSegMode(TessAPI.TessPageSegMode.PSM_SINGLE_WORD);
			tesseract.setHocr(false);

			String result;
			try {
				result = tesseract.doOCR(testImage);
			} catch (TesseractException e) {
				throw new RecognitionManagerException("Can't do recognition on a test image. Check if tessdata path is specified correctly and proper language files are present", e);
			}
			if (testRecognition && !result.trim().equalsIgnoreCase("test"))
				throw new RecognitionManagerException("Tesseract failed to recognize test image. Correct language/trained data may be missing from tessdata folder. Continuing with current settings may yeld low quality recognition results.");
		} catch (RecognitionManagerException e) {
			fireSystemException("RecognitionManager initialization failed", e);
			throw e;
		}

		tesseract.setHocr(true);
		isInitialized = true;
	}

	@Override
	public boolean isRunning() {

		return isRunning;
	}
	@Override
	public int getQueueSize() {

		return targets.size();
	}

	@Override
	public void setEventListener(RecognitionManagerEventListener eventListener) {

		synchronized (this) {
			this.eventListener = eventListener;
		}
	}
	@Override
	public void removeEventListener() {

		synchronized (this) {
			this.eventListener = null;
		}
	}

	@Override
	public void setTessDataPath(String value) throws RecognitionManagerException {

		if (isRunning) {
			RecognitionManagerException exception = new RecognitionManagerException("RecognitionManager is running!");
			fireMiscException("Can't change tessdata path", exception);

			throw exception;
		}

		tesseract.setDatapath(value);
	}
	@Override
	public boolean isDebugOutput() {

		return debugOutput;
	}
	@Override
	public void setDebugOutput(boolean debugOutput) throws RecognitionManagerException {

		if (isRunning) {
			RecognitionManagerException exception = new RecognitionManagerException("RecognitionManager is running!");
			fireMiscException("Can't change debug output", exception);

			throw exception;
		}

		this.debugOutput = debugOutput;
	}
	public File getDebugOutputDirectory() {

		return debugOutputDirectory;
	}
	public File getWorkingImagesDirectory() {

		return workingImagesDirectory;
	}
	@Override
	public void setTemporaryDirectoriesLocation(File temporaryDirectoriesLocation) throws RecognitionManagerException {

		if (isRunning) {
			RecognitionManagerException exception = new RecognitionManagerException("RecognitionManager is running!");
			fireMiscException("Can't change temporary directories location", exception);

			throw exception;
		}

		this.temporaryDirectoriesLocation = temporaryDirectoriesLocation;
	}
	@Override
	public void setRecognitionSettings(RecognitionSettings[] recognitionSettings) throws RecognitionManagerException {

		if (isRunning) {
			RecognitionManagerException exception = new RecognitionManagerException("RecognitionManager is running!");
			fireMiscException("Can't change settings", exception);

			throw exception;
		}

		this.recognitionSettings = recognitionSettings;
	}
	@Override
	public void PushAllFiles(File directory, RecognitionResultEventListener eventListener) throws InterruptedException {

		for (File file : directory.listFiles(SUPPORTED_FILES_FILTER)) {
			PushFile(file, eventListener);
		}
	}
	@Override
	public void PushFile(File file, RecognitionResultEventListener eventListener) throws InterruptedException {

		targets.put(new RecognitionTarget(file.getName(), file, eventListener));
	}

	@Override
	public synchronized void start() throws RecognitionManagerException {

		if (isRunning) return;
		if (!isInitialized) {
			RecognitionManagerException exception = new RecognitionManagerException("RecognitionManager wasn't initialized");
			fireSystemException("Can't start RecognitionManager", exception);

			throw exception;
		}

		isRunning = true;
		thread = new Thread(this, "RecognitionManager thread");
		thread.start();
	}
	@Override
	public synchronized void stop() throws InterruptedException {

		if (!isRunning) return;

		isRunning = false;
		isInitialized = false;
		thread.join();
	}

	@Override
	public void run() {

		while (isRunning) {
			RecognitionTarget target = null;
			try {
				target = targets.poll(1000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {}

			try {
				if (target != null) {
					Recognize(target, recognitionSettings);
					CleanWorkingDirectory();
				}
			} catch (RecognitionManagerException e) {
				fireRecognitionException("Failed to recognize file", target.getFile().getAbsolutePath(), e);
			}
		}

		if (targets.size() > 0) {
			targets.forEach(target -> target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(new RecognitionManagerException("Recognition process was aborted")).getEvent()));
			targets.clear();
		}
	}

	private void Recognize(RecognitionTarget target, RecognitionSettings[] recognitionSettings) throws RecognitionManagerException {

		// Set up debug output
		OutputStreamWriter debugWriter = null;
		if (debugOutput) {
			File debugFile = new File(debugOutputDirectory, target.getId() + ".txt");
			try {
				debugWriter = new OutputStreamWriter(new FileOutputStream(debugFile));
			} catch (IOException e) {
				fireMiscException("Can't make debug output file: " + debugFile.getAbsolutePath(), e);
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
		} catch (RecognitionManagerException e) {
			target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(e).getEvent());
			throw e;
		}

		// Prepare file for recognition
		ArrayList<File> images = null;
		try {
			images = PrepareImages(target.getFile());
		} catch (IOException e) {
			RecognitionManagerException exception = new RecognitionManagerException("Error processing file \"" + target.getId() + '\"', e);
			target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(exception).getEvent());
			throw exception;
		}

		// Do recognition
		ArrayList<RecognitionResult> recognitionResults = new ArrayList<>(recognitionSettings.length);
		for (int i = 0; i < recognitionSettings.length; i++) {
			RecognitionResult result = new RecognitionResult(recognitionSettings[i]);
			recognitionResults.add(i, result);

			IDocumentDataBuilder builder = documentType.getBuilder();
			result.setDocumentDataBuilder(builder);

			if (i > 0) try {
				result.getDocumentDataBuilder().FillEmptyFields(recognitionResults.get(i - 1).getDocumentDataBuilder().getDocumentData());
			} catch (RecognitionManagerException e) {}

			// Get all images from the file (pages of PDF or the file itself if it's single image file)
			for (File imageFile : images) {
				try {
					builder.ProcessImage(imageFile, result.settings);
				} catch (RecognitionManagerException e) {
					target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(e).getEvent());
					throw new RecognitionManagerException("Error recognizing image \"" + target.getId() + '\"', e);
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
		IDocumentData documentData = recognitionResults.get(recognitionResults.size() - 1).getDocumentDataBuilder().getDocumentData();
		RecognitionResultEvent.RecognitionResultEventBuilder eventBuilder = new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(documentType).setDocumentData(documentData).setRecognitionPercentage(documentData.getCompleteness());
		target.getEventListener().RecognitionFinished(eventBuilder.getEvent());
	}
	public String RecognizeFile(File target, Rectangle area, RecognitionSettings recognitionSettings) throws RecognitionManagerException {

		tesseract.setOcrEngineMode(recognitionSettings.getEngineMode());
		tesseract.setPageSegMode(recognitionSettings.getPageSegMode());

		try {
			return tesseract.doOCR(target, area);
		} catch (TesseractException e) {
			throw new RecognitionManagerException(e);
		}
	}

	private DocumentType GetDocumentType(RecognitionTarget target) throws RecognitionManagerException {

		DocumentType documentType = null;
		final Document hOCRText;
		float bestMatch = 0f;

		try {
			hOCRText = Jsoup.parse(RecognizeFile(target.getFile(), null, new RecognitionSettings(0, RecognitionSettings.ENGINE_MODE_BASIC, RecognitionSettings.PAGESEG_MODE_SINGLE_BLOCK)));
		} catch (RecognitionManagerException e) {
			throw new RecognitionManagerException("Error recognizing image \"" + target.getId() + '\"', e);
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
	private ArrayList<File> PrepareImages(File source) throws IOException {

		ArrayList<File> pages = Helper.GetImagesFromFile(source);
		for (File page : pages) {
			BufferedImage image = ImageIO.read(page);
			image = Helper.DeskewImage(image);
			ImageIO.write(image, "png", new File(FilenameUtils.removeExtension(page.getAbsolutePath()).concat(".png")));
		}

		return pages;
	}
	private void CleanWorkingDirectory() {

		for (File file : workingImagesDirectory.listFiles()) {
			file.delete();
		}
	}

	void fireSystemException(String message, Throwable cause){

		synchronized (eventListener){
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setCause(cause).getEvent();

			if (eventListener != null) eventListener.SystemExceptionOccurred(event);
		}
	}
	void fireRecognitionException(String message, String file, Throwable cause){

		synchronized (eventListener){
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setFileCause(file).setCause(cause).getEvent();

			if (eventListener != null) eventListener.RecognitionExceptionOccurred(event);
		}
	}
	void fireMiscException(String message, Throwable cause){

		synchronized (eventListener){
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setCause(cause).getEvent();

			if (eventListener != null) eventListener.MiscellaneousExceptionOccurred(event);
		}
	}

	private class RecognitionResult {

		private RecognitionSettings settings;
		private Document hOCR = null;
		private String rawText = null;
		private IDocumentDataBuilder documentDataBuilder = null;

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
		public IDocumentDataBuilder getDocumentDataBuilder() {

			return documentDataBuilder;
		}
		public void setDocumentDataBuilder(IDocumentDataBuilder documentDataBuilder) {

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
		public File getFile() {

			return file;
		}
		public RecognitionResultEventListener getEventListener() {

			return eventListener;
		}
	}
}
