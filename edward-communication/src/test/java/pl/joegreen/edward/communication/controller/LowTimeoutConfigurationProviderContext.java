package pl.joegreen.edward.communication.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.joegreen.edward.core.configuration.ConfigurationProvider;
import pl.joegreen.edward.core.configuration.ConfigurationProviderImpl;
import pl.joegreen.edward.core.configuration.Parameter;
import pl.joegreen.edward.persistence.testkit.ModelFixtures;

@Configuration
public class LowTimeoutConfigurationProviderContext {

	@Bean
	public ConfigurationProvider configurationProvider() {
		return new ConfigurationProvider() {
			@Override
			public String getValue(Parameter parameter) {
				return parameter.defaultValue;
			}

			@Override
			public long getValueAsLong(Parameter parameter) {
				if(parameter==Parameter.EXECUTIONS_EXECUTOR_CHECK_INTERVAL_MS){
					return 1200;
				}
				return 300;
			}
		};
	}
}
