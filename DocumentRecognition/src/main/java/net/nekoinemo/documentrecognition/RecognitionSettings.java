package net.nekoinemo.documentrecognition;

import net.sourceforge.tess4j.TessAPI;

public class RecognitionSettings {

	public static final int PAGESEG_MODE_AUTO = TessAPI.TessPageSegMode.PSM_AUTO;
	public static final int PAGESEG_MODE_SINGLE_COLUMN = TessAPI.TessPageSegMode.PSM_SINGLE_COLUMN;
	public static final int PAGESEG_MODE_SINGLE_BLOCK = TessAPI.TessPageSegMode.PSM_SINGLE_BLOCK;
	public static final int PAGESEG_MODE_SPARSE_TEXT = TessAPI.TessPageSegMode.PSM_SPARSE_TEXT;

	public static final int ENGINE_MODE_BASIC = TessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY;
	public static final int ENGINE_MODE_FULL = TessAPI.TessOcrEngineMode.OEM_TESSERACT_CUBE_COMBINED;

	public static final RecognitionSettings[] DEFAULT;
	public static final RecognitionSettings[] SLOPPY;
	public static final RecognitionSettings[] FAST;
	public static final RecognitionSettings[] PERFECTIONIST;
	public static final RecognitionSettings[] WHATEVER;

	private int passingCompleteness;
	private int pageSegMode;
	private int engineMode;

	static {

		DEFAULT = new RecognitionSettings[]{
				new RecognitionSettings(90, ENGINE_MODE_BASIC, PAGESEG_MODE_SINGLE_BLOCK),
				new RecognitionSettings(90, ENGINE_MODE_FULL, PAGESEG_MODE_SINGLE_BLOCK)
		};
		SLOPPY = new RecognitionSettings[]{
				new RecognitionSettings(75, ENGINE_MODE_BASIC, PAGESEG_MODE_SINGLE_BLOCK),
				new RecognitionSettings(75, ENGINE_MODE_FULL, PAGESEG_MODE_SINGLE_BLOCK)
		};
		FAST = new RecognitionSettings[]{
				new RecognitionSettings(80, ENGINE_MODE_BASIC, PAGESEG_MODE_SINGLE_BLOCK)
		};
		PERFECTIONIST = new RecognitionSettings[]{
				new RecognitionSettings(100, ENGINE_MODE_BASIC, PAGESEG_MODE_SINGLE_BLOCK),
				new RecognitionSettings(100, ENGINE_MODE_BASIC, PAGESEG_MODE_SPARSE_TEXT),
				new RecognitionSettings(100, ENGINE_MODE_FULL, PAGESEG_MODE_SINGLE_BLOCK),
				new RecognitionSettings(100, ENGINE_MODE_BASIC, PAGESEG_MODE_SPARSE_TEXT)
		};
		WHATEVER = new RecognitionSettings[]{
				new RecognitionSettings(0, ENGINE_MODE_BASIC, PAGESEG_MODE_AUTO)
		};
	}

	public RecognitionSettings(int passingCompleteness, int engineMode, int pageSegMode) {

		this.passingCompleteness = passingCompleteness;
		this.pageSegMode = pageSegMode;
		this.engineMode = engineMode;
	}

	public int getPassingCompliteness() {

		return passingCompleteness;
	}
	public int getPageSegMode() {

		return pageSegMode;
	}
	public int getEngineMode() {

		return engineMode;
	}

	@Override
	public String toString() {

		return "RecognitionSettings{" +
				"passingCompleteness=" + passingCompleteness +
				", pageSegMode=" + pageSegMode +
				", engineMode=" + engineMode +
				'}';
	}
}
