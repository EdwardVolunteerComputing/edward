package pl.joegreen.edward.persistence.dao;

import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.Volunteer;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.VolunteersRecord;

@Component
public class VolunteerDao extends EdwardDao<Volunteer, VolunteersRecord> {

	private final static long DEFAULT_VOLUNTEER_ID = 1L;

	public VolunteerDao() {
		super(Tables.VOLUNTEERS, Tables.VOLUNTEERS.ID, VolunteersRecord.class);
	}

	@Override
	public void deleteInternal(Long id) throws ObjectDoesntExistException {
		VolunteersRecord recordById = getRecordById(id);
		if (recordById == null) {
			throw new ObjectDoesntExistException(id, Volunteer.class);
		}
		recordById.delete();
	}

	@Override
	public Volunteer fromRecord(VolunteersRecord record) {
		Volunteer volunteer = new Volunteer();
		volunteer.setId(record.getId());
		return volunteer;
	}

	@Override
	public VolunteersRecord modelToRecord(Volunteer model,
			VolunteersRecord record) {
		record.setId(model.getId());
		return record;
	}

	public Volunteer getDefaultVolunteer() {
		return getById(DEFAULT_VOLUNTEER_ID);
	}

}
