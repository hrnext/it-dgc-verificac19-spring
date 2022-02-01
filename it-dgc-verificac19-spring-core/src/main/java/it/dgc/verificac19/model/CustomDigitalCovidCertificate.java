/**
 * 
 */
package it.dgc.verificac19.model;


import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import se.digg.dgc.payload.v1.DGCSchemaException;
import se.digg.dgc.payload.v1.DigitalCovidCertificate;

/**
 * @author NIGFRA
 *
 */
@JsonPropertyOrder({"ver", "nam", "dob", "v", "t", "r", "e"})
public class CustomDigitalCovidCertificate extends DigitalCovidCertificate {

  @JsonProperty("e")
  @JsonPropertyDescription("Exemption Group")
  @Valid
  private List<Exemption> e = null;

  public static CustomDigitalCovidCertificate decode(final byte[] cbor) throws DGCSchemaException {
    try {
      return getCBORMapper().readValue(cbor, CustomDigitalCovidCertificate.class);
    } catch (final IOException e) {
      throw new DGCSchemaException("Failed to decode DCC from CBOR encoding", e);
    }
  }

  /**
   * Exemption Group
   * 
   */
  @JsonProperty("e")
  public List<Exemption> getE() {
    return e;
  }

  /**
   * Exemption Group
   * 
   */
  @JsonProperty("e")
  public void setE(List<Exemption> e) {
    this.e = e;
  }

  public CustomDigitalCovidCertificate withE(List<Exemption> e) {
    this.e = e;
    return this;
  }

}
