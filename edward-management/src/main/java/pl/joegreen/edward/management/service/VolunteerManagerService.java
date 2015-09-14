package pl.joegreen.edward.management.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.joegreen.edward.core.configuration.ConfigurationProvider;
import pl.joegreen.edward.core.configuration.Parameter;
import pl.joegreen.edward.core.model.communication.VolunteerRegistrationResponse;
import pl.joegreen.edward.persistence.dao.ExecutionDao;
import pl.joegreen.edward.persistence.dao.VolunteerDao;

@Component
public class VolunteerManagerService {

    private final static Logger LOG = LoggerFactory
            .getLogger(VolunteerManagerService.class);
    private long heartbeatTimeoutMs;
    private long heartbeatIntervalMs;

    private final static long VOLUNTEER_COUNT_LOG_INTERVAL_MS = 20000L;
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            1);
    private SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private VolunteerDao volunteerDao;

    @Autowired
    private ExecutionDao executionDao;

    @Autowired
    private ConfigurationProvider configurationProvider;

    @PostConstruct
    private void initialize() {
        heartbeatIntervalMs = configurationProvider.getValueAsLong(Parameter.VOLUNTEER_HEARTBEAT_INTERVAL_MS);
        heartbeatTimeoutMs = heartbeatIntervalMs * 2;
        executor.scheduleAtFixedRate(this::disconnectIfNoHeartbeat,
                heartbeatIntervalMs, heartbeatIntervalMs,
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

    public VolunteerRegistrationResponse handleRegistration() {
        long id = generateIdForVolunteer();
        volunteerDao.addIfNotExist(id);
        handleHeartbeat(id);
        return new VolunteerRegistrationResponse(
                id, heartbeatIntervalMs);

    }

    public int getNumberOfConnectedVolunteers() {
        return connectedVolunteerToLastHeartbeatTime.size();
    }

    private void disconnectIfNoHeartbeat() {
        long currentTime = System.currentTimeMillis();
        List<Long> disconnectedVolunteers = connectedVolunteerToLastHeartbeatTime
                .entrySet()
                .stream()
                .filter(
                        entry -> (currentTime - entry.getValue()) > heartbeatTimeoutMs)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        disconnectedVolunteers.forEach(connectedVolunteerToLastHeartbeatTime::remove);
        disconnectedVolunteers.forEach(id -> LOG.info("Volunteer with id {} disconnected (no heartbeat)", id));
        disconnectedVolunteers.forEach(executionDao::timeoutExecutionForVolunteer);
    }

    private void logVolunteerCount() {
        LOG.info("Number of connected volunteers: {}",
                getNumberOfConnectedVolunteers());
    }

    private long generateIdForVolunteer() {
        return secureRandom.nextLong();
    }
}
