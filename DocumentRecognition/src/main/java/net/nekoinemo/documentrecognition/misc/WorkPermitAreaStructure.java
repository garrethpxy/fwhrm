package net.nekoinemo.documentrecognition.misc;

import net.nekoinemo.documentrecognition.Helper;

import java.awt.*;

public class WorkPermitAreaStructure {

	private final static float MODIFIER_MAIN_WIDTH = 4.8f;
	private final static float MODIFIER_MAIN_HEIGHT = 25.0f;

	private final static float MODIFIER_NAME_X = 0.28f;
	private final static float MODIFIER_NAME_WIDTH = 0.50f;
	private final static float MODIFIER_NAME_Y = 0.35f;
	private final static float MODIFIER_NAME_HEIGHT = 0.05f;
	private final static float MODIFIER_CATEGORY_X = 0.14f;
	private final static float MODIFIER_CATEGORY_WIDTH = 0.36f;
	private final static float MODIFIER_CATEGORY_Y = 0.26f;
	private final static float MODIFIER_CATEGORY_HEIGHT = 0.05f;
	private final static float MODIFIER_EMPLOYER_X = 0.05f;
	private final static float MODIFIER_EMPLOYER_WIDTH = 0.55f;
	private final static float MODIFIER_EMPLOYER_Y = 0.19f;
	private final static float MODIFIER_EMPLOYER_HEIGHT = 0.05f;

	private final Rectangle mainArea;
	private final Rectangle title;

	private final Rectangle name;
	private final Rectangle category;
	private final Rectangle employer;

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

		x = mainArea.x + (int) (mainArea.width * MODIFIER_NAME_X);
		y = mainArea.y + (int) (mainArea.height * MODIFIER_NAME_Y);
		w = (int) (mainArea.width * MODIFIER_NAME_WIDTH);
		h = (int) (mainArea.height * MODIFIER_NAME_HEIGHT);
		name = Helper.cropToRectangle(new Rectangle(x, y, w, h), imageArea);

		x = mainArea.x + (int) (mainArea.width * MODIFIER_CATEGORY_X);
		y = mainArea.y + (int) (mainArea.height * MODIFIER_CATEGORY_Y);
		w = (int) (mainArea.width * MODIFIER_CATEGORY_WIDTH);
		h = (int) (mainArea.height * MODIFIER_CATEGORY_HEIGHT);
		category = Helper.cropToRectangle(new Rectangle(x, y, w, h), imageArea);

		x = mainArea.x + (int) (mainArea.width * MODIFIER_EMPLOYER_X);
		y = mainArea.y + (int) (mainArea.height * MODIFIER_EMPLOYER_Y);
		w = (int) (mainArea.width * MODIFIER_EMPLOYER_WIDTH);
		h = (int) (mainArea.height * MODIFIER_EMPLOYER_HEIGHT);
		employer = Helper.cropToRectangle(new Rectangle(x, y, w, h), imageArea);
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
}
