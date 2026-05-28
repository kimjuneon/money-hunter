package com.money_hunter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import com.money_hunter.application.AdminMonitoringService;
import com.money_hunter.domain.Player;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
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

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private AdminMonitoringService adminMonitoringService;

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
				.andExpect(jsonPath("$.guestUserEnabled", is(true)))
				.andExpect(jsonPath("$.mockMonetizationEnabled", is(true)))
				.andExpect(jsonPath("$.integrationMode", is("review-mock")));

		mockMvc.perform(get("/api/player"))
				.andExpect(status().isOk());
	}

	@Test
	void mockMonetizationApisAreAvailableInReviewProfile() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/ads/auto-hunt/complete"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/shop/skill-points/purchase"))
				.andExpect(status().isOk());
	}

	@Test
	void tossLoginCreatesSessionTokenInReviewProfile() throws Exception {
		String response = mockMvc.perform(post("/api/auth/toss/login")
						.contentType(APPLICATION_JSON)
						.content("{\"authorizationCode\":\"mock-code\",\"referrer\":\"SANDBOX\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.userKey", is("test-player")))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String token = JsonPath.read(response, "$.accessToken");

		mockMvc.perform(get("/api/player")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userKey", is("test-player")));
	}

	@Test
	void guestHeaderWithoutOneStoreTargetDoesNotChangeAppsInTossReviewPlayer() throws Exception {
		mockMvc.perform(get("/api/player")
						.header("X-Money-Hunter-Guest-Key", "guest-onestore-only"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userKey", is("test-player")));
	}

	@Test
	void rapidCombatHitsAreThrottledByServerAttackInterval() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/ads/auto-hunt/complete"))
				.andExpect(status().isOk());

		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/player/combat/hit"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.experience", is(0)))
					.andExpect(jsonPath("$.monster.hp", is(120)));
		}
	}

	@Test
	void offlineAutoHuntSettlementResolvesCombatLikeOnlineTicks() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/ads/auto-hunt/complete"))
				.andExpect(status().isOk());

		Player player = playerRepository.findByUserKey("test-player").orElseThrow();
		Instant now = Instant.now();
		player.setLastSettledAt(now.minusSeconds(300));
		player.setAutoHuntEndsAt(now.plusSeconds(300));
		playerRepository.saveAndFlush(player);

		mockMvc.perform(get("/api/player"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gold", greaterThan(0)))
				.andExpect(jsonPath("$.experience", greaterThan(0)))
				.andExpect(jsonPath("$.monster.defeatedMonsters", greaterThan(0)));
	}

	@Test
	void adminAnomalyReportFlagsExcessiveAdRewardEvents() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		for (int i = 0; i < 20; i++) {
			mockMvc.perform(post("/api/player/ads/skill-point/complete"))
					.andExpect(status().isOk());
		}

		AdminMonitoringService.AdminAnomalyReport report = adminMonitoringService.anomalies();
		assertTrue(report.anomalies().stream().anyMatch(anomaly ->
				"HIGH_AD_EVENTS".equals(anomaly.category())
						&& "test-player".equals(anomaly.userKey())));
	}
}
