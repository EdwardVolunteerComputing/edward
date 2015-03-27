package pl.joegreen.edward.persistence.configuration;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import pl.joegreen.edward.core.configuration.ConfigurationProvider;
import pl.joegreen.edward.core.configuration.Parameter;

@Configuration
@ComponentScan(basePackageClasses = { ConfigurationProvider.class })
public class ProductionDataSourceConfig {

	@Autowired
	private ConfigurationProvider configurationProvider;

	@Bean(destroyMethod = "close")
	public BasicDataSource dataSource() throws SQLException {
		BasicDataSource source = new BasicDataSource();
		source.setUrl(configurationProvider.getValue(Parameter.JDBC_URL));
		source.setDriverClassName("org.h2.Driver");
		source.setUsername(configurationProvider.getValue(Parameter.JDBC_USER));
		source.setPassword(configurationProvider
				.getValue(Parameter.JDBC_PASSWORD));
		return source;
	}
}
