package pl.joegreen.edward.shell;

import java.io.File;
import java.io.IOException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

import org.apache.commons.io.FileUtils;

import pl.joegreen.edward.rest.client.RestClient;
import pl.joegreen.edward.rest.client.RestException;

public class CommandLineClient {

	public CommandLineClient(RestClient restClient) {
		super();
		this.restClient = restClient;
	}

	private static final String COMMAND_NAME_ATTRIBUTE = "command_name";

	private RestClient restClient;

	public static class Command {
		/*
		 * It can't be easily replaced with enum: String values are used as
		 * command names and they come back from arguments parser. We would like
		 * to switch on the command name. In enum it would have to be a field
		 * and enum's field isn't considered constant by Java compiler and it
		 * doesn't allow to use it in switch cases.
		 */
		public static final String PROJECT_ADD = "add-project";
		public static final String JOB_ADD = "add-job";
		public static final String TASKS_ADD = "add-tasks";
		public static final String GET_RESULT = "get-result";
		public static final String GET_TASK = "get-task";
		public static final String GET_TASK_INPUT = "get-task-input";
	}

	private enum Parameter {
		CODE_FILE("codeFile", "cf"), CODE("code", "c"), PROJECT_ID("projectId",
				"pid"), USER_ID("userId", "uid"), NAME("name", "n"), JOB_ID(
				"jobId", "jid"), DATA("data", "d"), DATA_FILE("dataFile", "df"), TASK_ID(
				"taskId", "tid"), TASK_PRIORITY("priority", "p"), TASK_CONCURRENT_EXECUTIONS(
				"concurrentExecutions", "ce");

		private String longName;
		private String shortName;

		Parameter(String longName, String shortName) {
			this.longName = longName;
			this.shortName = shortName;
		}

		public String getLongName() {
			return longName;
		}

		public String[] getParserArguments() {
			return new String[] { "-" + shortName, "--" + longName };
		}
	}

	public static void main(String[] args) throws RestException, IOException {
		try {
			CommandLineClient commandLineClient = new CommandLineClient(
					new RestClient("admin", "admin", "localhost", 8080, "http",
							""));
			Namespace parsedArguments = commandLineClient.parseArguments(args);
			commandLineClient.executeCommand(parsedArguments);
		} catch (ArgumentParserException ex) {
			System.err.println(ex.getMessage());
		}
	}

	public Namespace parseArguments(String[] args)
			throws ArgumentParserException {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("client");
		Subparsers subparsers = parser.addSubparsers().dest(
				COMMAND_NAME_ATTRIBUTE);

		Subparser projectAddParser = subparsers.addParser(Command.PROJECT_ADD);
		addNameArgument(projectAddParser);
		projectAddParser.addArgument(Parameter.USER_ID.getParserArguments())
				.required(true).type(Long.class);

		Subparser jobAddParser = subparsers.addParser(Command.JOB_ADD);
		addNameArgument(jobAddParser);
		jobAddParser.addArgument(Parameter.PROJECT_ID.getParserArguments())
				.required(true).type(Long.class);
		MutuallyExclusiveGroup jobCodeGroup = jobAddParser
				.addMutuallyExclusiveGroup().required(true);
		jobCodeGroup.addArgument(Parameter.CODE.getParserArguments());
		// TODO: check if system in works at all, delete if doesnt - do the same
		// for tasks data
		jobCodeGroup.addArgument(Parameter.CODE_FILE.getParserArguments())
				.type(Arguments.fileType().acceptSystemIn().verifyCanRead())
				.setDefault("-");
		Subparser tasksAddParser = subparsers.addParser(Command.TASKS_ADD);
		tasksAddParser.addArgument(Parameter.JOB_ID.getParserArguments())
				.required(true).type(Long.class);
		tasksAddParser
				.addArgument(Parameter.TASK_PRIORITY.getParserArguments())
				.required(true).type(Long.class);
		tasksAddParser
				.addArgument(
						Parameter.TASK_CONCURRENT_EXECUTIONS
								.getParserArguments()).required(true)
				.type(Long.class);
		MutuallyExclusiveGroup tasksDataGroup = tasksAddParser
				.addMutuallyExclusiveGroup();

		tasksDataGroup.addArgument(Parameter.DATA.getParserArguments());
		tasksDataGroup.addArgument(Parameter.DATA_FILE.getParserArguments())
				.type(Arguments.fileType().acceptSystemIn().verifyCanRead())
				.setDefault("-");

		Subparser getResultParser = subparsers.addParser(Command.GET_RESULT);
		getResultParser.addArgument(Parameter.TASK_ID.getParserArguments())
				.type(Long.class).required(true);

		Subparser getTaskParser = subparsers.addParser(Command.GET_TASK);
		getTaskParser.addArgument(Parameter.TASK_ID.getParserArguments())
				.type(Long.class).required(true);

		Subparser getTaskInputParser = subparsers
				.addParser(Command.GET_TASK_INPUT);
		getTaskInputParser.addArgument(Parameter.TASK_ID.getParserArguments())
				.type(Long.class).required(true);

		Namespace parsedArguments = parser.parseArgs(args);
		return parsedArguments;
	}

