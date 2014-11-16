package pl.joegreen.edward.communication.controller;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AddModelFixturesConfiguration {

	public ModelFixtures modelFixtures() {
		return new ModelFixtures();
	}
}
