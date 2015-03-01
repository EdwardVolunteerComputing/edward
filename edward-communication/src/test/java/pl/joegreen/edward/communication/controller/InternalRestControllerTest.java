package pl.joegreen.edward.communication.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import pl.joegreen.edward.core.model.Execution;
import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.core.model.Project;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.core.model.communication.IdContainer;
import pl.joegreen.edward.persistence.dao.InvalidObjectException;
import pl.joegreen.edward.persistence.dao.ObjectDoesntExistException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class InternalRestControllerTest extends RestControllerTestBase {

	@Test
	public void testData() throws Exception {

		// object not present
		assertEquals(0, jsonDataDao.getAll().size());

		// create
		JsonData data = new JsonData("\"jsonString\"");
		Long dataId = createOrUpdateObjectAndGetId(DATA_URL, data);
		data.setId(dataId);
		assertEquals(1, jsonDataDao.getAll().size());

		// get
		getObjectAndExpect(DATA_URL, dataId, data);

		// delete
		deleteObject(DATA_URL, dataId, true);
		assertEquals(0, jsonDataDao.getAll().size());

		// object not present
		getObjectAndExpectNotToFindIt(DATA_URL, dataId);

		// delete non-existent object
		deleteObject(DATA_URL, dataId, false);

	}

	@Test
	public void nonJsonDataCannotBePersisted() throws Exception {
		JsonData data = new JsonData("nonJsonData");
		createOrUpdateObjectAndExpectBadRequest(DATA_URL, data);
	}

	@Test
	public void testProject() throws Exception {

		// object not present
		assertEquals(0, projectDao.getAll().size());

		Project project = modelFixtures.createTestProject();

		// create
		Long projectId = createOrUpdateObjectAndGetId(PROJECT_URL, project);
		assertEquals(1, projectDao.getAll().size());
		project.setId(projectId);

		// get
		getObjectAndExpect(PROJECT_URL, projectId, project);

		// update
		project.setName("projectName2");
		createOrUpdateObjectAndGetId(PROJECT_URL, project);
		assertEquals(1, projectDao.getAll().size());
		getObjectAndExpect(PROJECT_URL, projectId, project);

		// delete

		deleteObject(PROJECT_URL, projectId, true);
		assertEquals(0, projectDao.getAll().size());

		// object not present
		getObjectAndExpectNotToFindIt(PROJECT_URL, projectId);

		// delete non-existent object
		deleteObject(PROJECT_URL, projectId, false);

	}

	@Test
	public void testJob() throws Exception {

		// create some test projects

		Long testProjectId1 = modelFixtures.createAndPersistTestProject()
				.getId();

		Long testProjectId2 = modelFixtures.createAndPersistTestProject()
				.getId();
		// try to create job without project id

		Job job = new Job();
		job.setProjectId(null);
		job.setName("JobName");
		job.setCode("code");

		createOrUpdateObjectAndExpectBadRequest(JOB_URL, job);

		// try to create job with bad project id

		job.setProjectId(100L);

		createOrUpdateObjectAndExpectBadRequest(JOB_URL, job);

		// now create job connected to project successfully
		job.setProjectId(testProjectId1);

		Long jobId = createOrUpdateObjectAndGetId(JOB_URL, job);
		job.setId(jobId);
		getObjectAndExpect(JOB_URL, job.getId(), job);
		assertEquals(1, jobDao.getAll().size());

		// update it
		job.setName("name2");
		job.setCode("code2");
		job.setProjectId(testProjectId2);

		createOrUpdateObjectAndGetId(JOB_URL, job);
		getObjectAndExpect(JOB_URL, job.getId(), job);
		assertEquals(1, jobDao.getAll().size());

		// delete it

		deleteObject(JOB_URL, job.getId(), true);
		assertEquals(0, jobDao.getAll().size());

		// cannot delete again

		deleteObject(JOB_URL, job.getId(), false);

		// cannot update with the same id

		createOrUpdateObjectAndExpectBadRequest(JOB_URL, job);

		// create again

		job.setId(null);
		Long newId = createOrUpdateObjectAndGetId(JOB_URL, job);
		job.setId(newId);
		assertEquals(1, jobDao.getAll().size());

		// delete by deleting parent project

		projectDao.delete(testProjectId2);
		assertEquals(0, jobDao.getAll().size());

	}

	@Test
	public void testTask() throws Exception {
		assertEquals(0, taskDao.getAll().size());

		Job job = modelFixtures.createAndPersistTestJob();
		JsonData data = new JsonData("\"jsonString\"");
		jsonDataDao.insert(data);

		JsonData data2 = new JsonData("\"jsonString\"");
		jsonDataDao.insert(data2);

		Task task = new Task();

		// cannot add task without appropriate job and data

		createOrUpdateObjectAndExpectBadRequest(TASK_URL, task);

		// cannot add task with bad job and data ids

		task.setInputDataId(100L);
		task.setJobId(100L);

		createOrUpdateObjectAndExpectBadRequest(TASK_URL, task);

		// create task successfully
		task.setInputDataId(data.getId());
		task.setJobId(job.getId());

		Long taskId = createOrUpdateObjectAndGetId(TASK_URL, task);
		task.setId(taskId);
		assertEquals(1, taskDao.getAll().size());

		// get
		getObjectAndExpect(TASK_URL, taskId, task);

		// update
		task.setInputDataId(data2.getId());
		createOrUpdateObjectAndGetId(TASK_URL, task);
		getObjectAndExpect(TASK_URL, taskId, task);
		assertEquals(1, taskDao.getAll().size());

		// delete

		deleteObject(TASK_URL, taskId, true);
		assertEquals(0, taskDao.getAll().size());

		// it should also delete its data

		assertEquals(null, jsonDataDao.getById(data2.getId()));

		// cannot create with the same id
		createOrUpdateObjectAndExpectBadRequest(TASK_URL, task);

		// create again
		task.setId(null);
		task.setInputDataId(data.getId()); // data2 was deleted
		taskId = createOrUpdateObjectAndGetId(TASK_URL, task);
		task.setId(taskId);
		getObjectAndExpect(TASK_URL, taskId, task);

		// delete by deleting job

		jobDao.delete(job.getId());
		assertEquals(0, taskDao.getAll().size());

	}

	@Test
	// TODO: move to dao tests
	public void getTasksWithoutOngoingOrFinishedExecutions()
			throws InvalidObjectException, ObjectDoesntExistException {
		Job job = modelFixtures.createAndPersistTestJob();
		JsonData jsonData = new JsonData("14");
		jsonDataDao.save(jsonData);
		Task task = new Task();
		task.setJobId(job.getId());
		task.setInputDataId(jsonData.getId());
		taskDao.insert(task);

		Task taskWithoutExecutions = taskDao
				.getNotAbortedAndWithoutOngoingOrFinishedExecutions();
		assertEquals(task.getId(), taskWithoutExecutions.getId());

		taskDao.abort(task.getId());
		taskWithoutExecutions = taskDao
				.getNotAbortedAndWithoutOngoingOrFinishedExecutions();
		assertNull(taskWithoutExecutions);

	}

	@Test
	public void canAddObjectWithoutIdFieldInJson() throws Exception {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ObjectNode projectContentJson = factory.objectNode();
		projectContentJson.put("name", "projectName");
		projectContentJson.put("ownerId", modelFixtures
				.createAndPersistTestUser().getId());

		mockMvc.perform(
				post(PROJECT_URL).contentType(JSON).content(
						projectContentJson.toString())).andExpect(OK);
	}

	@Test
	public void testAddingBatchOfTasks() throws Exception {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode array = factory.arrayNode();

		Long longData = 2L;
		String stringData = "testString";
		Double doubleData = 2.0;
		array.add(longData);
		array.add(stringData);
		array.add(doubleData);

		Job testJob = modelFixtures.createAndPersistTestJob();
		String addBatchUrl = String.format(INTERNAL_API_URL_BASE
				+ "/job/%d/tasks/0/1", testJob.getId());
		mockMvc.perform(
				post(addBatchUrl).contentType(JSON).content(array.toString()))
				.andExpect(OK);

		List<JsonData> jsonData = jsonDataDao.getAll();
		assertEquals(longData.toString(), jsonData.get(0).getData());
		assertTrue(jsonData.get(1).getData().contains(stringData)); // string
																	// gets
																	// quoted in
																	// json
		assertEquals(doubleData.toString(), jsonData.get(2).getData());
	}

	@Test
	public void getProjectJobsTest() throws Exception {
		Project testProject = modelFixtures.createAndPersistTestProject();
		List<Job> jobs = new ArrayList<Job>();
		for (int i = 0; i < 100; ++i) {
			Job job = new Job();
			job.setCode("Code for job with number: " + i);
			job.setName("Job number: " + i);
			job.setProjectId(testProject.getId());
			jobDao.save(job);
			jobs.add(job);
		}
		String content = performGetAndReturnContent(PROJECT_URL
				+ idUrl(testProject.getId()) + "/jobs");
		List<Job> jobsFromApi = mapper.readValue(content, mapper
				.getTypeFactory()
				.constructCollectionType(List.class, Job.class));
		assertEquals(jobs, jobsFromApi);
	}

	@Test
	public void getTaskResultsTest() throws Exception {
		Task task = modelFixtures.createAndPersistTestTask();
		List<JsonData> outputs = new ArrayList<JsonData>();
		for (int i = 0; i < 100; ++i) {
			JsonData jsonData = new JsonData("\"Output for execution : " + i
					+ "\"");
			jsonDataDao.insert(jsonData);
			outputs.add(jsonData);
			Execution execution = new Execution();
			execution.setOutputDataId(jsonData.getId());
			execution.setTaskId(task.getId());
			execution
					.setVolunteerId(volunteerDao.getDefaultVolunteer().getId());
			executionDao.insert(execution);
		}
		String content = performGetAndReturnContent(TASK_URL
				+ idUrl(task.getId()) + "/results");
		List<JsonData> dataFromApi = mapper.readValue(
				content,
				mapper.getTypeFactory().constructCollectionType(List.class,
						JsonData.class));
		assertEquals(outputs, dataFromApi);
	}

	private void getObjectAndExpectNotToFindIt(String basicUrl, long id)
			throws Exception {
		getObjectAndExpect(basicUrl, id, null);
	}

	private void getObjectAndExpect(String basicUrl, long id,
			Object expectedObject) throws Exception {
		ResultActions getAction = mockMvc.perform(get(basicUrl + idUrl(id))
				.accept(JSON));
		if (expectedObject != null) {
			getAction.andExpect(OK).andExpect(
					contentEqualsByJson(expectedObject));
		} else {
			getAction.andExpect(NOT_FOUND);
		}
	}

	private Long createOrUpdateObjectAndGetId(String basicUrl, Object object)
			throws Exception {
		return createOrUpdateObjectAndGetIdOnSuccess(basicUrl, object, OK);
	}

	private void createOrUpdateObjectAndExpectBadRequest(String basicUrl,
			Object object) throws Exception {
		createOrUpdateObjectAndGetIdOnSuccess(basicUrl, object, BAD_REQUEST);
	}

	private Long createOrUpdateObjectAndGetIdOnSuccess(String basicUrl,
			Object object, ResultMatcher status) throws Exception {
		ResultActions actionResult = mockMvc.perform(
				post(basicUrl).contentType(JSON).content(
						mapper.writeValueAsString(object))).andExpect(status);
		if (status == OK) {
			return mapper
					.readValue(
							actionResult.andReturn().getResponse()
									.getContentAsString(), IdContainer.class)
					.getId();
		} else {
			return null;
		}
	}

	private void deleteObject(String basicUrl, long id,
			boolean shouldBeSuccessful) throws Exception {
		ResultActions actionResult = mockMvc.perform(delete(basicUrl
				+ idUrl(id)));
		if (shouldBeSuccessful) {
			actionResult.andExpect(OK).andExpect(EMPTY_CONTENT);
		} else {
			actionResult.andExpect(BAD_REQUEST);
		}

	}

}
