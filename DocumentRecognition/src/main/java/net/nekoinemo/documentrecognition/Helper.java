package net.nekoinemo.documentrecognition;

import net.sourceforge.vietocr.PdfUtilities;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.ArrayList;

public class Helper {

	private Helper() {}

	public static ArrayList<File> GetImagesFromFile(File file) {

		ArrayList<File> result = new ArrayList<>();

		if (!file.isFile()) return result;

		String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));

		if (file.getName().toLowerCase().endsWith(".pdf")) {
			File[] extractedFiles = PdfUtilities.convertPdf2Png(file);
			for (int i = 0; i < extractedFiles.length; i++) {
				File fileSrc = extractedFiles[i];
				File fileDst = new File(Recognizer.getInstance().getWorkingImagesDirectory(), new StringBuilder(fileName).append('_').append(i).append(".png").toString());
				fileSrc.renameTo(fileDst);
				result.add(fileDst);
			}
		} else {
			result.add(file);
		}

		return result;
	}
	public static String GetProperTextFromJSoupDoc(Document document) {

		StringBuilder stringBuilder = new StringBuilder();

		for (Element ocrLine : document.getElementsByClass("ocr_line")) {
			stringBuilder.append(ocrLine.text());
			stringBuilder.append(System.lineSeparator());
		}

		return stringBuilder.toString();
	}
	public static BufferedImage BufferedImageDeepCopy(BufferedImage sourceImage) {

		ColorModel colorModel = sourceImage.getColorModel();
		WritableRaster raster = sourceImage.copyData(null);

		return new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null).getSubimage(0, 0, sourceImage.getWidth(), sourceImage.getHeight());
	}
	public static Rectangle CropToSize(Rectangle rectangle, Rectangle availableArea) {

		return new Rectangle(rectangle.x < availableArea.x ? availableArea.x : rectangle.x,
				rectangle.y < availableArea.y ? availableArea.y : rectangle.y,
				rectangle.x + rectangle.width > availableArea.x + availableArea.width ? availableArea.x + availableArea.width - rectangle.x : rectangle.width,
				rectangle.y + rectangle.height > availableArea.y + availableArea.height ? availableArea.y + availableArea.height - rectangle.y : rectangle.height);
	}
	public static void Graphics2DDrawRectangle(Graphics2D graphics2D, Rectangle rectangle){

		graphics2D.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
	}
}
