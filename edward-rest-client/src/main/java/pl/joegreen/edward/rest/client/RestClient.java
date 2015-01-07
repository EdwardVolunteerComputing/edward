package pl.joegreen.edward.rest.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.core.model.Project;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.core.model.TaskStatus;
import pl.joegreen.edward.core.model.communication.IdContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

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

	public RestClient(String user, String password, String host, int port) {
		this(user, password, host, port, "http", "");
	}

	public IdContainer addProject(String name, long userId)
			throws RestException {
		try { // TODO: how about using dynamicproxy to rethrow the exceptions
				// instead of copyPaste?
			Project project = new Project();
			project.setName(name);
			project.setOwnerId(userId);
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

	public List<Long> addTasks(long jobId, String data, long priority,
			long concurrentExecutions) throws RestException {
		try {
			String url = getBaseUrl() + "/job/" + jobId + "/tasks/" + priority
					+ "/" + concurrentExecutions;
			HttpPost post = createJsonPost(url, data);
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
			String response = executeAndGetResponse(post);
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
			throws ClientProtocolException, IOException {
		CloseableHttpResponse response = httpClient.execute(request, context);
		return IOUtils.toString(response.getEntity().getContent(), "UTF-8");
	}

	private HttpPost createJsonPost(String url, String content) {
		HttpPost post = new HttpPost(url);
		post.setEntity(new StringEntity(content, Charset.forName("UTF-8")));
		post.setHeader("Content-type", "application/json");
		return post;
	}

	private String getBaseUrl() {
		return String.format("%s://%s:%d/%s", getProtocol(), getHost(),
				getPort(), getPrefix());
	}
}
