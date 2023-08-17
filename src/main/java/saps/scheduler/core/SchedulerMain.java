/* (C)2020 */
package saps.scheduler.core;

import java.io.FileInputStream;
import java.util.Objects;
import java.util.Properties;
import org.apache.log4j.Logger;

public class SchedulerMain {

  private static final Logger LOGGER = Logger.getLogger(SchedulerMain.class);

  public static void main(String[] args) throws Exception {
    String confPath = args[0];
    String executionTagsFilePath = args[1];

    if (Objects.isNull(confPath) || confPath.isEmpty()) {
      throw new IllegalArgumentException(
          "The path to the configuration file cannot be null or empty");
    }

    if (Objects.isNull(executionTagsFilePath) || executionTagsFilePath.isEmpty()) {
      throw new IllegalArgumentException(
          "The path to the execution tags file cannot be null or empty");
    }

    System.setProperty(DefaultScheduler.EXECUTION_TAGS_FILE_PATH_KEY, executionTagsFilePath);

    LOGGER.info("Loading properties...");
    final Properties properties = new Properties();
    FileInputStream input = new FileInputStream(args[0]);
    properties.load(input);

    LOGGER.info("Trying to start Saps Controller");
    DefaultScheduler sapsController = new DefaultScheduler(properties);


    LOGGER.info("Saps Controller starting.");


	while (true) {
    sapsController.recovery();
    Thread.sleep(5000);
    
    sapsController.schedule();
    Thread.sleep(5000); 
    
    sapsController.checker();
    Thread.sleep(5000); 

	}
    
  }
}
