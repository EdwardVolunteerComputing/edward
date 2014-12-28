package pl.joegreen.edward.charles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import pl.joegreen.edward.core.model.JsonData;
import pl.joegreen.edward.rest.client.RestClient;
import pl.joegreen.edward.rest.client.RestException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class Charles {

	private static RestClient restClient = new RestClient("admin", "admin",
			"localhost", 8080);

	private final static org.slf4j.Logger logger = LoggerFactory
			.getLogger(Charles.class);
	private final static int RESULT_CHECK_INTERVAL_MS = 300;

	private String generatingCode;
	private String evolutionCode;
	private String migrationCode;
	private boolean runGenerateLocally;
	private boolean runImprovementLocally;
	private boolean runMigrationLocally;
	private int numberOfPopulations;
	private int metaIterations;
	private int iterationsInMetaIteration;
	private int maxMetaIterationTimeMs;

	private Long projectId;
	private Long evolutionJobId;
	private Long migrationJobId;

	private String projectName;

	private String generatingCodeOptions;

	private ScriptEngineManager manager = new ScriptEngineManager();
	private ScriptEngine engine = manager.getEngineByName("nashorn");

	public Charles(String generatingCodeFile, String evolutionCodeFile,
			String migrationCodeFile, String generatingCodeOptions,
			boolean runGenerateLocally, boolean runImprovementLocally,
			boolean runMigrationLocally, int numberOfPopulations,
			int metaIterations, int iterationsInMetaIteration,
			int maxMetaIterationTimeMs, String projectName) throws IOException {
		this.generatingCodeOptions = generatingCodeOptions;
		this.runGenerateLocally = runGenerateLocally;
		this.runImprovementLocally = runImprovementLocally;
		this.runMigrationLocally = runMigrationLocally;
		this.numberOfPopulations = numberOfPopulations;
		this.metaIterations = metaIterations;
		this.iterationsInMetaIteration = iterationsInMetaIteration;
		this.maxMetaIterationTimeMs = maxMetaIterationTimeMs;
		this.projectName = projectName;
		readCodes(generatingCodeFile, evolutionCodeFile, migrationCodeFile);
	}

	private void readCodes(String generatingCodeFile, String evolutionCodeFile,
			String migrationCodeFile) throws IOException {
		// TODO: encodings
		generatingCode = FileUtils
				.readFileToString(new File(generatingCodeFile));
		evolutionCode = FileUtils.readFileToString(new File(evolutionCodeFile));
		migrationCode = FileUtils.readFileToString(new File(migrationCodeFile));
	}

	public List<String> calculate() throws RestException,
			JsonProcessingException, IOException, ScriptException {
		defineAppropriateCodesInEngine();
		projectId = restClient.addProject(projectName, 1L).getId();
		evolutionJobId = restClient.addJob(projectId, "ImprovePopulationJob",
				evolutionCode).getId();
		migrationJobId = restClient.addJob(projectId,
				"MigrateBetweenPopulationsJob", migrationCode).getId();
		ImmutableList<String> populations = generatePopulations();
		for (int i = 0; i < metaIterations; ++i) {
			logger.info("Performing meta iteration " + i);
			populations = performMetaIteration(populations);
		}
		return populations;
	}

	private void defineAppropriateCodesInEngine() throws ScriptException {
		if (runGenerateLocally) {
			String finalGenerationCode = "var generate = function(input){"
					+ generatingCode + "}";
			engine.eval(finalGenerationCode);
		}
		if (runImprovementLocally) {
			String finalImprovementCode = "var improve = function(input){"
					+ evolutionCode + "}";
			engine.eval(finalImprovementCode);
		}
		if (runMigrationLocally) {
			String finalMigrationCode = "var migrate = function(input){"
					+ migrationCode + "}";
			engine.eval(finalMigrationCode);
		}

	}

	private String runFunctionLocally(String functionName, String input)
			throws ScriptException {
		engine.put("input", input);
		return engine.eval(
				"JSON.stringify(" + functionName + "(JSON.parse(input)))")
				.toString();
	}

	private ImmutableList<String> generatePopulations() throws RestException,
			JsonProcessingException, IOException, ScriptException {

		if (runGenerateLocally) {
			System.out.println("Generating populations locally");
			ImmutableList.Builder<String> listBuilder = new Builder<String>();
			for (int i = 0; i < numberOfPopulations; ++i) {
				listBuilder.add(runFunctionLocally("generate",
						generatingCodeOptions));
			}
			return listBuilder.build();
		}

		logger.info("Generating initial populations using Edward");
		long jobId = restClient.addJob(projectId, "Generate individuals",
				generatingCode).getId();

		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode array = factory.arrayNode();
		for (int i = 0; i < numberOfPopulations; ++i) {
			array.add(new ObjectMapper().readTree(generatingCodeOptions));
		}
		List<Long> taskIdentifiers = restClient.addTasks(jobId,
				array.toString());
		logger.info("Created populations generate tasks - waiting for results");
		ImmutableList.Builder<String> listBuilder = new Builder<String>();
		for (Long taskId : taskIdentifiers) {
			logger.info("Initial population received from task " + taskId);
			listBuilder.add(blockUntilResult(taskId));
		}

		return listBuilder.build();
	}

	private ImmutableList<String> performMetaIteration(
			ImmutableList<String> populations) throws RestException,
			JsonProcessingException, IOException, ScriptException {
		populations = migrate(populations);
		if (!runImprovementLocally) {
			ImmutableList<Long> taskIdentifiers = sendPopulationsToVolunteers(populations);
			return getImprovedPopulations(taskIdentifiers, populations);
		} else {
			ImmutableList.Builder<String> builder = new ImmutableList.Builder<String>();
			JsonNodeFactory factory = JsonNodeFactory.instance;
			for (String population : populations) {
				System.out.println("Improving population locally");
				ObjectNode object = factory.objectNode();
				object.set("iterations",
						factory.numberNode(iterationsInMetaIteration));
				object.set("population",
						new ObjectMapper().readTree(population));
				builder.add(runFunctionLocally("improve", object.toString()));
			}
			return builder.build();
		}
	}

	private ImmutableList<Long> sendPopulationsToVolunteers(
			ImmutableList<String> populations) throws RestException,
			JsonProcessingException, IOException {
		logger.info("Sending population improvements task to volunteers.");
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode array = factory.arrayNode();
		for (String population : populations) {
			ObjectNode object = factory.objectNode();
			object.set("iterations",
					factory.numberNode(iterationsInMetaIteration));
			object.set("population", new ObjectMapper().readTree(population)); // TODO:
																				// terribly
																				// inefficient
			array.add(object);
		}

		return ImmutableList.copyOf(restClient.addTasks(evolutionJobId,
				array.toString()));
	}

	private ImmutableList<String> getImprovedPopulations(
			ImmutableList<Long> taskIdentifiers,
			ImmutableList<String> oldPopulations) throws RestException {
		logger.info("Waiting for improved populations");
		Map<Long, String> results = new HashMap<Long, String>();
		long waitingStartTime = System.currentTimeMillis();
		while (results.size() < taskIdentifiers.size()
				&& (System.currentTimeMillis() - waitingStartTime) < maxMetaIterationTimeMs) {
			for (Long taskIdentifier : taskIdentifiers) {
				if (!results.containsKey(taskIdentifier)) {
					Optional<String> result = returnTaskResultIfDone(taskIdentifier);
					if (result.isPresent()) {
						logger.info("Received improved population from task "
								+ taskIdentifier);
						results.put(taskIdentifier, result.get());
					}

				}
			}
		}
		if (results.size() < taskIdentifiers.size()) {
			logger.info("Lacking " + (taskIdentifiers.size() - results.size())
					+ " improved populations after waiting "
					+ (System.currentTimeMillis() - waitingStartTime)
					+ " ms. Using old populations instead.");
			taskIdentifiers
					.stream()
					.filter(taskId -> !results.containsKey(taskId))
					.forEach(
							taskId -> results.put(taskId, oldPopulations
									.get(taskIdentifiers.indexOf(taskId))));
		}
		return ImmutableList.copyOf(taskIdentifiers.stream()
				.map(taskId -> results.get(taskId))
				.collect(Collectors.toCollection(ArrayList::new)));
	}

	private ImmutableList<String> migrate(ImmutableList<String> populations)
			throws JsonProcessingException, IOException, RestException,
			ScriptException {
		logger.info("Migrating between populations");
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode populationsAsSingleArray = factory.arrayNode();
		ObjectMapper mapper = new ObjectMapper();
		ArrayList<JsonNode> populationNodes = populations.stream()
				.map(populationString -> {
					try {
						return mapper.readTree(populationString);
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}).collect(Collectors.toCollection(ArrayList::new));
		populationsAsSingleArray.addAll(populationNodes);
		ObjectNode migrationTaskArgument = mapper.createObjectNode();
		migrationTaskArgument.put("populations", populationsAsSingleArray);
		String migrationResult;
		if (!runMigrationLocally) {
			ArrayNode addTasksArgument = factory.arrayNode();
			addTasksArgument.add(migrationTaskArgument);
			List<Long> taskIdentifiers = restClient.addTasks(migrationJobId,
					addTasksArgument.toString());
			migrationResult = blockUntilResult(taskIdentifiers.get(0));
		} else {
			System.out.println("Migrating locally");
			migrationResult = runFunctionLocally("migrate",
					migrationTaskArgument.toString());
		}
		JsonNode newPopulationsNodesArray = mapper.readTree(migrationResult)
				.get("populations");
		ArrayList<JsonNode> newPopulationNodes = new ArrayList<JsonNode>();
		for (int i = 0; i < newPopulationsNodesArray.size(); ++i) {
			newPopulationNodes.add(newPopulationsNodesArray.get(i));
		}

		return ImmutableList.copyOf(newPopulationNodes.stream()
				.map(node -> node.toString())
				.collect(Collectors.toCollection(ArrayList::new)));
	}

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

	public static void printAsPrettyJson(String jsonString) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			System.out
					.println(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(
									new ObjectMapper().readTree(jsonString)));
		} catch (IOException ex) {
			System.out.println("Cannot parse json: " + jsonString);
		}
	}

	public static void main(String[] args) throws IOException, RestException,
			ScriptException {
		long startTime = System.currentTimeMillis();
		Charles charles = new Charles(
				"C:\\Users\\joegreen\\Desktop\\Uczelnia\\praca-magisterska\\charles\\generatePopulation.js",
				"C:\\Users\\joegreen\\Desktop\\Uczelnia\\praca-magisterska\\charles\\iteratePopulation2.js",
				"C:\\Users\\joegreen\\Desktop\\Uczelnia\\praca-magisterska\\charles\\migratePopulations.js",
				"{\"number\":50, \"dimension\":6, \"range\":5.12}", true,
				false, true, 5, 3, 10000, 50000,
				"The Charles Evolution Project");
		List<String> populations = charles.calculate();
		for (int i = 0; i < populations.size(); ++i) {
			System.out.println("--- Population " + i + " ---");
			printAsPrettyJson(populations.get(i));
		}
		System.out.println("Time: " + (System.currentTimeMillis() - startTime)
				+ " ms");

	}
}
