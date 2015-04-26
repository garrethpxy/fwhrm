package net.nekoinemo.documentrecognition.processing;

import net.nekoinemo.documentrecognition.processing.math.Line;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

public class OpenCVTester {

	// Tested on MOM
	public static void doRotation2(File input) {

		String outputNameTemplate = FilenameUtils.removeExtension(input.getAbsolutePath());

		// Read image
		Mat original = Highgui.imread(input.getAbsolutePath(), 0);
		Size size = original.size();
		Mat tmp = new Mat();

		// Preparation
		Core.bitwise_not(original, tmp);
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 1));
		Imgproc.erode(tmp, tmp, kernel, new Point(-1, -1), 12);
		Imgproc.dilate(tmp, tmp, kernel, new Point(-1, -1), 12);
		Imgproc.medianBlur(tmp, tmp, 3);
		Imgproc.threshold(tmp, tmp, 80, 255, Imgproc.THRESH_BINARY);
		Highgui.imwrite(outputNameTemplate + "_" + "raw" + "_.png", tmp);

		// Lines
		double angle = 0;
		Mat imageLines = Mat.zeros(size, CvType.CV_8UC1);
		Mat lines = new Mat();
		ArrayList<Line> acceptedLines = new ArrayList<>();
		Imgproc.HoughLinesP(tmp, lines, 1, Math.PI / 180, 100, 100, 0);
		for (int i = 0; i < lines.cols(); i++) {
			double[] vec = lines.get(0, i);

			Line line = new Line(vec[0], vec[1], vec[2], vec[3]);
			if (line.isHorizontal() && !acceptedLines.stream().anyMatch(existingLine -> existingLine.isNear(line))) {
				acceptedLines.add(line);
				line.draw(imageLines);
				angle += Math.toDegrees(Math.atan2(line.getPoint2().y - line.getPoint1().y, line.getPoint2().x - line.getPoint1().x));
			}
		}
		angle /= acceptedLines.size();
		System.out.println(input.getName().concat(" : ").concat(String.valueOf(angle)));
		Highgui.imwrite(outputNameTemplate + "_" + "lines" + "_.png", imageLines);
		imageLines.release();
		tmp.release();

		// Rotating
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(size.width / 2, size.height / 2), angle, 1);
		Mat rotated = new Mat();
		Imgproc.warpAffine(original, rotated, rotationMatrix, size, Imgproc.INTER_CUBIC, Imgproc.BORDER_CONSTANT, new Scalar(255, 255, 255));
		Highgui.imwrite(outputNameTemplate + "_" + "rotated" + "_.png", rotated);
		rotated.release();

		original.release();
	}
	// WIP
	public static void doRotation3(File input) {

		String outputNameTemplate = FilenameUtils.removeExtension(input.getAbsolutePath());

		// Read image
		Mat original = Highgui.imread(input.getAbsolutePath(), 0);
		Size size = original.size();
		Mat tmp = new Mat();

		// Preparation
		Core.bitwise_not(original, tmp);
		Imgproc.medianBlur(tmp, tmp, 3);
		Imgproc.threshold(tmp, tmp, 80, 255, Imgproc.THRESH_BINARY);

		Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(20, 4));
		Imgproc.morphologyEx(tmp, tmp, Imgproc.MORPH_CLOSE, morphKernel);

		Imgproc.medianBlur(tmp, tmp, 3);

		Imgproc.Canny(tmp, tmp, 150, 200);
		Highgui.imwrite(outputNameTemplate + "_" + "raw" + "_.png", tmp);

		// Lines
		double angle = 0;
		Mat imageLines = Mat.zeros(size, CvType.CV_8UC1);
		Mat lines = new Mat();
		ArrayList<Line> acceptedLines = new ArrayList<>();
		Imgproc.HoughLinesP(tmp, lines, 1, Math.PI / 180, 100, 100, 50);
		for (int i = 0; i < lines.cols(); i++) {
			double[] vec = lines.get(0, i);

			Line line = new Line(vec[0], vec[1], vec[2], vec[3]);
			if (!line.isVertical() && !acceptedLines.stream().anyMatch(existingLine -> existingLine.isNear(line))) {
				acceptedLines.add(line);
				line.draw(imageLines);
				angle += Math.toDegrees(Math.atan2(line.getPoint2().y - line.getPoint1().y, line.getPoint2().x - line.getPoint1().x));
			}
		}
		angle /= acceptedLines.size();
		System.out.println(input.getName().concat(" : ").concat(String.valueOf(angle)));
		Highgui.imwrite(outputNameTemplate + "_" + "lines" + "_.png", imageLines);
		imageLines.release();
		tmp.release();

		// Rotating
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(size.width / 2, size.height / 2), angle, 1);
		Mat rotated = new Mat();
		Imgproc.warpAffine(original, rotated, rotationMatrix, size, Imgproc.INTER_CUBIC, Imgproc.BORDER_CONSTANT, new Scalar(255, 255, 255));
		Highgui.imwrite(outputNameTemplate + "_" + "aRotated" + "_.png", rotated);
		rotated.release();

		original.release();
	}
}
