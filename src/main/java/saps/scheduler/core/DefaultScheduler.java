package saps.scheduler.core;

import java.util.*;
import org.apache.log4j.Logger;

import saps.catalog.core.Catalog;
import saps.catalog.core.jdbc.JDBCCatalog;
import saps.catalog.core.retry.CatalogUtils;
import saps.common.core.dto.*;
import saps.common.core.model.SapsImage;
import saps.common.core.model.SapsJob;
import saps.common.core.model.SapsTask;
import saps.common.core.model.enums.ImageTaskState;
import saps.common.utils.ExecutionScriptTag;
import saps.common.utils.ExecutionScriptTagUtil;
import saps.common.utils.SapsPropertiesConstants;
import saps.common.utils.SapsPropertiesUtil;
import saps.scheduler.core.arrebol.Arrebol;
import saps.scheduler.core.arrebol.ArrebolUtils;
import saps.scheduler.core.arrebol.DefaultArrebol;
import saps.scheduler.core.arrebol.JobSubmitted;
import saps.scheduler.core.selector.DefaultRoundRobin;
import saps.scheduler.core.selector.Selector;
import saps.scheduler.interfaces.Scheduler;



public class DefaultScheduler implements Scheduler {

  public static final Logger LOGGER = Logger.getLogger(DefaultScheduler.class);
  public static final String EXECUTION_TAGS_FILE_PATH_KEY = "EXECUTION_SCRIPT_TAGS_FILE_PATH";
  public static final String REQUIREMENTS_CPU_REQUEST = "CPUUsage";
  public static final String REQUIREMENTS_RAM_REQUEST = "RAMUsage";

  private Selector selector;

  private Catalog catalog;
  private Arrebol arrebol;

  public DefaultScheduler(Properties properties) throws IllegalArgumentException {
    this(
        properties,
        new JDBCCatalog(properties),
        new DefaultArrebol(properties),
        new DefaultRoundRobin());
  }

  public DefaultScheduler(Properties properties, Catalog catalog,
    Arrebol arrebol, Selector selector) throws IllegalArgumentException {
    
    if (!checkProperties(properties))
      throw new IllegalArgumentException("Error on validate the file. Missing properties for start Scheduler Component.");

    this.catalog = catalog;
    this.arrebol = arrebol;
    this.selector = selector;
  }

  private static boolean checkProperties(Properties properties) {
    String[] propertiesSet = {
      SapsPropertiesConstants.IMAGE_DATASTORE_IP,
      SapsPropertiesConstants.IMAGE_DATASTORE_PORT,
      SapsPropertiesConstants.ARREBOL_BASE_URL
    };

    return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
  }

   
  public void recovery() {
    List<SapsImage> tasksInProcessingState = getProcessingTasksInCatalog();
    List<SapsImage> tasksForPopulateSubmittedJobList = new ArrayList<>();

      for (SapsImage task : tasksInProcessingState) {
        if (task.getArrebolJobId().equals(SapsImage.NONE_ARREBOL_JOB_ID)) {
        
          String jobName = task.getState().getValue() + "-" + task.getTaskId();
          LOGGER.info("--- Starting getJobByName In Arrebol ---");
          List<JobResponseDTO> jobsWithEqualJobName = getJobByNameInArrebol(jobName, "gets job by name [" + jobName + "]");
          LOGGER.info("Returned list: " + jobsWithEqualJobName);

          if (jobsWithEqualJobName.isEmpty()) {
            rollBackTaskState(task);
          }

        else {
          String arrebolJobId = jobsWithEqualJobName.get(0).getId();
          updateStateInCatalog(
              task,
              task.getState(),
              SapsImage.AVAILABLE,
              SapsImage.NON_EXISTENT_DATA,
              arrebolJobId,
              "updates task [" + task.getTaskId() + "] with Arrebol job ID [" + arrebolJobId + "]");
          tasksForPopulateSubmittedJobList.add(task);
        } 
      }
      else {
        String arrebolJobId = task.getArrebolJobId();
        arrebol.addJobInList(new JobSubmitted(arrebolJobId, task));
      }
    };
    arrebol.populateJobList(tasksForPopulateSubmittedJobList);
  }

/**
   * This function apply rollback in task state and updates in Catalog
   *
   * @param task task to be apply rollback
   */
  private void rollBackTaskState(SapsImage task) {
    ImageTaskState previousState = getPreviousState(task.getState());
    updateStateInCatalog(
        task,
        previousState,
        SapsImage.AVAILABLE,
        SapsImage.NON_EXISTENT_DATA,
        SapsImage.NONE_ARREBOL_JOB_ID,
        "updates task ["
            + task.getTaskId()
            + "] with previus state ["
            + previousState.getValue()
            + "]");
  }

