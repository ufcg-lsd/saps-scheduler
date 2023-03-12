/* (C)2020 */
package saps.scheduler.core;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import saps.catalog.core.Catalog;
import saps.catalog.core.jdbc.JDBCCatalog;
import saps.catalog.core.retry.CatalogUtils;
import saps.common.core.dto.*;
import saps.common.core.model.SapsImage;
import saps.common.core.model.SapsJob;
import saps.common.core.model.SapsTask;
import saps.common.core.model.SapsUserJob;
import saps.common.core.model.enums.ImageTaskState;
import saps.common.core.model.enums.JobState;
import saps.common.exceptions.SapsException;
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

public class Scheduler {

  // Constants
  public static final Logger LOGGER = Logger.getLogger(Scheduler.class);
  public static final String EXECUTION_TAGS_FILE_PATH_KEY = "EXECUTION_SCRIPT_TAGS_FILE_PATH";
  public static final String REQUIREMENTS_CPU_REQUEST = "CPUUsage";
  public static final String REQUIREMENTS_RAM_REQUEST = "RAMUsage";

  // Saps Controller Variables
  private ScheduledExecutorService sapsExecutor;
  private Selector selector;

  // FIXME Remove properties field and add new variables
  private Properties properties;
  private Catalog catalog;
  private Arrebol arrebol;

  public Scheduler(Properties properties) throws SapsException {
    this(
        properties,
        new JDBCCatalog(properties),
        Executors.newScheduledThreadPool(1),
        new DefaultArrebol(properties),
        new DefaultRoundRobin());
  }

  public Scheduler(
      Properties properties,
      Catalog catalog,
      ScheduledExecutorService sapsExecutor,
      Arrebol arrebol,
      Selector selector)
      throws SapsException {
    if (!checkProperties(properties))
      throw new SapsException(
          "Error on validate the file. Missing properties for start Scheduler Component.");

    this.properties = properties;
    this.catalog = catalog;
    this.sapsExecutor = sapsExecutor;
    this.arrebol = arrebol;
    this.selector = selector;
  }

  private static boolean checkProperties(Properties properties) {
    String[] propertiesSet = {
      SapsPropertiesConstants.IMAGE_DATASTORE_IP,
      SapsPropertiesConstants.IMAGE_DATASTORE_PORT,
      SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR,
      SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER,
      SapsPropertiesConstants.ARREBOL_BASE_URL
    };

    return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
  }

  public void start() {
    recovery();

    sapsExecutor.scheduleWithFixedDelay(
        () -> schedule(),
        0,
        Integer.parseInt(
            properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_SUBMISSOR)),
        TimeUnit.SECONDS);

