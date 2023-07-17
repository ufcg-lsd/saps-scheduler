package saps.scheduler.interfaces;

public enum ImageTaskState {
  CREATED("created"),
  DOWNLOADING("downloading"),
  DOWNLOADED("downloaded"),
  PREPROCESSING("preprocessing"),
  PREPROCESSED("preprocessed"),
  READY("ready"),
  RUNNING("running"),
  FINISHED("finished"),
  ARCHIVING("archiving"),
  ARCHIVED("archived"),
  FAILED("failed");
  private String value;

  
  private ImageTaskState(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

}
