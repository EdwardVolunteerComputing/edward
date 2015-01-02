package pl.joegreen.edward.charles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import pl.joegreen.edward.charles.configuration.CodeReader;
import pl.joegreen.edward.charles.configuration.model.Configuration;
import pl.joegreen.edward.charles.configuration.model.Population;
import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.rest.client.RestClient;
import pl.joegreen.edward.rest.client.RestException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;

public class Charles {

	private static RestClient restClient = new RestClient("admin", "admin",
			"localhost", 8080);

	private final static org.slf4j.Logger logger = LoggerFactory
			.getLogger(Charles.class);
	private final static int RESULT_CHECK_INTERVAL_MS = 300;

	private final Configuration configuration;

	private Map<PhaseType, String> phaseCodes;

	private Long projectId;
	private Map<PhaseType, Long> phaseJobIds;

	private ScriptEngine engine;

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

		List<Population> populations = generatePopulationsLocally();
		for (int i = 0; i < configuration.metaIterationsCount; ++i) {
			logger.info("Performing meta iteration " + i);
			if (i > 0) {
				populations = migratePopulationsLocally(populations);
			}
			populations = improvePopulationsLocally(populations);
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

	private void initializeVolunteerComputingJobs() throws RestException {
		projectId = restClient.addProject("Charles Project", 1L).getId();
		phaseJobIds = new HashMap<PhaseType, Long>();
		for (PhaseType phaseType : PhaseType.values()) {
			if (configuration.getPhaseConfiguration(phaseType).useVolunteerComputing) {
				Long jobId = restClient.addJob(projectId,
						phaseType.toFunctionName(), phaseCodes.get(phaseType))
						.getId();
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
		System.out.println("Generating populations locally");
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

	private List<Population> improvePopulationsLocally(
			Collection<Population> populations) {
		System.out.println("Improving populations locally");
		ImmutableList.Builder<Population> listBuilder = new Builder<Population>();
		populations
				.stream()
				.map(population -> {
					Map<Object, Object> argument = addOptionsToArgument(
							population, "population", PhaseType.IMPROVE);
					try {
						System.out.println("Improving population");
						Population improvedPopulation = new Population(
								runFunctionLocally(PhaseType.IMPROVE, argument));
						return improvedPopulation;
					} catch (Exception e) {
						throw new RuntimeException(e);// TODO: handle
					}
				}).forEach(population -> listBuilder.add(population));
		return listBuilder.build();

	}

	private List<Population> migratePopulationsLocally(
			Collection<Population> populations) throws JsonParseException,
			JsonMappingException, ScriptException, IOException {
		System.out.println("Migrating populations locally");
		Map<Object, Object> argument = addOptionsToArgument(populations,
				"populations", PhaseType.MIGRATE);
		Map<Object, Object> result = runFunctionLocally(PhaseType.MIGRATE,
				argument);
		List<Map<Object, Object>> newPopulations = (List<Map<Object, Object>>) result
				.get("populations");
		return newPopulations.stream().map(asMap -> new Population(asMap))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	// private ImmutableList<String> generatePopulations() throws RestException,
	// JsonProcessingException, IOException, ScriptException {
	//
	// if (runGenerateLocally) {
	// System.out.println("Generating populations locally");
	// ImmutableList.Builder<String> listBuilder = new Builder<String>();
	// for (int i = 0; i < numberOfPopulations; ++i) {
	// listBuilder.add(runFunctionLocally("generate",
	// generatingCodeOptions));
	// }
	// return listBuilder.build();
	// }
	//
	// logger.info("Generating initial populations using Edward");
	// long jobId = restClient.addJob(projectId, "Generate individuals",
	// generatingCode).getId();
	//
	// JsonNodeFactory factory = JsonNodeFactory.instance;
	// ArrayNode array = factory.arrayNode();
	// for (int i = 0; i < numberOfPopulations; ++i) {
	// array.add(new ObjectMapper().readTree(generatingCodeOptions));
	// }
	// List<Long> taskIdentifiers = restClient.addTasks(jobId,
	// array.toString());
	// logger.info("Created populations generate tasks - waiting for results");
	// ImmutableList.Builder<String> listBuilder = new Builder<String>();
	// for (Long taskId : taskIdentifiers) {
	// logger.info("Initial population received from task " + taskId);
	// listBuilder.add(blockUntilResult(taskId));
	// }
	//
	// return listBuilder.build();
	// }
	//
	// private ImmutableList<String> performMetaIteration(
	// ImmutableList<String> populations) throws RestException,
	// JsonProcessingException, IOException, ScriptException {
	// populations = migrate(populations);
	// if (!runImprovementLocally) {
	// ImmutableList<Long> taskIdentifiers =
	// sendPopulationsToVolunteers(populations);
	// return getImprovedPopulations(taskIdentifiers, populations);
	// } else {
	// ImmutableList.Builder<String> builder = new
	// ImmutableList.Builder<String>();
	// JsonNodeFactory factory = JsonNodeFactory.instance;
	// for (String population : populations) {
	// System.out.println("Improving population locally");
	// ObjectNode object = factory.objectNode();
	// object.set(
	// "options",
	// factory.objectNode().set("iterations",
	// factory.numberNode(iterationsInMetaIteration)));
	// object.set("population",
	// new ObjectMapper().readTree(population));
	// builder.add(runFunctionLocally("improve", object.toString()));
	// }
	// return builder.build();
	// }
	// }
	//
	// private ImmutableList<Long> sendPopulationsToVolunteers(
	// ImmutableList<String> populations) throws RestException,
	// JsonProcessingException, IOException {
	// logger.info("Sending population improvements task to volunteers.");
	// JsonNodeFactory factory = JsonNodeFactory.instance;
	// ArrayNode array = factory.arrayNode();
	// for (String population : populations) {
	// ObjectNode object = factory.objectNode();
	// object.set(
	// "options",
	// factory.objectNode().set("iterations",
	// factory.numberNode(iterationsInMetaIteration)));
	// object.set("population", new ObjectMapper().readTree(population));
	// // TODO: // terribly //inefficient
	// array.add(object);
	// }
	//
	// return ImmutableList.copyOf(restClient.addTasks(improveJobId,
	// array.toString()));
	// }
	//
	// private ImmutableList<String> getImprovedPopulations(
	// ImmutableList<Long> taskIdentifiers,
	// ImmutableList<String> oldPopulations) throws RestException {
	// logger.info("Waiting for improved populations");
	// Map<Long, String> results = new HashMap<Long, String>();
	// long waitingStartTime = System.currentTimeMillis();
	// while (results.size() < taskIdentifiers.size()
	// && (System.currentTimeMillis() - waitingStartTime) <
	// maxMetaIterationTimeMs) {
	// for (Long taskIdentifier : taskIdentifiers) {
	// if (!results.containsKey(taskIdentifier)) {
	// Optional<String> result = returnTaskResultIfDone(taskIdentifier);
	// if (result.isPresent()) {
	// logger.info("Received improved population from task "
	// + taskIdentifier);
	// results.put(taskIdentifier, result.get());
	// }
	//
	// }
	// }
	// }
	// if (results.size() < taskIdentifiers.size()) {
	// logger.info("Lacking " + (taskIdentifiers.size() - results.size())
	// + " improved populations after waiting "
	// + (System.currentTimeMillis() - waitingStartTime)
	// + " ms. Using old populations instead.");
	// taskIdentifiers
	// .stream()
	// .filter(taskId -> !results.containsKey(taskId))
	// .forEach(
	// taskId -> results.put(taskId, oldPopulations
	// .get(taskIdentifiers.indexOf(taskId))));
	// }
	// return ImmutableList.copyOf(taskIdentifiers.stream()
	// .map(taskId -> results.get(taskId))
	// .collect(Collectors.toCollection(ArrayList::new)));
	// }
	//
	// private ImmutableList<String> migrate(ImmutableList<String> populations)
	// throws JsonProcessingException, IOException, RestException,
	// ScriptException {
	// logger.info("Migrating between populations");
	// JsonNodeFactory factory = JsonNodeFactory.instance;
	// ArrayNode populationsAsSingleArray = factory.arrayNode();
	// ObjectMapper mapper = new ObjectMapper();
	// ArrayList<JsonNode> populationNodes = populations.stream()
	// .map(populationString -> {
	// try {
	// return mapper.readTree(populationString);
	// } catch (Exception ex) {
	// throw new RuntimeException(ex);
	// }
	// }).collect(Collectors.toCollection(ArrayList::new));
	// populationsAsSingleArray.addAll(populationNodes);
	// ObjectNode migrationTaskArgument = mapper.createObjectNode();
	// migrationTaskArgument.put("populations", populationsAsSingleArray);
	// String migrationResult;
	// if (!runMigrationLocally) {
	// ArrayNode addTasksArgument = factory.arrayNode();
	// addTasksArgument.add(migrationTaskArgument);
	// List<Long> taskIdentifiers = restClient.addTasks(migrateJobId,
	// addTasksArgument.toString());
	// migrationResult = blockUntilResult(taskIdentifiers.get(0));
	// } else {
	// System.out.println("Migrating locally");
	// migrationResult = runFunctionLocally("migrate",
	// migrationTaskArgument.toString());
	// }
	// JsonNode newPopulationsNodesArray = mapper.readTree(migrationResult);
	// ArrayList<JsonNode> newPopulationNodes = new ArrayList<JsonNode>();
	// for (int i = 0; i < newPopulationsNodesArray.size(); ++i) {
	// newPopulationNodes.add(newPopulationsNodesArray.get(i));
	// }
	//
	// return ImmutableList.copyOf(newPopulationNodes.stream()
	// .map(node -> node.toString())
	// .collect(Collectors.toCollection(ArrayList::new)));
	// }

	private String blockUntilResult(Long taskId) throws RestException {
		Optional<String> result;
		while (true) {
			result = returnTaskResultIfDone(taskId);
			if (result.isPresent()) {
				return result.get();
			} else {
				try {
					Thread.sleep(RESULT_CHECK_INTERVAL_MS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e); // TODO:
				}
			}
		}
	}

	private Optional<String> returnTaskResultIfDone(Long taskId)
			throws RestException {
		List<JsonData> results = restClient.getResults(taskId);
		if (results.isEmpty()) {
			return Optional.ofNullable(null);
		} else {
			return Optional.of(results.get(0).getData());
		}
	}

	public static void printAsPrettyJson(Map<Object, Object> map) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			System.out.println(mapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(map));
		} catch (IOException ex) {
			System.out.println("Cannot parse json: " + map);
		}
	}

	public static void main(String[] args) throws IOException, RestException,
			ScriptException {
		Configuration configuration = Configuration
				.fromFile("C:/Users/joegreen/Desktop/Uczelnia/praca-magisterska/edward/edward-charles/src/main/java/pl/joegreen/edward/charles/configuration/model/config");
		long startTime = System.currentTimeMillis();
		Charles charles = new Charles(configuration);
		List<Population> populations = charles.calculate();
		for (int i = 0; i < populations.size(); ++i) {
			System.out.println("--- Population " + i + " ---");
			printAsPrettyJson(populations.get(i));
		}
		System.out.println("Time: " + (System.currentTimeMillis() - startTime)
				+ " ms");

	}
}
