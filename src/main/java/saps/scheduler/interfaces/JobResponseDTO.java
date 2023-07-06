package saps.scheduler.interfaces;

public interface JobResponseDTO {

    String getId();

    String getJobState();

    TaskResponseDTO[] getTasks();

}
