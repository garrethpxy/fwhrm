package net.nekoinemo.documentrecognition;

import net.nekoinemo.documentrecognition.processing.OpenCVHelper;
import net.nekoinemo.documentrecognition.processing.OpenCVTester;
import net.nekoinemo.documentrecognition.processing.math.Line;
import net.nekoinemo.documentrecognition.processing.math.MathHelper;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.*;
import org.opencv.core.Point;
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

				Scalar colour = new Scalar(255, 255, 255);
				Mat imageLines = Mat.zeros(size, CvType.CV_8UC1);
				linesH.forEach(line -> line.draw(imageLines, colour, 3));
				linesV.forEach(line -> line.draw(imageLines, colour, 3));
				Highgui.imwrite(outputNameTemplate + "_lines_.png", imageLines);
				imageLines.release();

				BufferedImage output = new BufferedImage((int) size.width, (int) size.height, BufferedImage.TYPE_BYTE_BINARY);
				Graphics2D graphics2D = output.createGraphics();
				graphics2D.setColor(Color.WHITE);
				int regionN = 0;
				BufferedImage input = ImageIO.read(target);
				for (int i = 0; i < linesH.size(); i++) {
					Line line1 = linesH.get(i);
					Line line2;

					if ((i == linesH.size() - 1)) {
						line2 = new Line(line1.getPoint1().x, output.getHeight(), line1.getPoint2().x, output.getHeight()); // Till the bottom
					} else if (!OpenCVTester.isHorizontalPair(line1, linesH.get(i + 1), 50)) {
						if (i > 0) {
							graphics2D.drawLine((int) line1.getPoint1().x, (int) line1.getPoint1().y, (int) line1.getPoint2().x, (int) line1.getPoint2().y);
							continue;
						}
						line2 = new Line(line1.getPoint1().x, 0, line1.getPoint2().x, 0); // from the top
					} else {
						line2 = linesH.get(i + 1);
						linesH.remove(i + 1);
					}

					// Default coordinates, based on the lines ends
					int x1 = (int) Math.max(line1.getPoint1().x, line2.getPoint1().x);
					int y1 = (int) Math.min(line1.getPoint1().y, line2.getPoint1().y);
					int x2 = (int) Math.min(line1.getPoint2().x, line2.getPoint2().x) - 30;
					int y2 = (int) Math.max(line1.getPoint1().y, line2.getPoint1().y);

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

					// Rectangle creation
					int borderThickness = 4;
					Rectangle region = new Rectangle(x1 + borderThickness, y1 + borderThickness, x2 - x1 - 2 * borderThickness, y2 - y1 - 2 * borderThickness);
					if (region.width > 0 && region.height > 0) {
						Helper.graphics2DDrawRectangle(graphics2D, region);
						ImageIO.write(input.getSubimage(region.x, region.y, region.width, region.height), "png", new File(outputNameTemplate + "_reion" + regionN + "_.png"));
					}
					regionN++;
				}
				graphics2D.dispose();
				ImageIO.write(output, "png", new File(outputNameTemplate + "_areas_.png"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
