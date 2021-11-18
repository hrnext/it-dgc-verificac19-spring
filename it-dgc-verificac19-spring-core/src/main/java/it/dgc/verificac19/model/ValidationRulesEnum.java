package it.dgc.verificac19.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;

/**
 *
 * This class represents the various fields of the validation rules JSON.
 *
 */
public enum ValidationRulesEnum {

  @SerializedName(value = "android", alternate = {"ios"})
  APP_MIN_VERSION("android"),

  @SerializedName(value = "sdk")
  SDK_MIN_VERSION("sdk"),

  @SerializedName(value = "recovery_cert_start_day")
  RECOVERY_CERT_START_DAY("recovery_cert_start_day"),

  @SerializedName(value = "recovery_cert_end_day")
  RECOVERY_CERT_END_DAY("recovery_cert_end_day"),

  @SerializedName(value = "molecular_test_start_hours")
  MOLECULAR_TEST_START_HOUR("molecular_test_start_hours"),

  @SerializedName(value = "molecular_test_end_hours")
  MOLECULAR_TEST_END_HOUR("molecular_test_end_hours"),

  @SerializedName(value = "rapid_test_start_hours")
  RAPID_TEST_START_HOUR("rapid_test_start_hours"),

  @SerializedName(value = "rapid_test_end_hours")
  RAPID_TEST_END_HOUR("rapid_test_end_hours"),

  @SerializedName(value = "vaccine_start_day_not_complete")
  VACCINE_START_DAY_NOT_COMPLETE("vaccine_start_day_not_complete"),

  @SerializedName(value = "vaccine_end_day_not_complete")
  VACCINE_END_DAY_NOT_COMPLETE("vaccine_end_day_not_complete"),

  @SerializedName(value = "vaccine_start_day_complete")
  VACCINE_START_DAY_COMPLETE("vaccine_start_day_complete"),

  @SerializedName(value = "vaccine_end_day_complete")
  VACCINE_END_DAY_COMPLETE("vaccine_end_day_complete"),

  @SerializedName(value = "black_list_uvci")
  BLACK_LIST_UVCI("black_list_uvci");

  @JsonValue
  private String value;

  /**
   * @param value
   */
  private ValidationRulesEnum(String value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

}
