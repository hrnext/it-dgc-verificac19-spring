package it.dgc.verificac19.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import it.dgc.verificac19.data.VerifierRepository;

/**
 *
 * This class represents the [CoroutineWorker] of the SDK.
 *
 */
@Configuration
@EnableRetry
@EnableScheduling
public class LoadKeysWorker {

  private static final Logger LOG = LoggerFactory.getLogger(LoadKeysWorker.class);

  @Autowired
  VerifierRepository verifierRepository;

  /**
   *
   * This method represents the periodic asynchronously work that the Work Manager accomplishes each
   * 1 day on the background.
   *
   */
  @Retryable(maxAttempts = Integer.MAX_VALUE)
  @Scheduled(cron = "@daily")
  public void doWork() {
    LOG.info("key fetching start");
    boolean res = verifierRepository.syncData();
    LOG.info("key fetching result: {}", res);
    if (!res) {
      throw new RuntimeException("Error on sync data, retry");
    }
  }

}
