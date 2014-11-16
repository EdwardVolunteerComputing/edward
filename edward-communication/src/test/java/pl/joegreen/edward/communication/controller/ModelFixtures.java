package pl.joegreen.edward.communication.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.core.model.Project;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.core.model.User;
import pl.joegreen.edward.persistence.dao.InvalidObjectException;
import pl.joegreen.edward.persistence.dao.JobDao;
import pl.joegreen.edward.persistence.dao.JsonDataDao;
import pl.joegreen.edward.persistence.dao.ProjectDao;
import pl.joegreen.edward.persistence.dao.TaskDao;
import pl.joegreen.edward.persistence.dao.UserDao;

@Component
public class ModelFixtures {

	@Autowired
	ProjectDao projectDao;
	@Autowired
	JobDao jobDao;
	@Autowired
	JsonDataDao dataDao;
	@Autowired
	TaskDao taskDao;
	@Autowired
	private UserDao userDao;

	private final static String testName = "TestName";

	public User createTestUser() {
		User user = new User();
		user.setName(testName);
		user.setPassword("password");
		return user;
	}

	public User createAndPersistTestUser() throws InvalidObjectException {
		User user = createTestUser();
		userDao.insert(user);
		return user;
	}

	public Project createTestProject() throws InvalidObjectException {
		Project project = new Project();
		project.setName(testName);
		project.setOwnerId(createAndPersistTestUser().getId());
		return project;
	}

	public Project createAndPersistTestProject() throws InvalidObjectException {
		Project project = createTestProject();
		projectDao.insert(project);
		return project;
	}

	public Task createTestTask() throws InvalidObjectException {
		Task task = new Task();
		task.setInputDataId(createAndPersistTestJsonData().getId());
		task.setJobId(createAndPersistTestJob().getId());
		return task;
	}

	public Task createAndPersistTestTask() throws InvalidObjectException {
		Task task = createTestTask();
		taskDao.insert(task);
		return task;
	}

	public JsonData createTestJsonData() throws InvalidObjectException {
		JsonData jsonData = new JsonData("{\"msg\" : \"test\"}");
		return jsonData;
	}

	public JsonData createAndPersistTestJsonData()
			throws InvalidObjectException {
		JsonData jsonData = createTestJsonData();
		dataDao.insert(jsonData);
		return jsonData;
	}

	public Job createTestJob() throws InvalidObjectException {
		Job job = new Job();
		job.setName(testName);
		job.setCode("testCode");
		job.setProjectId(createAndPersistTestProject().getId());
		return job;
	}

	public Job createAndPersistTestJob() throws InvalidObjectException {
		Job job = createTestJob();
		jobDao.insert(job);
		return job;
	}

}
