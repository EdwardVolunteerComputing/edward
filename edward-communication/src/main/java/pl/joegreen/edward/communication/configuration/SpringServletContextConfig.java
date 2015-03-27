package pl.joegreen.edward.communication.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import pl.joegreen.edward.communication.controller.InternalRestController;
import pl.joegreen.edward.management.service.ExecutionManagerService;

@Configuration
@EnableWebMvc
@ComponentScan(basePackageClasses = { InternalRestController.class,
		ExecutionManagerService.class })
public class SpringServletContextConfig extends WebMvcConfigurerAdapter {

	@Bean
	public ViewResolver viewResolver() {
		InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
		viewResolver.setSuffix(".html");
		return viewResolver;
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		super.addViewControllers(registry);
		registry.addViewController("/").setViewName("index");
		registry.addRedirectViewController("", "/");
		registry.addViewController("/volunteer/").setViewName("index");
		registry.addRedirectViewController("/volunteer", "/volunteer/");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/volunteer/**").addResourceLocations(
				"/resources/volunteer/");
		registry.addResourceHandler("/**").addResourceLocations(
				"/resources/ui/");
	}
}