   public void schedule() {
    List<SapsImage> selectedTasks = selectTasks();
    submitTasks(selectedTasks);
  }

  protected List<SapsImage> selectTasks() {
    List<SapsImage> selectedTasks = new LinkedList<SapsImage>();
    ImageTaskState[] states = {
      ImageTaskState.READY, ImageTaskState.DOWNLOADED, ImageTaskState.CREATED
    };

    int countUpToTasks = getCountSlotsInArrebol("default");

    for (ImageTaskState state : states) {
      List<SapsImage> selectedTasksInCurrentState = selectTasks(countUpToTasks, state);
      selectedTasks.addAll(selectedTasksInCurrentState);
      countUpToTasks -= selectedTasksInCurrentState.size();
    }

    return selectedTasks;
  }

  private List<SapsImage> selectTasks(int count, final ImageTaskState state) {
    List<SapsImage> selectedTasks = new LinkedList<SapsImage>();

    if (count <= 0) {
      LOGGER.info(
          "There will be no selection of tasks in the "
              + state.getValue()
              + " state because there is no capacity for new jobs in Arrebol");
      return selectedTasks;
    }

    LOGGER.info("Trying select up to " + count + " tasks in state " + state);

    List<SapsImage> tasks =
        getTasksInCatalog(state, "gets tasks with " + state.getValue() + " state");

    Map<String, List<SapsImage>> tasksByUsers = mapUsers2Tasks(tasks);

    selectedTasks = selector.select(count, tasksByUsers);

    LOGGER.info("Number of selected tasks using " + selector.version() + ": " + selectedTasks.size());
    return selectedTasks;
  }

  public void submitTasks(List<SapsImage> selectedTasks) {
    for (SapsImage task : selectedTasks) {
      ImageTaskState nextState = getNextState(task.getState());

      updateStateInCatalog(
          task,
          nextState,
          SapsImage.AVAILABLE,
          SapsImage.NON_EXISTENT_DATA,
          SapsImage.NONE_ARREBOL_JOB_ID,
          "updates task [" + task.getTaskId() + "] state for " + nextState.getValue());
      try {
        String arrebolJobId = submitTaskToArrebol(task, nextState);
        updateStateInCatalog(
            task,
            task.getState(),
            SapsImage.AVAILABLE,
            SapsImage.NON_EXISTENT_DATA,
            arrebolJobId,
            "updates task [" + task.getTaskId() + "] with Arrebol job ID [" + arrebolJobId + "]");
        addTimestampTaskInCatalog(task, "updates task [" + task.getTaskId() + "] timestamp");
      } catch (Exception e) {
        updateStateInCatalog(
            task,
            ImageTaskState.FAILED,
            SapsImage.AVAILABLE,
            SapsImage.NON_EXISTENT_DATA,
            SapsImage.NONE_ARREBOL_JOB_ID,
            "Changed task [" + task.getTaskId() + "] state to FAILED");
        addTimestampTaskInCatalog(task, "updates task [" + task.getTaskId() + "] timestamp");
      }
    }
  }

  protected Map<String, List<SapsImage>> mapUsers2Tasks(List<SapsImage> tasks) {
    Map<String, List<SapsImage>> mapUsersToTasks = new TreeMap<String, List<SapsImage>>();

    for (SapsImage task : tasks) {
      String user = task.getUser();
      if (!mapUsersToTasks.containsKey(user)) mapUsersToTasks.put(user, new ArrayList<SapsImage>());

      mapUsersToTasks.get(user).add(task);
    }

    for (Map.Entry<String, List<SapsImage>> entry : mapUsersToTasks.entrySet()) {
      entry.getValue().sort(
              new Comparator<SapsImage>() {
                @Override
                public int compare(SapsImage task01, SapsImage task02) {
                  int priorityCompare = task02.getPriority() - task01.getPriority();
                  if (priorityCompare != 0) return priorityCompare;
                  else return task02.getCreationTime().compareTo(task01.getCreationTime());
                }
              });
    }

    return mapUsersToTasks;
  }

