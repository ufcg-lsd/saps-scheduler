/* (C)2020 */
package saps.scheduler.core.arrebol;

import java.util.List;
import org.apache.log4j.Logger;
import saps.common.core.dto.JobResponseDTO;
import saps.common.core.model.SapsJob;
import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;
import saps.scheduler.core.arrebol.exceptions.GetJobException;
import saps.scheduler.core.arrebol.exceptions.SubmitJobException;
import saps.scheduler.core.arrebol.retry.ArrebolRetry;
import saps.scheduler.core.arrebol.retry.GetJobByIdRetry;
import saps.scheduler.core.arrebol.retry.GetJobByNameRetry;
import saps.scheduler.core.arrebol.retry.LenQueueRetry;
import saps.scheduler.core.arrebol.retry.SubmitJobRetry;

public class ArrebolUtils {

  public static final Logger LOGGER = Logger.getLogger(ArrebolUtils.class);
  private static final int ARREBOL_DEFAULT_SLEEP_SECONDS = 5;

  /**
   * This function tries countless times to successfully execute the passed function.
   *
   * @param <T> Return type
   * @param function Function passed for execute
   * @param sleepInSeconds Time sleep in seconds (case fail)
   * @param message Information message about function passed
   * @return Function return
   */
  @SuppressWarnings("unchecked")
  private static <T> T retry(ArrebolRetry<?> function, int sleepInSeconds, String message) {
    LOGGER.info(
        "[Retry Arrebol function] Trying "
            + message
            + " using "
            + sleepInSeconds
            + " seconds with time sleep");

    while (true) {
      try {
        return (T) function.run();
      } catch (Exception | SubmitJobException | GetJobException | GetCountsSlotsException e) {
        LOGGER.error("Failed while " + message);
        e.printStackTrace();
      }

      try {
        LOGGER.info("Sleeping for " + sleepInSeconds + " seconds");
        Thread.sleep(Long.valueOf(sleepInSeconds) * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * This function gets Arrebol capacity for add new jobs.
   *
   * @param arrebol Arrebol service
   * @param queueId queue identifier in Arrebol
   * @return Arrebol queue capacity in queue with identifier
   */
  public static int getCountSlots(Arrebol arrebol, String queueId) {
    return retry(
        new LenQueueRetry(arrebol, queueId),
        ARREBOL_DEFAULT_SLEEP_SECONDS,
        "gets Arrebol capacity len for add news jobs in queue id [" + queueId + "]");
  }

  /**
   * This function gets job list in Arrebol that matching with name.
   *
   * @param Arrebol service
   * @param jobName job label to be used for matching
   * @param message information message
   * @return job response list that matching with label
   */
  public static List<JobResponseDTO> getJobByName(Arrebol arrebol, String jobName, String message) {
    return retry(new GetJobByNameRetry(arrebol, jobName), ARREBOL_DEFAULT_SLEEP_SECONDS, message);
  }

  /**
   * This function gets job in Arrebol that matching with id.
   *
   * @param Arrebol service
   * @param jobId job id to be used for matching
   * @param message information message
   * @return job response that matching with id
   */
  public static JobResponseDTO getJobById(Arrebol arrebol, String jobId, String message) {
    return retry(new GetJobByIdRetry(arrebol, jobId), ARREBOL_DEFAULT_SLEEP_SECONDS, message);
  }

  /**
   * This function submit job in Arrebol service.
   *
   * @param Arrebol service
   * @param imageJob SAPS job to be submitted
   * @param message information message
   * @return job id returned from Arrebol
   */
  public static String submitJob(Arrebol arrebol, SapsJob imageJob, String message) {
    return retry(new SubmitJobRetry(arrebol, imageJob), ARREBOL_DEFAULT_SLEEP_SECONDS, message);
  }
}
