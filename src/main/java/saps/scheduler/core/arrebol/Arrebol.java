/* (C)2020 */
package saps.scheduler.core.arrebol;

import java.util.List;
import saps.scheduler.interfaces.JobResponseDTO;
import saps.scheduler.interfaces.SapsImage;
import saps.scheduler.interfaces.SapsJob;
import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;
import saps.scheduler.core.arrebol.exceptions.GetJobException;
import saps.scheduler.core.arrebol.exceptions.SubmitJobException;

public interface Arrebol {

  public String addJob(SapsJob job) throws Exception, SubmitJobException;

  public void removeJob(JobSubmitted job);

  public void addJobInList(JobSubmitted newJob);

  public void populateJobList(List<SapsImage> taskList);

  public List<JobSubmitted> returnAllJobsSubmitted();

  public JobResponseDTO checkStatusJobById(String jobId) throws GetJobException;

  public List<JobResponseDTO> checkStatusJobByName(String JobName) throws GetJobException;

  public String checkStatusJobString(String jobId) throws GetJobException;

  public int getCountSlotsInQueue(String queueId) throws GetCountsSlotsException;
}
