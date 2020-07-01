package saps.scheduler.core.arrebol;

import java.util.List;
import java.util.LinkedList;
import java.util.Properties;

import saps.common.core.dto.JobResponseDTO;
import saps.common.core.model.SapsImage;
import saps.common.core.model.SapsJob;
import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;
import saps.scheduler.core.arrebol.exceptions.GetJobException;
import saps.scheduler.core.arrebol.exceptions.SubmitJobException;

public class DefaultArrebol implements Arrebol {

	private static final int ARREBOL_MAX_WAITING_JOBS = 20;
	
	private final ArrebolRequestsHelper arrebolRequestHelper;
	// private final Properties properties;
	private List<JobSubmitted> submittedJobID;

	public DefaultArrebol(Properties properties) {
		// this.properties = properties;
		this.arrebolRequestHelper = new ArrebolRequestsHelper(properties);
		this.submittedJobID = new LinkedList<JobSubmitted>();
	}

	@Override
	public String addJob(SapsJob job) throws Exception, SubmitJobException {
		return arrebolRequestHelper.submitJobToExecution(job);
	}

	@Override
	public void removeJob(JobSubmitted job) {
		submittedJobID.remove(job);
	}

	@Override
	public void addJobInList(JobSubmitted newJob) {
		submittedJobID.add(newJob);
	}

	@Override
	public void populateJobList(List<SapsImage> taskList) {
		for (SapsImage task : taskList)
			submittedJobID.add(new JobSubmitted(task.getArrebolJobId(), task));
	}

	@Override
	public List<JobSubmitted> returnAllJobsSubmitted() {
		return submittedJobID;
	}

	@Override
	public JobResponseDTO checkStatusJobById(String jobId) throws GetJobException {
		return arrebolRequestHelper.getJob(jobId);
	}

	// TODO implement method
	public List<JobResponseDTO> checkStatusJobByName(String JobName) throws GetJobException {
		return null;
		// return arrebolRequestHelper.getJobByName(jobName);
	}

	@Override
	public String checkStatusJobString(String jobId) throws GetJobException {
		return arrebolRequestHelper.getJobJSON(jobId);
	}

	@Override
	public int getCountSlotsInQueue(String queueId) throws GetCountsSlotsException {
		return ARREBOL_MAX_WAITING_JOBS - arrebolRequestHelper.getCountSlotsInQueue(queueId);
	}
}
