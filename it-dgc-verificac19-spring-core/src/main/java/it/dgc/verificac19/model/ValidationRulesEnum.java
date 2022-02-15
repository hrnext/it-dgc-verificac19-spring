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

  @SerializedName(value = "recovery_pv_cert_start_day")
  RECOVERY_CERT_PV_START_DAY("recovery_pv_cert_start_day"),

  @SerializedName(value = "recovery_pv_cert_end_day")
  RECOVERY_CERT_PV_END_DAY("recovery_pv_cert_end_day"),

  // @SerializedName(value = "recovery_cert_end_day_school")
  // RECOVERY_CERT_END_DAY_SCHOOL("recovery_cert_end_day_school"),

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

  // @SerializedName(value = "vaccine_end_day_school")
  // VACCINE_END_DAY_SCHOOL("vaccine_end_day_school"),

  @SerializedName(value = "vaccine_start_day_complete_IT")
  VACCINE_START_DAY_COMPLETE_IT("vaccine_start_day_complete_IT"),

  @SerializedName(value = "vaccine_end_day_complete_IT")
  VACCINE_END_DAY_COMPLETE_IT("vaccine_end_day_complete_IT"),

  @SerializedName(value = "vaccine_start_day_booster_IT")
  VACCINE_START_DAY_BOOSTER_IT("vaccine_start_day_booster_IT"),

  @SerializedName(value = "vaccine_end_day_booster_IT")
  VACCINE_END_DAY_BOOSTER_IT("vaccine_end_day_booster_IT"),

  @SerializedName(value = "vaccine_start_day_complete_NOT_IT")
  VACCINE_START_DAY_COMPLETE_NOT_IT("vaccine_start_day_complete_NOT_IT"),

  @SerializedName(value = "vaccine_end_day_complete_NOT_IT")
  VACCINE_END_DAY_COMPLETE_NOT_IT("vaccine_end_day_complete_NOT_IT"),

  @SerializedName(value = "vaccine_start_day_booster_NOT_IT")
  VACCINE_START_DAY_BOOSTER_NOT_IT("vaccine_start_day_booster_NOT_IT"),

  @SerializedName(value = "vaccine_end_day_booster_NOT_IT")
  VACCINE_END_DAY_BOOSTER_NOT_IT("vaccine_end_day_booster_NOT_IT"),

  @SerializedName(value = "recovery_cert_start_day_IT")
  RECOVERY_CERT_START_DAY_IT("recovery_cert_start_day_IT"),

  @SerializedName(value = "recovery_cert_end_day_IT")
  RECOVERY_CERT_END_DAY_IT("recovery_cert_end_day_IT"),

  @SerializedName(value = "recovery_cert_start_day_NOT_IT")
  RECOVERY_CERT_START_DAY_NOT_IT("recovery_cert_start_day_NOT_IT"),

  @SerializedName(value = "recovery_cert_end_day_NOT_IT")
  RECOVERY_CERT_END_DAY_NOT_IT("recovery_cert_end_day_NOT_IT"),

  @SerializedName(value = "black_list_uvci")
  BLACK_LIST_UVCI("black_list_uvci"),

  @SerializedName(value = "DRL_SYNC_ACTIVE")
  DRL_SYNC_ACTIVE("DRL_SYNC_ACTIVE"),

  @SerializedName(value = "MAX_RETRY")
  MAX_RETRY("MAX_RETRY");

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