  private String submitTaskToArrebol(SapsImage task, ImageTaskState state) throws Exception {
  LOGGER.info(
      "Trying submit task id ["
          + task.getTaskId()
          + "] in state "
          + task.getState().getValue()
          + " to arrebol");

  String repository = getRepository(state);
  ExecutionScriptTag scriptInfo = getExecutionScriptTag(task, repository);

  String formatImageWithDigest = getFormatImageWithDigest(scriptInfo, state, task);
  String memoryUsage = scriptInfo.getMemoryUsage();
  String cpuUsage = scriptInfo.getCpuUsage();

  Map<String, String> requirements = new HashMap<String, String>();
  requirements.put("image", formatImageWithDigest);
  requirements.put(REQUIREMENTS_RAM_REQUEST, memoryUsage);
  requirements.put(REQUIREMENTS_CPU_REQUEST, cpuUsage);

  List<String> commands = SapsTask.buildCommandList(task, repository);

  Map<String, String> metadata = new HashMap<String, String>();

  SapsTask sapsTask =
      new SapsTask(
          task.getTaskId() + "#" + formatImageWithDigest, requirements, commands, metadata);
  LOGGER.info("SAPS task: " + sapsTask.toJSON().toString());

  List<SapsTask> tasks = new LinkedList<SapsTask>();
  tasks.add(sapsTask);

  SapsJob imageJob = new SapsJob(task.getTaskId(), tasks);
  LOGGER.info("SAPS job: " + imageJob.toJSON().toString());

  String jobId = submitJobInArrebol(imageJob, "add new job");
  LOGGER.debug("Result submited job: " + jobId);

  arrebol.addJobInList(new JobSubmitted(jobId, task));
  LOGGER.info("Adding job in list");

  return jobId;
}

  public void checker() {
  List<JobSubmitted> submittedJobs = arrebol.returnAllJobsSubmitted();
  List<JobSubmitted> finishedJobs = new LinkedList<JobSubmitted>();

  LOGGER.info("Checking " + submittedJobs.size() + " submitted jobs for Arrebol service");
  LOGGER.info("Submmitteds jobs list: " + submittedJobs.toString());

  for (JobSubmitted job : submittedJobs) {
    System.out.println(job);
    String jobId = job.getJobId();
    SapsImage task = job.getImageTask();

    JobResponseDTO jobResponse = getJobByIdInArrebol(jobId, "gets job by ID [" + jobId + "]");
    LOGGER.debug("Job [" + jobId + "] information returned from Arrebol: " + jobResponse);

    if (!wasJobFound(jobResponse)) {
        LOGGER.info(
            "Job ["
                + jobId
                + "] not found in Arrebol service, applying status rollback to task ["
                + task.getTaskId()
                + "]");

        rollBackTaskState(task);
        finishedJobs.add(job);
        continue;
      }

    if (checkJobWasFinish(jobResponse)) {

      if (checkJobFinishedWithSucess(jobResponse)) {
        LOGGER.info("Job [" + jobId + "] has been finished with success");
        LOGGER.info("Current state of task: " + task.getState());

        ImageTaskState nextState = getNextState(task.getState());

        if (nextState == null) {
          nextState = ImageTaskState.FINISHED;
        } 

        LOGGER.info("Updated state of task: " + nextState);
        updateStateInCatalog(
            task,
            nextState,
            SapsImage.AVAILABLE,
            SapsImage.NON_EXISTENT_DATA,
            SapsImage.NONE_ARREBOL_JOB_ID,
            "updates task ["
                + task.getTaskId()
                + "] with next state ["
                + nextState.getValue()
                + "]");
      } else {
        LOGGER.info("Job [" + jobId + "] has been finished with failure");

        updateStateInCatalog(
            task,
            ImageTaskState.FAILED,
            SapsImage.AVAILABLE,
            "error while execute " + task.getState().getValue() + " phase",
            SapsImage.NONE_ARREBOL_JOB_ID,
            "updates task [" + task.getTaskId() + "] with failed state");
      }

      addTimestampTaskInCatalog(task, "updates task [" + task.getTaskId() + "] timestamp");

      finishedJobs.add(job);
    } else LOGGER.info("Job [" + jobId + "] has NOT been finished");
  }

    for (JobSubmitted jobFinished : finishedJobs) {
      LOGGER.info("Removing job [" + jobFinished.getJobId() + "] from the submitted job list");
      arrebol.removeJob(jobFinished);
    }
  }

  private boolean wasJobFound(JobResponseDTO jobResponse) {
    JobResponseDTO jobResponseDTO = ArrebolUtils.getJobById(arrebol, jobResponse.getId(), "Searching for job in arrebol");

    if (jobResponseDTO != null) {
      return true;
    }

    return false;
  }

  protected boolean updateStateInCatalog(SapsImage task, ImageTaskState state, String status,
      String error, String arrebolJobId, String message) {
        
    task.setState(state);
    task.setStatus(status);
    task.setError(error);
    task.setArrebolJobId(arrebolJobId);

    CatalogUtils.updateState(catalog, task);
    
    return true;
  }

  private void addTimestampTaskInCatalog(SapsImage task, String message) {
    CatalogUtils.addTimestampTask(catalog, task);
  }

  private String submitJobInArrebol(SapsJob imageJob, String message) {
    return ArrebolUtils.submitJob(arrebol, imageJob, message);
  }

