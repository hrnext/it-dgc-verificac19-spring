package it.dgc.verificac19.service;

import it.dgc.verificac19.model.CertificateSimple;
import it.dgc.verificac19.model.ValidationScanMode;

public interface VerifierService {

  CertificateSimple verify(String qrCodeTxt, ValidationScanMode validationScanMode);

  CertificateSimple verify(byte[] qrCodeImage, ValidationScanMode validationScanMode);

}
