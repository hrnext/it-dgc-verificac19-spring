/**
 * 
 */
package it.dgc.verificac19.model;

import java.io.Serializable;
import com.google.gson.annotations.SerializedName;

/**
 * @author NIGFRA
 *
 */
public class Exemption implements Serializable {

  private static final long serialVersionUID = 3222424362443398857L;

  @SerializedName("tg")
  private String disease;

  @SerializedName("co")
  private String countryOfVaccination;

  @SerializedName("is")
  private String certificateIssuer;

  @SerializedName("ci")
  private String certificateIdentifier;

  @SerializedName("df")
  private String certificateValidFrom;

  @SerializedName("du")
  private String certificateValidUntil;

  /**
   * @return the disease
   */
  public String getDisease() {
    return disease;
  }

  /**
   * @param disease the disease to set
   */
  public void setDisease(String disease) {
    this.disease = disease;
  }

  /**
   * @return the countryOfVaccination
   */
  public String getCountryOfVaccination() {
    return countryOfVaccination;
  }

  /**
   * @param countryOfVaccination the countryOfVaccination to set
   */
  public void setCountryOfVaccination(String countryOfVaccination) {
    this.countryOfVaccination = countryOfVaccination;
  }

  /**
   * @return the certificateIssuer
   */
  public String getCertificateIssuer() {
    return certificateIssuer;
  }

  /**
   * @param certificateIssuer the certificateIssuer to set
   */
  public void setCertificateIssuer(String certificateIssuer) {
    this.certificateIssuer = certificateIssuer;
  }

  /**
   * @return the certificateIdentifier
   */
  public String getCertificateIdentifier() {
    return certificateIdentifier;
  }

  /**
   * @param certificateIdentifier the certificateIdentifier to set
   */
  public void setCertificateIdentifier(String certificateIdentifier) {
    this.certificateIdentifier = certificateIdentifier;
  }

  /**
   * @return the certificateValidFrom
   */
  public String getCertificateValidFrom() {
    return certificateValidFrom;
  }

  /**
   * @param certificateValidFrom the certificateValidFrom to set
   */
  public void setCertificateValidFrom(String certificateValidFrom) {
    this.certificateValidFrom = certificateValidFrom;
  }

  /**
   * @return the certificateValidUntil
   */
  public String getCertificateValidUntil() {
    return certificateValidUntil;
  }

  /**
   * @param certificateValidUntil the certificateValidUntil to set
   */
  public void setCertificateValidUntil(String certificateValidUntil) {
    this.certificateValidUntil = certificateValidUntil;
  }

}
