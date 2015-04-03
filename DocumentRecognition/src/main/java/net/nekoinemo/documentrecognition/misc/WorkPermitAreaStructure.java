package net.nekoinemo.documentrecognition.misc;

import net.nekoinemo.documentrecognition.Helper;

import java.awt.*;

public class WorkPermitAreaStructure {

	private final static float MODIFIER_ROUGHT_MAIN_WIDTH = 6f;
	private final static float MODIFIER_ROUGHT_MAIN_HEIGHT_UP = 2.0f;
	private final static float MODIFIER_ROUGHT_MAIN_HEIGHT_DOWN = 29.0f;
	private final static float MODIFIER_MAIN_WIDTH = 4.8f;
	private final static float MODIFIER_MAIN_HEIGHT = 25.0f;

	private final Rectangle mainArea;
	private final Rectangle title;

	private final Rectangle name;
	private final Rectangle category;
	private final Rectangle employer;
	private final Rectangle number;
	private final Rectangle dateOfExpiry;

	public WorkPermitAreaStructure(Rectangle titleBBox, int imageWidth, int imageHeight) {

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

		name = Helper.cropToRectangle(Subarea.NAME.location(mainArea), imageArea);
		category = Helper.cropToRectangle(Subarea.CATEGORY.location(mainArea), imageArea);
		employer = Helper.cropToRectangle(Subarea.EMPLOYER.location(mainArea), imageArea);
		number = Helper.cropToRectangle(Subarea.NUMBER.location(mainArea), imageArea);
		dateOfExpiry = Helper.cropToRectangle(Subarea.DATE_OF_EXPIRY.location(mainArea), imageArea);
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
	public Rectangle getName() {

		return name;
	}
	public Rectangle getCategory() {

		return category;
	}
	public Rectangle getEmployer() {

		return employer;
	}
	public Rectangle getNumber() {

		return number;
	}
	public Rectangle getDateOfExpiry() {

		return dateOfExpiry;
	}

	private enum Subarea {

		NAME(0.28f, 0.35f, 0.5f, 0.05f),
		EMPLOYER(0.05f, 0.19f, 0.55f, 0.05f),
		CATEGORY(0.14f, 0.26f, 0.36f, 0.05f),
		NUMBER(0.28f, 0.57f, 0.15f, 0.05f),
		DATE_OF_EXPIRY(0.5f, 0.79f, 0.15f, 0.05f);

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
