/**
 * 
 */
package it.dgc.verificac19.model;

/**
 * @author NIGFRA
 *
 */
public enum CertCode {
  OID_RECOVERY("1.3.6.1.4.1.1847.2021.1.3"), OID_ALT_RECOVERY("1.3.6.1.4.1.0.1847.2021.1.3");

  private String value;

  /**
   * @param value
   */
  CertCode(String value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public synchronized String getValue() {
    return value;
  }

}
