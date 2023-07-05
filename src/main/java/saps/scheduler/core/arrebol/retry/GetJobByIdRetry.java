/* (C)2020 */
package saps.scheduler.core.arrebol.retry;

import saps.common.core.dto.JobResponseDTO;
import saps.scheduler.core.arrebol.Arrebol;
import saps.scheduler.core.arrebol.exceptions.GetJobException;

public class GetJobByIdRetry implements ArrebolRetry<JobResponseDTO> {

  private Arrebol arrebol;
  private String jobId;

  public GetJobByIdRetry(Arrebol arrebol, String jobId) {
    this.arrebol = arrebol;
    this.jobId = jobId;
  }

  @Override
  public JobResponseDTO run() throws GetJobException {
    return arrebol.checkStatusJobById(jobId);
  }
}
