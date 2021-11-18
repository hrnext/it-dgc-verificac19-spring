package it.dgc.verificac19.data.local;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "blacklist")
public class Blacklist implements Serializable {

  private static final long serialVersionUID = 8434434543126960123L;

  @Id
  private String bvalue;

  public Blacklist() {
    super();
  }

  /**
   * @param bvalue
   */
  public Blacklist(String bvalue) {
    super();
    this.bvalue = bvalue;
  }

  /**
   * @return the bvalue
   */
  public String getBvalue() {
    return bvalue;
  }

  /**
   * @param bvalue the bvalue to set
   */
  public void setBvalue(String bvalue) {
    this.bvalue = bvalue;
  }

}
