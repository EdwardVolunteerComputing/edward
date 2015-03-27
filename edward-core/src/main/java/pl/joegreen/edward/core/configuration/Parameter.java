package pl.joegreen.edward.core.configuration;

public enum Parameter {
	JDBC_URL("jdbc.url", "jdbc:h2:tcp://localhost/~/edward"), //
	JDBC_USER("jdbc.user", "edward"), //
	JDBC_PASSWORD("jdbc.password", "edward");

	public final String defaultValue;
	public final String propertyName;

	Parameter(String propertyName, String defaultValue) {
		this.propertyName = propertyName;
		this.defaultValue = defaultValue;
	}

}
