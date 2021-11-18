package it.dgc.verificac19.service;

import it.dgc.verificac19.model.CertificateSimple;

public interface VerifierService {

  CertificateSimple verify(String qrCodeTxt);

}
