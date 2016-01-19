package pl.joegreen.edward.core.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ConfigurationProvider.class)
public class ConfigurationProviderContextConfig {
}
