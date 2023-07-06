package saps.scheduler.interfaces;

import java.util.List;
import java.util.Map;

public class SapsTask {

    public SapsTask(String string, Map<String, String> requirements, List<String> commands,
            Map<String, String> metadata) {
    }

    public static List<String> buildCommandList(SapsImage task, String repository) {
        return null;
    }

    public Object toJSON() {
        return null;
    }

}
