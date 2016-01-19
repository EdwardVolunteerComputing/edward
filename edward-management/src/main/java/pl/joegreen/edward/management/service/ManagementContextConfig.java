package pl.joegreen.edward.management.service;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ExecutionManagerService.class)
public class ManagementContextConfig {
}
