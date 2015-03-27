package pl.joegreen.edward.communication.controller;

import org.springframework.context.annotation.Configuration;

import pl.joegreen.edward.persistence.testkit.ModelFixtures;

@Configuration
public class AddModelFixturesConfiguration {

	public ModelFixtures modelFixtures() {
		return new ModelFixtures();
	}
}
