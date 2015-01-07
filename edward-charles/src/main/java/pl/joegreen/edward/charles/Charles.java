package pl.joegreen.edward.charles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.LoggerFactory;

import pl.joegreen.edward.charles.communication.EdwardApiWrapper;
import pl.joegreen.edward.charles.configuration.CodeReader;
import pl.joegreen.edward.charles.configuration.model.Configuration;
import pl.joegreen.edward.charles.configuration.model.Population;
import pl.joegreen.edward.rest.client.RestException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

public class Charles {

	private final static org.slf4j.Logger logger = LoggerFactory
			.getLogger(Charles.class);
	private final Configuration configuration;

	private EdwardApiWrapper edwardApiWrapper = new EdwardApiWrapper();

	private Map<PhaseType, String> phaseCodes;

	private Long projectId;
	private Map<PhaseType, Long> phaseJobIds;

	private ScriptEngine engine;

	private ObjectMapper objectMapper = new ObjectMapper();

	public Charles(Configuration configuration) {
		this.configuration = configuration;
		String generateCode = CodeReader
				.readCode(
						configuration.getPhaseConfiguration(PhaseType.GENERATE).codeFiles,
						PhaseType.GENERATE);
		String improveCode = CodeReader
				.readCode(
						configuration.getPhaseConfiguration(PhaseType.IMPROVE).codeFiles,
						PhaseType.IMPROVE);
		String migrateCode = CodeReader
				.readCode(
						configuration.getPhaseConfiguration(PhaseType.MIGRATE).codeFiles,
						PhaseType.MIGRATE);
		phaseCodes = ImmutableMap.of(PhaseType.GENERATE, generateCode,
				PhaseType.IMPROVE, improveCode, PhaseType.MIGRATE, migrateCode);
	}

	public List<Population> calculate() throws RestException,
			JsonProcessingException, IOException, ScriptException {
		if (configuration.isAnyPhaseUsingLocalEngine()) {
			initializeLocalJavaScriptEngine();
		}
		if (configuration.isAnyPhaseUsingVolunteerComputing()) {
			initializeVolunteerComputingJobs();
		}

		List<Population> populations = configuration.generatePhase.useVolunteerComputing ? generatePopulationsRemotely()
				: generatePopulationsLocally();
		for (int i = 0; i < configuration.metaIterationsCount; ++i) {
			logger.info("Performing meta iteration " + i);
			if (i > 0) {
				populations = configuration.migratePhase.useVolunteerComputing ? migratePopulationsRemotely(populations)
						: migratePopulationsLocally(populations);
			}
			populations = configuration.improvePhase.useVolunteerComputing ? improvePopulationsRemotely(populations)
					: improvePopulationsLocally(populations);
		}
		return populations;
	}

	private void initializeLocalJavaScriptEngine() throws ScriptException {
		ScriptEngineManager manager = new ScriptEngineManager();
		engine = manager.getEngineByName("nashorn");
		for (PhaseType phaseType : PhaseType.values()) {
			if (!configuration.getPhaseConfiguration(phaseType).useVolunteerComputing) {
				initializePhaseCodeInEngine(phaseType, engine);
			}
		}
	}

	private void initializeVolunteerComputingJobs() {
		projectId = edwardApiWrapper.createProjectAndGetId("Charles Project");
		phaseJobIds = new HashMap<PhaseType, Long>();
		for (PhaseType phaseType : PhaseType.values()) {
			if (configuration.getPhaseConfiguration(phaseType).useVolunteerComputing) {
				Long jobId = edwardApiWrapper.createJobAndGetId(projectId,
						phaseType.toFunctionName(), phaseCodes.get(phaseType));
				phaseJobIds.put(phaseType, jobId);
			}
		}
	}

	private void initializePhaseCodeInEngine(PhaseType phaseType,
			ScriptEngine engine) throws ScriptException {
		CompiledScript compiled = ((Compilable) engine).compile(phaseCodes
				.get(phaseType));
		engine.put(phaseType.toFunctionName(), compiled.eval());
	}

	private Map<Object, Object> runFunctionLocally(PhaseType phaseType,
			Map<Object, Object> input) throws ScriptException,
			JsonParseException, JsonMappingException, IOException {
		engine.put("input", new ObjectMapper().writeValueAsString(input));
		engine.eval("input = JSON.parse(input)");
		String result = (String) engine.eval("JSON.stringify("
				+ phaseType.toFunctionName() + "(input))");
		return new ObjectMapper().readValue(result, Map.class);
	}

