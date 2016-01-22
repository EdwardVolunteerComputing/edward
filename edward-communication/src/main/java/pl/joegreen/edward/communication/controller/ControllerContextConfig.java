package pl.joegreen.edward.communication.controller;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import pl.joegreen.edward.communication.controller.configuration.ServerConfiguration;
import pl.joegreen.edward.communication.services.VersionProvider;

@Configuration
@ComponentScan(basePackageClasses = {InternalRestController.class, ServerConfiguration.class, VersionProvider.class})
public class ControllerContextConfig {
}
