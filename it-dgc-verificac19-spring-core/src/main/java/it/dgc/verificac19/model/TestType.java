package it.dgc.verificac19.model;

public enum TestType {
  RAPID("LP217198-3"), MOLECULAR("LP6464-4");

  private String value;

  /**
   * @param value
   */
  private TestType(String value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public synchronized String getValue() {
    return value;
  }

}