	private void addNameArgument(Subparser parser) {
		parser.addArgument(Parameter.NAME.getParserArguments()).required(true);
	}

	public void executeCommand(Namespace parsedArguments) throws RestException,
			IOException {
		String commandName = parsedArguments.get(COMMAND_NAME_ATTRIBUTE)
				.toString();
		switch (commandName) {
		case Command.PROJECT_ADD:
			addProject(parsedArguments);
			break;
		case Command.JOB_ADD:
			addJob(parsedArguments);
			break;
		case Command.TASKS_ADD:
			addTasks(parsedArguments);
			break;
		case Command.GET_RESULT:
			getResult(parsedArguments);
			break;
		case Command.GET_TASK:
			getTask(parsedArguments);
			break;
		case Command.GET_TASK_INPUT:
			getTaskInput(parsedArguments);
			break;
		}
	}

	private void printResult(Object object) {
		System.out.println("Result: ");
		System.out.println(object);
	}

	private void addProject(Namespace parsedArguments) throws RestException {
		String projectName = parsedArguments.getString(Parameter.NAME
				.getLongName());
		Long userId = parsedArguments.getLong(Parameter.USER_ID.getLongName());
		printResult(restClient.addProject(projectName, userId));
	}

	private void addJob(Namespace parsedArguments) throws RestException,
			IOException {
		Long projectId = parsedArguments.getLong(Parameter.PROJECT_ID
				.getLongName());
		String name = parsedArguments.getString(Parameter.NAME.getLongName());
		String code = parsedArguments.getString(Parameter.CODE.getLongName());

		if (code == null) {
			File codeFile = parsedArguments.get(Parameter.CODE_FILE
					.getLongName());
			code = FileUtils.readFileToString(codeFile);
		}
		printResult(restClient.addJob(projectId, name, code));
	}

	private void addTasks(Namespace parsedArguments) throws IOException,
			RestException {
		Long jobId = parsedArguments.getLong(Parameter.JOB_ID.getLongName());
		Long priority = parsedArguments.getLong(Parameter.TASK_PRIORITY
				.getLongName());
		Long concurrentExecutions = parsedArguments
				.getLong(Parameter.TASK_CONCURRENT_EXECUTIONS.getLongName());
		String data = parsedArguments.getString(Parameter.DATA.getLongName());
		if (data == null) {
			File dataFile = parsedArguments.get(Parameter.DATA_FILE
					.getLongName());
			data = FileUtils.readFileToString(dataFile);
		}

		printResult(restClient.addTasks(jobId, data, priority,
				concurrentExecutions));
	}

	private void getResult(Namespace parsedArguments) throws RestException {
		Long taskId = parsedArguments.getLong(Parameter.TASK_ID.getLongName());
		printResult(restClient.getResults(taskId));
	}

	private void getTask(Namespace parsedArguments) throws RestException {
		Long taskId = parsedArguments.getLong(Parameter.TASK_ID.getLongName());
		printResult(restClient.getTask(taskId));
	}

	private void getTaskInput(Namespace parsedArguments) throws RestException {
		Long taskId = parsedArguments.getLong(Parameter.TASK_ID.getLongName());
		printResult(restClient.getTaskInput(taskId));
	}

}
