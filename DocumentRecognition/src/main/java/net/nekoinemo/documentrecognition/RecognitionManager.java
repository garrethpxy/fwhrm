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

		// List of all supported extensions. Only files with those will be added by PushAllFiles()
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

			return SUPPORTED_EXTENSIONS.contains(FilenameUtils.getExtension(pathname.getName()));
		}
	};

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
		INSTANCE = new RecognitionManager();
	}

	/**
	 * Returns an instance of a class.
	 *
	 * @return
	 */
	public static RecognitionManager getInstance() {

		return INSTANCE;
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

			// Create workingImages folder
			workingImagesDirectory = new File(temporaryDirectoriesLocation, "workingImages");
			workingImagesDirectory.mkdir();

			// Create debug folder (if debugOutput is used)
			if (debugOutput) {
				debugOutputDirectory = new File(temporaryDirectoriesLocation, "debug");
				debugOutputDirectory.mkdir();
			}

			// Use default settings if none are specified
			if (recognitionSettings == null || recognitionSettings.length == 0)
				recognitionSettings = RecognitionSettings.DEFAULT;

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

			// Test recognition with the provided test image this will cause Tesseract to throw an exception if incorrect settings were set
			String result;
			try {
				result = tesseract.doOCR(testImage);
			} catch (TesseractException e) {
				throw new RecognitionManagerException("Can't do recognition on a test image. Check if tessdata path is specified correctly and proper language files are present", e);
			}

			// This will check if whatever Tesseract returned matches the text image contained.
			// If it didn't then there's probably something seriously wrong with either Tesseract installed ot tessdata provided
			if (testRecognition && !result.trim().equalsIgnoreCase("test"))
				throw new RecognitionManagerException("Tesseract failed to recognize test image. Correct language/trained data may be missing from tessdata folder. Continuing with current settings may yeld low quality recognition results.");
		} catch (RecognitionManagerException e) {
			fireSystemException("RecognitionManager initialization failed", e);
			throw e;
		}

		tesseract.setHocr(true); // Sets the output format to the hOCR (see http://en.wikipedia.org/wiki/HOCR)
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
		thread = new Thread(this, "RecognitionManager thread"); // Once stopped thread can't be restarted so new instance is created
		thread.start();
	}
	@Override
	public synchronized void stop() throws InterruptedException {

		if (!isRunning) return;

		isRunning = false;
		isInitialized = false; // init() checks if settings are valid so this forces init() to be performed before each start()
		thread.join();
	}

	@Override
	public void run() {

		// Main loop. Will run until stop() is called
		while (isRunning) {
			RecognitionTarget target = null;
			try {
				target = targets.poll(1000, TimeUnit.MILLISECONDS); // Waits 1s for target to appear then moves to the next iteration
			} catch (InterruptedException e) {}

			if (target != null) {
				try {
					Recognize(target, recognitionSettings);
					CleanWorkingDirectory(); // Clears the "workingImages" folder in the temporary directory
				} catch (RecognitionManagerException e) {
					fireRecognitionException("Failed to recognize file", target.getFile().getAbsolutePath(), e);
				}
			}
		}

		// Notify all remaining targets that recognition was aborted
		if (targets.size() > 0) {
			targets.forEach(target -> {

				RecognitionResultEvent event = new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(new RecognitionManagerException("Recognition process was aborted")).getEvent();
				target.getEventListener().RecognitionError(event);
			});
			targets.clear();
		}
	}

	private void Recognize(RecognitionTarget target, RecognitionSettings[] recognitionSettings) throws RecognitionManagerException {

		// Set up debug output file
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
				RecognitionResultEvent event = new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(null).getEvent();
				target.getEventListener().RecognitionFinished(event);
				return;
			}
		} catch (RecognitionManagerException e) {
			RecognitionResultEvent event = new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(e).getEvent();
			target.getEventListener().RecognitionError(event);
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

		// Do recognition with each set of settings (or until desired data completeness achieved)
		ArrayList<RecognitionResult> recognitionResults = new ArrayList<>(recognitionSettings.length);
		for (int i = 0; i < recognitionSettings.length; i++) {
			RecognitionResult result = new RecognitionResult(recognitionSettings[i]);
			recognitionResults.add(i, result);

			IDocumentDataBuilder builder = documentType.getBuilder();
			result.setDocumentDataBuilder(builder);

			if (i > 0) try {
				// Copy results from the previous iteration (in case in this one nothing will be recognized)
				result.getDocumentDataBuilder().FillEmptyFields(recognitionResults.get(i - 1).getDocumentDataBuilder().getDocumentData());
			} catch (RecognitionManagerException e) {}

			// Run recognition for each page
			for (File imageFile : images) {
				try {
					builder.ProcessImage(imageFile, result.settings);
				} catch (RecognitionManagerException e) {
					target.getEventListener().RecognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(e).getEvent());
					throw new RecognitionManagerException("Error recognizing image \"" + target.getId() + '\"', e);
				}
			}

			// Output debug recognition data
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

			if (result.getDocumentDataBuilder().getCompleteness() >= result.getSettings().getPassingCompliteness()) // If target completeness was achieved - stop
				break;
		}

		// Send recognition results
		// Get DocumentData from the builder in the last result
		IDocumentData documentData = recognitionResults.get(recognitionResults.size() - 1).getDocumentDataBuilder().getDocumentData();
		// Get event to be sent out.
		// Create new builder with this docID, set recognized type, set recognized data, set data completeness and get event from that builder
		RecognitionResultEvent event = new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(documentType).setDocumentData(documentData).setRecognitionPercentage(documentData.getCompleteness()).getEvent();
		target.getEventListener().RecognitionFinished(event);
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
			// Do (fast) recognition with the very basic settings to get all of the text on the image
			hOCRText = Jsoup.parse(RecognizeFile(target.getFile(), null, new RecognitionSettings(0, RecognitionSettings.ENGINE_MODE_BASIC, RecognitionSettings.PAGESEG_MODE_SINGLE_BLOCK)));
		} catch (RecognitionManagerException e) {
			throw new RecognitionManagerException("Error recognizing image \"" + target.getId() + '\"', e);
		}

		for (DocumentType type : DocumentType.values()) {
			// Get the chance that recognized text belongs to the document of this type
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

	void fireSystemException(String message, Throwable cause) {

		synchronized (eventListener) {
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setCause(cause).getEvent();

			if (eventListener != null) eventListener.SystemExceptionOccurred(event);
		}
	}
	void fireRecognitionException(String message, String file, Throwable cause) {

		synchronized (eventListener) {
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setFileCause(file).setCause(cause).getEvent();

			if (eventListener != null) eventListener.RecognitionExceptionOccurred(event);
		}
	}
	void fireMiscException(String message, Throwable cause) {

		synchronized (eventListener) {
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setCause(cause).getEvent();

			if (eventListener != null) eventListener.MiscellaneousExceptionOccurred(event);
		}
	}

	/**
	 * Class contains result of one recognition iteration along with the used settings
	 */
	private class RecognitionResult {

		private RecognitionSettings settings;
		private IDocumentDataBuilder documentDataBuilder = null;

		public RecognitionResult(RecognitionSettings settings) {

			this.settings = settings;
		}

		public RecognitionSettings getSettings() {

			return settings;
		}
		public IDocumentDataBuilder getDocumentDataBuilder() {

			return documentDataBuilder;
		}
		public void setDocumentDataBuilder(IDocumentDataBuilder documentDataBuilder) {

			this.documentDataBuilder = documentDataBuilder;
		}
	}
	/**
	 * Structure type class that encapsulates the file to be processed
	 */
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
