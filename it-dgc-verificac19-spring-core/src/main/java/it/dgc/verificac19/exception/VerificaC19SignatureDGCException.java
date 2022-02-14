/**
 * 
 */
package it.dgc.verificac19.exception;

import java.security.SignatureException;

/**
 * @author NIGFRA
 *
 */
public class VerificaC19SignatureDGCException extends SignatureException {


  private static final long serialVersionUID = 7460762061957165928L;

  private byte[] cbor;
  private String message;

  public VerificaC19SignatureDGCException(byte[] bs, String message) {
    super();
    this.cbor = bs;
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
