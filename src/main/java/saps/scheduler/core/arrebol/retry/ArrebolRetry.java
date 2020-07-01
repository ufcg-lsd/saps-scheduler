package saps.scheduler.core.arrebol.retry;

import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;
import saps.scheduler.core.arrebol.exceptions.GetJobException;
import saps.scheduler.core.arrebol.exceptions.SubmitJobException;

public interface ArrebolRetry<T> {

	public T run() throws Exception, SubmitJobException, GetJobException, GetCountsSlotsException;

}
