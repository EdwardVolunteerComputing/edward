package pl.joegreen.edward.persistence.configuration;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import pl.joegreen.edward.persistence.ExceptionTranslator;
import pl.joegreen.edward.persistence.dao.ProjectDao;
import pl.joegreen.edward.persistence.testkit.ModelFixtures;

@Configuration
@ComponentScan(basePackageClasses = { ProjectDao.class, ModelFixtures.class })
@EnableTransactionManagement
public class PersistenceContextConfiguration {

	@Autowired
	private DataSource dataSource;

	@Bean
	public DataSourceTransactionManager transactionManager() {
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(
				dataSource);
		return transactionManager;
	}

	@Bean
	public TransactionAwareDataSourceProxy transactionAwareDataSource() {
		return new TransactionAwareDataSourceProxy(dataSource);
	}

	@Bean
	public DataSourceConnectionProvider connectionProvider() {
		return new DataSourceConnectionProvider(transactionAwareDataSource());
	}

	@Bean
	public DSLContext dsl() {
		return new DefaultDSLContext(config());
	}

	@Bean
	public ExceptionTranslator exceptionTranslator() {
		return new ExceptionTranslator();
	}

	@Bean
	public DefaultConfiguration config() {
		DefaultConfiguration defaultConfiguration = new DefaultConfiguration();
		defaultConfiguration.set(connectionProvider());
		defaultConfiguration.set(new DefaultExecuteListenerProvider(
				exceptionTranslator()));
		defaultConfiguration.set(SQLDialect.H2);
		return defaultConfiguration;

	}
}
