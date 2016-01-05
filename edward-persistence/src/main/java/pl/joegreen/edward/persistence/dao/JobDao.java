package pl.joegreen.edward.persistence.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.persistence.generated.Tables;
import pl.joegreen.edward.persistence.generated.tables.records.JobsRecord;

@Component
public class JobDao extends EdwardDao<Job, JobsRecord> {

	@Autowired
	private TaskDao taskDao;

	public JobDao() {
		super(Tables.JOBS, Tables.JOBS.ID, JobsRecord.class);
	}

	@Override
	public Job fromRecord(JobsRecord record) {
		if (record == null) {
			return null;
		}
		Job job = new Job();
		job.setId(record.getId());
		job.setName(record.getName());

		job.setProjectId(record.getProjectId());
		String code = record.getCode();

		job.setCode(code);

		return job;
	}

	@Override
	public JobsRecord modelToRecord(Job model, JobsRecord record) {
		record.setId(model.getId());
		record.setName(model.getName());
		record.setProjectId(model.getProjectId());
		record.setCode(model.getCode());
		return record;
	}

	public List<Job> getJobsByProjectId(Long projectId) {
		return getByQuery(
				dslContext
						.select()
						.from(Tables.JOBS)
						.where(Tables.JOBS.PROJECT_ID.eq(projectId)));
	}

	@Override
	public void deleteInternal(Long id) throws ObjectDoesntExistException {
		JobsRecord recordById = getRecordById(id);
		if (recordById == null) {
			throw new ObjectDoesntExistException(id, Job.class);
		}
		List<Task> tasksByJobId = taskDao.getTasksByJobId(id);
		tasksByJobId
				.forEach(task -> {
					try {
						taskDao.delete(task.getId());
					} catch (ObjectDoesntExistException e) {
						throw new IllegalStateException(
								"Error while deleting tasks using job with id "
										+ id, e);
					}
				});
		recordById.delete();
	}
}
