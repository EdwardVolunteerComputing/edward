package pl.joegreen.edward.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import pl.joegreen.edward.communication.controller.ControllerContextConfig;
import pl.joegreen.edward.core.configuration.ConfigurationProviderContextConfig;
import pl.joegreen.edward.management.service.ManagementContextConfig;
import pl.joegreen.edward.persistence.configuration.DataSourceConfig;
import pl.joegreen.edward.persistence.configuration.PersistenceContextConfiguration;
import pl.joegreen.edward.persistence.dao.DaoContextConfig;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        JooqAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class})
@Import({ConfigurationProviderContextConfig.class,
        PersistenceContextConfiguration.class,
        DataSourceConfig.class,
        DaoContextConfig.class,
        ControllerContextConfig.class,
        ManagementContextConfig.class})
public class Start extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(Start.class, args);
    }

}