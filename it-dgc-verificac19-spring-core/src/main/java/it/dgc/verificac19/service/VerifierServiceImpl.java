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
import it.dgc.verificac19.model.Const;
import it.dgc.verificac19.model.Country;
import it.dgc.verificac19.model.Exemption;
import it.dgc.verificac19.model.TestResult;
import it.dgc.verificac19.model.TestType;
import it.dgc.verificac19.model.ValidationRulesEnum;
import it.dgc.verificac19.model.ValidationScanMode;
import it.dgc.verificac19.model.VerificaC19DefaultDGCBarcodeDecoder;
import it.dgc.verificac19.model.VerificaC19DefaultDGCSignatureVerifier;
import it.dgc.verificac19.model.VerificaC19DigitalCovidCertificate;
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
            return cert != null ? Lists.newArrayList(cert) : Lists.newArrayList();
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

      VerificaC19DigitalCovidCertificate digitalCovidCertificate =
          dgcBarcodeDecoder.decode(qrCodeTxt);

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
      VerificaC19DigitalCovidCertificate digitalCovidCertificate =
          VerificaC19DigitalCovidCertificate.getCBORMapper().readValue(cBor,
              VerificaC19DigitalCovidCertificate.class);
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

      return checkTests(cert.getT(), validationScanMode, cert.getDateOfBirth().asLocalDate());
    }

    if (!CollectionUtils.isEmpty(cert.getV())) {
      return checkVaccinations(cert.getV(), validationScanMode,
          cert.getDateOfBirth().asLocalDate());
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
          return CertificateStatus.EXPIRED;
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

      RecoveryEntry firstRecovery = Iterables.getFirst(list, null);

      String countryCode = Country.IT.getValue();

      String endDaysToAdd = null;
      if (isRecoveryBis) {
        endDaysToAdd = getRecoveryCertPvEndDay();
      } else {
        endDaysToAdd = getRecoveryCertEndDayUnified(countryCode);
      }


      String startDaysToAdd =
          isRecoveryBis ? getRecoveryCertPVStartDay() : getRecoveryCertStartDayUnified(countryCode);

      LocalDate startDate = firstRecovery.getDf();

      LocalDate endDate = startDate.plusDays(Long.parseLong(endDaysToAdd));

      LOG.debug("dates start:{} end:{}", startDate, endDate);

      if ((startDate.plusDays(Long.parseLong(startDaysToAdd)).isAfter(LocalDate.now()))) {
        return CertificateStatus.NOT_VALID_YET;
      } else if (LocalDate.now().isAfter(endDate)) {
        return CertificateStatus.EXPIRED;
      } else {
        if (validationScanMode.equals(ValidationScanMode.BOOSTER_DGP) && !isRecoveryBis) {
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
    if (firstRecovery != null && Country.IT.getValue().equals(firstRecovery.getCo())
        && cert.getExtendedKeyUsage() != null) {
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
   * @param birthDate
   *
   */
  private CertificateStatus checkVaccinations(List<VaccinationEntry> list,
      ValidationScanMode validationScanMode, LocalDate birthDate) {

    VaccinationEntry lastVaccination = Iterables.getLast(list);

    if (isNotComplete(lastVaccination.getDn(), lastVaccination.getSd())
        && !isEMA(lastVaccination.getMp(), lastVaccination.getCo()))
      return CertificateStatus.NOT_VALID;


    try {
      return validateVaccinationsWithScanMode(lastVaccination, validationScanMode, birthDate);
    } catch (Exception e) {
      return CertificateStatus.NOT_EU_DCC;
    }
  }

  private CertificateStatus validateVaccinationsWithScanMode(VaccinationEntry lastVaccination,
      ValidationScanMode validationScanMode, LocalDate birthDate) {
    if (ValidationScanMode.NORMAL_DGP.equals(validationScanMode)) {
      return vaccineStandardStrategy(lastVaccination);
    } else if (ValidationScanMode.SUPER_DGP.equals(validationScanMode)) {
      return vaccineStrengthenedStrategy(lastVaccination);
    } else if (ValidationScanMode.BOOSTER_DGP.equals(validationScanMode)) {
      return vaccineBoosterStrategy(lastVaccination);
    } else {
      return CertificateStatus.NOT_EU_DCC;
    }
  }

  private CertificateStatus vaccineBoosterStrategy(VaccinationEntry lastVaccination) {
    LocalDate dateOfVaccination = lastVaccination.getDt();

    String startDaysToAdd;
    if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {
      startDaysToAdd = getVaccineStartDayBoosterUnified(Country.IT.getValue());
    } else if (isNotComplete(lastVaccination.getDn(), lastVaccination.getSd())) {
      startDaysToAdd = getVaccineStartDayNotComplete(lastVaccination.getMp());
    } else {
      startDaysToAdd =
          getVaccineStartDayCompleteUnified(Country.IT.getValue(), lastVaccination.getMp());
    }

    String endDaysToAdd;
    if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {
      endDaysToAdd = getVaccineEndDayBoosterUnified(Country.IT.getValue());
    } else if (isNotComplete(lastVaccination.getDn(), lastVaccination.getSd())) {
      endDaysToAdd = getVaccineEndDayNotComplete(lastVaccination.getMp());
    } else {

      endDaysToAdd = getVaccineEndDayCompleteUnified(Country.IT.getValue());
    }

    LocalDate startDate = dateOfVaccination.plusDays(Long.parseLong(startDaysToAdd));
    LocalDate endDate = dateOfVaccination.plusDays(Long.parseLong(endDaysToAdd));

    if (LocalDate.now().isBefore(startDate))
      return CertificateStatus.NOT_VALID_YET;
    else if (LocalDate.now().isAfter(endDate))
      return CertificateStatus.EXPIRED;
    else if (isComplete(lastVaccination.getDn(), lastVaccination.getSd())) {
      if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {
        if (isEMA(lastVaccination.getMp(), lastVaccination.getCo())) {
          return CertificateStatus.VALID;
        } else
          return CertificateStatus.TEST_NEEDED;
      } else
        return CertificateStatus.TEST_NEEDED;
    } else
      return CertificateStatus.NOT_VALID;

  }

  private CertificateStatus vaccineStrengthenedStrategy(VaccinationEntry lastVaccination) {
    LocalDate startDate = null;
    LocalDate endDate = null;
    LocalDate extendedDate = null;

    String country = lastVaccination.getCo();
    LocalDate dateOfVaccination = lastVaccination.getDt();

    if (Country.IT.getValue().equals(country)) {
      return vaccineStandardStrategy(lastVaccination);
    } else {

      if (isNotComplete(lastVaccination.getDn(), lastVaccination.getSd())) {
        if (isEMA(lastVaccination.getMp(), lastVaccination.getCo())) {
          startDate = dateOfVaccination.plusDays(Long.parseLong(lastVaccination.getMp()));
          endDate = dateOfVaccination
              .plusDays(Long.parseLong(getVaccineEndDayNotComplete(lastVaccination.getMp())));
        } else {
          return CertificateStatus.NOT_VALID;
        }
      }

      if (isComplete(lastVaccination.getDn(), lastVaccination.getSd())) {
        String startDaysToAdd;
        if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {
          startDaysToAdd = getVaccineStartDayBoosterUnified(Country.IT.getValue());
        } else {
          startDaysToAdd =
              getVaccineStartDayCompleteUnified(Country.IT.getValue(), lastVaccination.getMp());
        }

        String endDaysToAdd;
        if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {
          endDaysToAdd = getVaccineEndDayBoosterUnified(Country.IT.getValue());
        } else {
          endDaysToAdd = getVaccineEndDayCompleteUnified(Country.IT.getValue());
        }

        String extendedDaysToAdd = getVaccineEndDayCompleteExtendedEMA();

        startDate = dateOfVaccination.plusDays(Long.parseLong(startDaysToAdd));
        endDate = dateOfVaccination.plusDays(Long.parseLong(endDaysToAdd));
        extendedDate = dateOfVaccination.plusDays(Long.parseLong(extendedDaysToAdd));
      }
    }



    if (isNotComplete(lastVaccination.getDn(), lastVaccination.getSd())) {

      if (!isEMA(lastVaccination.getMp(), country))
        return CertificateStatus.NOT_VALID;
      else if (LocalDate.now().isBefore(startDate))
        return CertificateStatus.NOT_VALID_YET;
      else if (LocalDate.now().isAfter(endDate))
        return CertificateStatus.EXPIRED;
      else
        return CertificateStatus.VALID;

    }
    if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {

      if (LocalDate.now().isBefore(startDate))
        return CertificateStatus.NOT_VALID_YET;
      else if (LocalDate.now().isAfter(endDate))
        return CertificateStatus.EXPIRED;
      else {
        if (isEMA(lastVaccination.getMp(), lastVaccination.getCo()))
          return CertificateStatus.VALID;
        else
          return CertificateStatus.TEST_NEEDED;
      }
    }

    else {

      if (isEMA(lastVaccination.getMp(), lastVaccination.getCo())) {

        if (LocalDate.now().isBefore(startDate))
          return CertificateStatus.NOT_VALID_YET;

        if (LocalDate.now().isBefore(endDate) || !LocalDate.now().isAfter(endDate))
          return CertificateStatus.VALID;
        if (LocalDate.now().isBefore(extendedDate) || !LocalDate.now().isAfter(extendedDate))
          return CertificateStatus.TEST_NEEDED;
        else
          return CertificateStatus.EXPIRED;


      } else {
        if (LocalDate.now().isBefore(startDate))
          return CertificateStatus.NOT_VALID_YET;
        else if (LocalDate.now().isBefore(extendedDate) || !LocalDate.now().isAfter(extendedDate))
          return CertificateStatus.TEST_NEEDED;
        else
          return CertificateStatus.EXPIRED;
      }


    }

  }

  private CertificateStatus vaccineStandardStrategy(VaccinationEntry lastVaccination) {

    LocalDate dateOfVaccination = lastVaccination.getDt();

    LocalDate startDate;
    if (isComplete(lastVaccination.getDn(), lastVaccination.getSd())) {
      String startDaysToAdd;
      if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {
        startDaysToAdd = getVaccineStartDayBoosterUnified(Country.IT.getValue());
      } else {
        startDaysToAdd =
            getVaccineStartDayCompleteUnified(Country.IT.getValue(), lastVaccination.getMp());
      }
      startDate = dateOfVaccination.plusDays(Long.parseLong(startDaysToAdd));
    } else {
      startDate = dateOfVaccination
          .plusDays(Long.parseLong(getVaccineStartDayNotComplete(lastVaccination.getMp())));
    }


    LocalDate endDate;
    if (isComplete(lastVaccination.getDn(), lastVaccination.getSd())) {
      String endDaysToAdd;
      if (isBooster(lastVaccination.getMp(), lastVaccination.getDn(), lastVaccination.getSd())) {
        endDaysToAdd = getVaccineEndDayBoosterUnified(Country.IT.getValue());
      } else {
        endDaysToAdd = getVaccineEndDayCompleteUnified(Country.IT.getValue());
      }
      endDate = dateOfVaccination.plusDays(Long.parseLong(endDaysToAdd));
    } else {
      endDate = dateOfVaccination
          .plusDays(Long.parseLong(getVaccineEndDayNotComplete(lastVaccination.getMp())));
    }

    if (LocalDate.now().isBefore(startDate))
      return CertificateStatus.NOT_VALID_YET;
    else if (LocalDate.now().isAfter(endDate))
      return CertificateStatus.EXPIRED;
    else if (!isEMA(lastVaccination.getMp(), lastVaccination.getCo()))
      return CertificateStatus.NOT_VALID;
    else
      return CertificateStatus.VALID;
  }



  /**
   *
   * This method checks the given tests passed as a [List] of [TestModel] and returns the proper
   * status as [CertificateStatus].
   * 
   * @param validationScanMode
   *
   */
  private CertificateStatus checkTests(List<TestEntry> list, ValidationScanMode validationScanMode,
      LocalDate birthDate) {

    TestEntry lastTest = Iterables.getLast(list);
    if (lastTest.getTr().equals(TestResult.DETECTED.getValue())) {
      return CertificateStatus.NOT_VALID;
    }

    try {
      LocalDateTime ldtDateTimeOfCollection =
          LocalDateTime.ofInstant(lastTest.getSc(), ZoneOffset.UTC);

      String testType = lastTest.getTt();

      LocalDateTime startDate = null;
      LocalDateTime endDate = null;

      if (testType.equals(TestType.MOLECULAR.getValue())) {
        startDate = ldtDateTimeOfCollection.plusHours(Long.parseLong(getMolecularTestStartHour()));
        endDate = ldtDateTimeOfCollection.plusHours(Long.parseLong(getMolecularTestEndHour()));
      } else if (testType.equals(TestType.RAPID.getValue())) {
        startDate = ldtDateTimeOfCollection.plusHours(Long.parseLong(getRapidTestStartHour()));
        endDate = ldtDateTimeOfCollection.plusHours(Long.parseLong(getRapidTestEndHour()));
      } else {
        return CertificateStatus.NOT_VALID;
      }

      LOG.debug("date start:{} end:{}", startDate, endDate);

      if (startDate.isAfter(LocalDateTime.now())) {
        return CertificateStatus.NOT_VALID_YET;
      } else if (LocalDateTime.now().isAfter(endDate)) {
        return CertificateStatus.NOT_VALID;
      } else
        return CertificateStatus.VALID;
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

  private String getRecoveryCertPVStartDay() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_PV_START_DAY);
  }

  private String getRecoveryCertPvEndDay() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_PV_END_DAY);
  }

  private String getVaccineEndDayCompleteUnder18() {
    return preferences
        .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_UNDER_18);
  }

  private String getVaccineCompleteUnder18Offset() {
    return preferences
        .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_COMPLETE_UNDER_18_OFFSET);
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

  private String getVaccineStartDayCompleteUnified(String countryCode, String medicalProduct) {
    int daysToAdd = MedicinalProduct.JANSEN.equals(medicalProduct)
        ? Integer.parseInt(getVaccineStartDayComplete(MedicinalProduct.JANSEN))
        : Const.NO_VALUE_NUMBER;

    int startDay = 0;
    if (Country.IT.getValue().equals(countryCode)) {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_START_DAY_COMPLETE_IT);
      if (StringUtils.hasText(value)) {
        startDay = Integer.parseInt(value);
      }
    } else {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_START_DAY_COMPLETE_NOT_IT);
      if (StringUtils.hasText(value)) {
        startDay = Integer.parseInt(value);
      }
    }

    int valueCal = startDay + daysToAdd;

    return String.valueOf(valueCal);

  }

  private String getVaccineEndDayCompleteUnified(String countryCode) {
    if (Country.IT.getValue().equals(countryCode)) {
      String value =
          preferences.getValidationRuleValueByName(ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "180";
      }

    } else {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_NOT_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "270";
      }
    }
  }

  private String getVaccineStartDayBoosterUnified(String countryCode) {
    if (Country.IT.getValue().equals(countryCode)) {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_START_DAY_BOOSTER_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "0";
      }
    } else {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_START_DAY_BOOSTER_NOT_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "0";
      }
    }
  }

  private String getVaccineEndDayBoosterUnified(String countryCode) {
    if (Country.IT.getValue().equals(countryCode)) {
      String value =
          preferences.getValidationRuleValueByName(ValidationRulesEnum.VACCINE_END_DAY_BOOSTER_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "180";
      }
    } else {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_END_DAY_BOOSTER_NOT_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "270";
      }
    }

  }

  private String getRecoveryCertStartDayUnified(String countryCode) {
    if (Country.IT.getValue().equals(countryCode)) {
      String value =
          preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_START_DAY_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "0";
      }
    } else {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_START_DAY_NOT_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "0";
      }
    }
  }

  private String getRecoveryCertEndDayUnified(String countryCode) {
    if (Country.IT.getValue().equals(countryCode)) {
      String value =
          preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_END_DAY_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "180";
      }
    } else {
      String value = preferences
          .getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_END_DAY_NOT_IT);
      if (StringUtils.hasText(value)) {
        return value;
      } else {
        return "270";
      }
    }
  }

  private String getVaccineEndDayCompleteExtendedEMA() {
    String value = preferences
        .getValidationRuleValueByName(ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_EXTENDED_EMA);
    if (StringUtils.hasText(value)) {
      return value;
    } else {
      return Const.NO_VALUE_TEXT;
    }
  }

  private boolean isEMA(String medicinalProduct, String countryOfVaccination) {
    boolean isStandardEma = false;

    String values = preferences.getValidationRuleValueByName(ValidationRulesEnum.EMA_VACCINES);
    String[] arrayValues;
    if (values != null) {
      arrayValues = values.split(";");
      isStandardEma = Arrays.asList(arrayValues).contains(medicinalProduct);
    }


    // also Sputnik is EMA, but only if from San Marino
    boolean isSpecialEma = MedicinalProduct.SPUTNIK.equals(medicinalProduct)
        && Country.SM.getValue().equals(countryOfVaccination);

    return isStandardEma || isSpecialEma;
  }

  private boolean isComplete(int doseNumber, int totalSeriesOfDoses) {
    return doseNumber >= totalSeriesOfDoses;
  }

  private boolean isNotComplete(int doseNumber, int totalSeriesOfDoses) {
    return doseNumber < totalSeriesOfDoses;
  }

  private boolean isBooster(String medicinalProduct, int doseNumber, int totalSeriesOfDoses) {
    if (isJansen(medicinalProduct)) {
      return doseNumber >= 2;
    } else {
      return doseNumber >= 3 || doseNumber > totalSeriesOfDoses;
    }
  }

  private boolean isJansen(String medicinalProduct) {
    return MedicinalProduct.JANSEN.equals(medicinalProduct);
  }

}
