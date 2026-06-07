package com.money_hunter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
		"money-hunter.admin.username=mode-admin",
		"money-hunter.admin.password-bcrypt=",
		"money-hunter.admin.password=mode-pass",
		"money-hunter.admin.allow-plain-password=true",
		"money-hunter.app.toss-login-enabled=true",
		"money-hunter.app.toss-user-key-enabled=true",
		"money-hunter.app.real-reward-ads-enabled=true",
		"money-hunter.app.real-banner-ads-enabled=true",
		"money-hunter.app.real-payments-enabled=true",
		"money-hunter.app.real-toss-point-rewards-enabled=true",
		"money-hunter.app.real-smart-message-enabled=true",
		"money-hunter.app.real-share-reward-enabled=true",
		"money-hunter.ads.mode=test",
		"money-hunter.promotion.reward-claim-code=TEST_REWARD_CLAIM_PROMOTION",
		"money-hunter.promotion.benefit-tab-new-user-code=TEST_BENEFIT_TAB_NEW_USER_PROMOTION"
})
@AutoConfigureMockMvc
@SpringBootTest
class AdminRuntimeModeStatusTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void adminOverviewShowsTestModesForTestAdsAndPromotionCode() throws Exception {
		String token = loginToken();

		String response = mockMvc.perform(get("/api/admin/overview")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertEquals(
				List.of("TEST"),
				JsonPath.read(response, "$.runtimeStatusItems[?(@.key == 'reward-ads')].mode"));
		assertEquals(
				List.of("리워드 테스트 광고 ID 사용"),
				JsonPath.read(response, "$.runtimeStatusItems[?(@.key == 'reward-ads')].detail"));
		assertEquals(
				List.of("TEST"),
				JsonPath.read(response, "$.runtimeStatusItems[?(@.key == 'banner-ads')].mode"));
		assertEquals(
				List.of("TEST"),
				JsonPath.read(response, "$.runtimeStatusItems[?(@.key == 'point-rewards')].mode"));
		assertEquals(
				List.of("보상 수령, 혜택 탭 테스트 프로모션 코드"),
				JsonPath.read(response, "$.runtimeStatusItems[?(@.key == 'point-rewards')].detail"));
	}

	@Test
	void adminRookieEventTestToolAppliesOnlyTargetUser() throws Exception {
		String token = loginToken();
		mockMvc.perform(post("/api/player/job")
						.with(user("rookie-admin-target"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.with(user("rookie-admin-other"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"ARCHER\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/test-tools/rookie-event/rookie-admin-target/state")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"completedDays\":7,\"rewardedDays\":6,\"finalRewardClaimed\":false,\"reason\":\"event test\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completedDays").value(7))
				.andExpect(jsonPath("$.rookieEvent.rewardClaimed").value(false))
				.andExpect(jsonPath("$.rookieEvent.days[6].rewardClaimable").value(true));

		mockMvc.perform(get("/api/player")
						.with(user("rookie-admin-other")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completedDays").value(0))
				.andExpect(jsonPath("$.rookieEvent.rewardClaimed").value(false));
	}

	@Test
	void prodAdminPromotionTestToolCanPrepareTargetUserWithoutMockLogClient() throws Exception {
		String token = loginToken();

		mockMvc.perform(post("/api/admin/test-tools/promotion/prod-admin-promotion-user/prepare")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"reason\":\"promotion prepare\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.player.userKey").value("prod-admin-promotion-user"))
				.andExpect(jsonPath("$.player.job").value("WARRIOR"))
				.andExpect(jsonPath("$.mockExecutionLogAvailable").value(false))
				.andExpect(jsonPath("$.executions").isArray())
				.andExpect(jsonPath("$.executions").isEmpty());
	}

	@Test
	void adminRookieEventSettingsToggleControlsNewEventStarts() throws Exception {
		String token = loginToken();
		String userKey = "rookie-event-settings-toggle";

		mockMvc.perform(get("/api/admin/events/rookie-event")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.startWindowUnlimited").value(true));

		mockMvc.perform(post("/api/player/job")
						.with(user(userKey))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/events/rookie-event")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"enabled\":false,\"reason\":\"toggle test\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));

		mockMvc.perform(post("/api/player/rookie-event/start")
						.with(user(userKey)))
				.andExpect(status().isConflict());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/events/rookie-event")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"enabled\":true,\"reason\":\"toggle test restore\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(true));

		mockMvc.perform(post("/api/player/rookie-event/start")
						.with(user(userKey)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.started").value(true));
	}

	@Test
	void adminRookieEventAdvanceDayUnlocksNextMissionAndReducesRemainingDays() throws Exception {
		String token = loginToken();
		mockMvc.perform(post("/api/player/job")
						.with(user("rookie-admin-advance"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/test-tools/rookie-event/rookie-admin-advance/state")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"completedDays\":1,\"rewardedDays\":0,\"finalRewardClaimed\":false,\"reason\":\"advance day\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completedDays").value(1))
				.andExpect(jsonPath("$.rookieEvent.currentDay").value(2))
				.andExpect(jsonPath("$.rookieEvent.lockedUntilTomorrow").value(true))
				.andExpect(jsonPath("$.rookieEvent.daysRemaining").value(10))
				.andExpect(jsonPath("$.rookieEvent.days[1].current").value(true))
				.andExpect(jsonPath("$.rookieEvent.days[1].locked").value(true));

		mockMvc.perform(post("/api/admin/test-tools/rookie-event/rookie-admin-advance/advance-day")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"reason\":\"advance day\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completedDays").value(1))
				.andExpect(jsonPath("$.rookieEvent.currentDay").value(2))
				.andExpect(jsonPath("$.rookieEvent.lockedUntilTomorrow").value(false))
				.andExpect(jsonPath("$.rookieEvent.daysRemaining").value(9))
				.andExpect(jsonPath("$.rookieEvent.days[1].current").value(true))
				.andExpect(jsonPath("$.rookieEvent.days[1].locked").value(false));
	}

	private String loginToken() throws Exception {
		String response = mockMvc.perform(post("/api/admin/auth/login")
						.contentType(APPLICATION_JSON)
						.content("{\"loginId\":\"mode-admin\",\"password\":\"mode-pass\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}
}
