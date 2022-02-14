/**
 * 
 */
package it.dgc.verificac19.exception;

import java.security.cert.CertificateExpiredException;

/**
 * @author NIGFRA
 *
 */
public class VerificaC19CertificateExpiredDGCException extends CertificateExpiredException {

  private static final long serialVersionUID = -6610889555121956902L;

  private byte[] cbor;
  private String message;

  public VerificaC19CertificateExpiredDGCException(byte[] cbor, String message) {
    super();
    this.cbor = cbor;
    this.message = message;
  }

  /**
   * @return the cbor
   */
  public byte[] getCbor() {
    return cbor;
  }

  /**
   * @param cbor the cbor to set
   */
  public void setCbor(byte[] cbor) {
    this.cbor = cbor;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(String message) {
    this.message = message;
  }


}
