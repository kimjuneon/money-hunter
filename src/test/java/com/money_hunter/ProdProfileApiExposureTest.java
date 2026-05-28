package com.money_hunter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
				.andExpect(jsonPath("$.guestUserEnabled", is(false)))
				.andExpect(jsonPath("$.mockMonetizationEnabled", is(false)))
				.andExpect(jsonPath("$.tossReleaseReady", is(false)))
				.andExpect(jsonPath("$.releaseBlockers", hasItems(
						"toss-identity-disabled",
						"toss-user-key-disabled",
						"real-reward-ads-disabled",
						"real-toss-point-rewards-disabled"
				)));
	}

	@Test
	void guestPlayerFallbackIsDisabledInProdProfile() throws Exception {
		mockMvc.perform(get("/api/player"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidLoginSessionIsRejectedInProdProfile() throws Exception {
		mockMvc.perform(get("/api/player")
						.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tossLoginRequiresFeatureFlagInProdProfile() throws Exception {
		mockMvc.perform(post("/api/auth/toss/login")
						.contentType("application/json")
						.content("{\"authorizationCode\":\"code\",\"referrer\":\"DEFAULT\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void mockMonetizationApisAreBlockedInProdProfile() throws Exception {
		mockMvc.perform(post("/api/player/ads/auto-hunt/complete").with(user("reviewer")))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/player/shop/companions/purchase").with(user("reviewer")))
				.andExpect(status().isConflict());
	}
}
