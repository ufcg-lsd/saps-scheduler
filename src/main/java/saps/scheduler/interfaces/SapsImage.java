package saps.scheduler.interfaces;

import java.sql.Timestamp;
import java.util.List;

public class SapsImage {

    public static String NONE_ARREBOL_JOB_ID = null;
    public static String AVAILABLE = null;
    public static String NON_EXISTENT_DATA = null;

    public String getArrebolJobId() {
        return "";
    }

    public ImageTaskState getState() {
        return ImageTaskState.CREATED;
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

    public void setState(ImageTaskState state) {}

    public void setStatus(String status) {}

    public void setError(String error) {}

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
