package net.nekoinemo.documentrecognition.document;

public class MOMData implements IDocumentData {

	protected static final String[] DATA_FIELDS = new String[]{ "date_of_birth", "employer_name", "employer_telephone", "employer_uen", "full_name", "nationality", "nric_or_fin_number", "occupation", "passport_number", "work_permit_number", "work_permit_expiry", "employment_agency_address" };

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
	protected String work_permit_expiry = null;
	protected String employment_agency_address = null;

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
	public String getWork_permit_expiry() {

		return work_permit_expiry;
	}
	public String getEmployment_agency_address() {

		return employment_agency_address;
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
				(work_permit_expiry != null ? " work_permit_expiry='" + work_permit_expiry + '\'' : "") +
				(employment_agency_address != null ? " employment_agency_address='" + employment_agency_address + '\'' : "") +
				" }";
	}
	@Override
	public String toString(boolean full) {

		if (!full) return toString();

		return "MOMData{" +
				"\n\tfull_name='" + full_name + '\'' +
				"\n\tpassport_number='" + passport_number + '\'' +
				"\n\tdate_of_birth='" + date_of_birth + '\'' +
				"\n\twork_permit_number='" + work_permit_number + '\'' +
				"\n\tnric_or_fin_number='" + nric_or_fin_number + '\'' +
				"\n\tnationality='" + nationality + '\'' +
				"\n\toccupation='" + occupation + '\'' +
				"\n\temployer_name='" + employer_name + '\'' +
				"\n\temployer_telephone='" + employer_telephone + '\'' +
				"\n\temployer_uen='" + employer_uen + '\'' +
				"\n\twork_permit_expiry='" + work_permit_expiry + '\'' +
				"\n\temployment_agency_address='" + employment_agency_address + '\'' +
				"\n}";
	}
}
