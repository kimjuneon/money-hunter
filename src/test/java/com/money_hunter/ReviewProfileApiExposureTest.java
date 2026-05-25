package com.money_hunter;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("review")
@AutoConfigureMockMvc
@SpringBootTest
class ReviewProfileApiExposureTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void reviewTestApiIsAvailableInReviewProfile() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
	}

	@Test
	void guestPlayerFallbackAndReviewToolsAreEnabledInReviewProfile() throws Exception {
		mockMvc.perform(get("/api/app/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewToolsEnabled", is(true)))
				.andExpect(jsonPath("$.guestUserEnabled", is(true)));

		mockMvc.perform(get("/api/player"))
				.andExpect(status().isOk());
	}
}
