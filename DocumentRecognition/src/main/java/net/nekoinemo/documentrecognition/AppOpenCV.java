package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.processing.OpenCVHelper;
import net.nekoinemo.documentrecognition.processing.OpenCVTester;
import net.nekoinemo.documentrecognition.processing.math.Line;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.*;
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
import java.util.Comparator;

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
				Size size = image.size();
				Core.bitwise_not(image, image);

				ArrayList<Line> linesH = OpenCVHelper.linesHV(image, OpenCVHelper.HORIZONTAL, 200);
				linesH.sort(new Comparator<Line>() {
					@Override
					public int compare(Line o1, Line o2) {

						return (int) (o1.getPoint1().y - o2.getPoint1().y);
					}
				});
				ArrayList<Line> linesV = OpenCVHelper.linesHV(image, OpenCVHelper.VERTICAL, 200);
				linesV.sort(new Comparator<Line>() {
					@Override
					public int compare(Line o1, Line o2) {

						return (int) (o1.getPoint1().x - o2.getPoint1().x);
					}
				});
				image.release();

				BufferedImage output = new BufferedImage((int)size.width, (int)size.height, BufferedImage.TYPE_BYTE_BINARY);
				Graphics2D graphics2D = output.createGraphics();
				graphics2D.setColor(Color.WHITE);
				for (int i = 0; i < linesH.size(); i++) {
					Line line1 = linesH.get(i);
					Line line2;
					ArrayList<Line> intersections1 = new ArrayList<>();
					ArrayList<Line> intersections2 = new ArrayList<>();

					for (Line line : linesV) {
						if (line1.intersects(line)) intersections1.add(line);
					}

					// todo if has intersections - use them as x1 & x2.
					// todo if pair line has intersections - use the min/max of them and original ones as x1 & x2
					// todo only count intersections if they're in the first/last 20% of the line

					if ((i == linesH.size() - 1)) {
						line2 = new Line(line1.getPoint1().x, output.getHeight(), line1.getPoint2().x, output.getHeight()); // Till the bottom
					} else if (!OpenCVTester.isHorizontalPair(line1, linesH.get(i + 1), 50)) {
						if (i > 0) {
							graphics2D.drawLine((int)line1.getPoint1().x, (int)line1.getPoint1().y, (int)line1.getPoint2().x, (int)line1.getPoint2().y);
							continue;
						}
						line2 = new Line(line1.getPoint1().x, 0, line1.getPoint2().x, 0); // from the top
					} else {
						line2 = linesH.get(i + 1);
						linesH.remove(i + 1);
					}

					Helper.graphics2DDrawRectangle(graphics2D, OpenCVTester.rectangle(line1, line2));
				}
				graphics2D.dispose();
				ImageIO.write(output, "png", new File(outputNameTemplate + "_lines_.png"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
