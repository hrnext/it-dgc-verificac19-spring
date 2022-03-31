package it.dgc.verificac19.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
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

  private static final String cronExpression = "0 0 1,13 * * *";
  private static final String timeZone = "CET";

  /**
   *
   * This method represents the periodic asynchronously work that the Work Manager accomplishes each
   * 1 day on the background.
   *
   */
  @Retryable(maxAttempts = Integer.MAX_VALUE)
  @Scheduled(cron = cronExpression, zone = timeZone)
  public void doWork() {
    LOG.info("::: SDK Updates in progress - START :::");
    boolean res = verifierRepository.syncData();
    LOG.info("::: SDK Updates in progress, result: {} - STOP :::", res);
    if (!res) {
      throw new RuntimeException("Error on sync data, retry");
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onSchedule() {
    doWork();
  }

}
