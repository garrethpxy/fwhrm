package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.*;
import net.nekoinemo.documentrecognition.misc.VisitPassAreaStructure;
import net.nekoinemo.documentrecognition.misc.WorkPermitAreaStructure;
import net.nekoinemo.documentrecognition.processing.ImageHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WPDataBuilder implements IDocumentDataBuilder {

	private final static Pattern PATTERN_BBOX = Pattern.compile("bbox (\\d+) (\\d+) (\\d+) (\\d+)");
	private final static Pattern PATTERN_DATE = Pattern.compile("(\\d\\d).*?(\\d\\d).*?(\\d{4})");

	private final StringBuilder debugText = new StringBuilder();
	private String debugFilePrefix = "";

	// Work Permit
	private final static Pattern WP_PATTERN_TITLE = Pattern.compile("WORK.*?PERMIT");
	private WorkPermitAreaStructure workPermitAreaStructure = null;
	private BufferedImage workPermitCropped = null;

	// Visit Pass
	private final static Pattern VP_PATTERN_TITLE = Pattern.compile("VISIT.*?PASS");
	private VisitPassAreaStructure visitPassAreaStructure = null;
	private BufferedImage visitPassCropped = null;

	private final WPData wpData;

	public WPDataBuilder() {

		wpData = new WPData();
	}

	@Override
	public IDocumentData getDocumentData() {

		return wpData;
	}
	@Override
	public void processImage(RecognitionManager.RecognitionTarget target, RecognitionSettings[] settings) throws RecognitionManagerException {

		debugFilePrefix = target.getFile().getName();

		int currentSettings = 0;
		while (currentSettings < settings.length && getCompleteness() < settings[currentSettings].getPassingCompliteness()) {
			debugText.append("\nIteration " + (currentSettings + 1) + '/' + settings.length + '\t' + settings[currentSettings].toString() + '\n');

			// Try find locations of card sides and crop them out
			int currentImage = 0;
			while (workPermitAreaStructure == null && visitPassAreaStructure == null && currentImage < target.getImages().size()) {
				try {
					findCardLocations(ImageIO.read(target.getImages().get(currentImage)), settings[currentSettings]);
				} catch (IOException e) {
					throw new RecognitionManagerException("Can't read image " + target.getImages().get(currentImage).getName(), e);
				}

				currentImage++;
			}

			// Do recognition with current settings
			if (workPermitAreaStructure != null) {
				// Get data in selected areas
				String full_name = recognizeRegion(workPermitCropped, workPermitAreaStructure.getName(), settings[currentSettings]);
				String employer_name = recognizeRegion(workPermitCropped, workPermitAreaStructure.getEmployer(), settings[currentSettings]);
				String work_permit_category = recognizeRegion(workPermitCropped, workPermitAreaStructure.getCategory(), settings[currentSettings]);
				String work_permit_number = recognizeRegion(workPermitCropped, workPermitAreaStructure.getNumber(), settings[currentSettings]);

				String work_permit_expiration_date_raw = recognizeRegion(workPermitCropped, workPermitAreaStructure.getDateOfExpiry(), settings[currentSettings]);
				Matcher matcherExpDate = PATTERN_DATE.matcher(work_permit_expiration_date_raw);
				String work_permit_expiration_date = "";
				if (matcherExpDate.find())
					work_permit_expiration_date = matcherExpDate.group(1) + '/' + matcherExpDate.group(2) + '/' + matcherExpDate.group(3);

				// Debug text
				debugText.append("full_name: { " + full_name + " }\n");
				debugText.append("employer_name: { " + employer_name + " }\n");
				debugText.append("work_permit_category: { " + work_permit_category + " }\n");
				debugText.append("work_permit_number: { " + work_permit_number + " }\n");
				debugText.append("work_permit_expiration_date_raw: { " + work_permit_expiration_date_raw + " }\n");
				debugText.append("work_permit_expiration_date: { " + work_permit_expiration_date + " }\n");

				// Save found data
				if (!full_name.isEmpty()) wpData.full_name = full_name;
				if (!employer_name.isEmpty()) wpData.employer_name = employer_name;
				if (!work_permit_category.isEmpty()) wpData.work_permit_category = work_permit_category;
				if (!work_permit_number.isEmpty()) wpData.work_permit_number = work_permit_number;
				if (!work_permit_expiration_date.isEmpty())
					wpData.work_permit_expiration_date = work_permit_expiration_date;
			}
			if (visitPassAreaStructure != null) {
				// Get data in selected areas
				String nationality = recognizeRegion(visitPassCropped, visitPassAreaStructure.getNationality(), settings[currentSettings]);
				String fin_number = recognizeRegion(visitPassCropped, visitPassAreaStructure.getFinNumber(), settings[currentSettings]);

				String date_of_birth_raw = recognizeRegion(visitPassCropped, visitPassAreaStructure.getDateOfBirth(), settings[currentSettings]);
				String date_of_birth = "";
				Matcher matcherExpDate = PATTERN_DATE.matcher(date_of_birth_raw);
				if (matcherExpDate.find())
					date_of_birth = matcherExpDate.group(1) + '/' + matcherExpDate.group(2) + '/' + matcherExpDate.group(3);

				// Debug text
				debugText.append("nationality: { " + nationality + " }\n");
				debugText.append("fin_number: { " + fin_number + " }\n");
				debugText.append("date_of_birth_raw: { " + date_of_birth_raw + " }\n");
				debugText.append("date_of_birth: { " + date_of_birth + " }\n");

				// Save found data
				if (!nationality.isEmpty()) wpData.nationality = nationality;
				if (!fin_number.isEmpty()) wpData.fin_number = fin_number;
				if (!date_of_birth.isEmpty()) wpData.date_of_birth = date_of_birth;
			}

			currentSettings++;
		}

		// Debug graphical output
		if (workPermitAreaStructure != null && RecognitionManager.INSTANCE.isDebugOutput()) {
			BufferedImage debugWP = ImageHelper.copyImage(workPermitCropped);
			Graphics2D debugGraph = debugWP.createGraphics();
			debugGraph.setColor(Color.BLACK);

			Helper.graphics2DDrawRectangle(debugGraph, workPermitAreaStructure.getMainArea());
			Helper.graphics2DDrawRectangle(debugGraph, workPermitAreaStructure.getTitle());
			Helper.graphics2DDrawRectangle(debugGraph, workPermitAreaStructure.getName());
			Helper.graphics2DDrawRectangle(debugGraph, workPermitAreaStructure.getCategory());
			Helper.graphics2DDrawRectangle(debugGraph, workPermitAreaStructure.getEmployer());
			Helper.graphics2DDrawRectangle(debugGraph, workPermitAreaStructure.getNumber());
			Helper.graphics2DDrawRectangle(debugGraph, workPermitAreaStructure.getDateOfExpiry());
			debugGraph.dispose();

			try {
				ImageIO.write(debugWP, "png", RecognitionManager.INSTANCE.getDebugFile(debugFilePrefix + "_WPMarked.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (visitPassAreaStructure != null && RecognitionManager.INSTANCE.isDebugOutput()) {
			BufferedImage debugWP = ImageHelper.copyImage(visitPassCropped);
			Graphics2D debugGraph = debugWP.createGraphics();
			debugGraph.setColor(Color.BLACK);

			Helper.graphics2DDrawRectangle(debugGraph, visitPassAreaStructure.getMainArea());
			Helper.graphics2DDrawRectangle(debugGraph, visitPassAreaStructure.getTitle());
			Helper.graphics2DDrawRectangle(debugGraph, visitPassAreaStructure.getNationality());
			Helper.graphics2DDrawRectangle(debugGraph, visitPassAreaStructure.getFinNumber());
			Helper.graphics2DDrawRectangle(debugGraph, visitPassAreaStructure.getDateOfBirth());
			debugGraph.dispose();

			try {
				ImageIO.write(debugWP, "png", RecognitionManager.INSTANCE.getDebugFile(debugFilePrefix + "_VPMarked.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public int getCompleteness() {

		return wpData.getCompleteness();
	}
	@Override
	public String getDebugText() {

		return debugText.toString();
	}

	private String recognizeRegion(BufferedImage image, Rectangle region, RecognitionSettings settings) throws RecognitionManagerException {

		return Helper.getProperTextFromJSoupDoc(Jsoup.parse(RecognitionManager.INSTANCE.recognize(image, region, settings))).trim();
	}
	/**
	 * Try to find location of the cards on this image. Image will be rotated in case both sides of card are present and have different orientation
	 *
	 * @param image
	 * @param settings
	 *
	 * @throws RecognitionManagerException
	 */
	private void findCardLocations(BufferedImage image, RecognitionSettings settings) throws RecognitionManagerException {

		int currentRotation = 0;

		while (currentRotation < 360 && workPermitAreaStructure == null) {
			Document hOCRText = Jsoup.parse(RecognitionManager.INSTANCE.recognize(image, null, settings));

			// Try find the location of a WP card side
			if (workPermitAreaStructure == null) {
				// Get card title location
				Rectangle titleBBox = findWorkPermitTitleBBox(hOCRText);
				if (titleBBox != null) {
					// Get area containing the card
					Rectangle wpEstimatedLocation = WorkPermitAreaStructure.estimateLocation(titleBBox, image.getWidth(), image.getHeight());
					// Get deskewed crop of the card
					workPermitCropped = ImageHelper.deskewImage(ImageHelper.copyImage(image, wpEstimatedLocation));

					// Debug save cropped region
					if (RecognitionManager.INSTANCE.isDebugOutput()) {
						try {
							ImageIO.write(workPermitCropped, "png", RecognitionManager.INSTANCE.getDebugFile(debugFilePrefix + "_WPCropped.png"));
						} catch (IOException e) {}
					}

					// Get card title fine location
					titleBBox = findWorkPermitTitleBBox(workPermitCropped, settings);
					// Get card fine location
					if (titleBBox != null)
						workPermitAreaStructure = new WorkPermitAreaStructure(titleBBox, workPermitCropped.getWidth(), workPermitCropped.getHeight());
					else workPermitCropped = null;
				}
			}
			// Try find the location of a VP card side
			if (visitPassAreaStructure == null) {
				// Get card title location
				Rectangle titleBBox = findVisitPassTitleBBox(hOCRText);
				if (titleBBox != null) {
					// Get area containing the card
					Rectangle vpEstimatedLocation = VisitPassAreaStructure.estimateLocation(titleBBox, image.getWidth(), image.getHeight());
					// Get deskewed crop of the card
					visitPassCropped = ImageHelper.deskewImage(ImageHelper.copyImage(image, vpEstimatedLocation));

					// Debug save cropped region
					if (RecognitionManager.INSTANCE.isDebugOutput()) {
						try {
							ImageIO.write(visitPassCropped, "png", RecognitionManager.INSTANCE.getDebugFile(debugFilePrefix + "_VPCropped.png"));
						} catch (IOException e) {}
					}

					// Get card title fine location
					titleBBox = findVisitPassTitleBBox(visitPassCropped, settings);
					// Get card fine location
					if (titleBBox != null)
						visitPassAreaStructure = new VisitPassAreaStructure(titleBBox, visitPassCropped.getWidth(), visitPassCropped.getHeight());
					else visitPassCropped = null;
				}
			}

			currentRotation += 90;
		}
	}

	private Rectangle findWorkPermitTitleBBox(BufferedImage image, RecognitionSettings settings) throws RecognitionManagerException {

		Document hOCRText = Jsoup.parse(RecognitionManager.INSTANCE.recognize(image, null, settings));
		return findWorkPermitTitleBBox(hOCRText);
	}
	private Rectangle findWorkPermitTitleBBox(Document hOCRText) {

		for (Element ocrLine : hOCRText.getElementsByClass("ocr_line")) {
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

				return new Rectangle(x, y, w, h);
			}
		}

		return null;
	}
	private Rectangle findVisitPassTitleBBox(BufferedImage image, RecognitionSettings settings) throws RecognitionManagerException {

		Document hOCRText = Jsoup.parse(RecognitionManager.INSTANCE.recognize(image, null, settings));
		return findVisitPassTitleBBox(hOCRText);
	}
	private Rectangle findVisitPassTitleBBox(Document hOCRText) {

		for (Element ocrLine : hOCRText.getElementsByClass("ocr_line")) {
			if (VP_PATTERN_TITLE.matcher(ocrLine.text()).find()) {
				int x = 0;
				int y = 0;
				int w = 0;
				int h = 0;

				for (Element ocrxWord : ocrLine.getElementsByClass("ocrx_word")) {
					if (ocrxWord.text().contains("VISIT")) {
						Matcher matcherWork = PATTERN_BBOX.matcher(ocrxWord.attr("title"));
						matcherWork.find();
						x = Integer.parseInt(matcherWork.group(1));
						y = Integer.parseInt(matcherWork.group(2));
					} else if (ocrxWord.text().contains("PASS")) {
						Matcher matcherPermit = PATTERN_BBOX.matcher(ocrxWord.attr("title"));
						matcherPermit.find();
						w = Integer.parseInt(matcherPermit.group(3)) - x;
						h = Integer.parseInt(matcherPermit.group(4)) - y;
					}
				}

				return new Rectangle(x, y, w, h);
			}
		}

		return null;
	}
}
