package saps.scheduler.interfaces;

import java.sql.Timestamp;
import java.util.List;

public class SapsImage {

    public static String NONE_ARREBOL_JOB_ID = "x";
    public static String AVAILABLE = "y";
    public static String NON_EXISTENT_DATA = "z";
    private ImageTaskState state = ImageTaskState.CREATED;
    private String status = "?";
    private String error = "!";

    public String getArrebolJobId() {
        return "x";
    }

    public ImageTaskState getState() {
        return this.state;
    }

    public String getTaskId() {
        return "id";
    }

    public String getUser() {
        return "user";
    }

    public int getPriority() {
        return 0;
    }

    public Timestamp getCreationTime() {
        return null;
    }

    public void setState(ImageTaskState state) {
        this.state = state;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setArrebolJobId(String arrebolJobId) {}

    public String getProcessingTag() {
        return "tag";
    }

    public String getPreprocessingTag() {
        return "tag";
    }

    public String getInputdownloadingTag() {
        return "tag";
    }

    public String getDigestProcessing() {
        return "tag";
    }

    public String getDigestInputdownloading() {
        return "tag";
    }

    public String getDigestPreprocessing() {
        return "tag";
    }

}
