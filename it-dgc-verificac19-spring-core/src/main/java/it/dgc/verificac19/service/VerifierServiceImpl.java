package it.dgc.verificac19.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import it.dgc.verificac19.data.VerifierRepository;
import it.dgc.verificac19.data.local.MedicinalProduct;
import it.dgc.verificac19.data.local.Preferences;
import it.dgc.verificac19.exception.VerificaC19CertificateExpiredDGCException;
import it.dgc.verificac19.exception.VerificaC19SignatureDGCException;
import it.dgc.verificac19.model.CertCode;
import it.dgc.verificac19.model.CertificateSimple;
import it.dgc.verificac19.model.CertificateSimple.SimplePersonModel;
import it.dgc.verificac19.model.CertificateStatus;
import it.dgc.verificac19.model.Country;
import it.dgc.verificac19.model.VerificaC19DefaultDGCBarcodeDecoder;
import it.dgc.verificac19.model.VerificaC19DefaultDGCSignatureVerifier;
import it.dgc.verificac19.model.VerificaC19DigitalCovidCertificate;
import it.dgc.verificac19.model.Exemption;
import it.dgc.verificac19.model.TestResult;
import it.dgc.verificac19.model.TestType;
import it.dgc.verificac19.model.ValidationRulesEnum;
import it.dgc.verificac19.model.ValidationScanMode;
import it.dgc.verificac19.utility.Utility;
import se.digg.dgc.encoding.impl.DefaultBarcodeDecoder;
import se.digg.dgc.payload.v1.RecoveryEntry;
import se.digg.dgc.payload.v1.TestEntry;
import se.digg.dgc.payload.v1.VaccinationEntry;
import se.digg.dgc.signatures.CertificateProvider;

@Service
public class VerifierServiceImpl implements VerifierService {

  private static final Logger LOG = LoggerFactory.getLogger(VerifierServiceImpl.class);

  @Autowired
  VerifierRepository verifierRepository;

  @Autowired
  Preferences preferences;

  private VerificaC19DefaultDGCBarcodeDecoder dgcBarcodeDecoder;

  private X509Certificate cert;

  @PostConstruct
  public void init() {
    this.dgcBarcodeDecoder = new VerificaC19DefaultDGCBarcodeDecoder(
        new VerificaC19DefaultDGCSignatureVerifier(), new CertificateProvider() {
          @Override
          public List<X509Certificate> getCertificates(String country, byte[] kid) {
            String base64Kid = Base64.encodeBase64String(kid);
            cert = (X509Certificate) verifierRepository.getCertificate(base64Kid);
            return cert != null ? Arrays.asList(cert) : Lists.newArrayList();
          }
        }, new DefaultBarcodeDecoder());

  }

  @Override
  public CertificateSimple verify(byte[] qrCodeImg, ValidationScanMode validationScanMode) {

    try {

      VerificaC19DigitalCovidCertificate digitalCovidCertificate =
          dgcBarcodeDecoder.decodeBarcode(qrCodeImg);

      return validate(digitalCovidCertificate, validationScanMode);

    } catch (VerificaC19CertificateExpiredDGCException e) {

      LOG.trace(e.getMessage());

      // CBOR encoding of a DGC v1 payload
      byte[] cBor = e.getCbor();

      return invalidateCertificate(cBor);

    } catch (VerificaC19SignatureDGCException e) {

      LOG.trace(e.getMessage());

      // CBOR encoding of a DGC v1 payload
      byte[] cBor = e.getCbor();

      return invalidateCertificate(cBor);

    } catch (SignatureException e) {
      LOG.trace(e.getMessage());
      return new CertificateSimple(CertificateStatus.NOT_VALID);
    } catch (Exception e) {
      LOG.trace(e.getMessage());
      return new CertificateSimple(CertificateStatus.NOT_EU_DCC);
    }

  }


