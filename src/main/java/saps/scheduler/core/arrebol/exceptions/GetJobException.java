package saps.scheduler.core.arrebol.exceptions;

public class GetJobException extends Throwable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public GetJobException(String s, Exception e) {
        super(s, e);
    }
}