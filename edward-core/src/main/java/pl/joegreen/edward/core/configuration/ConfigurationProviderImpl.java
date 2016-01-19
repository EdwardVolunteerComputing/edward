package pl.joegreen.edward.core.configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationProviderImpl implements ConfigurationProvider {
    private final Config config = ConfigFactory.load();

    @Override
    public String getValue(Parameter parameter) {
        try {
            return config.getString(parameter.propertyName);
        } catch (ConfigException ex) {
            return parameter.defaultValue;
        }
    }

    @Override
    public long getValueAsLong(Parameter parameter) {
        return Long.valueOf(getValue(parameter));
    }
}
