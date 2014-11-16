package pl.joegreen.edward.communication.configuration;

import java.io.InputStreamReader;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.tools.RunScript;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestDataSourceConfig {

	@Bean(destroyMethod = "close")
	public BasicDataSource dataSource() throws SQLException {
		BasicDataSource source = new BasicDataSource();
		source.setUrl("jdbc:h2:mem:test");
		source.setDriverClassName("org.h2.Driver");
		source.setUsername("alex");
		RunScript.execute(source.getConnection(), new InputStreamReader(
				ProductionDataSourceConfig.class.getClassLoader()
						.getResourceAsStream("createSchema.sql")));
		return source;
	}

}
