/**
 * 
 */
package it.dgc.verificac19.model;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author NIGFRA
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"cu", "fc"})
public class Exemption implements Serializable {

  private static final long serialVersionUID = 3222424362443398857L;

  @JsonProperty("tg")
  private String disease;

  @JsonProperty("co")
  private String countryOfVaccination;

  @JsonProperty("is")
  private String certificateIssuer;

  @JsonProperty("ci")
  private String certificateIdentifier;

  @JsonProperty("df")
  private String certificateValidFrom;

  @JsonProperty("du")
  private String certificateValidUntil;

  /**
   * @return the disease
   */
  @JsonProperty("tg")
  public String getDisease() {
    return disease;
  }

  /**
   * @param disease the disease to set
   */
  @JsonProperty("tg")
  public void setDisease(String disease) {
    this.disease = disease;
  }

  /**
   * @return the countryOfVaccination
   */
  @JsonProperty("co")
  public String getCountryOfVaccination() {
    return countryOfVaccination;
  }

  /**
   * @param countryOfVaccination the countryOfVaccination to set
   */
  @JsonProperty("co")
  public void setCountryOfVaccination(String countryOfVaccination) {
    this.countryOfVaccination = countryOfVaccination;
  }

  /**
   * @return the certificateIssuer
   */
  @JsonProperty("is")
  public String getCertificateIssuer() {
    return certificateIssuer;
  }

  /**
   * @param certificateIssuer the certificateIssuer to set
   */
  @JsonProperty("is")
  public void setCertificateIssuer(String certificateIssuer) {
    this.certificateIssuer = certificateIssuer;
  }

  /**
   * @return the certificateIdentifier
   */
  @JsonProperty("ci")
  public String getCertificateIdentifier() {
    return certificateIdentifier;
  }

  /**
   * @param certificateIdentifier the certificateIdentifier to set
   */
  @JsonProperty("ci")
  public void setCertificateIdentifier(String certificateIdentifier) {
    this.certificateIdentifier = certificateIdentifier;
  }

  /**
   * @return the certificateValidFrom
   */
  @JsonProperty("df")
  public String getCertificateValidFrom() {
    return certificateValidFrom;
  }

  /**
   * @param certificateValidFrom the certificateValidFrom to set
   */
  @JsonProperty("df")
  public void setCertificateValidFrom(String certificateValidFrom) {
    this.certificateValidFrom = certificateValidFrom;
  }

  /**
   * @return the certificateValidUntil
   */
  @JsonProperty("du")
  public String getCertificateValidUntil() {
    return certificateValidUntil;
  }

  /**
   * @param certificateValidUntil the certificateValidUntil to set
   */
  @JsonProperty("du")
  public void setCertificateValidUntil(String certificateValidUntil) {
    this.certificateValidUntil = certificateValidUntil;
  }

}
