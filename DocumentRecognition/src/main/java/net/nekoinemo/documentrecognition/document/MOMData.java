package net.nekoinemo.documentrecognition.document;

public class MOMData implements IDocumentData {

	protected static final String[] DATA_FIELDS = new String[]{ "date_of_birth", "employer_name", "employer_telephone", "employer_uen", "full_name", "nationality", "nric_or_fin_number", "occupation", "passport_number", "work_permit_number" };

	protected String date_of_birth = null;
	protected String employer_name = null;
	protected String employer_telephone = null;
	protected String employer_uen = null;
	protected String full_name = null;
	protected String nationality = null;
	protected String nric_or_fin_number = null;
	protected String occupation = null;
	protected String passport_number = null;
	protected String work_permit_number = null;

	protected MOMData() {}

	public String getDate_of_birth() {

		return date_of_birth;
	}
	public String getEmployer_name() {

		return employer_name;
	}
	public String getEmployer_telephone() {

		return employer_telephone;
	}
	public String getEmployer_uen() {

		return employer_uen;
	}
	public String getFull_name() {

		return full_name;
	}
	public String getNationality() {

		return nationality;
	}
	public String getNric_or_fin_number() {

		return nric_or_fin_number;
	}
	public String getOccupation() {

		return occupation;
	}
	public String getPassport_number() {

		return passport_number;
	}
	public String getWork_permit_number() {

		return work_permit_number;
	}

	@Override
	public DocumentType getDocumentType() {

		return DocumentType.MOM;
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

		return "MOMData{" +
				(full_name != null ? " full_name='" + full_name + '\'' : "") +
				(passport_number != null ? " passport_number='" + passport_number + '\'' : "") +
				(date_of_birth != null ? " date_of_birth='" + date_of_birth + '\'' : "") +
				(work_permit_number != null ? " work_permit_number='" + work_permit_number + '\'' : "") +
				(nric_or_fin_number != null ? " nric_or_fin_number='" + nric_or_fin_number + '\'' : "") +
				(nationality != null ? " nationality='" + nationality + '\'' : "") +
				(occupation != null ? " occupation='" + occupation + '\'' : "") +
				(employer_name != null ? " employer_name='" + employer_name + '\'' : "") +
				(employer_telephone != null ? " employer_telephone='" + employer_telephone + '\'' : "") +
				(employer_uen != null ? " employer_uen='" + employer_uen + '\'' : "") +
				" }";
	}
	@Override
	public String toString(boolean full) {

		if (!full) return toString();

		return "MOMData{" +
				"\nfull_name='" + full_name + '\'' +
				"\npassport_number='" + passport_number + '\'' +
				"\ndate_of_birth='" + date_of_birth + '\'' +
				"\nwork_permit_number='" + work_permit_number + '\'' +
				"\nnric_or_fin_number='" + nric_or_fin_number + '\'' +
				"\nnationality='" + nationality + '\'' +
				"\noccupation='" + occupation + '\'' +
				"\nemployer_name='" + employer_name + '\'' +
				"\nemployer_telephone='" + employer_telephone + '\'' +
				"\nemployer_uen='" + employer_uen + '\'' +
				"\n}";
	}
}
