package pl.joegreen.edward.core.configuration;

public enum Parameter {
	JDBC_URL("jdbc.url", "jdbc:h2:./edward"),
	JDBC_USER("jdbc.user", ""),
	JDBC_PASSWORD("jdbc.password", ""),
	VOLUNTEER_HEARTBEAT_INTERVAL_MS("volunteer.heartbeat.interval", "10000"),
	EXECUTIONS_EXECUTOR_CHECK_INTERVAL_MS("execution.executorCheck.interval", "30000"),
	TASK_REFRESH_INTERVAL_MS("task.refresh.interval", "1000");

	public final String defaultValue;
	public final String propertyName;

	Parameter(String propertyName, String defaultValue) {
		this.propertyName = propertyName;
		this.defaultValue = defaultValue;
	}

}
