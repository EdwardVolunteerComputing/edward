package pl.joegreen.edward.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.ResultQuery;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;
import org.jooq.impl.TableImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import pl.joegreen.edward.core.model.IdentifierProvider;

public abstract class EdwardDao<EdwardModel extends IdentifierProvider, EdwardRecord extends UpdatableRecord<EdwardRecord>> {

	private TableImpl<EdwardRecord> table;
	private TableField<EdwardRecord, Long> idField;
	private Class<EdwardRecord> edwardRecordClass;
	private String managedRecordName;

	private final static Logger logger = LoggerFactory
			.getLogger(EdwardDao.class);

	public EdwardDao(TableImpl<EdwardRecord> table,
			TableField<EdwardRecord, Long> idField,
			Class<EdwardRecord> edwardRecordClass) {
		this.table = table;
		this.idField = idField;
		this.edwardRecordClass = edwardRecordClass;
		managedRecordName = edwardRecordClass.getSimpleName();
	}

	@Autowired
	protected DSLContext dslContext;

	public EdwardModel getById(Long id) {
		logger.debug("Searching by ID: {} - {}", id, managedRecordName);
		EdwardRecord recordById = getRecordById(id);
		if (recordById == null) {
			return null;
		}
		return fromRecord(recordById);
	}

	protected EdwardRecord getRecordById(Long id) {
		Record record = dslContext.select().from(table).where(idField.eq(id))
				.fetchOne();
		if (record != null) {
			return record.into(edwardRecordClass);
		} else {
			return null;
		}
	}

	public void save(EdwardModel edwardModel)
			throws ObjectDoesntExistException, InvalidObjectException {
		if (edwardModel.getId() == null) {
			insert(edwardModel);
		} else {
			update(edwardModel);
		}

	}

	protected List<EdwardRecord> getAllRecords() {
		return dslContext.select().from(table).fetch()
				.into(edwardRecordClass);
	}

	public List<EdwardModel> getAll() {
		return getAllRecords().stream().map(this::fromRecord)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public void update(EdwardModel model) throws ObjectDoesntExistException,
			InvalidObjectException {
		logger.debug("Updating by ID: {} - {}", model.getId(),
				managedRecordName);
		EdwardRecord recordById = getRecordById(model.getId());
		if (recordById == null) {
			throw new ObjectDoesntExistException(model.getId(),
					model.getClass());
		}
		modelToRecord(model, recordById);
		recordById.store();
	}

	public void insert(EdwardModel model) throws InvalidObjectException {
		logger.debug("Inserting object - {}", managedRecordName);
		EdwardRecord newRecord = dslContext.newRecord(table);
		modelToRecord(model, newRecord);
		newRecord.store();
		model.setId(newRecord.getValue(idField));
	}

	public final void delete(Long id) throws ObjectDoesntExistException {
		logger.debug("Deleting by ID: {} - {}", id, managedRecordName);
		deleteInternal(id);
	}

	public abstract void deleteInternal(Long id)
			throws ObjectDoesntExistException;

	public abstract EdwardModel fromRecord(EdwardRecord record);

	public List<EdwardModel> fromRecord(List<EdwardRecord> list) {
		return list.stream().map(this::fromRecord)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	protected List<EdwardModel> getByQuery(ResultQuery<Record> query) {
		return fromRecord(query.fetch().into(edwardRecordClass));
	}

	public abstract EdwardRecord modelToRecord(EdwardModel model,
			EdwardRecord record);

}
