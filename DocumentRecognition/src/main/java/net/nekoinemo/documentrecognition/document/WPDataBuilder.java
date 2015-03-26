package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.*;
import net.nekoinemo.documentrecognition.misc.VisitPassAreaStructure;
import net.nekoinemo.documentrecognition.misc.WorkPermitAreaStructure;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WPDataBuilder implements DocumentDataBuilder {

	private final static Pattern PATTERN_BBOX = Pattern.compile("bbox (\\d+) (\\d+) (\\d+) (\\d+)");

	// Work Permit
	private final static float WP_MODIFIER_WIDTH = 4.8f;
	private final static float WP_MODIFIER_HEIGHT = 25.0f;
	private final static Pattern WP_PATTERN_TITLE = Pattern.compile("WORK.*?PERMIT");

	private WorkPermitAreaStructure workPermitAreaStructure = null;
	private VisitPassAreaStructure visitPassAreaStructure = null;

	private final WPData wpData;

	public WPDataBuilder() {

		wpData = new WPData();
	}

	@Override
	public DocumentData getDocumentData() {

		return wpData;
	}
	@Override
	public void ProcessImage(File target, RecognitionSettings settings) throws RecognizerException {

		BufferedImage image;
		Document documentWhole = null;
		Recognizer recognizer = Recognizer.getInstance();

		try {
			image = ImageIO.read(target);
		} catch (IOException e) {
			RecognizerException exception = new RecognizerException("Can't read image " + target.getName(), e);
			throw exception;
		}

		// If bounding boxes still not found - get full text and attempt to find them
		if (workPermitAreaStructure == null || visitPassAreaStructure == null) {
			documentWhole = Jsoup.parse(recognizer.RecognizeFile(target, null, settings));
		}

		// WP side

		if (workPermitAreaStructure == null) {
			for (Element ocrLine : documentWhole.getElementsByClass("ocr_line")) {
				if (WP_PATTERN_TITLE.matcher(ocrLine.text()).find()) {
					int x = 0;
					int y = 0;
					int w = 0;
					int h = 0;

					for (Element ocrxWord : ocrLine.getElementsByClass("ocrx_word")) {
						if (ocrxWord.text().contains("WORK")) {
							Matcher matcherWork = PATTERN_BBOX.matcher(ocrxWord.attr("title"));
							matcherWork.find();
							x = Integer.parseInt(matcherWork.group(1));
							y = Integer.parseInt(matcherWork.group(2));
						} else if (ocrxWord.text().contains("PERMIT")) {
							Matcher matcherPermit = PATTERN_BBOX.matcher(ocrxWord.attr("title"));
							matcherPermit.find();
							w = Integer.parseInt(matcherPermit.group(3)) - x;
							h = Integer.parseInt(matcherPermit.group(4)) - y;
						}
					}

					workPermitAreaStructure = new WorkPermitAreaStructure(new Rectangle(x, y, w, h), image.getWidth(), image.getHeight());
					break;
				}
			}
		}
		if (workPermitAreaStructure != null) {
			String employer = Helper.GetProperTextFromJSoupDoc(Jsoup.parse(recognizer.RecognizeFile(target, workPermitAreaStructure.getEmployer(), settings)));

			// Debug
			if (recognizer.isDebugOutput()) {
				try {
					BufferedImage debugImage = Helper.BufferedImageDeepCopy(image);
					// Only WP card (clean)
					ImageIO.write(debugImage.getSubimage(workPermitAreaStructure.getMainArea().x, workPermitAreaStructure.getMainArea().y, workPermitAreaStructure.getMainArea().width, workPermitAreaStructure.getMainArea().height), "png", new File(recognizer.getDebugOutputDirectory(), target.getName().concat("_debugWP.png")));
					Graphics2D debugGraphics = debugImage.createGraphics();
					debugGraphics.setColor(Color.BLACK);
					Helper.Graphics2DDrawRectangle(debugGraphics, workPermitAreaStructure.getMainArea());
					Helper.Graphics2DDrawRectangle(debugGraphics, workPermitAreaStructure.getTitle());
					// Full image with cards (marked)
					ImageIO.write(debugImage, "png", new File(recognizer.getDebugOutputDirectory(), target.getName().concat("_debugCards.png")));
					debugGraphics.dispose();

					debugImage = Helper.BufferedImageDeepCopy(image);
					debugGraphics = debugImage.createGraphics();
					debugGraphics.setColor(Color.BLACK);
					Helper.Graphics2DDrawRectangle(debugGraphics, workPermitAreaStructure.getName());
					Helper.Graphics2DDrawRectangle(debugGraphics, workPermitAreaStructure.getCategory());
					Helper.Graphics2DDrawRectangle(debugGraphics, workPermitAreaStructure.getEmployer());
					// Only WP card (marked)
					ImageIO.write(debugImage.getSubimage(workPermitAreaStructure.getMainArea().x, workPermitAreaStructure.getMainArea().y, workPermitAreaStructure.getMainArea().width, workPermitAreaStructure.getMainArea().height), "png", new File(recognizer.getDebugOutputDirectory(), target.getName().concat("_debugWPMarked.png")));
					debugGraphics.dispose();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// VP side

	}
	@Override
	public int getCompleteness() {

		return wpData.getCompleteness();
	}
	@Override
	public void FillEmptyFields(DocumentData value) throws RecognizerException {

		if (!value.getClass().equals(MOMData.class))
			throw new RecognizerException("Passed subclass of DocumentData doesn't match this subclass");

		for (String fieldName : wpData.DATA_FIELDS) {
			try {
				Field field = MOMData.class.getDeclaredField(fieldName);

				field.setAccessible(true);
				if (field.get(wpData) == null) field.set(wpData, field.get(value));
				field.setAccessible(false);
			} catch (Exception e) { }
		}

		workPermitAreaStructure = ((WPDataBuilder) value).workPermitAreaStructure;
		visitPassAreaStructure = ((WPDataBuilder) value).visitPassAreaStructure;
	}
	@Override
	public void FillFields(DocumentData value) throws RecognizerException {

		if (!value.getClass().equals(MOMData.class))
			throw new RecognizerException("Passed subclass of DocumentData doesn't match this subclass");

		for (String fieldName : wpData.DATA_FIELDS) {
			try {
				Field field = MOMData.class.getDeclaredField(fieldName);

				field.setAccessible(true);
				Object valuesValue = field.get(value);
				if (valuesValue != null) field.set(wpData, valuesValue);
				field.setAccessible(false);
			} catch (Exception e) { }
		}

		workPermitAreaStructure = ((WPDataBuilder) value).workPermitAreaStructure;
		visitPassAreaStructure = ((WPDataBuilder) value).visitPassAreaStructure;
	}
}
