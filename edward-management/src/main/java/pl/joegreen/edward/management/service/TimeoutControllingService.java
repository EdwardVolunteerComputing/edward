package pl.joegreen.edward.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.joegreen.edward.persistence.dao.ExecutionDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class TimeoutControllingService {

	private volatile boolean interrupted = false;
	private TimeoutControllingThread thread;

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
		executionDao.updateTimeoutStates();
	}

}