  @Override
  public CertificateSimple verify(String qrCodeTxt, ValidationScanMode validationScanMode) {

    try {

      VerificaC19DigitalCovidCertificate digitalCovidCertificate = dgcBarcodeDecoder.decode(qrCodeTxt);

      return validate(digitalCovidCertificate, validationScanMode);

    } catch (VerificaC19CertificateExpiredDGCException e) {

      LOG.trace(e.getMessage());

      // CBOR encoding of a DGC v1 payload
      byte[] cBor = e.getCbor();

      return invalidateCertificate(cBor);

    } catch (VerificaC19SignatureDGCException e) {

      LOG.trace(e.getMessage());

      // CBOR encoding of a DGC v1 payload
      byte[] cBor = e.getCbor();

      return invalidateCertificate(cBor);

    } catch (SignatureException e) {
      LOG.trace(e.getMessage());
      return new CertificateSimple(CertificateStatus.NOT_VALID);
    } catch (Exception e) {
      LOG.trace(e.getMessage());
      return new CertificateSimple(CertificateStatus.NOT_EU_DCC);
    }

  }

  private CertificateSimple validate(VerificaC19DigitalCovidCertificate digitalCovidCertificate,
      ValidationScanMode validationScanMode) throws NoSuchAlgorithmException {

    CertificateSimple certificateSimple = new CertificateSimple();

    final String certificateIdentifier = extractUVCI(digitalCovidCertificate);

    certificateSimple.setCertificateStatus(
        getCertificateStatus(digitalCovidCertificate, validationScanMode, certificateIdentifier));

    certificateSimple.setPerson(new SimplePersonModel(digitalCovidCertificate.getNam().getFnt(),
        digitalCovidCertificate.getNam().getFn(), digitalCovidCertificate.getNam().getGnt(),
        digitalCovidCertificate.getNam().getGn()));

    certificateSimple.setDateOfBirth(digitalCovidCertificate.getDateOfBirth().asLocalDate());
    certificateSimple.setTimeStamp(LocalDateTime.now());

    return certificateSimple;
  }

  private CertificateSimple invalidateCertificate(byte[] cBor) {
    CertificateSimple certificateSimple = new CertificateSimple();

    certificateSimple.setCertificateStatus(CertificateStatus.NOT_VALID);
    try {
      VerificaC19DigitalCovidCertificate digitalCovidCertificate = VerificaC19DigitalCovidCertificate
          .getCBORMapper().readValue(cBor, VerificaC19DigitalCovidCertificate.class);
      certificateSimple.setPerson(new SimplePersonModel(digitalCovidCertificate.getNam().getFnt(),
          digitalCovidCertificate.getNam().getFn(), digitalCovidCertificate.getNam().getGnt(),
          digitalCovidCertificate.getNam().getGn()));
      certificateSimple.setDateOfBirth(digitalCovidCertificate.getDateOfBirth().asLocalDate());
      certificateSimple.setTimeStamp(LocalDateTime.now());
    } catch (IOException e1) {
      LOG.error("Failed to decode DCC from CBOR encoding", e1);
    }
    return certificateSimple;
  }

  /**
   *
   * This method checks the given [DigitalCovidCertificate] and returns the proper status as
   * [CertificateStatus].
   * 
   * @param validationScanMode
   * @param certificateIdentifier
   * @throws NoSuchAlgorithmException
   *
   */
  private CertificateStatus getCertificateStatus(VerificaC19DigitalCovidCertificate cert,
      ValidationScanMode validationScanMode, String certificateIdentifier)
      throws NoSuchAlgorithmException {

    if (isCertificateRevoked(Utility.sha256(certificateIdentifier))) {
      return CertificateStatus.REVOKED;
    }

    if (Strings.isNullOrEmpty(certificateIdentifier)) {
      return CertificateStatus.NOT_EU_DCC;
    }

    if (verifierRepository.checkInBlackList(certificateIdentifier)) {
      return CertificateStatus.NOT_VALID;
    }

    if (!CollectionUtils.isEmpty(cert.getR())) {
      return checkRecoveryStatements(cert.getR(), validationScanMode);
    }


    if (!CollectionUtils.isEmpty(cert.getT())) {
      if (validationScanMode.equals(ValidationScanMode.BOOSTER_DGP)
          || validationScanMode.equals(ValidationScanMode.SUPER_DGP)) {
        return CertificateStatus.NOT_VALID;
      }

      return checkTests(cert.getT());
    }

    if (!CollectionUtils.isEmpty(cert.getV())) {
      return checkVaccinations(cert.getV(), validationScanMode);
    }

    if (!CollectionUtils.isEmpty(cert.getE())) {
      return checkExemptions(cert.getE(), validationScanMode);
    }


    return CertificateStatus.NOT_VALID;
  }

