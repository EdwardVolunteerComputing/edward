package pl.joegreen.edward.communication.configuration;

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
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import pl.joegreen.edward.communication.controller.InternalRestController;
import pl.joegreen.edward.management.service.ExecutionManagerService;
import pl.joegreen.edward.persistence.ExceptionTranslator;
import pl.joegreen.edward.persistence.dao.ProjectDao;

@Configuration
@EnableWebMvc
@EnableTransactionManagement
@ComponentScan(basePackageClasses = { InternalRestController.class,
		ProjectDao.class, ExecutionManagerService.class })
public class SpringServletContextConfig extends WebMvcConfigurerAdapter {

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

	@Bean
	public ViewResolver viewResolver() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setSuffix(".html");
		return viewResolver;
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		super.addViewControllers(registry);
		registry.addViewController("/").setViewName("index");
		registry.addViewController("/volunteer/").setViewName("index");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/volunteer/**").addResourceLocations(
				"/resources/volunteer/");
		registry.addResourceHandler("/**").addResourceLocations(
				"/resources/ui/");
	}
}
