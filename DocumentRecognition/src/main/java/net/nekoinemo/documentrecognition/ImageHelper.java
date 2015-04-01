package net.nekoinemo.documentrecognition;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageHelper {

	public static BufferedImage copyImage(BufferedImage sourceImage) {

		return copyImage(sourceImage, new Rectangle(0, 0, sourceImage.getWidth(), sourceImage.getHeight()));
	}
	public static BufferedImage copyImage(BufferedImage sourceImage, Rectangle sourceArea) {

		BufferedImage result = new BufferedImage(sourceArea.width, sourceArea.height, sourceImage.getType());
		Graphics2D graphics2D = result.createGraphics();

		graphics2D.drawImage(sourceImage, 0, 0, result.getWidth() - 1, result.getHeight() - 1, sourceArea.x, sourceArea.y, sourceArea.x + sourceArea.width - 1, sourceArea.y + sourceArea.height - 1, null);

		return result;
	}
	public static BufferedImage deskewImage(BufferedImage image) {

		return deskewImage(image, image.getWidth() / 2, image.getHeight() / 2);
	}
	public static BufferedImage deskewImage(BufferedImage image, int centerX, int centerY) {

		double skewAngle = Deskew.getSkewAngle(image);
		return Deskew.rotate(image, skewAngle, centerX, centerY);
	}
	public static BufferedImage rotate(BufferedImage image, double angle) {

		double sin = Math.abs(Math.sin(Math.toRadians(angle)));
		double cos = Math.abs(Math.cos(Math.toRadians(angle)));

		int w = image.getWidth();
		int h = image.getHeight();

		int newW = (int) Math.floor(w * cos + h * sin);
		int newH = (int) Math.floor(h * cos + w * sin);
		BufferedImage newImage = new BufferedImage(newW, newH, image.getType());
		Graphics2D graphics2D = newImage.createGraphics();

		graphics2D.translate((newW - w) / 2, (newH - h) / 2);
		graphics2D.rotate(Math.toRadians(angle), w / 2, h / 2);
		graphics2D.drawRenderedImage(image, null);
		graphics2D.dispose();

		return newImage;
	}
}
