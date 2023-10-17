/* (C)2020 */
package saps.scheduler.core.selector;

import java.util.List;
import java.util.Map;
import saps.common.core.model.SapsImage;

public interface Selector {

  /**
   * This function select tasks for schedule up to count.
   *
   * @param count slots number in Arrebol queue
   * @param tasks user map by tasks
   * @return list of selected tasks
   */
  public List<SapsImage> select(int count, Map<String, List<SapsImage>> tasks);

  /**
   * This function returns selector version information
   *
   * @return selector version
   */
  public String version();
}
