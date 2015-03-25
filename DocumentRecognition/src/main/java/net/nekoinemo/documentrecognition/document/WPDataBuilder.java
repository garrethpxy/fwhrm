package net.nekoinemo.documentrecognition.document;

import net.nekoinemo.documentrecognition.RecognitionSettings;
import net.nekoinemo.documentrecognition.RecognizerException;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Created by krdm on 25.03.2015.
 */
public class WPDataBuilder implements DocumentDataBuilder {

	private final WPData wpData;

	public WPDataBuilder() {

		wpData = new WPData();
	}

	@Override
	public DocumentData getDocumentData() {

		return wpData;
	}
	@Override
	public void ProcessImage(File target, RecognitionSettings settings) {


	}
	@Override
	public int getCompleteness() {

		return wpData.getCompleteness();
	}
	@Override
	public void FillEmptyFields(DocumentData value) throws RecognizerException {

		if (!value.getClass().equals(MOMData.class)) throw new RecognizerException("Passed subclass of DocumentData doesn't match this subclass");

		for (String fieldName : wpData.DATA_FIELDS) {
			try {
				Field field = MOMData.class.getDeclaredField(fieldName);

				field.setAccessible(true);
				if (field.get(wpData) == null)
					field.set(wpData, field.get(value));
				field.setAccessible(false);
			} catch (Exception e) {	}
		}
	}
	@Override
	public void FillFields(DocumentData value) throws RecognizerException {

		if (!value.getClass().equals(MOMData.class)) throw new RecognizerException("Passed subclass of DocumentData doesn't match this subclass");

		for (String fieldName : wpData.DATA_FIELDS) {
			try {
				Field field = MOMData.class.getDeclaredField(fieldName);

				field.setAccessible(true);
				Object valuesValue = field.get(value);
				if (valuesValue != null)
					field.set(wpData, valuesValue);
				field.setAccessible(false);
			} catch (Exception e) {	}
		}
	}
}
