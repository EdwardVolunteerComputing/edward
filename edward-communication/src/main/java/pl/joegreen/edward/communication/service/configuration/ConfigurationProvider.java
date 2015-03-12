package pl.joegreen.edward.communication.service.configuration;

import pl.joegreen.edward.communication.configuration.Parameter;


public interface ConfigurationProvider {

	String getValue(Parameter parameter);
}
