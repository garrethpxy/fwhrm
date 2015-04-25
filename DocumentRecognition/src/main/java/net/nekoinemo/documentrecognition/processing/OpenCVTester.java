package net.nekoinemo.documentrecognition.processing;

import org.apache.commons.io.FilenameUtils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;

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

		// Write image (linesHV)
		Highgui.imwrite(outputNameTemplate + "_lines_.png", imageLines);

		// Rotating
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(new Point(size.width / 2, size.height / 2), -angle, 1);
		Mat imageRotated = new Mat();
		Imgproc.warpAffine(imageSrc, imageRotated, rotationMatrix, size, Imgproc.INTER_CUBIC);

		// Write image (rotated)
		Highgui.imwrite(outputNameTemplate + "_rotated_.png", imageRotated);
	}
}