  /**
   * This method checks the [Exemption] and returns a proper [CertificateStatus] after checking the
   * validity start and end dates.
   */
  private CertificateStatus checkExemptions(List<Exemption> list,
      ValidationScanMode validationScanMode) {


    try {

      Exemption lastExemption = Iterables.getLast(list);

      LocalDate startDate = LocalDate.parse(lastExemption.getCertificateValidFrom());

      LocalDate endDate = lastExemption.getCertificateValidUntil() != null
          ? LocalDate.parse(lastExemption.getCertificateValidUntil())
          : null;

      LOG.debug("dates start:{} end:{}", startDate, endDate);

      if (startDate.isAfter(LocalDate.now())) {
        return CertificateStatus.NOT_VALID_YET;
      }

      if (endDate != null) {
        if (LocalDate.now().isAfter(endDate)) {
          return CertificateStatus.NOT_VALID;
        }
      }

      if (validationScanMode.equals(ValidationScanMode.BOOSTER_DGP)) {
        return CertificateStatus.TEST_NEEDED;
      } else {
        return CertificateStatus.VALID;
      }
    } catch (Exception e) {
      return CertificateStatus.NOT_EU_DCC;
    }

  }

  /**
   *
   * This method checks the given recovery statements passed as a [List] of [RecoveryModel] and
   * returns the proper status as [CertificateStatus].
   * 
   * @param validationScanMode
   *
   */
  private CertificateStatus checkRecoveryStatements(List<RecoveryEntry> list,
      ValidationScanMode validationScanMode) {
    try {

      boolean isRecoveryBis = isRecoveryBis(list, validationScanMode);

      String recoveryCertEndDay =
          isRecoveryBis ? getRecoveryCertPvEndDay() : getRecoveryCertEndDay();
      String recoveryCertStartDay =
          isRecoveryBis ? getRecoveryCertPVStartDay() : getRecoveryCertStartDay();

      RecoveryEntry lastRecovery = Iterables.getLast(list);

      LocalDate startDate = lastRecovery.getDf();
      LocalDate endDate = lastRecovery.getDu();

      LOG.debug("dates start:{} end:{}", startDate, endDate);


      if ((startDate.plusDays(Long.valueOf(recoveryCertStartDay)).isAfter(LocalDate.now()))) {
        return CertificateStatus.NOT_VALID_YET;
      } else if (LocalDate.now().isAfter(startDate.plusDays(Long.valueOf(recoveryCertEndDay)))) {
        return CertificateStatus.NOT_VALID;
      } else {
        if (validationScanMode.equals(ValidationScanMode.BOOSTER_DGP)) {
          return CertificateStatus.TEST_NEEDED;
        } else {
          return CertificateStatus.VALID;
        }

      }

    } catch (Exception e) {
      return CertificateStatus.NOT_VALID;
    }
  }

  private boolean isRecoveryBis(List<RecoveryEntry> list, ValidationScanMode validationScanMode)
      throws CertificateParsingException {

    RecoveryEntry firstRecovery = Iterables.getFirst(list, null);
    if (firstRecovery != null && Country.IT.getValue().equals(firstRecovery.getCo())) {
      List<String> keysUsage = cert.getExtendedKeyUsage();
      Iterator<String> it = keysUsage.iterator();
      while (it.hasNext()) {
        String keyUsage = it.next();
        if (CertCode.OID_RECOVERY.getValue().equals(keyUsage)
            || CertCode.OID_ALT_RECOVERY.getValue().equals(keyUsage)) {
          return true;
        }
      }
    }

    return false;

  }

