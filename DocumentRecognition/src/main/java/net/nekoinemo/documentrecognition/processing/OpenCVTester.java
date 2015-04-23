package net.nekoinemo.documentrecognition.processing;

import net.nekoinemo.documentrecognition.processing.math.Line;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

public class OpenCVTester {

	public static void doRotation(File inputFile) {

		String outputNameTemplate = FilenameUtils.removeExtension(inputFile.getAbsolutePath());

		// Read image
		Mat imageSrc = Highgui.imread(inputFile.getAbsolutePath(), 0);
		Mat imageLines = new Mat();

		//		Core.bitwise_not(imageSrc, imageLines);
		Imgproc.Canny(imageSrc, imageLines, 80, 100, 3, true);

		// Calculating the skew angle
		Size size = imageSrc.size();
		Mat lines = new Mat();
		Imgproc.HoughLinesP(imageLines, lines, 1, Math.PI / 180, 50, 80, 10);

		double angle = 0;
		for (int x = 0; x < lines.cols(); x++) {
			double[] vec = lines.get(0, x);
			double x1 = vec[0],
					y1 = vec[1],
					x2 = vec[2],
					y2 = vec[3],
					xd = x2 - x1,
					yd = y2 - y1;

			// calculate
			angle += Math.toDegrees(Math.atan2(yd, xd));
			//angle += Core.fastAtan2((float) yd, (float) xd);

			// Draw
			Point start = new Point(x1, y1);
			Point end = new Point(x2, y2);

			Core.line(imageLines, start, end, new Scalar(255, 0, 0), 1);
		}
		angle /= lines.cols();
		System.out.println(inputFile.getName().concat(" : ").concat(String.valueOf(angle)));

		// Write image (lines)
		Highgui.imwrite(outputNameTemplate + "_lines_.png", imageLines);

		// Rotating
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(size.width / 2, size.height / 2), -angle, 1);
		Mat imageRotated = new Mat();
		Imgproc.warpAffine(imageSrc, imageRotated, rotationMatrix, size, Imgproc.INTER_CUBIC);

		// Write image (rotated)
		Highgui.imwrite(outputNameTemplate + "_rotated_.png", imageRotated);
	}
	public static void removeBarcode(File inputFile) {

		String outputNameTemplate = FilenameUtils.removeExtension(inputFile.getAbsolutePath());

		// Read image
		Mat imageSrc = Highgui.imread(inputFile.getAbsolutePath(), 0);
		Core.bitwise_not(imageSrc, imageSrc);
		Size size = imageSrc.size();

		Mat imageClean = new Mat();
		Mat imageSobelX = new Mat();
		Mat imageSobelY = new Mat();
		Mat imageTmp = new Mat();
		Mat imageMask = Mat.zeros(size, CvType.CV_8UC1);

		Imgproc.Sobel(imageSrc, imageSobelX, CvType.CV_32F, 1, 0, -1, 1, 0);
		Imgproc.Sobel(imageSrc, imageSobelY, CvType.CV_32F, 0, 1, -1, 1, 0);

		Core.subtract(imageSobelX, imageSobelY, imageTmp);
		Core.convertScaleAbs(imageTmp, imageTmp);

		Imgproc.blur(imageTmp, imageTmp, new Size(9, 9));
		Imgproc.threshold(imageTmp, imageTmp, 200, 255, Imgproc.THRESH_BINARY);

		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 4));
		Imgproc.morphologyEx(imageTmp, imageTmp, Imgproc.MORPH_CLOSE, kernel);

		//		Highgui.imwrite(outputNameTemplate + "_" + "morph" + "_.png", imageTmp);

		Imgproc.erode(imageTmp, imageTmp, new Mat(), new Point(-1, -1), 14);
		Imgproc.dilate(imageTmp, imageTmp, new Mat(), new Point(-1, -1), 14);

		//		Highgui.imwrite(outputNameTemplate + "_" + "erodeDilate" + "_.png", imageTmp);

		ArrayList<MatOfPoint> contours = new ArrayList<>();
		Mat hierarcy = new Mat();
		Imgproc.findContours(imageTmp, contours, hierarcy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		contours.sort(Comparator.comparingInt(cont -> (int) Imgproc.contourArea(cont)));
		double minArea = 30000;
		for (int i = contours.size() - 1; i > -1; i--) {
			if (Imgproc.contourArea(contours.get(i)) < minArea) break;

			MatOfPoint2f poly = new MatOfPoint2f();
			Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), poly, 3, true);

			Rect bBox = Imgproc.boundingRect(new MatOfPoint(poly.toArray()));
			Core.rectangle(imageMask, bBox.tl(), bBox.br(), new Scalar(255, 255, 255), -1);
		}

		Core.bitwise_not(imageMask, imageMask);
		//		Core.bitwise_not(imageSrc, imageClean, imageMask);
		Core.bitwise_and(imageSrc, imageMask, imageClean);
		Highgui.imwrite(outputNameTemplate + "_" + "clean" + "_.png", imageClean);
	}
	public static void prepare(File inputFile) {

		// Read image
		Mat imageSrc = Highgui.imread(inputFile.getAbsolutePath(), 0);

		Core.bitwise_not(imageSrc, imageSrc);
		//Imgproc.Canny(imageSrc, imageSrc, 80, 100, 3, true);

		Highgui.imwrite(inputFile.getAbsolutePath(), imageSrc);
	}

	public static void findLines(File inputFile, boolean horizontal) {

		String outputNameTemplate = FilenameUtils.removeExtension(inputFile.getAbsolutePath()) + "_test";

		Size morph = horizontal ? new Size(10, 1) : new Size(1, 10);

		// Read image
		Mat imageSrc = Highgui.imread(inputFile.getAbsolutePath(), 0);
		Size size = imageSrc.size();
		Core.bitwise_not(imageSrc, imageSrc);

		Mat imageErode = new Mat();
		Imgproc.threshold(imageSrc, imageErode, 60, 255, Imgproc.THRESH_BINARY);
		Imgproc.erode(imageErode, imageErode, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, morph), new Point(-1, -1), 5);
		Imgproc.dilate(imageErode, imageErode, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, morph), new Point(-1, -1), 12);
		Imgproc.threshold(imageErode, imageErode, 80, 255, Imgproc.THRESH_BINARY);
		Highgui.imwrite(outputNameTemplate + "_erode" + "_.png", imageErode);

		Mat lines = new Mat();
		Mat imageLines = Mat.zeros(size, CvType.CV_8UC1);
		Imgproc.HoughLinesP(imageErode, lines, 1, Math.PI / 180, 100, 200, 200);
		ArrayList<Line> acceptedLines = new ArrayList<>();
		for (int i = 0; i < lines.cols(); i++) {
			double[] vec = lines.get(0, i);

			Line line = new Line(vec[0], vec[1], vec[2], vec[3]);
			if (!acceptedLines.stream().anyMatch(existingLine -> existingLine.isNear(line))) {
				acceptedLines.add(line);
				line.draw(imageLines);
			}
		}
		Highgui.imwrite(outputNameTemplate + "_lines" + "_.png", imageLines);
	}
}
