package pl.joegreen.edward.persistence.dao;

import java.util.List;

import org.jooq.Condition;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.Execution;
import pl.joegreen.edward.core.model.ExecutionStatus;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.ExecutionsRecord;

@Component
public class ExecutionDao extends EdwardDao<Execution, ExecutionsRecord> {

	public ExecutionDao() {
		super(Tables.EXECUTIONS, Tables.EXECUTIONS.ID, ExecutionsRecord.class);
	}

	@Override
	public void deleteInternal(Long id) throws ObjectDoesntExistException {
		// TODO:
	}

	@Override
	public Execution fromRecord(ExecutionsRecord record) {
		Execution execution = new Execution();
		execution.setId(record.getId());
		execution.setTaskId(record.getTaskId());
		execution.setOutputDataId(record.getOutputDataId());
		execution.setVolunteerId(record.getVolunteerId());
		execution.setStatus(ExecutionStatus.valueOf(record.getStatus()));
		execution.setCreationTime(record.getCreationTime());
		execution.setError(record.getError());
		return execution;
	}

	@Override
	public ExecutionsRecord modelToRecord(Execution model,
			ExecutionsRecord record) {
		record.setId(model.getId());
		record.setTaskId(model.getTaskId());
		record.setOutputDataId(model.getOutputDataId());
		record.setVolunteerId(model.getVolunteerId());
		record.setStatus(model.getStatus().name());
		record.setCreationTime(model.getCreationTime());
		record.setError(model.getError());
		return record;
	}

	public List<Execution> getExecutionsByTaskId(Long taskId) {
		return getByQuery(dslContext.select().from(Tables.EXECUTIONS)
				.where(Tables.EXECUTIONS.TASK_ID.eq(taskId)));
	}

	public void updateTimeoutStates(long timeout) {
		long createdBefore = System.currentTimeMillis() - timeout;
		Condition timeCondition = Tables.EXECUTIONS.CREATION_TIME
				.lessThan(createdBefore);
		Condition stateCondition = Tables.EXECUTIONS.STATUS
				.eq(ExecutionStatus.CREATED.toString());
		Condition timeoutCondition = timeCondition.and(stateCondition);
		dslContext.update(Tables.EXECUTIONS)
				.set(Tables.EXECUTIONS.STATUS, ExecutionStatus.TIMEOUT.name())
				.where(timeoutCondition).execute();
	}
}
