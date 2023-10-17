/* (C)2020 */
package saps.scheduler.core.arrebol.exceptions;

public class SubmitJobException extends Throwable {

  private static final long serialVersionUID = 1L;

  public SubmitJobException(String s, Exception e) {
    super(s, e);
  }
}