	private Map<Object, Object> addOptionsToArgument(Object argument,
			String argumentName, PhaseType phaseType) {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put(argumentName, argument);
		map.put("parameters",
				configuration.getPhaseConfiguration(phaseType).parameters);
		return map;
	}

	private List<Population> generatePopulationsLocally()
			throws ScriptException, JsonParseException, JsonMappingException,
			IOException {
		logger.info("Generating populations locally");
		ImmutableList.Builder<Population> listBuilder = new Builder<Population>();
		for (int i = 0; i < configuration.populationsCount; ++i) {
			Map<Object, Object> phaseParameters = configuration
					.getPhaseConfiguration(PhaseType.GENERATE).parameters;
			Population generatedPopulation = new Population(runFunctionLocally(
					PhaseType.GENERATE, phaseParameters));
			listBuilder.add(generatedPopulation);
		}
		return listBuilder.build();
	}

	private List<Population> generatePopulationsRemotely()
			throws RestException, JsonProcessingException, IOException,
			ScriptException {

		logger.info("Generating initial populations using Edward");

		ArrayList<Map<Object, Object>> inputs = new ArrayList<Map<Object, Object>>();
		for (int i = 0; i < configuration.populationsCount; ++i) {
			inputs.add(configuration.getPhaseConfiguration(PhaseType.GENERATE).parameters);
		}
		List<Long> taskIdentifiers = edwardApiWrapper.addTasks(
				phaseJobIds.get(PhaseType.GENERATE), inputs,
				configuration.priority, configuration.concurrentExecutions);
		logger.info("Created populations generate tasks - waiting for results");

		ImmutableList.Builder<Population> listBuilder = new Builder<Population>();
		for (Long taskId : taskIdentifiers) {
			logger.info("Initial population received from task " + taskId);
			listBuilder.add(new Population(objectMapper.readValue(
					edwardApiWrapper.blockUntilResult(taskId), Map.class)));
		}

		return listBuilder.build();
	}

	private List<Population> improvePopulationsLocally(
			Collection<Population> populations) {
		logger.info("Improving populations locally");
		ImmutableList.Builder<Population> listBuilder = new Builder<Population>();
		populations
				.stream()
				.map(population -> {
					Map<Object, Object> argument = addOptionsToArgument(
							population, "population", PhaseType.IMPROVE);
					try {
						logger.info("Improving next population");
						Population improvedPopulation = new Population(
								runFunctionLocally(PhaseType.IMPROVE, argument));
						return improvedPopulation;
					} catch (Exception e) {
						throw new RuntimeException(e);// TODO: handle
					}
				}).forEach(population -> listBuilder.add(population));
		return listBuilder.build();
	}

	private List<Population> improvePopulationsRemotely(
			List<Population> populations) throws JsonProcessingException,
			RestException, IOException {
		List<Long> taskIdentifiers = sendPopulationsToVolunteers(populations);
		return getImprovedPopulations(taskIdentifiers, populations);
	}

	private List<Long> sendPopulationsToVolunteers(
			Collection<Population> populations) {
		logger.info("Sending population improvements task to volunteers.");
		ArrayList<Map<Object, Object>> arguments = populations
				.stream()
				.map(population -> {
					return addOptionsToArgument(population, "population",
							PhaseType.IMPROVE);
				}).collect(Collectors.toCollection(ArrayList::new));
		return edwardApiWrapper.addTasks(phaseJobIds.get(PhaseType.IMPROVE),
				arguments, configuration.priority,
				configuration.concurrentExecutions);
	}

	private List<Population> getImprovedPopulations(List<Long> taskIdentifiers,
			List<Population> oldPopulations) throws RestException,
			JsonParseException, JsonMappingException, IOException {
		logger.info("Waiting for improved populations");
		Map<Long, Population> results = new HashMap<Long, Population>();
		long waitingStartTime = System.currentTimeMillis();
		while (results.size() < taskIdentifiers.size()
				&& (System.currentTimeMillis() - waitingStartTime) < configuration.maxMetaIterationTime) {
			retrieveImprovedPopulations(taskIdentifiers, results);
		}
		if (results.size() < taskIdentifiers.size()) {
			logger.info("Lacking " + (taskIdentifiers.size() - results.size())
					+ " improved populations after waiting "
					+ (System.currentTimeMillis() - waitingStartTime)
					+ " ms. Using old populations instead.");
			useOldPopulationWhereNoImprovedYet(taskIdentifiers, oldPopulations,
					results);
		}
		return ImmutableList.copyOf(taskIdentifiers.stream()
				.map(taskId -> results.get(taskId))
				.collect(Collectors.toCollection(ArrayList::new)));
	}

