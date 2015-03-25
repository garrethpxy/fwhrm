package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Created by krdm on 25.03.2015.
*/
public class MOMDataBuilder implements DocumentDataBuilder {

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
	}

	private final MOMData momData;

	public MOMDataBuilder() {

		momData = new MOMData();
	}

	@Override
	public DocumentData getDocumentData() {

		return momData;
	}
	@Override
	public void ProcessImage(File target, RecognitionSettings settings) throws RecognizerException {

		Recognizer recognizer = Recognizer.getInstance();

		Document document = Jsoup.parse(recognizer.RecognizeFile(target, null, settings));
		String rawText = document.text();

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
				momData.date_of_birth = matcher.group(1) + '/' + matcher.group(2) + '/' +  matcher.group(3);
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
	}
	@Override
	public int getCompleteness(){

		return momData.getCompleteness();
	}
	@Override
	public void FillEmptyFields(DocumentData value) throws RecognizerException {

		if (!value.getClass().equals(MOMData.class)) throw new RecognizerException("Passed subclass of DocumentData doesn't match this subclass");

		for (String fieldName : momData.DATA_FIELDS) {
			try {
				Field field = MOMData.class.getDeclaredField(fieldName);

				field.setAccessible(true);
				if (field.get(momData) == null)
					field.set(momData, field.get(value));
				field.setAccessible(false);
			} catch (Exception e) {	}
		}
	}
	@Override
	public void FillFields(DocumentData value) throws RecognizerException {

		if (!value.getClass().equals(MOMData.class)) throw new RecognizerException("Passed subclass of DocumentData doesn't match this subclass");

		for (String fieldName : momData.DATA_FIELDS) {
			try {
				Field field = MOMData.class.getDeclaredField(fieldName);

				field.setAccessible(true);
				Object valuesValue = field.get(value);
				if (valuesValue != null)
					field.set(momData, valuesValue);
				field.setAccessible(false);
			} catch (Exception e) {	}
		}
	}

	public MOMDataBuilder setDate_of_birth(String date_of_birth) {

		momData.date_of_birth = date_of_birth;
		return this;
	}
	public MOMDataBuilder setEmployer_name(String employer_name) {

		momData.employer_name = employer_name;
		return this;
	}
	public MOMDataBuilder setEmployer_telephone(String employer_telephone) {

		momData.employer_telephone = employer_telephone;
		return this;
	}
	public MOMDataBuilder setEmployer_uen(String employer_uen) {

		momData.employer_uen = employer_uen;
		return this;
	}
	public MOMDataBuilder setFull_name(String full_name) {

		momData.full_name = full_name;
		return this;
	}
	public MOMDataBuilder setNationality(String nationality) {

		momData.nationality = nationality;
		return this;
	}
	public MOMDataBuilder setNric_or_fin_number(String nric_or_fin_number) {

		momData.nric_or_fin_number = nric_or_fin_number;
		return this;
	}
	public MOMDataBuilder setOccupation(String occupation) {

		momData.occupation = occupation;
		return this;
	}
	public MOMDataBuilder setPassport_number(String passport_number) {

		momData.passport_number = passport_number;
		return this;
	}
	public MOMDataBuilder setWork_permit_number(String work_permit_number) {

		momData.work_permit_number = work_permit_number;
		return this;
	}
}
