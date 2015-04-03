package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.document.DocumentType;
import net.nekoinemo.documentrecognition.document.IDocumentDataBuilder;
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

		// List of all supported extensions. Only files with those will be added by pushAllFiles()
		private final ArrayList SUPPORTED_EXTENSIONS = new ArrayList() {{
			add("pdf");
			add("jpg");
			add("jpeg");
			add("png");
			add("gif");
			add("bmp");
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
	private RecognitionTarget currentTarget = null;

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
	public ArrayList<RecognitionTarget> getQueue() {

		return new ArrayList<>(targets);
	}
	public RecognitionTarget getCurrentTarget() {

		return currentTarget;
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
	public File getDebugFile(String name){

		return new File(INSTANCE.debugOutputDirectory, name);
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
	public void pushAllFiles(File directory, RecognitionResultEventListener eventListener) throws InterruptedException {

		for (File file : directory.listFiles(SUPPORTED_FILES_FILTER)) {
			pushFile(file, eventListener);
		}
	}
	@Override
	public void pushFile(File file, RecognitionResultEventListener eventListener) throws InterruptedException {

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

		cleanWorkingDirectory(); // Clears the "workingImages" folder in the temporary directory

		// Main loop. Will run until stop() is called
		while (isRunning) {
			currentTarget = null;
			try {
				currentTarget = targets.poll(1000, TimeUnit.MILLISECONDS); // Waits 1s for target to appear then moves to the next iteration
			} catch (InterruptedException e) {}

			if (currentTarget != null) {
				try {
					doRecognition(currentTarget, recognitionSettings);
					cleanWorkingDirectory(); // Clears the "workingImages" folder in the temporary directory
				} catch (RecognitionManagerException e) {
					currentTarget.getEventListener().recognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(currentTarget.getId()).setCause(e).getEvent()); // Notify even listener that file recognition failed
					fireRecognitionException("Failed to recognize file", currentTarget.getFile().getAbsolutePath(), e);
				}
			}
		}

		// Notify all remaining targets that recognition was aborted
		if (targets.size() > 0) {
			targets.forEach(target -> target.getEventListener().recognitionError(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setCause(new RecognitionManagerException("Recognition process was aborted")).getEvent()));
			targets.clear();
		}
	}

	public String recognize(File target, Rectangle area, RecognitionSettings recognitionSettings) throws RecognitionManagerException {

		tesseract.setOcrEngineMode(recognitionSettings.getEngineMode());
		tesseract.setPageSegMode(recognitionSettings.getPageSegMode());

		try {
			return tesseract.doOCR(target, area);
		} catch (TesseractException e) {
			throw new RecognitionManagerException(e);
		}
	}
	public String recognize(BufferedImage target, Rectangle area, RecognitionSettings recognitionSettings) throws RecognitionManagerException {

		tesseract.setOcrEngineMode(recognitionSettings.getEngineMode());
		tesseract.setPageSegMode(recognitionSettings.getPageSegMode());

		try {
			return tesseract.doOCR(target, area);
		} catch (TesseractException e) {
			throw new RecognitionManagerException(e);
		}
	}

	private void doRecognition(RecognitionTarget target, RecognitionSettings[] recognitionSettings) throws RecognitionManagerException {

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

		try {
			target.prepare();

			if (target.getDocumentType() == null) {
				target.getEventListener().recognitionFinished(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).getEvent());
				return;
			}

			IDocumentDataBuilder builder = target.getDocumentType().getBuilder();
			builder.processImage(target, recognitionSettings);

			if (debugWriter != null) try {
				debugWriter.write(target.getId() + '\t' + target.getDocumentType() + '\t' + builder.getCompleteness() + System.lineSeparator());
				debugWriter.write(builder.getDocumentData().toString(true) + System.lineSeparator());
				debugWriter.write(builder.getDebugText());
			} catch (IOException e) {
				fireMiscException("Can't write to debug file", e);
			}

			target.getEventListener().recognitionFinished(new RecognitionResultEvent.RecognitionResultEventBuilder(target.getId()).setDocumentType(target.getDocumentType()).setDocumentData(builder.getDocumentData()).setRecognitionPercentage(builder.getCompleteness()).getEvent());
		} finally {
			try {
				if (debugWriter != null) {
					debugWriter.close();
				}
			} catch (IOException e) {}
		}
	}
	private void cleanWorkingDirectory() {

		for (File file : workingImagesDirectory.listFiles()) {
			file.delete();
		}
	}

	// Event triggers

	void fireSystemException(String message, Throwable cause) {

		synchronized (eventListener) {
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setCause(cause).getEvent();

			if (eventListener != null) eventListener.systemExceptionOccurred(event);
		}
	}
	void fireRecognitionException(String message, String file, Throwable cause) {

		synchronized (eventListener) {
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setFileCause(file).setCause(cause).getEvent();

			if (eventListener != null) eventListener.recognitionExceptionOccurred(event);
		}
	}
	void fireMiscException(String message, Throwable cause) {

		synchronized (eventListener) {
			RecognitionManagerEvent event = new RecognitionManagerEvent.RecognitionManagerEventBuilder().setMessage(message).setCause(cause).getEvent();

			if (eventListener != null) eventListener.miscellaneousExceptionOccurred(event);
		}
	}

	/**
	 * Structure type class that encapsulates the file to be processed
	 */
	public class RecognitionTarget {

		private final String id;
		private final File file;
		private final RecognitionResultEventListener eventListener;
		private ArrayList<File> images = new ArrayList<>();
		private int rotationAngle = 0;
		private DocumentType documentType = null;

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
		public ArrayList<File> getImages() {

			return new ArrayList<>(images);
		}
		public int getRotationAngle() {

			return rotationAngle;
		}
		public DocumentType getDocumentType() {

			return documentType;
		}

		/**
		 * Extracts images from the file, then determines document type and orientation. The rotates all extracted images to the correct orientation
		 *
		 * @throws RecognitionManagerException
		 */
		protected void prepare() throws RecognitionManagerException {

			try {
				extractImages();
			} catch (IOException e) {
				throw makeException("Error extracting images", e);
			}

			int currentAngle = 0;

			int i = 0;
			while (i < images.size() && documentType == null) { // Check all files until docType is determined
				currentAngle = -90;
				BufferedImage image;

				try {
					image = ImageIO.read(images.get(i));
				} catch (IOException e) {
					throw makeException("Error reading tmp file " + images.get(i).getName(), e);
				}

				while (currentAngle < 270 && documentType == null) { // Rotate image CW 90 until docType is determined
					currentAngle += 90;
					image = ImageHelper.rotate(image, currentAngle);

					documentType = checkDocumentType(image);
				}
			}

			if (documentType != null && currentAngle != 0) {
				documentType = documentType;
				rotationAngle = currentAngle;

				rotateImages();
			}
		}

		private void extractImages() throws IOException {

			images = Helper.imagesFromFile(file);
			for (File page : images) {
				BufferedImage image = ImageIO.read(page);
				image = ImageHelper.deskewImage(image);
				ImageIO.write(image, "png", new File(FilenameUtils.removeExtension(page.getAbsolutePath()).concat(".png")));
			}
		}
		private DocumentType checkDocumentType(BufferedImage image) throws RecognitionManagerException {

			DocumentType docType = null;
			Document hOCRText = null;
			float bestMatch = 0f;

			// Do (fast) recognition with the very basic settings to get all of the text on the image
			try {
				hOCRText = Jsoup.parse(recognize(image, null, new RecognitionSettings(0, RecognitionSettings.ENGINE_MODE_BASIC, RecognitionSettings.PAGESEG_MODE_SINGLE_BLOCK)));
			} catch (RecognitionManagerException e) {
				throw makeException(e.getMessage(), e.getCause()); // Repack exception for standardized look
			}

			for (DocumentType type : DocumentType.values()) {
				// Get the chance that recognized text belongs to the document of this type
				float match = type.matchText(Helper.getProperTextFromJSoupDoc(hOCRText));

				if (match > bestMatch) {
					docType = type;
					bestMatch = match;
				}
			}

			return docType;
		}
		private void rotateImages() throws RecognitionManagerException {

			for (File image : images) {
				try {
					Helper.rotateImageFile(image, rotationAngle);
				} catch (IOException e) {
					throw makeException("Error rotating tmp file \" + image.getName()", e);
				}
			}
		}

		private RecognitionManagerException makeException(String message, Throwable cause) {

			return new RecognitionManagerException(id + " - " + message, cause);
		}
	}
}
