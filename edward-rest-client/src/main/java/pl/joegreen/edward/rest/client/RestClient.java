package pl.joegreen.edward.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import pl.joegreen.edward.core.model.*;
import pl.joegreen.edward.core.model.communication.IdContainer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class RestClient {
	private String host;
	private String protocol;
	private int port;
	private String prefix;

	private ObjectMapper objectMapper = new ObjectMapper();
	private HttpClientContext context;
	private CloseableHttpClient httpClient;

	public RestClient(String user, String password, String host, int port,
			String protocol, String prefix) {
		this.host = host;
		this.protocol = protocol;
		this.port = port;
		this.prefix = prefix;
		initializeHttpTools(user, password);
	}

	public List<Project> getProjects() throws RestException {
		try {
			HttpGet get = new HttpGet(getBaseUrl() + "/project");
			String response = executeAndGetResponse(get);
			List<Project> projects = objectMapper.readValue(
					response,
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, Project.class));
			return projects;
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public IdContainer addProject(String name)
			throws RestException {
		try { // TODO: how about using dynamicproxy to rethrow the exceptions
				// instead of copyPaste?
			Project project = new Project();
			project.setName(name);
			String projectString = objectMapper.writeValueAsString(project);
			HttpPost post = createJsonPost(getBaseUrl() + "/project",
					projectString);
			String response = executeAndGetResponse(post);
			IdContainer identifier = objectMapper.readValue(response,
					IdContainer.class);
			return identifier;
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public IdContainer addJob(long projectId, String name, String code)
			throws RestException {
		try {
			Job job = new Job();
			job.setProjectId(projectId);
			job.setName(name);
			job.setCode(code);
			String jobString = new ObjectMapper().writeValueAsString(job);
			HttpPost post = createJsonPost(getBaseUrl() + "/job", jobString);
			String response = executeAndGetResponse(post);
			IdContainer identifier = objectMapper.readValue(response,
					IdContainer.class);
			return identifier;
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public long getVolunteerCount() throws RestException {
		String url = getBaseUrl() + "/volunteerCount";
		try {
			String response = executeAndGetResponse(new HttpGet(url));
			return Long.valueOf(response);
		} catch (IOException e) {
			throw new RestException(e);
		}
	}

	public List<Long> addTasks(long jobId, String tasksJsonArray,
			long priority, long concurrentExecutions, long timeout)
			throws RestException {
		try {
			String url = getBaseUrl() + "/job/" + jobId + "/tasks/" + priority
					+ "/" + concurrentExecutions + "/" + timeout;
			HttpPost post = createJsonPost(url, tasksJsonArray);
			String response = executeAndGetResponse(post);
			List<Long> identifiers = objectMapper.readValue(
					response,
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, Long.class));
			return identifiers;
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public List<Long> addTasks(long jobId, List<String> jsonTasks,
			long priority, long concurrentExecutions, long timeout)
			throws RestException {
		String tasksArray = "[" + StringUtils.join(jsonTasks, ", ") + "]";
		return addTasks(jobId, tasksArray, priority, concurrentExecutions,
				timeout);
	}

	public List<JsonData> getResults(long taskId) throws RestException {
		try {
			String url = getBaseUrl() + "/task/" + taskId + "/results";
			HttpGet get = new HttpGet(url);
			String response = executeAndGetResponse(get);
			return objectMapper.readValue(
					response,
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, JsonData.class));
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public void abortTasks(List<Long> identifiers) throws RestException {
		try {
			String url = getBaseUrl() + "/task/abort/"
					+ StringUtils.join(identifiers, ",");
			HttpPost post = createJsonPost(url, "");
			executeAndGetResponse(post);
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public Map<Long, JsonData> getResults(List<Long> identifiers)
			throws RestException {
		try {
			String url = getBaseUrl() + "/task/results/"
					+ StringUtils.join(identifiers, ",");
			HttpGet get = new HttpGet(url);
			String response = executeAndGetResponse(get);
			List<JsonData> taskResults = objectMapper.readValue(
					response,
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, JsonData.class));
			Map<Long, JsonData> result = new HashMap<Long, JsonData>();
			for (int i = 0; i < identifiers.size(); ++i) {
				result.put(identifiers.get(i), taskResults.get(i));
			}
			return result;
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public Task getTask(long id) throws RestException {
		try {
			String url = getBaseUrl() + "/task/" + id;
			HttpGet get = new HttpGet(url);
			String response = executeAndGetResponse(get);
			return objectMapper.readValue(response, Task.class);
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public TaskStatus getTaskStatus(long id) throws RestException {
		try {
			String url = getBaseUrl() + "/task/" + id + "/status";
			HttpGet get = new HttpGet(url);
			String response = executeAndGetResponse(get);
			return objectMapper.readValue(response, TaskStatus.class);
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public Map<Long, TaskStatus> getTasksStatuses(List<Long> identifiers)
			throws RestException {
		if (identifiers.isEmpty()) {
			return new HashMap<Long, TaskStatus>();
		}
		try {
			String url = getBaseUrl() + "/task/statuses/"
					+ StringUtils.join(identifiers, ",");
			HttpGet get = new HttpGet(url);
			String response = executeAndGetResponse(get);
			List<TaskStatus> statuses = objectMapper.readValue(
					response,
					objectMapper.getTypeFactory().constructCollectionType(
							List.class, TaskStatus.class));
			Map<Long, TaskStatus> result = new HashMap<Long, TaskStatus>();
			for (int i = 0; i < identifiers.size(); ++i) {
				result.put(identifiers.get(i), statuses.get(i));
			}
			return result;
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public JsonData getTaskInput(long id) throws RestException {
		try {
			String url = getBaseUrl() + "/task/" + id + "/input";
			HttpGet get = new HttpGet(url);
			String response = executeAndGetResponse(get);
			return objectMapper.readValue(response, JsonData.class);
		} catch (Exception ex) {
			throw new RestException(ex);
		}
	}

	public String getTaskResultBlocking(Long taskId, long checkInterval)
			throws RestException {
		Optional<String> result;
		while (true) {
			result = getTaskResultIfDone(taskId);
			if (result.isPresent()) {
				return result.get();
			} else {
				try {
					Thread.sleep(checkInterval);
				} catch (InterruptedException e) {
					throw new RestException(
							"Thread was interrupted when waiting for task result",
							e);
				}
			}
		}
	}

	public Optional<String> getTaskResultIfDone(Long taskId)
			throws RestException {
		return getTasksResultsIfDone(Collections.singletonList(taskId)).get(0);
	}

	public List<Optional<String>> getTasksResultsIfDone(List<Long> identifiers)
			throws RestException {
		Map<Long, TaskStatus> idToStatus = getTasksStatuses(identifiers);
		List<Long> finishedTaskIdentifiers = identifiers.stream()
				.filter(id -> idToStatus.get(id).equals(TaskStatus.FINISHED))
				.collect(Collectors.toList());

		Map<Long, JsonData> idToResult = finishedTaskIdentifiers.isEmpty() ? null
				: getResults(finishedTaskIdentifiers);

		return identifiers
				.stream()
				.map(id -> {
					TaskStatus status = idToStatus.get(id);
					switch (status) {
					case ABORTED:
						throw new RuntimeException(
								"Computation cannot be finished because the task was aborted. Task id: "
										+ id);
					case FAILED:
						throw new RuntimeException(
								"Computation cannot be finished because the task failed. Task id: "
										+ id);
					case IN_PROGRESS:
						return Optional.<String> ofNullable(null);
					case FINISHED:
						return Optional.<String> of(idToResult.get(id)
								.getData());
					default:
						throw new IllegalStateException();
					}
				}).collect(Collectors.toList());
	}

	private void initializeHttpTools(String user, String password) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(getHost(), getPort()),
				new UsernamePasswordCredentials(user, password));

		HttpHost targetHost = new HttpHost(getHost(), getPort(), getProtocol());
		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);
		context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);
		httpClient = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider).build();

	}

	private String getHost() {
		return host;
	}

	private int getPort() {
		return port;
	}

	private String getProtocol() {
		return protocol;
	}

	private String getPrefix() {
		return prefix;
	}

	private String executeAndGetResponse(HttpUriRequest request)
			throws ClientProtocolException, IOException, RestException {
		CloseableHttpResponse response = httpClient.execute(request, context);
		String responseContent = IOUtils.toString(response.getEntity()
				.getContent(), "UTF-8");
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new RestException(
					"Rest response status is not OK. Response: "
							+ responseContent);
		}
		return responseContent;
	}

	private HttpPost createJsonPost(String url, String content) {
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(content, Charset.forName("UTF-8")));
		post.setHeader("Content-type", "application/json");
		return post;
	}

	private String getBaseUrl() {
		return String.format("%s://%s:%d/%s/api/internal/", getProtocol(),
				getHost(), getPort(), getPrefix());
	}
}