	private void retrieveImprovedPopulations(List<Long> taskIdentifiers,
			Map<Long, Population> results) throws RestException, IOException,
			JsonParseException, JsonMappingException {
		List<Long> identifiersToGet = taskIdentifiers.stream()
				.filter(taskId -> !results.containsKey(taskId))
				.collect(Collectors.toList());
		List<Optional<String>> intermediateResults = edwardApiWrapper
				.returnTaskResultsIfDone(identifiersToGet);
		for (int i = 0; i < identifiersToGet.size(); ++i) {
			Long taskIdentifier = identifiersToGet.get(i);
			Optional<String> result = intermediateResults.get(i);
			if (result.isPresent()) {
				logger.info("Received improved population from task "
						+ taskIdentifier);
				results.put(
						taskIdentifier,
						new Population(objectMapper.readValue(result.get(),
								Map.class)));

			}
		}
	}

	private void useOldPopulationWhereNoImprovedYet(List<Long> taskIdentifiers,
			List<Population> oldPopulations, Map<Long, Population> results) {
		taskIdentifiers
				.stream()
				.filter(taskId -> !results.containsKey(taskId))
				.forEach(
						taskId -> results.put(taskId, oldPopulations
								.get(taskIdentifiers.indexOf(taskId))));
		try {
			edwardApiWrapper.abortTasks(taskIdentifiers);
		} catch (RestException e) {
			logger.warn("Cannot abort some tasks ", e);
			// do not stop execution
		}
	}

	private List<Population> migratePopulationsLocally(
			Collection<Population> populations) throws JsonParseException,
			JsonMappingException, ScriptException, IOException {
		logger.info("Migrating populations locally");
		Map<Object, Object> argument = addOptionsToArgument(populations,
				"populations", PhaseType.MIGRATE);
		Map<Object, Object> result = runFunctionLocally(PhaseType.MIGRATE,
				argument);
		List<Map<Object, Object>> newPopulations = (List<Map<Object, Object>>) result
				.get("populations");
		return newPopulations.stream().map(asMap -> new Population(asMap))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private List<Population> migratePopulationsRemotely(
			Collection<Population> populations) throws JsonProcessingException,
			IOException, RestException, ScriptException {
		logger.info("Migrating between populations using Edward");

		Map<Object, Object> migrateArgument = addOptionsToArgument(populations,
				"populations", PhaseType.MIGRATE);
		List<Long> taskIdentifiers = edwardApiWrapper.addTasks(
				phaseJobIds.get(PhaseType.MIGRATE),
				Collections.singletonList(migrateArgument),
				configuration.priority, configuration.concurrentExecutions);
		String migrationResult = edwardApiWrapper
				.blockUntilResult(taskIdentifiers.get(0));
		return ((List<Map>) objectMapper.readValue(migrationResult, Map.class)
				.get("populations")).stream().map(Population::new)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public static void printAsPrettyJson(Map<Object, Object> map) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			logger.info(mapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(map));
		} catch (IOException ex) {
			logger.error("Cannot parse json: " + map);
		}
	}

	public static void main(String[] args) throws IOException, RestException,
			ScriptException {
		// Configuration configuration = Configuration
		// .fromFile("C:/Users/joegreen/Desktop/Uczelnia/praca-magisterska/charles/labs/config");
		Configuration configuration = Configuration
				.fromFile("C:/Users/joegreen/Desktop/Uczelnia/praca-magisterska/charles/rastrigin/config");
		if (!configuration.isValid()) {
			throw new RuntimeException("Configuration is invalid: "
					+ configuration.toString());
		}
		ArrayList<Long> times = new ArrayList<Long>();
		for (int i = 0; i < 1; ++i) {
			long startTime = System.currentTimeMillis();
			Charles charles = new Charles(configuration);
			List<Population> populations = charles.calculate();
			for (int population = 0; i < populations.size(); ++i) {
				logger.info("--- Population " + i + " ---");
				printAsPrettyJson(populations.get(i));
			}
			Long time = System.currentTimeMillis() - startTime;
			times.add(time);
			logger.info("Time: " + time + " ms");
		}
		logger.info("All times: " + times);
	}
}
