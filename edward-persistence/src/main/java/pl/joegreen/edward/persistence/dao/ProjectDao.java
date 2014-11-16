package pl.joegreen.edward.persistence.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.Project;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.ProjectsRecord;

@Component
public class ProjectDao extends EdwardDao<Project, ProjectsRecord> {

	@Autowired
	private JobDao jobDao;

	protected ProjectDao() {
		super(Tables.PROJECTS, Tables.PROJECTS.ID, ProjectsRecord.class);
	}

	@Override
	public Project fromRecord(ProjectsRecord record) {
		if (record == null) {
			return null;
		}
		Project project = new Project();
		project.setId(record.getId());
		project.setOwnerId(record.getOwnerId());
		project.setName(record.getName());
		return project;
	}

	@Override
	public ProjectsRecord modelToRecord(Project model, ProjectsRecord record) {
		record.setId(model.getId());
		record.setName(model.getName());
		record.setOwnerId(model.getOwnerId());
		return record;

	}

	public List<Project> getProjectsByUserId(Long ownerId) {
		return getByQuery(dslContext.select().from(Tables.PROJECTS)
				.where(Tables.PROJECTS.OWNER_ID.eq(ownerId)));
	}

	@Override
	@Transactional
	public void deleteInternal(Long id) throws ObjectDoesntExistException {
		ProjectsRecord recordById = getRecordById(id);
		if (recordById == null) {
			throw new ObjectDoesntExistException(id, Project.class);
		}
		List<Job> jobsByProjectId = jobDao.getJobsByProjectId(id);
		jobsByProjectId
				.forEach(job -> {
					try {
						jobDao.delete(job.getId());
					} catch (ObjectDoesntExistException e) {
						throw new IllegalStateException(
								"Error while deleting jobs using project with id "
										+ id, e);
					}
				});
		recordById.delete();

	}
}
