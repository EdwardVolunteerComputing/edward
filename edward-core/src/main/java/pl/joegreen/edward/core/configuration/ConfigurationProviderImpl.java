package pl.joegreen.edward.core.configuration;

import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationProviderImpl implements ConfigurationProvider {
	private final static Logger LOG = LoggerFactory
			.getLogger(ConfigurationProviderImpl.class);
	public static final String EDWARD_CONFIGURATION_PROPERTY = "edward.config";

	private EnumMap<Parameter, String> parameterValuesFromFile = new EnumMap<Parameter, String>(
			Parameter.class);

	@PostConstruct
	public void initialize() {
		String systemPropertyPath = System
				.getProperty(EDWARD_CONFIGURATION_PROPERTY);
		if (systemPropertyPath != null) {
			Properties propertiesFromFile = new Properties();

			try {
				propertiesFromFile.load(new FileReader(systemPropertyPath));

				for (Object keyObject : propertiesFromFile.keySet()) {
					String key = (String) keyObject;
					for (Parameter parameter : Parameter.values()) {
						if (parameter.propertyName.equals(key)) {
							parameterValuesFromFile.put(parameter,
									(String) propertiesFromFile.get(key));
						}
					}
				}
			} catch (IOException e) {
				LOG.error("Cannot load properties from file "
						+ systemPropertyPath + " - defaults will be used", e);
			}
		}
	}

	@Override
	public String getValue(Parameter parameter) {
		String valueFromFile = parameterValuesFromFile.get(parameter);
		if (valueFromFile == null) {
			return parameter.defaultValue;
		} else {
			return valueFromFile;
		}
	}
}
