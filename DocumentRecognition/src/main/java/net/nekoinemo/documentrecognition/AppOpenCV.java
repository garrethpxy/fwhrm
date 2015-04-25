package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.processing.OpenCVHelper;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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

				ArrayList<Rectangle> regions = OpenCVHelper.regionsMOM(image);
				image.release();

				BufferedImage input = ImageIO.read(target);
				for (int i = 0; i < regions.size(); i++) {
					Rectangle region = regions.get(i);
					ImageIO.write(input.getSubimage(region.x, region.y, region.width, region.height), "png", new File(outputNameTemplate + "_reion" + i + "_.png"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
