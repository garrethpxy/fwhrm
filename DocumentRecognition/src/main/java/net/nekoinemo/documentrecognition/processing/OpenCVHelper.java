package net.nekoinemo.documentrecognition.processing;

import com.sun.jna.Platform;
import net.nekoinemo.documentrecognition.AppOpenCV;
import net.nekoinemo.documentrecognition.processing.math.Line;
import net.nekoinemo.documentrecognition.processing.math.MathHelper;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;

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
	/**
	 * Finds bordered regions on MOMDoc
	 * @param invertedImage OpenCV image after bitwise inversion.
	 * @return
	 */
	public static ArrayList<Rectangle> regionsMOM(Mat invertedImage){

		ArrayList<Rectangle> regions = new ArrayList<>();
		Size size = invertedImage.size();

		//Find horizontal and vertical lines
		ArrayList<Line> linesH = OpenCVHelper.linesHV(invertedImage, OpenCVHelper.HORIZONTAL, 200);
		linesH.sort(new Comparator<Line>() {
			@Override
			public int compare(Line o1, Line o2) {

				return (int) (o1.getPoint1().y - o2.getPoint1().y);
			}
		});
		ArrayList<Line> linesV = OpenCVHelper.linesHV(invertedImage, OpenCVHelper.VERTICAL, 200);
		linesV.sort(new Comparator<Line>() {
			@Override
			public int compare(Line o1, Line o2) {

				return (int) (o1.getPoint1().x - o2.getPoint1().x);
			}
		});

		for (int i = 0; i < linesH.size(); i++) {
			Line line1 = linesH.get(i);
			Line line2;

			if ((i == linesH.size() - 1)) {
				line2 = new Line(line1.getPoint1().x, size.height, line1.getPoint2().x, size.height); // If last line - set bottom of the image as pair
			} else if (!line1.isHorizontalPair(linesH.get(i + 1), 50)) {
				if (i > 0) {
					continue; // If no pair - skip
				}
				line2 = new Line(line1.getPoint1().x, 0, line1.getPoint2().x, 0); // If first line - set top of the image as pair
			} else {
				line2 = linesH.get(i + 1);
				linesH.remove(i + 1);
			}

			// Default coordinates, based on the lines ends
			int x1 = (int) Math.max(line1.getPoint1().x, line2.getPoint1().x);
			int y1 = (int) Math.min(line1.getPoint1().y, line2.getPoint1().y);
			int x2 = (int) Math.min(line1.getPoint2().x, line2.getPoint2().x) - 30; // -30 to cut off right on the right side
			int y2 = (int) Math.max(line1.getPoint1().y, line2.getPoint1().y);

			// Intersection coordinates
			int x1i1 = x1;
			int x2i1 = x2;
			int x1i2 = x1;
			int x2i2 = x2;

			// Line1 intersections
			ArrayList<Line> intersections1 = new ArrayList<>();
			for (Line line : linesV) {
				if (line1.intersects(line)) intersections1.add(line);
			}
			if (intersections1.size() == 1) { // Only one intersection
				Point intersectionPoint = new Point();
				line1.intersects(intersections1.get(0), intersectionPoint);

				if (MathHelper.distance(intersectionPoint, line1.getPoint1()) < line1.length() * 0.4d) // Close to the left end
					x1i1 = (int) intersectionPoint.x;
				else if (MathHelper.distance(intersectionPoint, line1.getPoint2()) < line1.length() * 0.4d) // Close to the right end
					x2i1 = (int) intersectionPoint.x;
			} else if (intersections1.size() > 1) {
				Point intersectionPoint1 = new Point();
				line1.intersects(intersections1.get(0), intersectionPoint1); // Get leftmost intersection
				x1i1 = (int) intersectionPoint1.x;

				Point intersectionPoint2 = new Point();
				line1.intersects(intersections1.get(intersections1.size() - 1), intersectionPoint2); // Get rightmost intersection
				x2i1 = (int) intersectionPoint2.x;
			}

			// Line2 intersections
			ArrayList<Line> intersections2 = new ArrayList<>();
			for (Line line : linesV) {
				if (line2.intersects(line)) intersections2.add(line);
			}
			if (intersections2.size() == 1) { // Only one intersection
				Point intersectionPoint = new Point();
				line2.intersects(intersections2.get(0), intersectionPoint);

				if (MathHelper.distance(intersectionPoint, line2.getPoint1()) < line2.length() * 0.4d) // Close to the left end
					x1i2 = (int) intersectionPoint.x;
				else if (MathHelper.distance(intersectionPoint, line2.getPoint2()) < line2.length() * 0.4d) // Close to the right end
					x2i2 = (int) intersectionPoint.x;
			} else if (intersections2.size() > 1) {
				Point intersectionPoint1 = new Point();
				line2.intersects(intersections2.get(0), intersectionPoint1); // Get leftmost intersection
				x1i2 = (int) intersectionPoint1.x;

				Point intersectionPoint2 = new Point();
				line2.intersects(intersections2.get(intersections2.size() - 1), intersectionPoint2); // Get rightmost intersection
				x2i2 = (int) intersectionPoint2.x;
			}

			x1 = Math.max(x1, Math.max(x1i1, x1i2)); // Left side - rightmost point
			x2 = Math.min(x2, Math.min(x2i1, x2i2)); // Right side - leftmost point

			// How much space to cut off on the sides
			int cutoffLR = 4; // Left-right
			int cutoffTB = 4; // Top-bottom

			Rectangle region = new Rectangle(x1 + cutoffLR, y1 + cutoffTB, x2 - x1 - 2 * cutoffLR, y2 - y1 - 2 * cutoffTB);
			if (region.width > 0 && region.height > 0 && region.height < 2 * region.width) { // Validness check.
				regions.add(region);
			}
		}

		return regions;
	}
}
