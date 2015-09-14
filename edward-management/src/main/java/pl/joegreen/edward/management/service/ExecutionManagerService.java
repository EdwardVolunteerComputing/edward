package pl.joegreen.edward.management.service;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.security.auth.login.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.configuration.ConfigurationProvider;
import pl.joegreen.edward.core.configuration.Parameter;
import pl.joegreen.edward.core.model.Execution;
import pl.joegreen.edward.core.model.ExecutionStatus;
import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.core.model.Task;
import pl.joegreen.edward.core.model.TaskStatus;
import pl.joegreen.edward.core.model.communication.ClientExecutionInfo;
import pl.joegreen.edward.persistence.dao.ExecutionDao;
import pl.joegreen.edward.persistence.dao.InvalidObjectException;
import pl.joegreen.edward.persistence.dao.JsonDataDao;
import pl.joegreen.edward.persistence.dao.ObjectDoesntExistException;
import pl.joegreen.edward.persistence.dao.TaskDao;
import pl.joegreen.edward.persistence.dao.VolunteerDao;

@Component
public class ExecutionManagerService {
	private final static Logger LOG = LoggerFactory
			.getLogger(ExecutionManagerService.class);

	@Autowired
	private TaskDao taskDao;

	@Autowired
	private VolunteerDao volunteerDao;

	@Autowired
	private JsonDataDao jsonDataDao;

	@Autowired
	private ExecutionDao executionDao;

	@Autowired
	private ConfigurationProvider configurationProvider;

	private Queue<Task> tasks = new ConcurrentLinkedQueue<Task>();
	private ReadWriteLock tasksLock = new ReentrantReadWriteLock(true);
	private Lock readTasksLock = tasksLock.readLock();
	private Lock writeTasksLock = tasksLock.writeLock();

	private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

	private static class TasksLoadingThread implements Runnable {

		private final Lock writeTasksLock;
		private final TaskDao taskDao;
		private final Queue<Task> tasks;

		private final static Logger LOGGER = LoggerFactory
				.getLogger(TasksLoadingThread.class);

		public TasksLoadingThread(Lock writeTasksLock, TaskDao taskDao,
				Queue<Task> tasks) {
			this.writeTasksLock = writeTasksLock;
			this.taskDao = taskDao;
			this.tasks = tasks;
		}

		@Override
		public void run() {
			LOGGER.debug("Loading new tasks from the database");
			try {
				writeTasksLock.lock();
				tasks.clear();
				List<Task> notAbortedAndWithoutOngoingOrFinishedExecutions = taskDao
						.getNotAbortedAndWithoutOngoingOrFinishedExecutions();

				List<Long> taskIdentifiers = notAbortedAndWithoutOngoingOrFinishedExecutions
						.stream().map(task -> task.getId())
						.collect(Collectors.toList());

				Map<Long, Integer> numberOfConcurrentExecutionsRunningForTasks = taskDao
						.getNumberOfConcurrentExecutionsRunningForTasks(taskIdentifiers);
				for (Task task : notAbortedAndWithoutOngoingOrFinishedExecutions) {
					long concurrentExecutionsCount = task
							.getConcurrentExecutionsCount();
					int runningExecutions = numberOfConcurrentExecutionsRunningForTasks
							.get(task.getId());
					long executionsLeft = concurrentExecutionsCount
							- runningExecutions;
					for (int i = 0; i < executionsLeft; ++i) {
						tasks.add(task);
					}
				}
				LOGGER.debug("Refreshed tasks queue, size: " + tasks.size());
			} catch (Throwable e) {
				LOGGER.error(
						"Tasks loading thread failed, new tasks will not be executed.",
						e);
			} finally {
				writeTasksLock.unlock();
			}
		}
	}

	@PostConstruct
	public void init() {
		scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
		scheduledThreadPoolExecutor.scheduleAtFixedRate(new TasksLoadingThread(
				writeTasksLock, taskDao, tasks), 0,
				configurationProvider.getValueAsLong(Parameter.TASK_REFRESH_INTERVAL_MS), TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void destroy() {
		LOG.debug("Destroying ExecutionManagerService - stopping thread");
		scheduledThreadPoolExecutor.shutdownNow();
	}

	public ClientExecutionInfo createNextExecutionForClient(long volunteerId) {
		try {
			readTasksLock.lock();
			Task taskWithoutExecutions = null;
			do {
				taskWithoutExecutions = tasks.poll();
			} while (taskWithoutExecutions != null
					&& taskDao.getTaskStatus(taskWithoutExecutions.getId()) == TaskStatus.FINISHED);
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
		} finally {
			readTasksLock.unlock();
		}
	}

	public void saveExecutionResult(Long executionId, String result) {
		try {
			JsonData jsonData = new JsonData(result);
			jsonDataDao.insert(jsonData);
			Execution execution = executionDao.getById(executionId);
			execution.setOutputDataId(jsonData.getId());
			execution.setStatus(ExecutionStatus.FINISHED);
			execution.setCompletionTime(System.currentTimeMillis());
			executionDao.update(execution);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void saveExecutionError(Long executionId, String error) {
		Execution execution = executionDao.getById(executionId);
		execution.setStatus(ExecutionStatus.FAILED);
		execution.setCompletionTime(System.currentTimeMillis());
		execution.setError(error);
		try {
			executionDao.update(execution);
		} catch (ObjectDoesntExistException | InvalidObjectException e) {
			// TODO: handle
			throw new RuntimeException(e);
		}
	}
}