    sapsExecutor.scheduleWithFixedDelay(
        () -> checker(),
        0,
        Integer.parseInt(
            properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER)),
        TimeUnit.SECONDS);

    sapsExecutor.scheduleWithFixedDelay(
        () -> userJobChecker(),
        0,
        Integer.parseInt(
            properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_CHECKER)),
        TimeUnit.SECONDS);
  }

  private void userJobChecker() {
    List<SapsUserJob> ongoingUserJobs = getOngoingUserJobsInCatalog();

    for (SapsUserJob userJob : ongoingUserJobs) {    
      if (userJob.getState().equals(JobState.CREATED)) { 
        List<SapsImage> ongoingTasks = getJobTasksInCatalog(userJob.getJobId(), "", true);
        if (ongoingTasks.size() > 0) {
          updateUserJobSateInCatalog(userJob.getJobId(), JobState.RUNNING);
          LOGGER.info("Setting the user job [" + userJob.getJobId() + "] state to RUNNING");
        }
      }

      if (userJob.getState().equals(JobState.RUNNING)) { 
        List<SapsImage> ongoingTasks = getJobTasksInCatalog(userJob.getJobId(), "", true);
        List<SapsImage> archivedTasks = getJobTasksInCatalog(userJob.getJobId(), ImageTaskState.ARCHIVED.getValue(), false);

        if (ongoingTasks.size() == 0 && archivedTasks.size() == 0) {
          updateUserJobSateInCatalog(userJob.getJobId(), JobState.FAILED);
          LOGGER.info("Setting the user job [" + userJob.getJobId() + "] state to FAILED");
        } else if (ongoingTasks.size() == 0 && archivedTasks.size() > 0){
          updateUserJobSateInCatalog(userJob.getJobId(), JobState.FINISHED);
          LOGGER.info("Setting the user job [" + userJob.getJobId() + "] state to FINISHED");
        }
      }
    }
  }

  /**
   * This function retrieves consistency between the information present in Catalog and Arrebol, and
   * starts the list of submitted jobs.
   */
  private void recovery() {
    List<SapsImage> tasksInProcessingState = getProcessingTasksInCatalog();
    List<SapsImage> tasksForPopulateSubmittedJobList = new LinkedList<SapsImage>();

    for (SapsImage task : tasksInProcessingState) {
      if (task.getArrebolJobId().equals(SapsImage.NONE_ARREBOL_JOB_ID)) {
        String jobName = task.getState().getValue() + "-" + task.getTaskId();
        List<JobResponseDTO> jobsWithEqualJobName =
            getJobByNameInArrebol(jobName, "gets job by name [" + jobName + "]");
        if (jobsWithEqualJobName.size() == 0) rollBackTaskState(task);
        else if (jobsWithEqualJobName.size() == 1) { // TODO add check jobName ==
          // jobsWithEqualJobName.get(0).get...
          String arrebolJobId = jobsWithEqualJobName.get(0).getId();
          updateStateInCatalog(
              task,
              task.getState(),
              SapsImage.AVAILABLE,
              SapsImage.NON_EXISTENT_DATA,
              arrebolJobId,
              "updates task [" + task.getTaskId() + "] with Arrebol job ID [" + arrebolJobId + "]");
          tasksForPopulateSubmittedJobList.add(task);
        } else {
          // TODO ????
        }
      } else {
        String arrebolJobId = task.getArrebolJobId();
        arrebol.addJobInList(new JobSubmitted(arrebolJobId, task));
      }
    }

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

  /** This function schedules up to tasks. */
  private void schedule() {
    List<SapsImage> selectedTasks = selectTasks();
    submitTasks(selectedTasks);
  }

  /**
   * This function selects tasks following a strategy for submit in Arrebol.
   *
   * @return selected tasks list
   */
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

  /**
   * This function selects tasks in specific state following a strategy for submit in Arrebol.
   *
   * @param count count of available slots in Arrebol queue
   * @param state specific state for selection
   * @return selected tasks list in specific state
   */
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

  /**
   * This function submits tasks for Arrebol and updates state and job IDs in BD.
   *
   * @param selectedTasks selected task list for submit to Arrebol
   */
  protected void submitTasks(List<SapsImage> selectedTasks) {
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

  /**
   * This function get next state based in current state.
   *
   * @param currentState current state
   * @return next state
   */
  private ImageTaskState getNextState(ImageTaskState currentState) {
    if (currentState == ImageTaskState.CREATED) return ImageTaskState.DOWNLOADING;
    if (currentState == ImageTaskState.DOWNLOADING) return ImageTaskState.DOWNLOADED;
    if (currentState == ImageTaskState.DOWNLOADED) return ImageTaskState.PREPROCESSING;
    if (currentState == ImageTaskState.PREPROCESSING) return ImageTaskState.READY;
    if (currentState == ImageTaskState.READY) return ImageTaskState.RUNNING;
    if (currentState == ImageTaskState.RUNNING) return ImageTaskState.FINISHED;

    return null;
  }

  /**
   * This function get previous state based in current state.
   *
   * @param currentState current state
   * @return previous state
   */
  private ImageTaskState getPreviousState(ImageTaskState currentState) {
    if (currentState == ImageTaskState.RUNNING) return ImageTaskState.READY;
    if (currentState == ImageTaskState.PREPROCESSING) return ImageTaskState.DOWNLOADED;
    if (currentState == ImageTaskState.DOWNLOADING) return ImageTaskState.CREATED;

    return null;
  }

  /**
   * * This function associate each task with a specific user by building an map. After that, it
   * sorts each task list by task priority.
   *
   * @param tasks list with tasks
   * @return user map by tasks
   */
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
                  else return task02.getCreationTime().compareTo(task02.getCreationTime());
                }
              });
    }

    return mapUsersToTasks;
  }

  /**
   * This function try submit task to Arrebol.
   *
   * @param task task to be submitted
   * @return Arrebol job id
   * @throws Exception
   */
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

    LOGGER.info("Creating SAPS task ...");
    SapsTask sapsTask =
        new SapsTask(
            task.getTaskId() + "#" + formatImageWithDigest, requirements, commands, metadata);
    LOGGER.info("SAPS task: " + sapsTask.toJSON().toString());

    LOGGER.info("Creating SAPS job ...");
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

  /**
   * This function get key (repository representation in execution_script_tags file) based in state
   * parameter.
   *
   * @param state task state to be submitted
   * @return key (repository representation)
   */
  private String getRepository(ImageTaskState state) {
    if (state == ImageTaskState.RUNNING) return ExecutionScriptTagUtil.PROCESSING;
    else if (state == ImageTaskState.PREPROCESSING) return ExecutionScriptTagUtil.PRE_PROCESSING;
    else return ExecutionScriptTagUtil.INPUT_DOWNLOADER;
  }

  /**
   * This function gets script tag based in repository.
   *
   * @param task task to be submitted
   * @param repository task repository
   * @return task information (tag, repository, type and name)
   */
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

  /**
   * This function checks if each submitted job was finished. If exists finished jobs, for each job
   * is updates state in Catalog and removes a job by list of submitted jobs to Arrebol.
   */
  private void checker() {
    List<JobSubmitted> submittedJobs = arrebol.returnAllJobsSubmitted();
    List<JobSubmitted> finishedJobs = new LinkedList<JobSubmitted>();

    LOGGER.info("Checking " + submittedJobs.size() + " submitted jobs for Arrebol service");
    LOGGER.info("Submmitteds jobs list: " + submittedJobs.toString());

    for (JobSubmitted job : submittedJobs) {
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

      boolean checkFinish = checkJobWasFinish(jobResponse);
      if (checkFinish) {
        LOGGER.info("Job [" + jobId + "] has been finished");

        boolean checkOK = checkJobFinishedWithSucess(jobResponse);

        if (checkOK) {
          LOGGER.info("Job [" + jobId + "] has been finished with success");

          ImageTaskState nextState = getNextState(task.getState());
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

  // TODO implement method

  /**
   * This function checks if job was found in Arrebol service.
   *
   * @param jobResponse job response
   * @return boolean representation, true (case job was found) and false (otherwise)
   */
  private boolean wasJobFound(JobResponseDTO jobResponse) {
    return true;
  }

  /**
   * This function checks job state was finish.
   *
   * @param jobResponse job to be check
   * @return boolean representation, true (is was finished) or false (otherwise)
   */
  private boolean checkJobWasFinish(JobResponseDTO jobResponse) {
    String jobId = jobResponse.getId();
    String jobState = jobResponse.getJobState().toUpperCase();

    LOGGER.info("Checking if job [" + jobId + "] was finished");
    LOGGER.info("State job [" + jobId + "]: " + jobState);

    if (jobState.compareTo(TaskResponseDTO.STATE_FAILED) != 0
        && jobState.compareTo(TaskResponseDTO.STATE_FINISHED) != 0) return false;

    return true;
  }

  /**
   * This function checks a finished job was completed with success or failure.
   *
   * @param jobResponse job to be check
   * @return boolean representation, true (success completed) or false (otherwise)
   */
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

  /**
   * This function gets Arrebol capacity for add new jobs.
   *
   * @param queueId queue identifier in Arrebol
   * @return Arrebol queue capacity in queue with identifier
   */
  private int getCountSlotsInArrebol(String queueId) {
    return ArrebolUtils.getCountSlots(arrebol, queueId);
  }

  /**
   * This function gets job in Arrebol that matching with id.
   *
   * @param jobId job id to be used for matching
   * @param message information message
   * @return job response that matching with id
   */
  private JobResponseDTO getJobByIdInArrebol(String jobId, String message) {
    return ArrebolUtils.getJobById(arrebol, jobId, message);
  }

  /**
   * This function gets job list in Arrebol that matching with name.
   *
   * @param jobName job label to be used for matching
   * @param message information message
   * @return job response list that matching with label
   */
  private List<JobResponseDTO> getJobByNameInArrebol(String jobName, String message) {
    return ArrebolUtils.getJobByName(arrebol, jobName, message);
  }

  /**
   * This function submit job in Arrebol service.
   *
   * @param imageJob SAPS job to be submitted
   * @param message information message
   * @return job id returned from Arrebol
   */
  private String submitJobInArrebol(SapsJob imageJob, String message) {
    return ArrebolUtils.submitJob(arrebol, imageJob, message);
  }

  /**
   * This function gets tasks in specific state in Catalog.
   *
   * @param state specific state for get tasks
   * @param message information message
   * @return tasks in specific state
   */
  private List<SapsImage> getTasksInCatalog(ImageTaskState state, String message) {
    return CatalogUtils.getTasks(catalog, state);
  }

  /**
   * This function gets tasks in processing state in catalog component.
   *
   * @return processing tasks list
   */
  private List<SapsImage> getProcessingTasksInCatalog() {
    return CatalogUtils.getProcessingTasks(catalog, "gets tasks in processing state");
  }

  /**
   * This function updates task state in catalog component.
   *
   * @param task task to be updated
   * @param state new task state
   * @param status new task status
   * @param error new error message
   * @param arrebolJobId new Arrebol job id
   * @param message information message
   * @return boolean representation reporting success (true) or failure (false) in update state task
   *     in catalog
   */
  private boolean updateStateInCatalog(
      SapsImage task,
      ImageTaskState state,
      String status,
      String error,
      String arrebolJobId,
      String message) {
    task.setState(state);
    task.setStatus(status);
    task.setError(error);
    task.setArrebolJobId(arrebolJobId);
    return CatalogUtils.updateState(catalog, task);
  }

  /**
   * This function add new tuple in time stamp table and updates task time stamp.
   *
   * @param task task to be update
   * @param message information message
   */
  private void addTimestampTaskInCatalog(SapsImage task, String message) {
    CatalogUtils.addTimestampTask(catalog, task);
  }


  /**
   * This function gets ongoing user jobs in catalog component.
   * @return ongoing user jobs list
   */
  private List<SapsUserJob> getOngoingUserJobsInCatalog() {
    return CatalogUtils.getUserJobs(catalog, "", "", 1, 5, "", "", false, true, "get jobs");
  }

  /**
   * This function gets user job tasks in catalog component.
   * @param jobId user job id
   * @param state task state
   * @param recoverOnlyOngoing flag to recover only ongoing tasks
   * @return user job tasks list
   */
  private List<SapsImage> getJobTasksInCatalog(String jobId, String state, boolean recoverOnlyOngoing) {
    return CatalogUtils.getUserJobTasks(catalog, jobId, state, "", 0, 0, "state", "asc", recoverOnlyOngoing, "get job tasks");
  }

  /**
   * This function updates user job state in catalog component.
   * @param jobId user job id
   * @param state user job state
   */
  private void updateUserJobSateInCatalog(String jobId, JobState state) {
    CatalogUtils.updateUserJob(catalog, jobId, state, "updating user job state");
  }
}
