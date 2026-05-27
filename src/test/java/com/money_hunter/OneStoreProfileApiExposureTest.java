package com.money_hunter;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
@ActiveProfiles("onestore")
@AutoConfigureMockMvc
@SpringBootTest
class OneStoreProfileApiExposureTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void oneStoreConfigUsesGuestGameRewardModeWithoutReviewTools() throws Exception {
		mockMvc.perform(get("/api/app/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.distributionTarget", is("ONESTORE")))
				.andExpect(jsonPath("$.integrationMode", is("onestore-review")))
				.andExpect(jsonPath("$.reviewToolsEnabled", is(false)))
				.andExpect(jsonPath("$.guestUserEnabled", is(true)))
				.andExpect(jsonPath("$.mockMonetizationEnabled", is(true)));
	}

	@Test
	void oneStoreGameRewardApisAreAvailableWithoutLogin() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/onestore/auto-hunt/claim"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/onestore/skill-point/claim"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/onestore/shop/skill-points/claim"))
				.andExpect(status().isOk());
	}

	@Test
	void oneStoreGuestHeaderSeparatesPlayerStateByDevice() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.header("X-Money-Hunter-Guest-Key", "guest-onestore-device-a")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.header("X-Money-Hunter-Guest-Key", "guest-onestore-device-b")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"MAGE\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/player")
						.header("X-Money-Hunter-Guest-Key", "guest-onestore-device-a"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userKey", is("guest-onestore-device-a")))
				.andExpect(jsonPath("$.job", is("WARRIOR")));
		mockMvc.perform(get("/api/player")
						.header("X-Money-Hunter-Guest-Key", "guest-onestore-device-b"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userKey", is("guest-onestore-device-b")))
				.andExpect(jsonPath("$.job", is("MAGE")));
	}

	@Test
	void reviewTestApiIsNotShownInOneStoreProfile() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isNotFound());
	}
}
