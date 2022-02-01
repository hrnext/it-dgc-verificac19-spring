/**
 * 
 */
package it.dgc.verificac19.model;

import java.io.IOException;
import java.security.SignatureException;
import java.security.cert.CertificateExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.digg.dgc.encoding.BarcodeDecoder;
import se.digg.dgc.encoding.BarcodeException;
import se.digg.dgc.payload.v1.DGCSchemaException;
import se.digg.dgc.service.impl.DefaultDGCBarcodeDecoder;
import se.digg.dgc.signatures.CertificateProvider;
import se.digg.dgc.signatures.DGCSignatureVerifier;

/**
 * @author NIGFRA
 *
 */
public class CustomDefaultDGCBarcodeDecoder extends DefaultDGCBarcodeDecoder {

  private static final Logger LOG = LoggerFactory.getLogger(CustomDefaultDGCBarcodeDecoder.class);

  public CustomDefaultDGCBarcodeDecoder(DGCSignatureVerifier dgcSignatureVerifier,
      CertificateProvider certificateProvider, BarcodeDecoder barcodeDecoder) {
    super(dgcSignatureVerifier, certificateProvider, barcodeDecoder);
  }

  @Override
  public CustomDigitalCovidCertificate decode(String base45)
      throws DGCSchemaException, SignatureException, CertificateExpiredException, IOException {

    final byte[] dccEncoding = this.decodeToBytes(base45);

    LOG.trace("CBOR decoding DCC ...");
    final CustomDigitalCovidCertificate dcc = CustomDigitalCovidCertificate.decode(dccEncoding);
    LOG.trace("Decoded into: {}", dcc);

    return dcc;
  }

  @Override
  public CustomDigitalCovidCertificate decodeRaw(byte[] cwt)
      throws DGCSchemaException, SignatureException, CertificateExpiredException, IOException {
    final byte[] encodedDcc = this.decodeRawToBytes(cwt);

    LOG.trace("CBOR decoding DCC ...");
    final CustomDigitalCovidCertificate dcc = CustomDigitalCovidCertificate.decode(encodedDcc);
    LOG.trace("Decoded into: {}", dcc);

    return dcc;
  }

  @Override
  public CustomDigitalCovidCertificate decodeBarcode(byte[] image) throws DGCSchemaException,
      SignatureException, CertificateExpiredException, BarcodeException, IOException {

    final byte[] encodedDcc = this.decodeBarcodeToBytes(image);

    LOG.trace("CBOR decoding DGC ...");
    final CustomDigitalCovidCertificate dgc = CustomDigitalCovidCertificate.decode(encodedDcc);
    LOG.trace("Decoded into: {}", dgc);
    return dgc;
  }

}
