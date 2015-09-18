package pl.joegreen.edward.communication.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.junit.Test;

import org.springframework.test.context.ContextConfiguration;
import pl.joegreen.edward.core.configuration.ConfigurationProvider;
import pl.joegreen.edward.core.configuration.Parameter;
import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.communication.ClientExecutionInfo;
import pl.joegreen.edward.core.model.communication.VolunteerRegistrationResponse;
import pl.joegreen.edward.persistence.dao.InvalidObjectException;

import java.util.concurrent.TimeUnit;

public class VolunteerRestControllerIntegrationTest extends
		RestControllerTestBase {


	@Test
	public void testTaskProcessing() throws Exception {

		// create and add tasks
		Job testJob = modelFixtures.createAndPersistTestJob();
		String addBatchUrl = String.format(INTERNAL_API_URL_BASE
				+ "/job/%d/tasks/0/1/5000", testJob.getId());
		String tasksData = "[1, 2]";
		mockMvc.perform(post(addBatchUrl).contentType(JSON).content(tasksData))
				.andExpect(OK);

		// get firstExecutionInfo
		String firstExecutionInfoString = "{}";

		for (int i = 0; i < 3 && firstExecutionInfoString.equals("{}"); ++i) {
			firstExecutionInfoString = performGetAndReturnContent(VOLUNTEER_API_URL_BASE
					+ "/getNextTask/"
					+ volunteerDao.getDefaultVolunteer().getId());
			if (firstExecutionInfoString.equals("{}")) {
				Thread.sleep(2000);
			}
		}

		ClientExecutionInfo firstExecutionInfo = mapper.readValue(
				firstExecutionInfoString, ClientExecutionInfo.class);

		// get code

		String jobString = performGetAndReturnContent(JOB_URL
				+ idUrl(firstExecutionInfo.getJobId()));

		Job firstJob = mapper.readValue(jobString, Job.class);
		assertEquals(testJob.getCode(), firstJob.getCode());
		// send result

		mockMvc.perform(
				post(
						String.format(
								VOLUNTEER_API_URL_BASE + "/sendResult/%d",
								firstExecutionInfo.getExecutionId()))
						.contentType(JSON).content("\"executionResult\""))
				.andExpect(OK);

		// get next executionInfo
		String secondExecutionInfoString = mockMvc
				.perform(
						get(
								VOLUNTEER_API_URL_BASE
										+ "/getNextTask/"
										+ +volunteerDao.getDefaultVolunteer()
												.getId()).accept(JSON))
				.andExpect(OK).andReturn().getResponse().getContentAsString();

		ClientExecutionInfo secondExecutionInfo = mapper.readValue(
				secondExecutionInfoString, ClientExecutionInfo.class);

		assertNotEquals(secondExecutionInfo.getInputData(),
				firstExecutionInfo.getInputData());
		assertEquals(secondExecutionInfo.getJobId(),
				firstExecutionInfo.getJobId());
		assertNotEquals(secondExecutionInfo.getExecutionId(),
				firstExecutionInfo.getExecutionId());

	}

	@Test
	public void executionShouldTimeoutIfVolunteerDisconnects() throws Exception {
        // create and add task
		Job testJob = modelFixtures.createAndPersistTestJob();
		//large timeout, execution will not timeout itself
		String addBatchUrl = String.format(INTERNAL_API_URL_BASE
				+ "/job/%d/tasks/0/1/100000", testJob.getId());
		String dataPart = "12345";
		String tasksData = "["+ dataPart + "]";
		mockMvc.perform(post(addBatchUrl).contentType(JSON).content(tasksData))
				.andExpect(OK);
		Thread.sleep(2 * configurationProvider.getValueAsLong(Parameter.TASK_REFRESH_INTERVAL_MS));

		String registrationResponseContent = mockMvc.perform(post(VOLUNTEER_API_URL_BASE
				+ "/register")).andExpect(OK).andReturn().getResponse().getContentAsString();
		VolunteerRegistrationResponse volunteerRegistrationResponse = mapper.readValue(registrationResponseContent, VolunteerRegistrationResponse.class);
		String actualTask = performGetAndReturnContent(VOLUNTEER_API_URL_BASE
				+ "/getNextTask/"
				+ volunteerRegistrationResponse.getVolunteerId());
		assertTrue(actualTask.contains(dataPart));

		String noTask = performGetAndReturnContent(VOLUNTEER_API_URL_BASE
				+ "/getNextTask/"
				+ volunteerDao.getDefaultVolunteer().getId());
		assertEquals("{}",noTask);
		Thread.sleep(5*volunteerRegistrationResponse.getHeartbeatIntervalMs());
		String renewedTask = performGetAndReturnContent(VOLUNTEER_API_URL_BASE
				+ "/getNextTask/"
				+ volunteerRegistrationResponse.getVolunteerId());
		assertTrue(renewedTask.contains(dataPart));


	}
}
