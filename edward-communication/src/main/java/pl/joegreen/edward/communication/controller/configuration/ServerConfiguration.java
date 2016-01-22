package pl.joegreen.edward.communication.controller.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.joegreen.edward.core.configuration.ConfigurationProvider;
import pl.joegreen.edward.core.configuration.Parameter;

@Configuration
public class ServerConfiguration {

    @Autowired
    private ConfigurationProvider configurationProvider;

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return (container -> container.setPort(configurationProvider.getValueAsInt(Parameter.PORT)));
    }
}