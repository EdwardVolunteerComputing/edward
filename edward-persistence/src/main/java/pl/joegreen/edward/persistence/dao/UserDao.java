package pl.joegreen.edward.persistence.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pl.joegreen.edward.core.model.Project;
import pl.joegreen.edward.core.model.User;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.UsersRecord;

@Component
public class UserDao extends EdwardDao<User, UsersRecord> {

	@Autowired
	private ProjectDao projectDao;

	public UserDao() {
		super(Tables.USERS, Tables.USERS.ID, UsersRecord.class);
	}

	@Override
	@Transactional
	public void deleteInternal(Long id) throws ObjectDoesntExistException {
		UsersRecord recordById = getRecordById(id);
		if (recordById == null) {
			throw new ObjectDoesntExistException(id, User.class);
		}
		List<Project> userProjects = projectDao.getProjectsByUserId(id);
		userProjects.forEach(user -> {
			try {
				projectDao.delete(user.getId());
			} catch (ObjectDoesntExistException e) {
				throw new IllegalStateException(
						"Error while deleting projects of user with id " + id,
						e);
			}
		});
		recordById.delete();

	}

	public User getByName(String name) {
		List<User> users = getByQuery(dslContext.select().from(Tables.USERS)
				.where(Tables.USERS.NAME.eq(name)));
		if (users.isEmpty()) {
			return null;
		} else {
			return users.get(0);
		}
	}

	@Override
	public User fromRecord(UsersRecord record) {
		User user = new User();
		user.setId(record.getId());
		user.setName(record.getName());
		user.setPassword(record.getPassword());
		return user;
	}

	@Override
	public UsersRecord modelToRecord(User model, UsersRecord record) {
		record.setId(model.getId());
		record.setName(model.getName());
		record.setPassword(model.getPassword());
		return record;
	}

}
