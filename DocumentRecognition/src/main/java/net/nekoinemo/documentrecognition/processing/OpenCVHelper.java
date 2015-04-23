package net.nekoinemo.documentrecognition.processing;

import com.sun.jna.Platform;
import net.nekoinemo.documentrecognition.AppOpenCV;
import net.nekoinemo.documentrecognition.processing.math.Line;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;

public class OpenCVHelper {

	/**
	 * Size (area) of the barcode on the WPDoc.
	 */
	public static int WPDOC_BARCODE_MIN_AREA = 30000;
	public static int HORIZONTAL = 0;
	public static int VERTICAL = 1;

	/**
	 * Extracts and Loads OpenCV libraries from resources.
	 * Linux and Mac OS libraries are loaded from OpenCV jar, Win libraries are loaded from this jar.
	 *
	 * @throws IOException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	public static void loadOpenCVLibs() throws IOException, IllegalAccessException, NoSuchFieldException {

		// Load OpenCV library
		if (Platform.isWindows()) {
			File tmpDir = new File(System.getProperty("java.io.tmpdir"), "opencv");
			tmpDir.mkdirs();
			URL winLib = AppOpenCV.class.getResource("opencv/" + Platform.RESOURCE_PREFIX + '/' + Core.NATIVE_LIBRARY_NAME + ".dll");

			//Files.copy(winLib, tmpDir.toPath().resolve(Core.NATIVE_LIBRARY_NAME + ".dll"));
			org.apache.commons.io.FileUtils.copyURLToFile(winLib, tmpDir.toPath().resolve(Core.NATIVE_LIBRARY_NAME + ".dll").toFile());

			System.setProperty("java.library.path", System.getProperty("java.library.path") + System.getProperty("path.separator") + tmpDir.getAbsolutePath());
			Field systemPathsField = ClassLoader.class.getDeclaredField("sys_paths");
			systemPathsField.setAccessible(true);
			systemPathsField.set(null, null);

			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		} else {
			OpenCV.loadLibrary();
		}
	}

	/**
	 * Removes barcodes (and occasionally other images)
	 * @param invertedImage  bitwise inverted OpenCV image
	 * @param minBarcodeArea Minimal area of the object to be removed
	 *
	 * @return
	 */
	public static Mat removeBarcode(Mat invertedImage, int minBarcodeArea) {

		Size size = invertedImage.size();

		Mat imageClean = new Mat();
		Mat imageSobelX = new Mat();
		Mat imageSobelY = new Mat();
		Mat imageTmp = new Mat();
		Mat imageMask = Mat.zeros(size, CvType.CV_8UC1);

		Imgproc.Sobel(invertedImage, imageSobelX, CvType.CV_32F, 1, 0, -1, 1, 0);
		Imgproc.Sobel(invertedImage, imageSobelY, CvType.CV_32F, 0, 1, -1, 1, 0);
		Core.subtract(imageSobelX, imageSobelY, imageTmp);
		Core.convertScaleAbs(imageTmp, imageTmp);

		Imgproc.blur(imageTmp, imageTmp, new Size(9, 9));
		Imgproc.threshold(imageTmp, imageTmp, 200, 255, Imgproc.THRESH_BINARY);

		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 4));
		Imgproc.morphologyEx(imageTmp, imageTmp, Imgproc.MORPH_CLOSE, kernel);

		Imgproc.erode(imageTmp, imageTmp, new Mat(), new Point(-1, -1), 14);
		Imgproc.dilate(imageTmp, imageTmp, new Mat(), new Point(-1, -1), 14);

		ArrayList<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(imageTmp, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		contours.sort(Comparator.comparingInt(cont -> (int) Imgproc.contourArea(cont)));
		for (int i = contours.size() - 1; i > -1; i--) {
			if (Imgproc.contourArea(contours.get(i)) < minBarcodeArea) break;

			MatOfPoint2f poly = new MatOfPoint2f();
			Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), poly, 3, true);

			Rect bBox = Imgproc.boundingRect(new MatOfPoint(poly.toArray()));
			Core.rectangle(imageMask, bBox.tl(), bBox.br(), new Scalar(255, 255, 255), -1);
		}

		Core.bitwise_not(imageMask, imageMask);
		Core.bitwise_and(invertedImage, imageMask, imageClean);

		return imageClean;
	}

	/**
	 * Gets lines on the passed image. Will only find strictly horizontal or vertical lines
	 * @param invertedImage bitwise inverted OpenCV image
	 * @param orientation
	 * @param minLineLength
	 * @return
	 */
	public static ArrayList<Line> lines(Mat invertedImage, int orientation, int minLineLength) {

		Size morph = orientation == HORIZONTAL ? new Size(10, 1) : new Size(1, 10);

		Mat imageTmp = new Mat();
		Imgproc.erode(invertedImage, imageTmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, morph), new Point(-1, -1), 5);
		Imgproc.threshold(imageTmp, imageTmp, 80, 255, Imgproc.THRESH_BINARY);

		Mat lines = new Mat();
		Imgproc.HoughLinesP(imageTmp, lines, 1, Math.PI / 40, 100, minLineLength, 0);
		ArrayList<Line> acceptedLines = new ArrayList<>();
		for (int i = 0; i < lines.cols(); i++) {
			double[] vec = lines.get(0, i);

			Line line = new Line(vec[0], vec[1], vec[2], vec[3]);
			if (!acceptedLines.stream().anyMatch(existingLine -> existingLine.isNear(line))) {
				acceptedLines.add(line);
			}
		}

		return acceptedLines;
	}
}
