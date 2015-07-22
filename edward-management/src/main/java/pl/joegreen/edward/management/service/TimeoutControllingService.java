package pl.joegreen.edward.management.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.persistence.dao.ExecutionDao;

@Component
public class TimeoutControllingService {

	private volatile boolean interrupted = false;
	private TimeoutControllingThread thread;

	public static final long DEFAULT_TIMEOUT_MS = 5000;

	@Autowired
	private ExecutionDao executionDao;

	public boolean isInterrupted() {
		return interrupted;
	}

	@PreDestroy
	public void stopThread() {
		interrupted = true;
	}

	@PostConstruct
	public void startThread() {
		interrupted = false;
		thread = new TimeoutControllingThread(this);
		thread.start();
	}

	public void checkTimeouts() {
		executionDao.updateTimeoutStates(DEFAULT_TIMEOUT_MS);
	}

}
