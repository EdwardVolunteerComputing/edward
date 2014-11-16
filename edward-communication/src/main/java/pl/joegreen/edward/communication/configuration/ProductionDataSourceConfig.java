package pl.joegreen.edward.communication.configuration;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductionDataSourceConfig {

	@Bean(destroyMethod = "close")
	public BasicDataSource dataSource() throws SQLException {
		BasicDataSource source = new BasicDataSource();
		source.setUrl("jdbc:h2:tcp://localhost/~/test");
		source.setDriverClassName("org.h2.Driver");
		source.setUsername("alex");
		return source;
	}
}
