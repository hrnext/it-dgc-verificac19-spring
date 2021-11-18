package it.dgc.verificac19.data.remote.model;

import java.io.Serializable;

import it.dgc.verificac19.model.ValidationRulesEnum;

/**
 *
 * This data class specifies the object where the deserialized JSON of the validation rules will be
 * stored.
 *
 */
public class Rule implements Serializable {

  private static final long serialVersionUID = 5323665870433200800L;

  private ValidationRulesEnum name;

  private String type;

  private String value;

  public Rule() {
    super();
  }

  /**
   * @return the name
   */
  public ValidationRulesEnum getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(ValidationRulesEnum name) {
    this.name = name;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

}
