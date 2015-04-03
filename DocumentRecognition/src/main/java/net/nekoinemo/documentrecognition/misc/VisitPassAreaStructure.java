package net.nekoinemo.documentrecognition.misc;

import net.nekoinemo.documentrecognition.Helper;

import java.awt.*;

public class VisitPassAreaStructure {

	private final static float MODIFIER_ROUGHT_MAIN_WIDTH = 6f;
	private final static float MODIFIER_ROUGHT_MAIN_HEIGHT_UP = 2.0f;
	private final static float MODIFIER_ROUGHT_MAIN_HEIGHT_DOWN = 29.0f;
	private final static float MODIFIER_MAIN_WIDTH = 5.5f;
	private final static float MODIFIER_MAIN_HEIGHT = 22.0f;

	private final Rectangle mainArea;
	private final Rectangle title;

	private final Rectangle nationality;
	private final Rectangle finNumber;
	private final Rectangle dateOfBirth;

	public VisitPassAreaStructure(Rectangle titleBBox, int imageWidth, int imageHeight) {

		Rectangle imageArea = new Rectangle(0, 0, imageWidth, imageHeight);
		title = new Rectangle(titleBBox);

		int titleHalfWidth = titleBBox.width / 2;
		int resultHalfWidth = (int) (titleHalfWidth * MODIFIER_MAIN_WIDTH);

		int x = titleBBox.x + titleHalfWidth - resultHalfWidth;
		int y = titleBBox.y;
		int w = resultHalfWidth * 2;
		int h = (int) (titleBBox.height * MODIFIER_MAIN_HEIGHT);

		// Make sure that bounding box fits inside the image
		mainArea = Helper.cropToRectangle(new Rectangle(x, y, w, h), imageArea);

		nationality = Helper.cropToRectangle(Subarea.NATIONALITY.location(mainArea), imageArea);
		finNumber = Helper.cropToRectangle(Subarea.FIN_NUMBER.location(mainArea), imageArea);
		dateOfBirth = Helper.cropToRectangle(Subarea.DATE_OF_BIRTH.location(mainArea), imageArea);
	}

	/**
	 * Get estimated area WITHIN which card is located. Area then should be deskewed and then properly recognized
	 *
	 * @param roughTitleBBox
	 * @param imageWidth
	 * @param imageHeight
	 *
	 * @return
	 */
	public static Rectangle estimateLocation(Rectangle roughTitleBBox, int imageWidth, int imageHeight) {

		Rectangle imageArea = new Rectangle(0, 0, imageWidth, imageHeight);

		int titleHalfWidth = roughTitleBBox.width / 2;
		int resultHalfWidth = (int) (titleHalfWidth * MODIFIER_ROUGHT_MAIN_WIDTH);

		int x = roughTitleBBox.x + titleHalfWidth - resultHalfWidth;
		int y = roughTitleBBox.y - (int) (roughTitleBBox.height * MODIFIER_ROUGHT_MAIN_HEIGHT_UP);
		int w = resultHalfWidth * 2;
		int h = (int) (roughTitleBBox.height * MODIFIER_ROUGHT_MAIN_HEIGHT_DOWN);

		// Make sure that bounding box fits inside the image
		return Helper.cropToRectangle(new Rectangle(x, y, w, h), imageArea);
	}

	public Rectangle getMainArea() {

		return new Rectangle(mainArea);
	}
	public Rectangle getTitle() {

		return new Rectangle(title);
	}
	public Rectangle getNationality() {

		return nationality;
	}
	public Rectangle getFinNumber() {

		return finNumber;
	}
	public Rectangle getDateOfBirth() {

		return dateOfBirth;
	}

	private enum Subarea {

		DATE_OF_BIRTH(0.31f, 0.50f, 0.17f, 0.07f),
		FIN_NUMBER(0.31f, 0.65f, 0.17f, 0.07f),
		NATIONALITY(0.68f, 0.5f, 0.22f, 0.08f);

		private final float areaX;
		private final float areaY;
		private final float areaWidth;
		private final float areaHeight;

		private Subarea(float x, float y, float width, float height) {

			this.areaX = x;
			this.areaY = y;
			this.areaWidth = width;
			this.areaHeight = height;
		}

		public Rectangle location(Rectangle mainArea) {

			int x = mainArea.x + (int) (mainArea.width * areaX);
			int y = mainArea.y + (int) (mainArea.height * areaY);
			int width = (int) (mainArea.width * areaWidth);
			int height = (int) (mainArea.height * areaHeight);

			return new Rectangle(x, y, width, height);
		}
	}
}
