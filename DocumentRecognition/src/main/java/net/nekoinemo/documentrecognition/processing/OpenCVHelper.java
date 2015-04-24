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

public class OpenCVHelper {

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
	 * Gets lines on the passed image. Will only find strictly horizontal or vertical lines
	 *
	 * @param invertedImage bitwise inverted OpenCV image
	 * @param orientation
	 * @param minLineLength
	 *
	 * @return
	 */
	public static ArrayList<Line> linesHV(Mat invertedImage, int orientation, int minLineLength) {

		Size morphSize = orientation == HORIZONTAL ? new Size(10, 1) : new Size(1, 10);

		Mat imageTmp = new Mat();
		Imgproc.threshold(invertedImage, imageTmp, 20, 255, Imgproc.THRESH_BINARY);
		Imgproc.erode(imageTmp, imageTmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, morphSize), new Point(-1, -1), 5);
		Imgproc.dilate(imageTmp, imageTmp, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, morphSize), new Point(-1, -1), 9);
		Imgproc.medianBlur(imageTmp, imageTmp, 3);

		Mat lines = new Mat();
		Imgproc.HoughLinesP(imageTmp, lines, 1, Math.PI / 2, 100, minLineLength, 200);
		imageTmp.release();
		ArrayList<Line> acceptedLines = new ArrayList<>();
		for (int i = 0; i < lines.cols(); i++) {
			double[] vec = lines.get(0, i);

			Line line = new Line(vec[0], vec[1], vec[2], vec[3]);
			if ((orientation == HORIZONTAL ? line.isHorizontal() : line.isVertical()) && !acceptedLines.stream().anyMatch(existingLine -> existingLine.isNear(line))) {
				acceptedLines.add(line);
			}
		}

		return acceptedLines;
	}
}
