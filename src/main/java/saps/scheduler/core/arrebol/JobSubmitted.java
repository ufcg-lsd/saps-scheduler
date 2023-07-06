/* (C)2020 */
package saps.scheduler.core.arrebol;

import saps.scheduler.interfaces.SapsImage;

import java.util.Objects;

public class JobSubmitted {

  private String jobId;
  private SapsImage imageTask;

  public JobSubmitted(String jobId, SapsImage imageTask) {
    this.jobId = jobId;
    this.imageTask = imageTask;
  }

  public String getJobId() {
    return jobId;
  }

  public SapsImage getImageTask() {
    return imageTask;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JobSubmitted otherJob = (JobSubmitted) o;

    return this.getJobId().equals(otherJob.getJobId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId);
  }

  @Override
  public String toString() {
    return jobId;
  }
}
