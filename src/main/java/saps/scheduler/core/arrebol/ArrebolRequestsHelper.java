/* (C)2020 */
package saps.scheduler.core.arrebol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import saps.common.core.dto.JobRequestDTO;
import saps.common.core.dto.JobResponseDTO;
import saps.common.core.model.SapsJob;
import saps.common.utils.SapsPropertiesConstants;
import saps.scheduler.core.arrebol.exceptions.GetCountsSlotsException;
import saps.scheduler.core.arrebol.exceptions.GetJobException;
import saps.scheduler.core.arrebol.exceptions.SubmitJobException;
import saps.scheduler.core.arrebol.http.HttpWrapper;

// TODO implement tests
public class ArrebolRequestsHelper {

  // TODO review this names
  private final Properties properties;
  private final String arrebolBaseUrl;
  private final Gson gson;

  private static final Logger LOGGER = Logger.getLogger(ArrebolRequestsHelper.class);

  public ArrebolRequestsHelper(Properties properties) {
    this.properties = properties;
    this.arrebolBaseUrl = this.properties.getProperty(SapsPropertiesConstants.ARREBOL_BASE_URL);
    this.gson = new GsonBuilder().create();
  }

  public String submitJobToExecution(SapsJob job) throws Exception, SubmitJobException {
    StringEntity requestBody;

    try {
      requestBody = makeJSONBody(job);
    } catch (UnsupportedEncodingException e) {
      throw new Exception("Job is not well formed to built JSON.");
    }

    final String jobEndpoint = this.arrebolBaseUrl + "/queues/default/jobs";

    String jobIdArrebol;
    final String JSON_KEY_JOB_ID_ARREBOL = "id";

    try {
      final String jsonResponse =
          HttpWrapper.doRequest(
              HttpPost.METHOD_NAME, jobEndpoint, new LinkedList<Header>(), requestBody);

      JsonObject jobResponse = this.gson.fromJson(jsonResponse, JsonObject.class);

      jobIdArrebol = jobResponse.get(JSON_KEY_JOB_ID_ARREBOL).getAsString();

      LOGGER.info("Job was submitted with success to Arrebol.");

    } catch (Exception e) {
      throw new SubmitJobException("Submit Job to Arrebol has FAILED: " + e.getMessage(), e);
    }

    return jobIdArrebol;
  }

  public JobResponseDTO getJob(String jobArrebolId) throws GetJobException {
    return this.gson.fromJson(getJobJSON(jobArrebolId), JobResponseDTO.class);
  }

  public String getJobJSON(String jobArrebolId) throws GetJobException {
    final String endpoint = this.arrebolBaseUrl + "/queues/default/jobs/" + jobArrebolId;

    String jsonResponse;
    try {
      jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, endpoint, null);
    } catch (Exception e) {
      throw new GetJobException("Get Job from Arrebol has FAILED: " + e.getMessage(), e);
    }

    return jsonResponse;
  }

  public StringEntity makeJSONBody(SapsJob job) throws UnsupportedEncodingException {
    LOGGER.info("Building JSON body of Job ...");

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JobRequestDTO jobDTO = new JobRequestDTO(job);
    String json = gson.toJson(jobDTO);

    LOGGER.info("JSON body: " + json);

    return new StringEntity(json);
  }

  public int getCountSlotsInQueue(String queueId) throws GetCountsSlotsException {
    final String endpoint = this.arrebolBaseUrl + "/queues/" + queueId;
    final String JSON_KEY_WAITING_JOBS_ARREBOL = "waiting_jobs";

    int waitingJobs;
    try {
      final String jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, endpoint, null);
      JsonObject jobResponse = this.gson.fromJson(jsonResponse, JsonObject.class);
      waitingJobs = jobResponse.get(JSON_KEY_WAITING_JOBS_ARREBOL).getAsInt();

      LOGGER.info("Arrebol in queue id [" + queueId + "] was " + waitingJobs + " waiting jobs");
    } catch (Exception e) {
      throw new GetCountsSlotsException(
          "Get waiting jobs from Arrebol queue id [" + queueId + "] has FAILED: " + e.getMessage(),
          e);
    }

    return waitingJobs;
  }
}
