package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MOMDataBuilder implements IDocumentDataBuilder {

	private static ArrayList<Pattern> patterns_date_of_birth = new ArrayList<>();
	private static ArrayList<Pattern> patterns_employer_name = new ArrayList<>();
	private static ArrayList<Pattern> patterns_employer_telephone = new ArrayList<>();
	private static ArrayList<Pattern> patterns_employer_uen = new ArrayList<>();
	private static ArrayList<Pattern> patterns_full_name = new ArrayList<>();
	private static ArrayList<Pattern> patterns_nationality = new ArrayList<>();
	private static ArrayList<Pattern> patterns_nric_or_fin_number = new ArrayList<>();
	private static ArrayList<Pattern> patterns_occupation = new ArrayList<>();
	private static ArrayList<Pattern> patterns_passport_number = new ArrayList<>();
	private static ArrayList<Pattern> patterns_work_permit_number = new ArrayList<>();
	private static ArrayList<Pattern> patterns_work_permit_expiry = new ArrayList<>();
	private static ArrayList<Pattern> patterns_employment_agency_address = new ArrayList<>();
	static {

		patterns_full_name.add(Pattern.compile("Name.*?worker.*?((?:[A-Z\\d]{3,}[ ]?)+)", Pattern.MULTILINE));

		patterns_passport_number.add(Pattern.compile("Passport.*?No.*?([A-Z\\d]{5,})", Pattern.MULTILINE));

		patterns_date_of_birth.add(Pattern.compile("Date.*?Birth.*?(\\d{2})\\/(\\d{2})\\/(\\d{4})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		patterns_date_of_birth.add(Pattern.compile("Date.*?Birth.*?(\\d{2})[\\-\\.\\/\\\\](\\d{2})[\\-\\.\\/\\\\](\\d{4})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));

		patterns_work_permit_number.add(Pattern.compile("WP.*?No.*?(\\d{5,})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));

		patterns_nric_or_fin_number.add(Pattern.compile("FIN.*?([A-Z]\\d{5,}[A-Z])", Pattern.MULTILINE));

		patterns_nationality.add(Pattern.compile("Nationality.*?([A-Z]{6,})", Pattern.MULTILINE));
		patterns_nationality.add(Pattern.compile("Nationality.*?([A-Z]{4,})", Pattern.MULTILINE));

		patterns_occupation.add(Pattern.compile("Occupation.*?((?:[A-Z\\d]{3,}[ ]?)+)", Pattern.MULTILINE));

		patterns_employer_name.add(Pattern.compile("Name.*?Employer.*?:.*?((?:[A-Z\\d]{3,}[\\.\\-]?[ ]?)+)", Pattern.MULTILINE));
		patterns_employer_name.add(Pattern.compile("Name.*?Employer.*?\\w\\s*((?:[A-Z\\d]{3,}[\\.\\-]?[ ]?)+)", Pattern.MULTILINE));
		patterns_employer_name.add(Pattern.compile("Name.*?Employer.*?((?:[A-Z\\d]{3,}[\\.\\-]?[ ]?)+)", Pattern.MULTILINE));

		patterns_employer_telephone.add(Pattern.compile("Employer.*?Tel.*?No.*?(\\d{5,})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));

		patterns_employer_uen.add(Pattern.compile("CPF.*?Submission.*?No.*?(\\d{6,}?\\w-[a-z]{3}-\\d{2})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		patterns_employer_uen.add(Pattern.compile("CPF.*?Submission.*?No.*?(\\d{6,}?\\w[^a-z\\d]*[a-z]{3}[^a-z\\d]*\\d{2})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
		patterns_employer_uen.add(Pattern.compile("(\\d{6,}?\\w[^a-z\\d]*[a-z]{3}[^a-z\\d]*\\d{2})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));

		patterns_work_permit_expiry.add(Pattern.compile("Date.*?WP.*?(\\d{2})\\/(\\d{2})\\/(\\d{4}).*?Expiry", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL));

		patterns_employment_agency_address.add(Pattern.compile("Employment.*?:.*?\\s(.{6,})\\s.*?Employment", Pattern.MULTILINE));
	}

	private final MOMData momData;

	private final StringBuilder debugText = new StringBuilder();

	public MOMDataBuilder() {

		momData = new MOMData();
	}

	@Override
	public IDocumentData getDocumentData() {

		return momData;
	}
	@Override
	public void processImage(RecognitionManager.RecognitionTarget target, RecognitionSettings[] settings) throws RecognitionManagerException {

		int currentSettings = 0;
		while (currentSettings < settings.length && getCompleteness() < settings[currentSettings].getPassingCompliteness()){
			debugText.append("\nIteration " + (currentSettings + 1) + '/' + settings.length + '\t' + settings[currentSettings].toString() + '\n');

			for (File file : target.getImages()) {
				debugText.append("\nFile " + file.getName() + '\n');
				doRecognition(file, settings[currentSettings]);
			}

			currentSettings++;
		}
	}
	@Override
	public int getCompleteness() {

		return momData.getCompleteness();
	}
	@Override
	public String getDebugText() {

		return debugText.toString();
	}

	private void doRecognition(File target,  RecognitionSettings settings) throws RecognitionManagerException {

		RecognitionManager recognitionManager = RecognitionManager.getInstance();
		Document document = Jsoup.parse(recognitionManager.recognize(target, null, settings));
		String rawText = Helper.getProperTextFromJSoupDoc(document);

		debugText.append("rawText: { " + rawText + " }\n\n");

		Matcher matcher;
		for (int i = 0; i < patterns_full_name.size(); i++) {
			matcher = patterns_full_name.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.full_name = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_passport_number.size(); i++) {
			matcher = patterns_passport_number.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.passport_number = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_date_of_birth.size(); i++) {
			matcher = patterns_date_of_birth.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.date_of_birth = matcher.group(1) + '/' + matcher.group(2) + '/' + matcher.group(3);
				break;
			}
		}
		for (int i = 0; i < patterns_work_permit_number.size(); i++) {
			matcher = patterns_work_permit_number.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.work_permit_number = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_nric_or_fin_number.size(); i++) {
			matcher = patterns_nric_or_fin_number.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.nric_or_fin_number = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_nationality.size(); i++) {
			matcher = patterns_nationality.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.nationality = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_occupation.size(); i++) {
			matcher = patterns_occupation.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.occupation = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_employer_name.size(); i++) {
			matcher = patterns_employer_name.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.employer_name = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_employer_telephone.size(); i++) {
			matcher = patterns_employer_telephone.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.employer_telephone = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_employer_uen.size(); i++) {
			matcher = patterns_employer_uen.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.employer_uen = matcher.group(1).trim();
				break;
			}
		}
		for (int i = 0; i < patterns_work_permit_expiry.size(); i++) {
			matcher = patterns_work_permit_expiry.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.work_permit_expiry = matcher.group(1) + '/' + matcher.group(2) + '/' + matcher.group(3);
				break;
			}
		}
		for (int i = 0; i < patterns_employment_agency_address.size(); i++) {
			matcher = patterns_employment_agency_address.get(i).matcher(rawText);
			if (matcher.find()) {
				momData.employment_agency_address = matcher.group(1).trim();
				break;
			}
		}
	}
}
