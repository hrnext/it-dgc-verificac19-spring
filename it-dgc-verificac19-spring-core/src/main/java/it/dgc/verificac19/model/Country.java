/**
 * 
 */
package it.dgc.verificac19.model;

/**
 * @author NIGFRA
 *
 */
public enum Country {

  IT("IT"), NOT_IT("NOT_IT"), SM("SM");

  private String value;

  /**
   * @param value
   */
  Country(String value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public synchronized String getValue() {
    return value;
  }
}
