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
@Table(name = "revokedpass")
public class RevokedPass implements Serializable {

    private static final long serialVersionUID = 6379223893578219279L;

    @Id
    String hashedUVCI;

    public RevokedPass() {
        super();
    }

    public RevokedPass(String hashedUVCI) {
        super();
        this.hashedUVCI = hashedUVCI;
    }

    /**
     * @return the hashedUVCI
     */
    public String getHashedUVCI() {
        return hashedUVCI;
    }

    /**
     * @param hashedUVCI
     *            the hashedUVCI to set
     */
    public void setHashedUVCI(String hashedUVCI) {
        this.hashedUVCI = hashedUVCI;
    }

}
