package it.dgc.verificac19.data;

import java.security.cert.Certificate;

/**
 *
 * This interface defines the methods to download public certificates (i.e. settings) and check the
 * download status. These are overridden by the implementing class [VerifierRepositoryImpl].
 *
 */
public interface VerifierRepository {

  boolean syncData();

  Certificate getCertificate(String kid);

  boolean checkInBlackList(String kid);

  boolean checkInRevokedList(String hashedUVCI);

}
