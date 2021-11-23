package it.dgc.verificac19.data.local;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 *
 * This class defines a [Key] data entity. Each instance of this class represents a row in keys
 * table in the app's database. [kid] contains the key ID which was used to sign the DGC and the
 * [key] contains the corresponding Public Key.
 *
 */
@Entity
@Table(name = "keys")
public class Key implements Serializable {

  private static final long serialVersionUID = -4952886422729748392L;

  @Id
  private String kid;

  @Lob
  private String value;

  public Key() {
    super();
  }

  /**
   * @param kid
   * @param key
   */
  public Key(String kid, String key) {
    super();
    this.kid = kid;
    this.value = key;
  }

  /**
   * @return the kid
   */
  public String getKid() {
    return kid;
  }

  /**
   * @param kid the kid to set
   */
  public void setKid(String kid) {
    this.kid = kid;
  }

  /**
   * @return the key
   */
  public String getValue() {
    return value;
  }

  /**
   * @param key the key to set
   */
  public void setValue(String key) {
    this.value = key;
  }

}
