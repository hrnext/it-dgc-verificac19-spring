package it.dgc.verificac19.data;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import it.dgc.verificac19.data.local.Drl;
import it.dgc.verificac19.data.local.DrlDao;
import it.dgc.verificac19.data.local.Key;
import it.dgc.verificac19.data.local.KeyDao;
import it.dgc.verificac19.data.local.Preferences;
import it.dgc.verificac19.data.local.RevokedPass;
import it.dgc.verificac19.data.local.RevokedPassDao;
import it.dgc.verificac19.data.remote.ApiServiceClient;
import it.dgc.verificac19.data.remote.model.CertificateRevocationList;
import it.dgc.verificac19.data.remote.model.CrlStatus;
import it.dgc.verificac19.data.remote.model.Rule;
import it.dgc.verificac19.model.ValidationRulesEnum;
import it.dgc.verificac19.utility.Utility;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.HttpException;
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

  @Autowired
  DrlDao drlDao;

  @Autowired
  RevokedPassDao revokedPassDao;

  private int revokedPassesSize;

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

    if (preferences.isDrlSyncActive()) {
      getCRLStatus();
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
        return Utility.base64ToX509Certificate(key.getValue());
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

  @Override
  public boolean checkInRevokedList(String hashedUVCI) {
    try {
      return revokedPassDao.findOneByHashedUVCI(hashedUVCI).isPresent();
    } catch (Exception e) {
      LOG.error("checkInRevokedList exception: ", e);
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

      Optional<Rule> oDrlSynctActiveRule = ValidationRules.stream().filter(new Predicate<Rule>() {
        @Override
        public boolean test(Rule t) {
          return t.getName() != null && t.getName().equals(ValidationRulesEnum.DRL_SYNC_ACTIVE);
        }
      }).findFirst();

      if (oDrlSynctActiveRule.isPresent()) {
        String value = oBlacklistRule.get().getValue();
        if (value != null && value == "true") {
          preferences.setDrlSyncActive(true);
        }
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

  private boolean outDatedVersion(CrlStatus crlStatus) {
    return (crlStatus.getVersion().intValue() != preferences.getCurrentVersion().intValue());
  }

  private long getCurrentVersionDrl() {
    Optional<Drl> drl = drlDao.findFirstByOrderById();
    if (drl.isPresent()) {
      return drl.get().getVersion();
    } else {
      return 0;
    }
  }

  private void saveLastFetchDate() {
    preferences.setDrlDateLastFetch(LocalDateTime.now());
  }

  private boolean isDownloadCompleted() {
    return preferences.getTotalNumberUCVI() == revokedPassesSize;
  }

  private void manageFinalReconciliation() {
    saveLastFetchDate();
    checkCurrentDownloadSize();
    if (!isDownloadCompleted()) {
      LOG.info("Reconciliation", "final reconciliation failed!");
      handleErrorState();
    } else {
      LOG.info("Reconciliation", "final reconciliation completed!");
    }
  }

  private void handleErrorState() {
    clearDBAndPrefs();
    this.syncData();
  }

  private void clearDBAndPrefs() {
    try {
      preferences.clearDrlPrefs();
      deleteAllFromDB();
    } catch (Exception e) {
      LOG.error("clearDBAndPrefs exception: ", e);
    }
  }

  private void deleteAllFromDB() {
    try {
      revokedPassDao.deleteAll();
      drlDao.deleteAll();
    } catch (Exception e) {
      LOG.error("deleteAllFromRealm", e);
    }
  }

  private void checkCurrentDownloadSize() {
    long revokedPassesCount = revokedPassDao.count();
    revokedPassesSize = (int) revokedPassesCount;
  }

  private void getCRLStatus() {
    try {

      long version = getCurrentVersionDrl();
      preferences.setCurrentVersion(version);
      LOG.info("Version DRL: {} ", version);

      Call<CrlStatus> call =
          apiServiceClient.getApiCertificate().getCRLStatus(preferences.getCurrentVersion());
      Response<CrlStatus> response = call.execute();
      if (response.isSuccessful()) {
        CrlStatus crlStatus = response.body();
        if (crlStatus != null) {
          saveCrlStatusInfo(crlStatus);

          if (outDatedVersion(crlStatus)) {
            downloadChunks(crlStatus);
          } else {
            manageFinalReconciliation();
          }
        }

      } else {
        throw new HttpException(response);
      }

    } catch (HttpException e) {
      if (e.code() >= 400 && e.code() <= 407) {
        clearDBAndPrefs();
        throw e;

      }
    } catch (IOException e) {
      LOG.error("Error: ", e);
    }
  }

  private void downloadChunks(CrlStatus crlStatus) {
    if (crlStatus != null) {

      while (noMoreChunks(crlStatus)) {
        try {
          LOG.info("downloadChunks: {}", preferences.getCurrentChunk() + 1);
          Call<CertificateRevocationList> call = apiServiceClient.getApiCertificate()
              .getRevokeList(preferences.getCurrentVersion(), preferences.getCurrentChunk() + 1);

          Response<CertificateRevocationList> response = call.execute();

          if (response.isSuccessful()) {
            getRevokeList(crlStatus.getVersion(), response.body());
          } else {
            throw new HttpException(response);
          }
        } catch (HttpException e) {
          if (e.code() >= 400 && e.code() <= 407) {
            LOG.error(e.toString(), e.message());
            clearDBAndPrefs();
            throw e;
          } else {
            LOG.error("downloadChunks: ", e.message());
            break;
          }
        } catch (Exception e) {
          LOG.error("downloadChunks", e);
          break;
        }
      }

      if (isDownloadComplete(crlStatus)) {

        preferences.setCurrentVersion(preferences.getRequestedVersion());
        preferences.setCurrentChunk(0);
        preferences.setTotalChunk(0);

        saveDrlStatusInDB(crlStatus);
        getCRLStatus();

        LOG.info("DownloadChunks: Last chunk processed, versions updated.");
      }
    }
  }

  private void saveDrlStatusInDB(CrlStatus crlStatus) {
    // delete old drl
    drlDao.deleteAll();
    // persist new drl
    Drl drl = new Drl(crlStatus.getId(), crlStatus.getVersion());
    drlDao.save(drl);
  }

  private boolean isDownloadComplete(CrlStatus crlStatus) {
    return preferences.getCurrentChunk() == crlStatus.getTotalChunk();
  }

  private void getRevokeList(Long version, CertificateRevocationList certificateRevocationList) {
    if (version.intValue() == certificateRevocationList.getVersion().intValue()) {
      preferences.setCurrentChunk(preferences.getCurrentChunk() + 1);
      boolean isFirstChunk = preferences.getCurrentChunk() == 1;
      if (isFirstChunk && certificateRevocationList.getDelta() == null) {
        deleteAllFromDB();
      }
      persistRevokes(certificateRevocationList);
    } else {
      clearDBAndPrefs();
      this.syncData();
    }

  }

  private void persistRevokes(CertificateRevocationList certificateRevocationList) {

    try {
      List<String> revokedUcviList = certificateRevocationList.getRevokedUcvi();

      if (revokedUcviList != null) {
        LOG.info("persistRevokes: adding UCVI list.");
        insertListToDB(revokedUcviList);
      } else if (certificateRevocationList.getDelta() != null) {
        LOG.info("persistRevokes: manage delta insertions and deletions.");
        List<String> deltaInsertList = certificateRevocationList.getDelta().getInsertions();
        List<String> deltaDeleteList = certificateRevocationList.getDelta().getDeletions();

        if (deltaInsertList != null) {
          LOG.info("DeltaInsertions size: " + deltaInsertList.size());
          insertListToDB(deltaInsertList);
        }
        if (deltaDeleteList != null) {
          LOG.info("DeltaDeletion size: " + deltaDeleteList.size());
          deleteListFromDB(deltaDeleteList);
        }
      }
    } catch (Exception e) {
      LOG.error("persistRevokes exception: ", e);
    }

  }

  private void saveCrlStatusInfo(CrlStatus crlStatus) {
    preferences.setTotalChunk(crlStatus.getTotalChunk());
    preferences.setRequestedVersion(crlStatus.getVersion());
    preferences
        .setCurrentVersion(crlStatus.getFromVersion() != null ? crlStatus.getFromVersion() : 0);
    preferences.setChunk(crlStatus.getChunk());
    preferences.setTotalNumberUCVI(crlStatus.getTotalNumberUCVI());
  }

  private boolean noMoreChunks(CrlStatus status) {

    return preferences.getCurrentChunk() < status.getTotalChunk().intValue();
  }

  private void insertListToDB(List<String> deltaInsertList) {
    try {

      Iterator<String> it = deltaInsertList.iterator();
      List<RevokedPass> revokedPasses = new ArrayList<RevokedPass>();
      while (it.hasNext()) {
        String hashedUVCI = it.next();
        revokedPasses.add(new RevokedPass(hashedUVCI));
      }

      revokedPassDao.saveAll(revokedPasses);
      LOG.info("Revoke inserted count: " + deltaInsertList.size());
    } catch (Exception e) {
      LOG.error("insertListToDB exception", e);
    }
  }

  private void deleteListFromDB(List<String> deltaDeleteList) {

    try {
      List<String> subList = null;
      int num = (int) Math.ceil((deltaDeleteList.size() / 1000d));
      for (int i = 0; i < num; i++) {
        subList = new ArrayList<String>(
            deltaDeleteList.subList(i * 1000, Math.min((i + 1) * 1000, deltaDeleteList.size())));
        revokedPassDao.deleteAllByHashedUVCIIn(subList);
      }
      LOG.info("Revoke deleted count: " + deltaDeleteList.size());

    } catch (Exception e) {
      LOG.info("deleteListFromDB exception", e);
    }

  }

}
