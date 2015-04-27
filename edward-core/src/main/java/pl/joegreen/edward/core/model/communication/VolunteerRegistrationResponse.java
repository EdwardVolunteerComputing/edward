package pl.joegreen.edward.core.model.communication;

public class VolunteerRegistrationResponse {

	private long heartbeatIntervalMs;

	public long getHeartbeatIntervalMs() {
		return heartbeatIntervalMs;
	}

	public VolunteerRegistrationResponse(long heartbeatIntervalMs) {
		this.heartbeatIntervalMs = heartbeatIntervalMs;
	}
}
