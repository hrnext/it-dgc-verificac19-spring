package it.dgc.verificac19.model;

/**
 *
 * This enum class defines all the possible status of certifications after their verification.
 */
public enum CertificateStatus {
  NOT_VALID("notValid"), NOT_VALID_YET("notValidYet"), VALID("valid"), EXPIRED("expired"), REVOKED(
      "revoked"), NOT_EU_DCC("notEuDCC"), TEST_NEEDED("verificationIsNeeded");

  private String value;

  private CertificateStatus(String value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

}
