package it.dgc.verificac19.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * This data class represents the information contained in the scanned certification in an easier
 * and shorter model.
 *
 */
public class CertificateSimple implements Serializable {

  private static final long serialVersionUID = 4431486598329621773L;

  private SimplePersonModel person = new SimplePersonModel();
  private LocalDate dateOfBirth;
  private CertificateStatus certificateStatus;
  private LocalDateTime timeStamp;

  public CertificateSimple() {
    super();
  }

  /**
   * 
   * @param certificateStatus
   * @param timeStamp
   */
  public CertificateSimple(CertificateStatus certificateStatus) {
    super();
    this.certificateStatus = certificateStatus;
    this.timeStamp = LocalDateTime.now();
  }

  /**
   * @return the person
   */
  public SimplePersonModel getPerson() {
    return person;
  }

  /**
   * @param person the person to set
   */
  public void setPerson(SimplePersonModel person) {
    this.person = person;
  }

  /**
   * @return the dateOfBirth
   */
  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  /**
   * @param dateOfBirth the dateOfBirth to set
   */
  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  /**
   * @return the certificateStatus
   */
  public CertificateStatus getCertificateStatus() {
    return certificateStatus;
  }

  /**
   * @param certificateStatus the certificateStatus to set
   */
  public void setCertificateStatus(CertificateStatus certificateStatus) {
    this.certificateStatus = certificateStatus;
  }

  /**
   * @return the timeStamp
   */
  public LocalDateTime getTimeStamp() {
    return timeStamp;
  }

  /**
   * @param timeStamp the timeStamp to set
   */
  public void setTimeStamp(LocalDateTime timeStamp) {
    this.timeStamp = timeStamp;
  }

  public static class SimplePersonModel implements Serializable {

    private static final long serialVersionUID = -2023923156076664170L;

    private String standardisedFamilyName;
    private String familyName;
    private String standardisedGivenName;
    private String givenName;

    public SimplePersonModel() {
      super();
    }

    public SimplePersonModel(String standardisedFamilyName, String familyName,
        String standardisedGivenName, String givenName) {
      super();
      this.standardisedFamilyName = standardisedFamilyName;
      this.familyName = familyName;
      this.standardisedGivenName = standardisedGivenName;
      this.givenName = givenName;
    }

    /**
     * @return the standardisedFamilyName
     */
    public String getStandardisedFamilyName() {
      return standardisedFamilyName;
    }

    /**
     * @param standardisedFamilyName the standardisedFamilyName to set
     */
    public void setStandardisedFamilyName(String standardisedFamilyName) {
      this.standardisedFamilyName = standardisedFamilyName;
    }

    /**
     * @return the familyName
     */
    public String getFamilyName() {
      return familyName;
    }

    /**
     * @param familyName the familyName to set
     */
    public void setFamilyName(String familyName) {
      this.familyName = familyName;
    }

    /**
     * @return the standardisedGivenName
     */
    public String getStandardisedGivenName() {
      return standardisedGivenName;
    }

    /**
     * @param standardisedGivenName the standardisedGivenName to set
     */
    public void setStandardisedGivenName(String standardisedGivenName) {
      this.standardisedGivenName = standardisedGivenName;
    }

    /**
     * @return the givenName
     */
    public String getGivenName() {
      return givenName;
    }

    /**
     * @param givenName the givenName to set
     */
    public void setGivenName(String givenName) {
      this.givenName = givenName;
    }

  }
}
