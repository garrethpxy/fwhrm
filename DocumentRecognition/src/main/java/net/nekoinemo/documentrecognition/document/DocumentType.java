package net.nekoinemo.documentrecognition.document;

import java.util.ArrayList;
import java.util.regex.Pattern;

public enum DocumentType {

	MOM(new ArrayList() {{
		add(Pattern.compile("foreign.*?worker", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		add(Pattern.compile("employment.*?details", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		add(Pattern.compile("employer.*?details", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		add(Pattern.compile("Name.*?Employer", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		add(Pattern.compile("WP.*?No", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		add(Pattern.compile("Name.*?worker", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		add(Pattern.compile("mom", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
	}}), WORK_PERMIT(new ArrayList<Pattern>()), PASSPORT(new ArrayList<Pattern>());

	private ArrayList<Pattern> patterns;

	DocumentType(ArrayList<Pattern> patterns) {
		this.patterns = patterns;
	}
	public float MatchText(String text) {

		int result = 0;

		for (Pattern pattern : patterns) if (pattern.matcher(text).find()) result++;

		return (((float) result / patterns.size()) * 100);
	}
	public DocumentDataBuilder getBuilder() {

		switch (this) {
			case MOM:
				return new MOMDataBuilder();
			default:
				return null;
		}
	}
}
