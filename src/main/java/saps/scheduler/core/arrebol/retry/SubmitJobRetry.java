package saps.scheduler.core.arrebol.retry;

import saps.common.core.model.SapsJob;
import saps.scheduler.core.arrebol.Arrebol;
import saps.scheduler.core.arrebol.exceptions.SubmitJobException;

public class SubmitJobRetry implements ArrebolRetry<String>{

	private Arrebol arrebol;
	private SapsJob job;
	
	public SubmitJobRetry(Arrebol arrebol, SapsJob job) {
		this.arrebol = arrebol;
		this.job = job;
	}
	
	@Override
	public String run() throws Exception, SubmitJobException {
		return arrebol.addJob(job);
	}

}
