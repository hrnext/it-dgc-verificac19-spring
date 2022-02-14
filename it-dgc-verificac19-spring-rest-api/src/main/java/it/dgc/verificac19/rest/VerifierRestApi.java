package it.dgc.verificac19.rest;

import java.io.IOException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import it.dgc.verificac19.data.local.Preferences;
import it.dgc.verificac19.model.CertificateSimple;
import it.dgc.verificac19.model.ValidationScanMode;
import it.dgc.verificac19.models.ApiResponse;
import it.dgc.verificac19.service.VerifierService;

@RestController
@RequestMapping(value = "api")
public class VerifierRestApi {

  private static final Logger logger = LoggerFactory.getLogger(VerifierRestApi.class);

  @Autowired
  VerifierService verifierService;

  @Autowired
  Preferences preferences;

  @GetMapping(value = "/verify-string")
  public ResponseEntity<ApiResponse> verifyByString(
      @RequestParam(name = "qrCodeTxt", required = true) String qrCodeTxt,
      @RequestParam(name = "scanMode", required = false) ValidationScanMode inputScanMode) {

    if (preferences.getDateLastFetch() == null
        || !preferences.getDateLastFetch().toLocalDate().isEqual(LocalDate.now())) {
      logger.info("DRL not updated yet");
      return new ResponseEntity<>(ApiResponse.buildUpdatingResponse(), HttpStatus.OK);
    } else {
      final ValidationScanMode actualScanMode =
          inputScanMode == null ? ValidationScanMode.NORMAL_DGP : inputScanMode;

      CertificateSimple validatedCertificate = verifierService.verify(qrCodeTxt, actualScanMode);
      VerifierRestApi.logResponse(validatedCertificate);
      return new ResponseEntity<>(ApiResponse.buildOkResponse(validatedCertificate), HttpStatus.OK);
    }
  }

  @PostMapping(value = "/verify-image")
  public ResponseEntity<ApiResponse> verifyByImage(
      @RequestPart(name = "file", required = true) MultipartFile file,
      @RequestParam(name = "scanMode", required = true) ValidationScanMode inputScanMode) {
    if (preferences.getDateLastFetch() == null
        || !preferences.getDateLastFetch().toLocalDate().isEqual(LocalDate.now())) {
      logger.info("DRL not updated yet");
      return ResponseEntity.ok().build();
    } else {
      final ValidationScanMode actualScanMode =
          inputScanMode == null ? ValidationScanMode.NORMAL_DGP : inputScanMode;

      CertificateSimple validatedCertificate;
      try {
        validatedCertificate = verifierService.verify(file.getBytes(), actualScanMode);
      } catch (IOException e) {
        return new ResponseEntity<>(ApiResponse.buildDecodeError(), HttpStatus.BAD_REQUEST);

      }
      VerifierRestApi.logResponse(validatedCertificate);
      return new ResponseEntity<>(ApiResponse.buildOkResponse(validatedCertificate), HttpStatus.OK);
    }
  }

  private static void logResponse(CertificateSimple certificateSimple) {
    switch (certificateSimple.getCertificateStatus()) {
      case NOT_EU_DCC:
      case NOT_VALID:
      case NOT_VALID_YET:
      case TEST_NEEDED:
        logger.info("Invalid");
        break;
      case VALID:
        logger.info("Valid");
        break;
      default:
        break;
    }
  }
}
