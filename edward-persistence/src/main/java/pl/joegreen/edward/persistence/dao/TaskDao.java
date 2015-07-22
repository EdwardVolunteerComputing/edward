package pl.joegreen.edward.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectHavingConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.Execution;
import pl.joegreen.edward.core.model.ExecutionStatus;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.core.model.TaskStatus;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.TasksRecord;

@Component
public class TaskDao extends EdwardDao<Task, TasksRecord> {

	private static final int MAX_TIMEOUTS = 5;

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

	public void abort(Long id) {
		Task task = getById(id);
		task.setAborted(true);
		try {
			update(task);
		} catch (ObjectDoesntExistException | InvalidObjectException e) {
			throw new RuntimeException(e);
		}
	}

	public TaskStatus getTaskStatus(Long taskId) {
		Task task = getById(taskId);
		if (task.isAborted()) {
			return TaskStatus.ABORTED;
		}
		List<Execution> executionsByTaskId = executionDao
				.getExecutionsByTaskId(taskId);
		if (executionsByTaskId.size() == 0) {
			return TaskStatus.IN_PROGRESS;
		} else {
			if (executionsByTaskId.stream().anyMatch(
					execution -> execution.getStatus().equals(
							ExecutionStatus.FINISHED))) {
				return TaskStatus.FINISHED;
			}
			if (executionsByTaskId.stream().anyMatch(
					execution -> execution.getStatus().equals(
							ExecutionStatus.CREATED))) {
				return TaskStatus.IN_PROGRESS;
			}
			if (executionsByTaskId.stream().anyMatch(
					execution -> execution.getStatus().equals(
							ExecutionStatus.FAILED))) {
				return TaskStatus.FAILED;
			}
			// at this moment there may be no executions or only timeouted
			// executions; if there are none or there are timeouts but not many,
			// it's still "in progress", otherwise it's "failed" because of too
			// many timeouts
			long timeoutedExecutions = executionsByTaskId
					.stream()
					.filter(execution -> execution.getStatus().equals(
							ExecutionStatus.TIMEOUT)).count();
			if (timeoutedExecutions >= MAX_TIMEOUTS) {
				return TaskStatus.FAILED;
			} else {
				return TaskStatus.IN_PROGRESS;
			}
		}
	}

	public Map<Long, Integer> getNumberOfConcurrentExecutionsRunningForTasks(
			List<Long> taskIdentifiers) {
		Result<Record2<Long, Object>> fetchedResults = dslContext
				.select(Tables.TASKS.ID,
						dslContext
								.selectCount()
								.from(Tables.EXECUTIONS)
								.where(Tables.EXECUTIONS.TASK_ID.eq(
										Tables.TASKS.ID).and(
										Tables.EXECUTIONS.STATUS
												.eq(ExecutionStatus.CREATED
														.toString())))
								.asField()).from(Tables.TASKS)
				.where(Tables.TASKS.ID.in(taskIdentifiers)).fetch();

		Map<Long, Integer> taskIdToRunningExecutions = new HashMap<Long, Integer>();
		for (Record2<Long, Object> fetchedResult : fetchedResults) {
			taskIdToRunningExecutions.put(fetchedResult.value1(),
					(Integer) fetchedResult.value2());
		}
		return taskIdToRunningExecutions;
	}

	public List<Task> getNotAbortedAndWithoutOngoingOrFinishedExecutions() {
		Condition stateOfExecutionFinishesTaskCondition = Tables.EXECUTIONS.STATUS
				.in(ExecutionStatus.FINISHED.name(),
						ExecutionStatus.FAILED.name());

		SelectHavingConditionStep<Record1<Long>> identifiersOfTasksThatHaveMaxConcurrentExecutionsRunning = dslContext
				.selectDistinct(Tables.TASKS.ID)
				.from(Tables.TASKS)
				.join(Tables.EXECUTIONS)
				.on(Tables.TASKS.ID.eq(Tables.EXECUTIONS.TASK_ID))
				.where(Tables.EXECUTIONS.STATUS.eq(ExecutionStatus.CREATED
						.toString()))
				.groupBy(Tables.EXECUTIONS.TASK_ID)
				.having(DSL.count().greaterOrEqual(
						Tables.TASKS.CONCURRENT_EXECUTIONS_COUNT));

		SelectHavingConditionStep<Record1<Long>> identifiersOfTasksWithTooManyTimeouts = dslContext
				.selectDistinct(Tables.TASKS.ID)
				.from(Tables.TASKS)
				.join(Tables.EXECUTIONS)
				.on(Tables.TASKS.ID.eq(Tables.EXECUTIONS.TASK_ID))
				.where(Tables.EXECUTIONS.STATUS.eq(ExecutionStatus.TIMEOUT
						.toString())).groupBy(Tables.EXECUTIONS.TASK_ID)
				.having(DSL.count().greaterOrEqual(MAX_TIMEOUTS));

		SelectConditionStep<Record1<Long>> identifiersOfTasksWithFinishedExecutions = dslContext
				.selectDistinct(Tables.TASKS.ID).from(Tables.TASKS)
				.join(Tables.EXECUTIONS)
				.on(Tables.TASKS.ID.eq(Tables.EXECUTIONS.TASK_ID))
				.where(stateOfExecutionFinishesTaskCondition);

		Condition notAborted = Tables.TASKS.ABORTED.eq(false);

		List<Task> tasks = getByQuery(dslContext
				.selectDistinct()
				.from(Tables.TASKS)
				.where(Tables.TASKS.ID
						.notIn(identifiersOfTasksWithFinishedExecutions)
						.and(Tables.TASKS.ID
								.notIn(identifiersOfTasksThatHaveMaxConcurrentExecutionsRunning))
						.and(Tables.TASKS.ID
								.notIn(identifiersOfTasksWithTooManyTimeouts))
						.and(notAborted))
				.orderBy(Tables.TASKS.PRIORITY.desc(),
						Tables.TASKS.CREATION_TIME.asc()));
		return tasks;
	}
}