  private boolean checkJobWasFinish(JobResponseDTO jobResponse) {
    String jobId = jobResponse.getId();
    String jobState = jobResponse.getJobState().toUpperCase();

    LOGGER.info("Checking if job [" + jobId + "] was finished");
    LOGGER.info("State job [" + jobId + "]: " + jobState);

    if (jobState.compareTo(TaskResponseDTO.STATE_FAILED) != 0
        && jobState.compareTo(TaskResponseDTO.STATE_FINISHED) != 0) return false;

    return true;
  }

  private boolean checkJobFinishedWithSucess(JobResponseDTO jobResponse) {

    for (TaskResponseDTO task : jobResponse.getTasks()) {
      TaskSpecResponseDTO taskSpec = task.getTaskSpec();

      for (CommandResponseDTO command : taskSpec.getCommands()) {

        String commandDesc = command.getCommand();
        String commandState = command.getState();
        Integer commandExitCode = command.getExitCode();

        LOGGER.info("Command: " + commandDesc);
        LOGGER.info("State:" + commandState);
        LOGGER.info("Exit code: " + commandExitCode);

        if (commandExitCode != 0 || !commandState.equals(TaskResponseDTO.STATE_FINISHED))
          return false;
      }
    }
    return true;
  }

   private ImageTaskState getNextState(ImageTaskState currentState) {
    Map<ImageTaskState, ImageTaskState> statesMap = new HashMap<>();

    statesMap.put(ImageTaskState.CREATED, ImageTaskState.DOWNLOADING);
    statesMap.put(ImageTaskState.DOWNLOADING, ImageTaskState.DOWNLOADED);
    statesMap.put(ImageTaskState.DOWNLOADED, ImageTaskState.PREPROCESSING);
    statesMap.put(ImageTaskState.PREPROCESSING, ImageTaskState.READY);
    statesMap.put(ImageTaskState.READY, ImageTaskState.RUNNING);
    statesMap.put(ImageTaskState.RUNNING, ImageTaskState.FINISHED);

    return statesMap.get(currentState);
  }

  private ImageTaskState getPreviousState(ImageTaskState currentState) {
    HashMap<ImageTaskState, ImageTaskState> statesMap = new HashMap<>();

    statesMap.put(ImageTaskState.DOWNLOADING, ImageTaskState.CREATED);
    statesMap.put(ImageTaskState.PREPROCESSING, ImageTaskState.DOWNLOADED);
    statesMap.put(ImageTaskState.RUNNING, ImageTaskState.READY);

    return statesMap.get(currentState);
  }

  private ExecutionScriptTag getExecutionScriptTag(SapsImage task, String repository)
      throws Exception {
    String tagsFilePath = System.getProperty(EXECUTION_TAGS_FILE_PATH_KEY);
    String tag;
    if (repository.equals(ExecutionScriptTagUtil.PROCESSING)) tag = task.getProcessingTag();
    else if (repository.equals(ExecutionScriptTagUtil.PRE_PROCESSING)) tag = task.getPreprocessingTag();
    else tag = task.getInputdownloadingTag();

    return ExecutionScriptTagUtil.getExecutionScriptTag(tagsFilePath, tag, repository);
  }

  private String getFormatImageWithDigest(
        ExecutionScriptTag imageDockerInfo, ImageTaskState state, SapsImage task) {
      if (state == ImageTaskState.RUNNING)
        return imageDockerInfo.getDockerRepository() + "@" + task.getDigestProcessing();
      else if (state == ImageTaskState.PREPROCESSING)
        return imageDockerInfo.getDockerRepository() + "@" + task.getDigestPreprocessing();
      else return imageDockerInfo.getDockerRepository() + "@" + task.getDigestInputdownloading();
    }

  private String getRepository(ImageTaskState state) {
    if (state == ImageTaskState.RUNNING) return ExecutionScriptTagUtil.PROCESSING;
    else if (state == ImageTaskState.PREPROCESSING) return ExecutionScriptTagUtil.PRE_PROCESSING;
    else return ExecutionScriptTagUtil.INPUT_DOWNLOADER;
  }

  private int getCountSlotsInArrebol(String queueId) {
      return ArrebolUtils.getCountSlots(arrebol, queueId);
    }

  private JobResponseDTO getJobByIdInArrebol(String jobId, String message) {
    return ArrebolUtils.getJobById(arrebol, jobId, message);
  }

  private List<JobResponseDTO> getJobByNameInArrebol(String jobName, String message) {
      return ArrebolUtils.getJobByName(arrebol, jobName, message);
    }

  private List<SapsImage> getTasksInCatalog(ImageTaskState state, String message) { 
    return CatalogUtils.getTasks(catalog, state);
  }

  private List<SapsImage> getProcessingTasksInCatalog() {
    return CatalogUtils.getProcessingTasks(catalog, "gets tasks in processing state");
  }
}
