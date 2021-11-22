package it.dgc.verificac19.service;

import java.security.SignatureException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import it.dgc.verificac19.data.VerifierRepository;
import it.dgc.verificac19.data.local.Preferences;
import it.dgc.verificac19.model.CertificateSimple;
import it.dgc.verificac19.model.CertificateSimple.SimplePersonModel;
import it.dgc.verificac19.model.CertificateStatus;
import it.dgc.verificac19.model.TestResult;
import it.dgc.verificac19.model.TestType;
import it.dgc.verificac19.model.ValidationRulesEnum;
import se.digg.dgc.encoding.impl.DefaultBarcodeDecoder;
import se.digg.dgc.payload.v1.DigitalCovidCertificate;
import se.digg.dgc.payload.v1.RecoveryEntry;
import se.digg.dgc.payload.v1.TestEntry;
import se.digg.dgc.payload.v1.VaccinationEntry;
import se.digg.dgc.service.DGCBarcodeDecoder;
import se.digg.dgc.service.impl.DefaultDGCBarcodeDecoder;
import se.digg.dgc.signatures.CertificateProvider;

@Service
public class VerifierServiceImpl implements VerifierService {

  private static final Logger LOG = LoggerFactory.getLogger(VerifierServiceImpl.class);

  @Autowired
  VerifierRepository verifierRepository;

  @Autowired
  Preferences preferences;

  private DGCBarcodeDecoder dgcBarcodeDecoder;

  @PostConstruct
  public void init() {
    this.dgcBarcodeDecoder = new DefaultDGCBarcodeDecoder(null, new CertificateProvider() {
      @Override
      public List<X509Certificate> getCertificates(String country, byte[] kid) {
        String base64Kid = Base64.encodeBase64String(kid);
        X509Certificate cert = (X509Certificate) verifierRepository.getCertificate(base64Kid);
        return cert != null ? Arrays.asList(cert) : Lists.newArrayList();
      }
    }, new DefaultBarcodeDecoder());

  }

  @Override
  public CertificateSimple verify(byte[] qrCodeImg) {

    try {

      DigitalCovidCertificate digitalCovidCertificate = dgcBarcodeDecoder.decodeBarcode(qrCodeImg);

      return validate(digitalCovidCertificate);

    } catch (CertificateExpiredException | SignatureException e) {
      return new CertificateSimple(CertificateStatus.NOT_EU_DCC);
    } catch (Exception e) {
      return new CertificateSimple(CertificateStatus.NOT_VALID);
    }

  }

  @Override
  public CertificateSimple verify(String qrCodeTxt) {

    try {

      DigitalCovidCertificate digitalCovidCertificate = dgcBarcodeDecoder.decode(qrCodeTxt);

      return validate(digitalCovidCertificate);

    } catch (CertificateExpiredException | SignatureException e) {
      return new CertificateSimple(CertificateStatus.NOT_EU_DCC);
    } catch (Exception e) {
      return new CertificateSimple(CertificateStatus.NOT_VALID);
    }
  }

  private CertificateSimple validate(DigitalCovidCertificate digitalCovidCertificate) {

    CertificateSimple certificateSimple = new CertificateSimple();

    String certificateIdentifier = extractUVCI(digitalCovidCertificate);

    if (Strings.isNullOrEmpty(certificateIdentifier)) {
      certificateSimple.setCertificateStatus(CertificateStatus.NOT_VALID);
    } else if (verifierRepository.checkInBlackList(certificateIdentifier)) {
      certificateSimple.setCertificateStatus(CertificateStatus.NOT_VALID);
    } else {
      certificateSimple.setCertificateStatus(getCertificateStatus(digitalCovidCertificate));
    }

    certificateSimple.setPerson(new SimplePersonModel(digitalCovidCertificate.getNam().getFnt(),
        digitalCovidCertificate.getNam().getFn(), digitalCovidCertificate.getNam().getGnt(),
        digitalCovidCertificate.getNam().getGn()));

    certificateSimple.setDateOfBirth(digitalCovidCertificate.getDateOfBirth().asLocalDate());
    certificateSimple.setTimeStamp(LocalDateTime.now());

    return certificateSimple;
  }

  /**
   *
   * This method checks the given [DigitalCovidCertificate] and returns the proper status as
   * [CertificateStatus].
   *
   */
  private CertificateStatus getCertificateStatus(DigitalCovidCertificate cert) {

    if (!CollectionUtils.isEmpty(cert.getR())) {
      return checkRecoveryStatements(cert.getR());
    } else if (!CollectionUtils.isEmpty(cert.getT())) {
      return checkTests(cert.getT());
    } else if (!CollectionUtils.isEmpty(cert.getV())) {
      return checkVaccinations(cert.getV());
    }

    return CertificateStatus.NOT_VALID;
  }

  /**
   *
   * This method checks the given recovery statements passed as a [List] of [RecoveryModel] and
   * returns the proper status as [CertificateStatus].
   *
   */
  private CertificateStatus checkRecoveryStatements(List<RecoveryEntry> list) {
    try {

      RecoveryEntry lastRecovery = Iterables.getLast(list);

      LocalDate startDate = lastRecovery.getDf().plusDays(Long.valueOf(getRecoveryCertStartDay()));
      LocalDate endDate = lastRecovery.getDu();

      LOG.debug("dates start:{} end:{}", startDate, endDate);

      if (startDate.isAfter(LocalDate.now())) {
        return CertificateStatus.NOT_VALID_YET;
      } else if (LocalDate.now()
          .isAfter(startDate.plusDays(Long.valueOf(getRecoveryCertEndDay())))) {
        return CertificateStatus.NOT_VALID;
      } else if (LocalDate.now().isAfter(endDate)) {
        return CertificateStatus.PARTIALLY_VALID;
      } else {
        return CertificateStatus.VALID;
      }

    } catch (Exception e) {
      return CertificateStatus.NOT_VALID;
    }
  }

  /**
   *
   * This method checks the given vaccinations passed as a [List] of [VaccinationModel] and returns
   * the proper status as [CertificateStatus].
   *
   */
  private CertificateStatus checkVaccinations(List<VaccinationEntry> list) {

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
          return CertificateStatus.PARTIALLY_VALID;
        }

      } else if (lastVaccination.getDn() >= lastVaccination.getSd()) {

        LocalDate startDate = null;
        LocalDate endDate = null;

        // j&j booster
        if (lastVaccination.getMp().equals("EU/1/20/1525")
            && (lastVaccination.getDn() > lastVaccination.getSd())) {
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
        } else if (LocalDate.now().isAfter(endDate)) {
          return CertificateStatus.NOT_VALID;
        } else {
          return CertificateStatus.VALID;
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

  private String extractUVCI(DigitalCovidCertificate digitalCovidCertificate) {

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

  private String getRecoveryCertEndDay() {
    return preferences.getValidationRuleValueByName(ValidationRulesEnum.RECOVERY_CERT_END_DAY);
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


}
