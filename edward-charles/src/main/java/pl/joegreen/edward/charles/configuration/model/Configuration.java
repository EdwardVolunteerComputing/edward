package pl.joegreen.edward.charles.configuration.model;

import java.io.File;
import java.io.IOException;

import pl.joegreen.edward.charles.PhaseType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Configuration {
	public final static ObjectMapper objectMapper = new ObjectMapper()
			.configure(Feature.ALLOW_COMMENTS, true).configure(
					Feature.ALLOW_SINGLE_QUOTES, true);
	public Integer metaIterationsCount;
	public Integer populationsCount;
	public Integer maxMetaIterationTime;
	public Integer priority;
	public Integer concurrentExecutions;

	public PhaseConfiguration generatePhase;
	public PhaseConfiguration improvePhase;
	public PhaseConfiguration migratePhase;

	private Configuration() {

	}

	public PhaseConfiguration getPhaseConfiguration(PhaseType type) {
		switch (type) {
		case GENERATE:
			return generatePhase;
		case IMPROVE:
			return improvePhase;
		case MIGRATE:
			return migratePhase;
		default:
			throw new IllegalArgumentException("Unknown phase: " + type);

		}
	}

	public boolean isAnyPhaseUsingVolunteerComputing() {
		for (PhaseType phaseType : PhaseType.values()) {
			if (getPhaseConfiguration(phaseType).useVolunteerComputing) {
				return true;
			}
		}
		return false;
	}

	public boolean isAnyPhaseUsingLocalEngine() {
		for (PhaseType phaseType : PhaseType.values()) {
			if (!getPhaseConfiguration(phaseType).useVolunteerComputing) {
				return true;
			}
		}
		return false;
	}

	public static Configuration fromFile(String path)
			throws JsonParseException, JsonMappingException, IOException {
		return fromFile(new File(path));
	}

	public static Configuration fromFile(File file) throws JsonParseException,
			JsonMappingException, IOException {
		return objectMapper.readValue(file, Configuration.class);
	}

	public boolean isValid() {
		return Utils.noNulls(metaIterationsCount, populationsCount,
				maxMetaIterationTime, generatePhase, improvePhase,
				migratePhase, priority, concurrentExecutions)
				&& generatePhase.isValid()
				&& improvePhase.isValid()
				&& migratePhase.isValid();
	}

	@Override
	public String toString() {
		return "Configuration [metaIterationsCount=" + metaIterationsCount
				+ ", populationsCount=" + populationsCount
				+ ", maxMetaIterationTime=" + maxMetaIterationTime
				+ ", priority=" + priority + ", concurrentExecutions="
				+ concurrentExecutions + ", generatePhase=" + generatePhase
				+ ", improvePhase=" + improvePhase + ", migratePhase="
				+ migratePhase + "]";
	}

}
