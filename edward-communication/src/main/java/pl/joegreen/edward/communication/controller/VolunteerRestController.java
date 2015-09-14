package pl.joegreen.edward.communication.controller;

import javax.servlet.http.HttpServletRequest;

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
import pl.joegreen.edward.core.model.communication.VolunteerRegistrationResponse;
import pl.joegreen.edward.persistence.dao.VolunteerDao;

@Controller
@RequestMapping("/api/volunteer/")
public class VolunteerRestController extends RestControllerBase {
	private final static Logger logger = LoggerFactory
			.getLogger(VolunteerRestController.class);

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
		logger.info(String.format("Execution %d finished with error",executionId));
		executionManagerService.saveExecutionError(executionId, error);
	}

	@RequestMapping(value = "getNextTask/{volunteerId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ClientExecutionInfo getExecution(
			@PathVariable long volunteerId, HttpServletRequest request)
			throws NoTaskForClientException {
		String remoteAddress = request.getRemoteAddr();

		logger.info(String.format(
				"Volunteer(address: %s, id: %d) asks for task", remoteAddress, volunteerId));
		ClientExecutionInfo executionInfo = executionManagerService
				.createNextExecutionForClient(volunteerId);
		if (executionInfo != null) {
			logger.info(String.format(
					"Volunteer(address: %s, id: %d) starts %d",
					remoteAddress, volunteerId,
					executionInfo.getExecutionId()));
			return executionInfo;
		} else {
			throw new NoTaskForClientException();
		}
	}

	@RequestMapping(value = "register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public VolunteerRegistrationResponse register(HttpServletRequest request) {
		String remoteAddress = request.getRemoteAddr();
		logger.info(String.format(
				"Volunteer with address %s registers", remoteAddress));
		return volunteerManagerService.handleRegistration();
	}

	@RequestMapping(value = "heartbeat/{volunteerId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public void heartbeat(@PathVariable long volunteerId,
			HttpServletRequest request) {
		String remoteAddress = request.getRemoteAddr();

		logger.debug(String.format(
				"Volunteer(address: %s, id: %d) heartbeats",
				remoteAddress, volunteerId));
		volunteerManagerService.handleHeartbeat(volunteerId);
	}

	@RequestMapping(value = "getCode/{jobId}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public String getCodeForJob(@PathVariable Long jobId) {
		return getById(jobId, jobDao).getCode();
	}

}
