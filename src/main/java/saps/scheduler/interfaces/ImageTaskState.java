package saps.scheduler.interfaces;

public interface ImageTaskState {

    ImageTaskState READY = null;
    ImageTaskState DOWNLOADED = null;
    ImageTaskState CREATED = null;
    ImageTaskState FAILED = null;
    ImageTaskState DOWNLOADING = null;
    ImageTaskState PREPROCESSING = null;
    ImageTaskState RUNNING = null;
    ImageTaskState FINISHED = null;
    String getValue();

}
