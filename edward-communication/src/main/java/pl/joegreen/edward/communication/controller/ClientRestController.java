package pl.joegreen.edward.communication.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import pl.joegreen.edward.communication.controller.exception.NoTaskForClientException;
import pl.joegreen.edward.core.model.communication.ClientExecutionInfo;

@Controller
@RequestMapping("/client/")
public class ClientRestController extends RestControllerBase {

	@RequestMapping(value = "sendResult/{executionId}", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public void sendTaskResult(@PathVariable Long executionId,
			@RequestBody String result) {
		executionManagerService.saveExecutionResult(executionId, result);
	}

	@RequestMapping(value = "sendError/{executionId}", method = RequestMethod.POST, consumes = MediaType.ALL_VALUE)
	@ResponseBody
	public void sendExecutionError(@PathVariable Long executionId,
			@RequestBody String error) {
		executionManagerService.saveExecutionError(executionId, error);
	}

	@RequestMapping(value = "getNextTask", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ClientExecutionInfo getExecution() throws NoTaskForClientException {
		ClientExecutionInfo executionInfo = executionManagerService
				.createNextExecutionForClient();
		if (executionInfo != null) {
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
