package pl.joegreen.edward.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.Execution;
import pl.joegreen.edward.core.model.ExecutionStatus;
import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.core.model.communication.ClientExecutionInfo;
import pl.joegreen.edward.persistence.dao.ExecutionDao;
import pl.joegreen.edward.persistence.dao.InvalidObjectException;
import pl.joegreen.edward.persistence.dao.JsonDataDao;
import pl.joegreen.edward.persistence.dao.ObjectDoesntExistException;
import pl.joegreen.edward.persistence.dao.TaskDao;
import pl.joegreen.edward.persistence.dao.VolunteerDao;

@Component
public class ExecutionManagerService {

	@Autowired
	private TaskDao taskDao;

	@Autowired
	private VolunteerDao volunteerDao;

	@Autowired
	private JsonDataDao jsonDataDao;

	@Autowired
	private ExecutionDao executionDao;

	public ClientExecutionInfo createNextExecutionForClient(long volunteerId) {
		try {
			Task taskWithoutExecutions = taskDao
					.getWithoutOngoingOrFinishedExecutions();
			if (taskWithoutExecutions == null) {
				return null;
			}

			volunteerDao.addIfNotExist(volunteerId);
			Execution execution = new Execution();
			execution.setVolunteerId(volunteerId);
			execution.setTaskId(taskWithoutExecutions.getId());
			executionDao.insert(execution);

			JsonData taskInputData = jsonDataDao.getById(taskWithoutExecutions
					.getInputDataId());
			return new ClientExecutionInfo(taskWithoutExecutions.getJobId(),
					taskInputData.getData(), execution.getId());
		} catch (Exception ex) {
			throw new RuntimeException(ex); // TODO: handle
		}
	}

	public void saveExecutionResult(Long executionId, String result) {
		try {
			JsonData jsonData = new JsonData(result);
			jsonDataDao.insert(jsonData);
			Execution execution = executionDao.getById(executionId);
			execution.setOutputDataId(jsonData.getId());
			execution.setStatus(ExecutionStatus.FINISHED);
			executionDao.update(execution);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void saveExecutionError(Long executionId, String error) {
		Execution execution = executionDao.getById(executionId);
		execution.setStatus(ExecutionStatus.FAILED);
		execution.setError(error);
		try {
			executionDao.update(execution);
		} catch (ObjectDoesntExistException | InvalidObjectException e) {
			// TODO: handle
			throw new RuntimeException(e);
		}
	}
}