  /**
   *
   * This method checks the given vaccinations passed as a [List] of [VaccinationModel] and returns
   * the proper status as [CertificateStatus].
   * 
   * @param validationScanMode
   *
   */
  private CertificateStatus checkVaccinations(List<VaccinationEntry> list,
      ValidationScanMode validationScanMode) {

    VaccinationEntry lastVaccination = Iterables.getLast(list);

    // Check if vaccine is present in setting list; otherwise, return not valid
    String vaccineEndDayComplete = getVaccineEndDayComplete(lastVaccination.getMp());

    boolean isValid = !vaccineEndDayComplete.isEmpty();
    if (!isValid) {
      return CertificateStatus.NOT_VALID;
    }

    boolean isSputnikNotFromSanMarino =
        lastVaccination.getMp().equals("Sputnik-V") && !lastVaccination.getCo().equals("SM");
    if (isSputnikNotFromSanMarino) {
      return CertificateStatus.NOT_VALID;
    }

    try {
      if (lastVaccination.getDn() < lastVaccination.getSd()) {

        LocalDate startDate = lastVaccination.getDt()
            .plusDays(Long.valueOf(getVaccineStartDayNotComplete(lastVaccination.getMp())));
        LocalDate endDate = lastVaccination.getDt()
            .plusDays(Long.valueOf(getVaccineEndDayNotComplete(lastVaccination.getMp())));

        LOG.debug("dates start:{} end: {}", startDate, endDate);

        if (startDate.isAfter(LocalDate.now())) {
          return CertificateStatus.NOT_VALID_YET;
        } else if (LocalDate.now().isAfter(endDate)) {
          return CertificateStatus.NOT_VALID;
        } else {
          return CertificateStatus.VALID;
        }

      } else if (lastVaccination.getDn() >= lastVaccination.getSd()) {

        LocalDate startDate = null;
        LocalDate endDate = null;

        if (lastVaccination.getMp().equals(MedicinalProduct.JOHNSON)
            && ((lastVaccination.getDn() > lastVaccination.getSd())
                || (lastVaccination.getDn() == lastVaccination.getSd()
                    && lastVaccination.getDn() >= 2))) {
          startDate = lastVaccination.getDt();

          endDate = lastVaccination.getDt()
              .plusDays(Long.valueOf(getVaccineEndDayComplete(lastVaccination.getMp())));
        } else {
          startDate = lastVaccination.getDt()
              .plusDays(Long.valueOf(getVaccineStartDayComplete(lastVaccination.getMp())));
          endDate = lastVaccination.getDt()
              .plusDays(Long.valueOf(getVaccineEndDayComplete(lastVaccination.getMp())));
        }
        LOG.debug("dates start:{} end: {}", startDate, endDate);

        if (startDate.isAfter(LocalDate.now())) {
          return CertificateStatus.NOT_VALID_YET;
        }
        if (LocalDate.now().isAfter(endDate)) {
          return CertificateStatus.NOT_VALID;
        } else {

          if (validationScanMode == ValidationScanMode.BOOSTER_DGP) {
            if (lastVaccination.getMp().equals(MedicinalProduct.JOHNSON)) {
              if (lastVaccination.getDn() == lastVaccination.getSd() && lastVaccination.getDn() < 2)
                return CertificateStatus.TEST_NEEDED;
            } else {
              if ((lastVaccination.getDn() == lastVaccination.getSd()
                  && lastVaccination.getDn() < 3))
                return CertificateStatus.TEST_NEEDED;
            }
            return CertificateStatus.VALID;
          } else {
            return CertificateStatus.VALID;
          }
        }

      } else {
        return CertificateStatus.NOT_VALID;
      }
    } catch (Exception e) {
      return CertificateStatus.NOT_EU_DCC;
    }
  }

