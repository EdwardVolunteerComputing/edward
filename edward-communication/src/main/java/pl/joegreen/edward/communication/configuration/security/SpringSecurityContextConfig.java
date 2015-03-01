package pl.joegreen.edward.communication.configuration.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pl.joegreen.edward.communication.service.authentication.AuthenticationService;

@EnableWebSecurity
@Configuration
// TODO: won't that load classes twice?
@ComponentScan(basePackageClasses = { CustomAuthenticationEntryPoint.class,
		AuthenticationService.class })
public class SpringSecurityContextConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

	@Autowired
	AuthenticationService authenticationService;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.exceptionHandling()
				.authenticationEntryPoint(customAuthenticationEntryPoint).and()
				.authorizeRequests().antMatchers("/**")
				.permitAll()
				//
				.antMatchers("/api/client/**")
				.permitAll()
				//
				.antMatchers("/api/internal/**").authenticated().and()
				.httpBasic().and()
				//
				.csrf().disable()
				//
				.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth)
			throws Exception {
		auth.userDetailsService(authenticationService).passwordEncoder(
				new BCryptPasswordEncoder());

	}

}
