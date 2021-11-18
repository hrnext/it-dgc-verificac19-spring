package it.dgc.verificac19.model;

public enum TestResult {
  DETECTED("260373001"), NOT_DETECTED("260415000");

  private String value;

  /**
   * @param value
   */
  private TestResult(String value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public synchronized String getValue() {
    return value;
  }

}
