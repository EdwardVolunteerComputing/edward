package pl.joegreen.edward.communication.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import pl.joegreen.edward.communication.controller.exception.NoTaskForClientException;
import pl.joegreen.edward.core.model.communication.ClientExecutionInfo;
import pl.joegreen.edward.persistence.dao.VolunteerDao;

@Controller
@RequestMapping("/client/")
public class ClientRestController extends RestControllerBase {
	private final static Logger logger = LoggerFactory
			.getLogger(ClientRestController.class);

	@Autowired
	private VolunteerDao volunteerDao;

	@RequestMapping(value = "sendResult/{executionId}", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public void sendTaskResult(@PathVariable Long executionId,
			@RequestBody String result) {
		logger.info(String.format("Execution %d result received", executionId));
		executionManagerService.saveExecutionResult(executionId, result);
	}

	@RequestMapping(value = "sendError/{executionId}", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public void sendExecutionError(@PathVariable Long executionId,
			@RequestBody String error) {
		logger.info(String.format("Execution %d finished with error",
				executionId));
		executionManagerService.saveExecutionError(executionId, error);
	}

	@RequestMapping(value = "getNextTask/{volunteerId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ClientExecutionInfo getExecution(@PathVariable long volunteerId)
			throws NoTaskForClientException {
		logger.info(String.format("Volunteer %d asks for task", volunteerId));
		ClientExecutionInfo executionInfo = executionManagerService
				.createNextExecutionForClient(volunteerId);
		if (executionInfo != null) {
			logger.info(String.format("Volunteer %d starts execution %d",
					volunteerId, executionInfo.getExecutionId()));
			return executionInfo;
		} else {
			throw new NoTaskForClientException();
		}
	}

	@RequestMapping(value = "getCode/{jobId}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public String getCodeForJob(@PathVariable Long jobId) {
		return getById(jobId, jobDao).getCode();
	}

}
