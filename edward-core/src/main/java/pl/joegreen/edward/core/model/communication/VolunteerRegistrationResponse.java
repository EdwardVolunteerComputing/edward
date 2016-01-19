package pl.joegreen.edward.core.model.communication;

public class VolunteerRegistrationResponse {

	private long heartbeatIntervalMs;
	private String volunteerId;

	public long getHeartbeatIntervalMs() {
		return heartbeatIntervalMs;
	}

	public String getVolunteerId() {
		return volunteerId;
	}

	public VolunteerRegistrationResponse(long volunteerId, long heartbeatIntervalMs) {
		this.heartbeatIntervalMs = heartbeatIntervalMs;
		this.volunteerId = String.valueOf(volunteerId);
	}

	private VolunteerRegistrationResponse(){}
}
