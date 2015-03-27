package net.nekoinemo.documentrecognition.document;

public class WPData implements IDocumentData {

	protected static final String[] DATA_FIELDS = new String[]{ "date_of_birth", "employer_name", "full_name", "nationality", "fin_number", "work_permit_number", "work_permit_category", "work_permit_expiration_date" };

	protected String full_name = null;
	protected String date_of_birth = null;
	protected String work_permit_number = null;
	protected String work_permit_category = null;
	protected String work_permit_expiration_date = null;
	protected String fin_number = null;
	protected String nationality = null;
	protected String employer_name = null;

	protected WPData() {}

	public String getFull_name() {

		return full_name;
	}
	public String getDate_of_birth() {

		return date_of_birth;
	}
	public String getWork_permit_number() {

		return work_permit_number;
	}
	public String getWork_permit_category() {

		return work_permit_category;
	}
	public String getWork_permit_expiration_date() {

		return work_permit_expiration_date;
	}
	public String getFin_number() {

		return fin_number;
	}
	public String getNationality() {

		return nationality;
	}
	public String getEmployer_name() {

		return employer_name;
	}

	@Override
	public DocumentType getDocumentType() {

		return DocumentType.WORK_PERMIT;
	}
	@Override
	public int getCompleteness() {

		int completeness = 0;

		for (String fieldName : DATA_FIELDS) {
			try {
				if (MOMData.class.getDeclaredField(fieldName).get(this) != null) completeness++;
			} catch (Exception e) {}
		}
		return (int) (((float) completeness / DATA_FIELDS.length) * 100);
	}

	@Override
	public String toString() {

		return "WPData{" +
				(full_name != null ? " full_name='" + full_name + '\'' : "") +
				(date_of_birth != null ? " date_of_birth='" + date_of_birth + '\'' : "") +
				(work_permit_number != null ? " work_permit_number='" + work_permit_number + '\'' : "") +
				(work_permit_category != null ? " work_permit_category='" + work_permit_category + '\'' : "") +
				(work_permit_expiration_date != null ? " work_permit_expiration_date='" + work_permit_expiration_date + '\'' : "") +
				(fin_number != null ? " fin_number='" + fin_number + '\'' : "") +
				(nationality != null ? " nationality='" + nationality + '\'' : "") +
				(employer_name != null ? " employer_name='" + employer_name + '\'' : "") +
				'}';
	}
	@Override
	public String toString(boolean full) {

		if (!full) return toString();

		return "MOMData{" +
				"\nfull_name='" + full_name + '\'' +
				"\ndate_of_birth='" + date_of_birth + '\'' +
				"\nwork_permit_number='" + work_permit_number + '\'' +
				"\nwork_permit_category='" + work_permit_category + '\'' +
				"\nwork_permit_expiration_date='" + work_permit_expiration_date + '\'' +
				"\nfin_number='" + fin_number + '\'' +
				"\nnationality='" + nationality + '\'' +
				"\nemployer_name='" + employer_name + '\'' +
				"\n}";
	}
}
