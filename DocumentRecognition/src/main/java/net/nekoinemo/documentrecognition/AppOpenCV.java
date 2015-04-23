package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.processing.OpenCVHelper;
import net.nekoinemo.documentrecognition.processing.math.Line;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class AppOpenCV {

	public static Path AppLocation;

	public static void main(String[] args) throws URISyntaxException, FileNotFoundException {

		AppLocation = Paths.get(AppOpenCV.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

		try {
			// Load OpenCV library
			OpenCVHelper.loadOpenCVLibs();

			// Process all images in provided folder
			ArrayList<File> targets = new ArrayList<>();

			for (File inputFile : new File(args[0]).listFiles()) {
				if (!inputFile.isFile()) continue;
				if (FilenameUtils.removeExtension(inputFile.getName()).endsWith("_")) {
					inputFile.delete();
					continue;
				}

				targets.add(inputFile);
			}
			for (File target : targets) {
				String outputNameTemplate = FilenameUtils.removeExtension(target.getAbsolutePath());

				Mat image = Highgui.imread(target.getAbsolutePath(), 0);
				Core.bitwise_not(image, image);

				ArrayList<Line> linesH = OpenCVHelper.lines(image, OpenCVHelper.HORIZONTAL, 200);
				ArrayList<Line> linesV = OpenCVHelper.lines(image, OpenCVHelper.VERTICAL, 200);

				Mat imageLines = Mat.zeros(image.size(), CvType.CV_8UC3);
				Scalar colour = new Scalar(0, 0, 255);
				for (Line line : linesH) {
					line.draw(imageLines, colour, 3);
				}
				for (Line line : linesV) {
					line.draw(imageLines, colour, 3);
				}
				Highgui.imwrite(outputNameTemplate + "_lines" + "_.png", imageLines);
				image.release();
				imageLines.release();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
