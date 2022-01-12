package it.dgc.verificac19.utility;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import org.apache.commons.codec.binary.Base64;

public class Utility {

  public static final String SDK_VERSION = "1.0.2";

  public static Certificate base64ToX509Certificate(String value) throws CertificateException {
    byte[] decoded = Base64.decodeBase64(value);
    InputStream inStream = new ByteArrayInputStream(decoded);
    return CertificateFactory.getInstance("X.509").generateCertificate(inStream);
  }

  public static String sha256(String value) throws NoSuchAlgorithmException {
    return hashString(value, "SHA-256");
  }

  private static String hashString(String input, String algorithm) throws NoSuchAlgorithmException {
    return encodeBase64(MessageDigest.getInstance(algorithm).digest(input.getBytes()));
  }

  private static String encodeBase64(byte[] input) {
    return Base64.encodeBase64String(input);
  }
}
