package pl.joegreen.edward.core.model.communication;

public class VolunteerRegistrationResponse {

	private final long heartbeatIntervalMs;
	private final long volunteerId;

	public long getHeartbeatIntervalMs() {
		return heartbeatIntervalMs;
	}

	public long getVolunteerId() {
		return volunteerId;
	}

	public VolunteerRegistrationResponse(long volunteerId, long heartbeatIntervalMs) {
		this.heartbeatIntervalMs = heartbeatIntervalMs;
		this.volunteerId = volunteerId;
	}
}
