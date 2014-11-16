package pl.joegreen.edward.persistence.dao;

import java.util.List;

import org.jooq.Condition;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.Execution;
import pl.joegreen.edward.core.model.ExecutionStatus;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.TasksRecord;

@Component
public class TaskDao extends EdwardDao<Task, TasksRecord> {

	@Autowired
	private JsonDataDao dataDao;

	@Autowired
	private ExecutionDao executionDao;

	public TaskDao() {
		super(Tables.TASKS, Tables.TASKS.ID, TasksRecord.class);
	}

	@Override
	public Task fromRecord(TasksRecord record) {
		return record.into(Task.class);
	}

	@Override
	public TasksRecord modelToRecord(Task model, TasksRecord record) {
		record.from(model);
		return record;
	}

	public List<Task> getTasksByJobId(Long jobId) {
		return getByQuery(dslContext.select().from(Tables.TASKS)
				.where(Tables.TASKS.JOB_ID.eq(jobId)));

	}

	@Override
	public void deleteInternal(Long id) throws ObjectDoesntExistException {
		TasksRecord recordById = getRecordById(id);
		if (recordById == null) {
			throw new ObjectDoesntExistException(id, Task.class);
		}
		List<Execution> executionsByTaskId = executionDao
				.getExecutionsByTaskId(id);
		executionsByTaskId.forEach(execution -> {
			try {
				executionDao.delete(execution.getId());
			} catch (ObjectDoesntExistException e) {
				throw new IllegalStateException(
						"Error while deleting executions using task with id "
								+ id, e);
			}
		});
		recordById.delete();
		Long inputDataId = recordById.getInputDataId();
		dataDao.delete(inputDataId);
	}

	public Task getWithoutOngoingOrFinishedExecutions() {
		Condition stateOfExecutionFinishesTaskCondition = Tables.EXECUTIONS.STATUS
				.in(ExecutionStatus.FINISHED.name(),
						ExecutionStatus.CREATED.name(),
						ExecutionStatus.FAILED.name());

		SelectConditionStep<Record1<Long>> identifiersOfTasksWeDontWantSend = dslContext
				.selectDistinct(Tables.TASKS.ID).from(Tables.TASKS)
				.join(Tables.EXECUTIONS)
				.on(Tables.TASKS.ID.eq(Tables.EXECUTIONS.TASK_ID))
				.where(stateOfExecutionFinishesTaskCondition);

		List<Task> tasks = getByQuery(dslContext.selectDistinct()
				.from(Tables.TASKS)
				.where(Tables.TASKS.ID.notIn(identifiersOfTasksWeDontWantSend)));
		if (tasks.isEmpty()) {
			return null;
		} else {
			return tasks.get(0);
		}
	}
}
