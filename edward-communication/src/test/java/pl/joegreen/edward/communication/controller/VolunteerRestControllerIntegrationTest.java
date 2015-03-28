package pl.joegreen.edward.communication.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.junit.Test;

import pl.joegreen.edward.core.model.Job;
import pl.joegreen.edward.core.model.communication.ClientExecutionInfo;

public class VolunteerRestControllerIntegrationTest extends
		RestControllerTestBase {

	@Test
	public void testTaskProcessing() throws Exception {

		// create and add tasks
		Job testJob = modelFixtures.createAndPersistTestJob();
		String addBatchUrl = String.format(INTERNAL_API_URL_BASE
				+ "/job/%d/tasks/1/0/5000", testJob.getId());
		String tasksData = "[1, 2]";
		mockMvc.perform(post(addBatchUrl).contentType(JSON).content(tasksData))
				.andExpect(OK);

		// get firstExecutionInfo
		String firstExecutionInfoString = performGetAndReturnContent(VOLUNTEER_API_URL_BASE
				+ "/getNextTask/" + volunteerDao.getDefaultVolunteer().getId());

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
}
