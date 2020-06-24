package saps.scheduler.core.arrebol.retry;

import saps.scheduler.core.arrebol.Arrebol;
import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;

public class LenQueueRetry implements ArrebolRetry<Integer> {

	private Arrebol arrebol;
	private String queueId;

	public LenQueueRetry(Arrebol arrebol, String queueId) {
		this.arrebol = arrebol;
		this.queueId = queueId;
	}

	@Override
	public Integer run() throws GetCountsSlotsException {
		return arrebol.getCountSlotsInQueue(queueId);
	}

}
