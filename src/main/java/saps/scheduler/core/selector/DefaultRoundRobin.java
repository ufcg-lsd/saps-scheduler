/* (C)2020 */
package saps.scheduler.core.selector;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import saps.scheduler.interfaces.SapsImage;

public class DefaultRoundRobin implements Selector {

  @Override
  public List<SapsImage> select(int count, Map<String, List<SapsImage>> tasks) {
    List<SapsImage> selectedTasks = new LinkedList<SapsImage>();
    List<String> usersForDelete = new LinkedList<String>();

    while (count > 0 && tasks.size() > 0) {
      for (Map.Entry<String, List<SapsImage>> entry : tasks.entrySet()) {
        if (count > 0) {
          selectedTasks.add(entry.getValue().remove(0));
          count--;
        }
        if (entry.getValue().size() == 0) usersForDelete.add(entry.getKey());
      }

      for (String user : usersForDelete) tasks.remove(user);
    }

    return selectedTasks;
  }

  @Override
  public String version() {
    return "Default Round Robin";
  }
}
