package pl.joegreen.edward.management.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.model.communication.VolunteerRegistrationResponse;
import pl.joegreen.edward.persistence.dao.VolunteerDao;

@Component
public class VolunteerManagerService {

	private final static Logger LOG = LoggerFactory
			.getLogger(VolunteerManagerService.class);
	private final static long HEARTBEAT_TIMEOUT_MS = 10000L;
	private final static long VOLUNTEER_HEARTBEAT_INTERVAL_MS = HEARTBEAT_TIMEOUT_MS / 2;
	private final static long VOLUNTEER_COUNT_LOG_INTERVAL_MS = 20000L;
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
			1);

	@Autowired
	private VolunteerDao volunteerDao;

	@PostConstruct
	private void initialize() {
		executor.scheduleAtFixedRate(this::disconnectIfNoHeartbeat,
				HEARTBEAT_TIMEOUT_MS, HEARTBEAT_TIMEOUT_MS,
				TimeUnit.MILLISECONDS);
		executor.scheduleAtFixedRate(this::logVolunteerCount,
				VOLUNTEER_COUNT_LOG_INTERVAL_MS,
				VOLUNTEER_COUNT_LOG_INTERVAL_MS, TimeUnit.MILLISECONDS);
	}

	private final ConcurrentHashMap<Long, Long> connectedVolunteerToLastHeartbeatTime = new ConcurrentHashMap<>();

	public void handleHeartbeat(long volunteerId) {
		connectedVolunteerToLastHeartbeatTime.put(volunteerId,
				System.currentTimeMillis());
	}

	public VolunteerRegistrationResponse handleRegistration(long volunteerId) {
		volunteerDao.addIfNotExist(volunteerId);
		return new VolunteerRegistrationResponse(
				VOLUNTEER_HEARTBEAT_INTERVAL_MS);

	}

	public int getNumberOfConnectedVolunteers() {
		return connectedVolunteerToLastHeartbeatTime.size();
	}

	private void disconnectIfNoHeartbeat() {
		long currentTime = System.currentTimeMillis();
		connectedVolunteerToLastHeartbeatTime
				.entrySet()
				.removeIf(
						entry -> {
							boolean shouldBeRemoved = currentTime
									- entry.getValue() > HEARTBEAT_TIMEOUT_MS;
							if (shouldBeRemoved) {
								LOG.info(
										"Volunteer with id {} disconnected (no heartbeat)",
										entry.getKey());
							}
							return shouldBeRemoved;
						});
	}

	private void logVolunteerCount() {
		LOG.info("Number of connected volunteers: {}",
				getNumberOfConnectedVolunteers());
	}
}
