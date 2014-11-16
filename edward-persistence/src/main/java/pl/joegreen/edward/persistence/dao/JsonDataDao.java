package pl.joegreen.edward.persistence.dao;

import java.util.List;

import org.jooq.JoinType;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.DataRecord;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonDataDao extends EdwardDao<JsonData, DataRecord> {

	public JsonDataDao() {
		super(Tables.DATA, Tables.DATA.ID, DataRecord.class);
	}

	private void validateAsJson(String inputData) throws InvalidObjectException {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.readTree(inputData);
		} catch (Exception ex) {
			throw new InvalidObjectException(
					"Data is not in a valid JSON format: " + inputData, ex);
		}
	}

	@Override
	public void update(JsonData model) throws ObjectDoesntExistException,
			InvalidObjectException {
		validateAsJson(model.getData());
		super.update(model);
	}

	@Override
	public void insert(JsonData model) throws InvalidObjectException {
		validateAsJson(model.getData());
		super.insert(model);
	}

	@Override
	public JsonData fromRecord(DataRecord record) {
		if (record == null) {
			return null;
		}
		JsonData stringData = new JsonData(record.getData());
		stringData.setId(record.getId());
		return stringData;
	}

	@Override
	public DataRecord modelToRecord(JsonData model, DataRecord record) {
		record.setId(model.getId());
		record.setData(model.getData());
		return record;
	}

	@Override
	public void deleteInternal(Long id) throws ObjectDoesntExistException {
		DataRecord recordById = getRecordById(id);
		if (recordById == null) {
			throw new ObjectDoesntExistException(id, JsonData.class);
		}
		recordById.delete();
	}

	public JsonData getResultByExecutionId(Long executionId) {
		List<JsonData> results = getByQuery(dslContext.select()
				.from(Tables.DATA).join(Tables.EXECUTIONS, JoinType.JOIN)
				.on(Tables.EXECUTIONS.OUTPUT_DATA_ID.eq(Tables.DATA.ID))
				.where(Tables.EXECUTIONS.OUTPUT_DATA_ID.eq(executionId)));
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}

	public List<JsonData> getResultsByTaskId(Long taskId) {
		return getByQuery( //
		dslContext.select().from(Tables.DATA)
				.join(Tables.EXECUTIONS, JoinType.JOIN)
				.on(Tables.EXECUTIONS.OUTPUT_DATA_ID.eq(Tables.DATA.ID))
				.join(Tables.TASKS, JoinType.JOIN)
				.on(Tables.EXECUTIONS.TASK_ID.eq(Tables.TASKS.ID))
				.where(Tables.TASKS.ID.eq(taskId)));
	}

	public JsonData getInputByTaskId(Long taskId) {
		List<JsonData> results = getByQuery( //
		dslContext.select().from(Tables.DATA).join(Tables.TASKS, JoinType.JOIN)
				.on(Tables.TASKS.INPUT_DATA_ID.eq(Tables.DATA.ID))
				.where(Tables.TASKS.ID.eq(taskId)));
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}
}
