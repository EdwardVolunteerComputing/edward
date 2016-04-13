import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.joegreen.edward.core.model.communication.IdContainer;
import pl.joegreen.edward.rest.client.RestClient;
import pl.joegreen.edward.rest.client.RestException;

public class AddOneWithEdward {

	public static void main(String[] args) throws RestException {

		RestClient restClient = new RestClient("admin", "admin", "localhost", 8080, "http", "");
		
		//Project 
		IdContainer projectId = restClient.addProject("Edward Example Project");

		// Job code can have helper functions, but compute(input) needs to be
		// defined.
		IdContainer jobId = restClient.addJob(projectId.getId(), "addOneJob",
				"function compute(input){return input+1;}");
		List<String> inputs = Arrays.asList("1", "2", "3", "100");

		// increase timeout for longer tasks (last param)
		List<Long> taskIdentifiers = restClient.addTasks(jobId.getId(), inputs,
				0, 1, 1000);

		Map<Long, String> taskIdentifierToInput = new HashMap<Long, String>();
		for (int i = 0; i < inputs.size(); ++i) {
			taskIdentifierToInput.put(taskIdentifiers.get(i), inputs.get(i));
		}
		
		
		for (Long taskIdentifier : taskIdentifiers) {
			String taskResult = restClient.getTaskResultBlocking(
					taskIdentifier, 100);
			System.out.println(String.format(
					"Task id: %d input: %s, output: %s", taskIdentifier,
					taskIdentifierToInput.get(taskIdentifier), taskResult));
		}

		// Volunteer's code uses exponential backoff when computing
		// how often it should ask server for a new tasks.
		// If volunteer gets one task after another it goes smoothly (the task-pull time interval is short).
		// If volunteer gets no tasks for a long time it can take even up to
		// 10 seconds before volunteer starts computing anything at all.
		// To prevent this behaviour in tests, browser can be restarted or some
		// simple tasks can be sent before actual test is conducted and time measurement taken.
		
	}
}
