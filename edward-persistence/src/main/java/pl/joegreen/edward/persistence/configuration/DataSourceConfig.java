package pl.joegreen.edward.persistence.configuration;

import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import pl.joegreen.edward.core.configuration.ConfigurationProvider;
import pl.joegreen.edward.core.configuration.Parameter;

import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Configuration
@ComponentScan(basePackageClasses = {ConfigurationProvider.class})
public class DataSourceConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataSourceConfig.class);

    @Autowired
    private ConfigurationProvider configurationProvider;

    @Bean(destroyMethod = "close")
    public BasicDataSource dataSource() throws SQLException {
        BasicDataSource source = createDataSource();
        boolean schemaCreated = isSchemaCreated(source);
        if (!schemaCreated) {
            createSchema(source);
        }
        return source;
    }

    private BasicDataSource createDataSource() {
        BasicDataSource source = new BasicDataSource();
        source.setUrl(configurationProvider.getValue(Parameter.JDBC_URL));
        LOGGER.info("Creating a data source with JDBC URL  = {}", source.getUrl());
        source.setDriverClassName("org.h2.Driver");
        source.setUsername(configurationProvider.getValue(Parameter.JDBC_USER));
        source.setPassword(configurationProvider
                .getValue(Parameter.JDBC_PASSWORD));
        return source;
    }

    private boolean isSchemaCreated(BasicDataSource source) {
        try {
            PreparedStatement testSchemaStatement = source.getConnection().prepareStatement("select * from edward_properties");
            ResultSet resultSet = testSchemaStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    private void createSchema(BasicDataSource source) throws SQLException {
        LOGGER.info("Edward schema not found in the database, creating a new database schema");
        RunScript.execute(source.getConnection(), new InputStreamReader(
                DataSourceConfig.class.getClassLoader()
                        .getResourceAsStream("createSchema.sql")));
    }
}
