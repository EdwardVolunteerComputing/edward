package pl.joegreen.edward.communication.controller;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = InternalRestController.class)
public class ControllerContextConfig {
}
