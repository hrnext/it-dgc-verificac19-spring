/**
 * 
 */
package it.dgc.verificac19.data.local;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author NIGFRA
 *
 */
@Entity
@Table(name = "drl")
public class Drl implements Serializable {

  private static final long serialVersionUID = -1898981522118233348L;

  @Id
  String id;

  long version;

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the version
   */
  public long getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(long version) {
    this.version = version;
  }

  public Drl(String id, long version) {
    super();
    this.id = id;
    this.version = version;
  }

  public Drl() {
    super();
  }

}
