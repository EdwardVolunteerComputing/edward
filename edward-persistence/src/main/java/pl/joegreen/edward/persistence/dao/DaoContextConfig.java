package pl.joegreen.edward.persistence.dao;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ProjectDao.class)
public class DaoContextConfig {
}
