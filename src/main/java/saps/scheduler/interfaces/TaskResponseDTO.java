package saps.scheduler.interfaces;

public interface TaskResponseDTO {

    String STATE_FAILED = null;
    String STATE_FINISHED = null;
    TaskSpecResponseDTO getTaskSpec();

}
