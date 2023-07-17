package saps.scheduler.interfaces;

import java.util.List;

public interface CatalogUtils {

    static boolean updateState(Catalog catalog, SapsImage task) {
        return true;
    }

    static void addTimestampTask(Catalog catalog, SapsImage task) {
    }

    static List<SapsImage> getTasks(Catalog catalog, ImageTaskState state) {
        return null;
    }

    static List<SapsImage> getProcessingTasks(Catalog catalog, String string) {
        return null;
    }

}
