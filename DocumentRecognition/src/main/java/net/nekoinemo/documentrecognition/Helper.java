package net.nekoinemo.documentrecognition;

import net.sourceforge.vietocr.PdfUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Helper {

	private Helper() {}

	public static String getProperTextFromJSoupDoc(Document document) {

		StringBuilder stringBuilder = new StringBuilder();

		for (Element ocrLine : document.getElementsByClass("ocr_line")) {
			stringBuilder.append(ocrLine.text());
			stringBuilder.append(System.lineSeparator());
		}

		return stringBuilder.toString();
	}
	public static Rectangle cropToRectangle(Rectangle rectangle, Rectangle availableArea) {

		return new Rectangle(rectangle.x < availableArea.x ? availableArea.x : rectangle.x, rectangle.y < availableArea.y ? availableArea.y : rectangle.y, rectangle.x + rectangle.width > availableArea.x + availableArea.width ? availableArea.x + availableArea.width - rectangle.x : rectangle.width, rectangle.y + rectangle.height > availableArea.y + availableArea.height ? availableArea.y + availableArea.height - rectangle.y : rectangle.height);
	}
	public static ArrayList<File> imagesFromFile(File file) throws IOException {

		ArrayList<File> result = new ArrayList<>();

		if (!file.isFile()) return result;

		if (FilenameUtils.isExtension(file.getName().toLowerCase(), "pdf")) {
			File[] extractedFiles = PdfUtilities.convertPdf2Png(file);
			for (int i = 0; i < extractedFiles.length; i++) {
				File fileSrc = extractedFiles[i];
				File fileDst = new File(RecognitionManager.getInstance().getWorkingImagesDirectory(), new StringBuilder(FilenameUtils.removeExtension(file.getName())).append('_').append(i).append(".png").toString());
				FileUtils.moveFile(fileSrc, fileDst);
				result.add(fileDst);
			}
		} else {
			File fileDst = new File(RecognitionManager.getInstance().getWorkingImagesDirectory(), file.getName());
			FileUtils.copyFile(file, fileDst);
			result.add(fileDst);
		}

		return result;
	}
	public static void rotateImageFile(File file, int angle) throws IOException {

		if (angle % 360 == 0) return;

		BufferedImage image = ImageIO.read(file);
		image = ImageHelper.rotate(image, angle);
		ImageIO.write(image, FilenameUtils.getExtension(file.getName()), file);
	}
	public static void graphics2DDrawRectangle(Graphics2D graphics2D, Rectangle rectangle) {

		graphics2D.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
	}
}