  /**
   *
   * This method checks the given tests passed as a [List] of [TestModel] and returns the proper
   * status as [CertificateStatus].
   *
   */
  private CertificateStatus checkTests(List<TestEntry> list) {

    TestEntry lastTest = Iterables.getLast(list);
    if (lastTest.getTr() == TestResult.DETECTED.getValue()) {
      return CertificateStatus.NOT_VALID;
    }

    try {
      LocalDateTime ldtDateTimeOfCollection =
          LocalDateTime.ofInstant(lastTest.getSc(), ZoneOffset.UTC);

      String testType = lastTest.getTt();

      LocalDateTime startDate = null;
      LocalDateTime endDate = null;

      if (testType.equals(TestType.MOLECULAR.getValue())) {
        startDate = ldtDateTimeOfCollection.plusHours(Long.valueOf(getMolecularTestStartHour()));
        endDate = ldtDateTimeOfCollection.plusHours(Long.valueOf(getMolecularTestEndHour()));
      } else if (testType.equals(TestType.RAPID.getValue())) {
        startDate = ldtDateTimeOfCollection.plusHours(Long.valueOf(getRapidTestStartHour()));
        endDate = ldtDateTimeOfCollection.plusHours(Long.valueOf(getRapidTestEndHour()));
      } else {
        return CertificateStatus.NOT_VALID;
      }

      LOG.debug("date start:{} end:{}", startDate, endDate);

      if (startDate.isAfter(LocalDateTime.now())) {
        return CertificateStatus.NOT_VALID_YET;
      } else if (LocalDateTime.now().isAfter(endDate)) {
        return CertificateStatus.NOT_VALID;
      } else {
        return CertificateStatus.VALID;
      }

    } catch (Exception e) {
      return CertificateStatus.NOT_EU_DCC;
    }
  }

  /**
   * This method extracts the UCVI from an Exemption, Vaccine, Recovery or Test based on what was
   * received.
   */
  private String extractUVCI(VerificaC19DigitalCovidCertificate digitalCovidCertificate) {

    Optional<Exemption> e = Optional.ofNullable(digitalCovidCertificate.getE())
        .orElseGet(Collections::emptyList).stream().findFirst();
    if (e.isPresent())
      return e.get().getCertificateIdentifier();

    Optional<VaccinationEntry> v = Optional.ofNullable(digitalCovidCertificate.getV())
        .orElseGet(Collections::emptyList).stream().findFirst();
    if (v.isPresent())
      return v.get().getCi();

    Optional<TestEntry> t = Optional.ofNullable(digitalCovidCertificate.getT())
        .orElseGet(Collections::emptyList).stream().findFirst();
    if (t.isPresent())
      return t.get().getCi();

    Optional<RecoveryEntry> r = Optional.ofNullable(digitalCovidCertificate.getR())
        .orElseGet(Collections::emptyList).stream().findFirst();
    if (r.isPresent())
      return r.get().getCi();

    return "";
  }

  private String getRecoveryCertStartDay() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_START_DAY);
  }

  private String getRecoveryCertPVStartDay() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_PV_START_DAY);
  }

  private String getRecoveryCertEndDay() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_END_DAY);
  }

  private String getRecoveryCertPvEndDay() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_PV_END_DAY);
  }

  private String getMolecularTestStartHour() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.MOLECULAR_TEST_START_HOUR);
  }

  private String getMolecularTestEndHour() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.MOLECULAR_TEST_END_HOUR);
  }

  private String getRapidTestStartHour() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RAPID_TEST_START_HOUR);
  }

  private String getRapidTestEndHour() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RAPID_TEST_END_HOUR);
  }

  private String getVaccineStartDayNotComplete(String vaccineType) {
    return preferences.getValidationRuleValueByNameAndType(
        ValidationRulesEnum.VACCINE_START_DAY_NOT_COMPLETE, vaccineType);
  }

  private String getVaccineEndDayNotComplete(String vaccineType) {
    return preferences.getValidationRuleValueByNameAndType(
        ValidationRulesEnum.VACCINE_END_DAY_NOT_COMPLETE, vaccineType);
  }

  private String getVaccineStartDayComplete(String vaccineType) {
    return preferences.getValidationRuleValueByNameAndType(
        ValidationRulesEnum.VACCINE_START_DAY_COMPLETE, vaccineType);
  }

  private String getVaccineEndDayComplete(String vaccineType) {
    return preferences.getValidationRuleValueByNameAndType(
        ValidationRulesEnum.VACCINE_END_DAY_COMPLETE, vaccineType);
  }

  private boolean isCertificateRevoked(String hash) {
    if (!preferences.isDrlSyncActive()) {
      return false;
    }

    if (StringUtils.hasText(hash)) {

      LOG.debug("Revoke", "Searching");
      boolean isFound = verifierRepository.checkInRevokedList(hash);

      if (isFound) {
        LOG.info("Revoke Pass hash: " + hash + " found!");
        return true;
      } else
        return false;
    } else {
      return true;
    }
  }

}
