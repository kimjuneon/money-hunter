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
@ActiveProfiles("prod")
@AutoConfigureMockMvc
@SpringBootTest
class ProdProfileApiExposureTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void reviewTestApiIsNotAvailableInProdProfile() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isNotFound());
	}

	@Test
	void reviewToolsAreDisabledInProdConfig() throws Exception {
		mockMvc.perform(get("/api/app/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewToolsEnabled", is(false)))
				.andExpect(jsonPath("$.guestUserEnabled", is(false)));
	}

	@Test
	void guestPlayerFallbackIsDisabledInProdProfile() throws Exception {
		mockMvc.perform(get("/api/player"))
				.andExpect(status().isUnauthorized());
	}
}
