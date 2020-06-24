package saps.scheduler.core.arrebol.retry;

import java.util.List;

import saps.common.core.dto.JobResponseDTO;
import saps.scheduler.core.arrebol.Arrebol;
import saps.scheduler.core.arrebol.exceptions.GetJobException;

public class GetJobByNameRetry implements ArrebolRetry<List<JobResponseDTO>> {

	private Arrebol arrebol;
	private String jobName;

	public GetJobByNameRetry(Arrebol arrebol, String jobName) {
		this.arrebol = arrebol;
		this.jobName = jobName;
	}

	@Override
	public List<JobResponseDTO> run() throws GetJobException {
		return arrebol.checkStatusJobByName(jobName);
	}

}
