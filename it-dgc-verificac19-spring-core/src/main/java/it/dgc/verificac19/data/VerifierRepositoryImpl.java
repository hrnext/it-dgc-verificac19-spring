package it.dgc.verificac19.data;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.dgc.verificac19.data.local.BlackListDao;
import it.dgc.verificac19.data.local.Blacklist;
import it.dgc.verificac19.data.local.Key;
import it.dgc.verificac19.data.local.KeyDao;
import it.dgc.verificac19.data.local.Preferences;
import it.dgc.verificac19.data.remote.ApiServiceClient;
import it.dgc.verificac19.data.remote.model.Rule;
import it.dgc.verificac19.model.ValidationRulesEnum;
import it.dgc.verificac19.utility.Utility;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 *
 * This class contains several methods to download public certificates (i.e. settings) and check the
 * download status. It implements the interface [VerifierRepository].
 *
 */
@Service
public class VerifierRepositoryImpl implements VerifierRepository {

  private static final Logger LOG = LoggerFactory.getLogger(VerifierRepositoryImpl.class);

  private final static String HEADER_KID = "x-kid";
  private final static String HEADER_RESUME_TOKEN = "x-resume-token";

  private List<String> validCertList;

  @Autowired
  ApiServiceClient apiServiceClient;

  @Autowired
  Preferences preferences;

  @Autowired
  BlackListDao blackListDao;

  @Autowired
  KeyDao keyDao;

  @PostConstruct
  public void init() {
    this.validCertList = new ArrayList<String>();
  }

  @Override
  public boolean syncData() {
    try {
      if (fetchValidationRules() == false || fetchCertificates() == false) {
        return false;
      }
    } catch (IOException | RuntimeException e) {
      LOG.error("Fetch validation rules / certificates error", e);
      return false;
    }

    preferences.setDateLastFetch(LocalDateTime.now());
    return true;
  }

  @Override
  public Certificate getCertificate(String kid) {
    Optional<Key> oKey = keyDao.findById(kid);

    if (oKey.isPresent()) {
      try {
        Key key = oKey.get();
        return Utility.base64ToX509Certificate(key.getKey());
      } catch (CertificateException e) {
        LOG.error("Certificate error", e);
        return null;
      }

    } else {
      return null;
    }

  }

  @Override
  public boolean checkInBlackList(String kid) {
    try {
      return blackListDao.findOneByBvalue(kid).isPresent();
    } catch (Exception e) {
      LOG.error("Error", e);
      return false;
    }
  }

  private boolean fetchCertificates() throws IOException {

    Call<List<String>> call = apiServiceClient.getApiCertificate().getCertStatus();

    Response<List<String>> response = call.execute();

    if (response.isSuccessful()) {

      List<String> body = response.body();

      validCertList.clear();
      validCertList.addAll(body);

      long recordCount = keyDao.count();

      if (body.isEmpty() || recordCount == 0L) {
        preferences.setResumeToken(-1L);
      }

      long resumeToken = preferences.getResumeToken();

      if (fetchCertificate(resumeToken) == false) {
        return false;
      } else {
        keyDao.deleteAllByKidNotIn(validCertList);
        return true;
      }

    } else {
      return false;
    }
  }

  private boolean fetchCertificate(long resumeToken) throws IOException {

    String tokenFormatted = (resumeToken == -1L) ? Strings.EMPTY : String.valueOf(resumeToken);

    Call<ResponseBody> call = apiServiceClient.getApiCertificate().getCertUpdate(tokenFormatted);

    Response<ResponseBody> response = call.execute();

    if (!response.isSuccessful()) {
      return false;
    }

    if (response.isSuccessful() && response.code() == HttpURLConnection.HTTP_OK) {
      Headers headers = response.headers();
      String responseKid = headers.get(HEADER_KID);
      String newResumeToken = headers.get(HEADER_RESUME_TOKEN);

      String responseString = response.body().string();

      if (validCertList.contains(responseKid)) {
        LOG.info("Cert KID verified {}", responseKid);
        Key key = new Key(responseKid, responseString);
        keyDao.saveAndFlush(key);
        preferences.setResumeToken(resumeToken);
        if (newResumeToken != null) {
          Long newToken = Long.valueOf(newResumeToken);
          fetchCertificate(newToken);
        }
      }
    }

    return true;
  }

  private boolean fetchValidationRules() throws IOException {

    Call<List<Rule>> call = apiServiceClient.getApiCertificate().getValidationRules();

    Response<List<Rule>> response = call.execute();

    if (response.isSuccessful()) {

      List<Rule> ValidationRules = response.body();
      preferences.setValidationRulesJson(ValidationRules);

      blackListDao.deleteAll();

      Optional<Rule> oBlacklistRule = ValidationRules.stream().filter(new Predicate<Rule>() {
        @Override
        public boolean test(Rule t) {
          return t.getName() != null && t.getName().equals(ValidationRulesEnum.BLACK_LIST_UVCI);
        }
      }).findFirst();

      if (oBlacklistRule.isPresent()) {

        List<Blacklist> listBlacklist = Arrays.stream(oBlacklistRule.get().getValue().split(";"))
            .map(Blacklist::new).collect(Collectors.toList());

        blackListDao.saveAllAndFlush(listBlacklist);
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * @return the validCertList
   */
  public List<String> getValidCertList() {
    return validCertList;
  }

  /**
   * @param validCertList the validCertList to set
   */
  public void setValidCertList(List<String> validCertList) {
    this.validCertList = validCertList;
  }

}
