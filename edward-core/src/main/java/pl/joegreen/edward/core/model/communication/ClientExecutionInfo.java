package pl.joegreen.edward.core.model.communication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientExecutionInfo {

	/*
	 * Object of this class is sent to a client when the client requests a new
	 * task. We could send the "execution" object but then the client would have
	 * to make additional api calls to get task and to get input data. We send
	 * jobId instead of job's code because client can already have the code for
	 * that job. Execution id is needed for the client to send back the result.
	 */
	private Long jobId;
	private String inputData;
	private Long executionId;

	@JsonCreator
	public ClientExecutionInfo(@JsonProperty("jobId") Long jobId,
			@JsonProperty("inputData") String inputData,
			@JsonProperty("executionId") Long executionId) {
		this.jobId = jobId;
		this.inputData = inputData;
		this.executionId = executionId;
	}

	@SuppressWarnings("unused")
	/* for Jackson */
	public Long getJobId() {
		return jobId;
	}

	public String getInputData() {
		return inputData;
	}

	public Long getExecutionId() {
		return executionId;
	}

}
