package pl.joegreen.edward.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.core.model.Task;

public class TaskDaoTest extends BaseDaoTest {

	@Test
	public void getTasksWithoutOngoingOrFinishedExecutions()
			throws InvalidObjectException, ObjectDoesntExistException {
		Job job = modelFixtures.createAndPersistTestJob();
		JsonData jsonData = new JsonData("14");
		jsonDataDao.save(jsonData);
		Task task = new Task();
		task.setJobId(job.getId());
		task.setInputDataId(jsonData.getId());
		taskDao.insert(task);

		List<Task> tasksWithoutExecutions = taskDao
				.getNotAbortedAndWithoutOngoingOrFinishedExecutions();
		assertEquals(task.getId(), tasksWithoutExecutions.get(0).getId());

		taskDao.abort(task.getId());
		tasksWithoutExecutions = taskDao
				.getNotAbortedAndWithoutOngoingOrFinishedExecutions();
		assertTrue(tasksWithoutExecutions.isEmpty());

	}
}
