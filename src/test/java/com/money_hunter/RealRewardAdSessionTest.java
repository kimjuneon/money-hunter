package com.money_hunter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;
import com.money_hunter.domain.PlayerSkill;
import com.money_hunter.domain.SkillType;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
		"money-hunter.app.toss-login-enabled=false",
		"money-hunter.app.toss-user-key-enabled=false",
		"money-hunter.app.real-reward-ads-enabled=true",
		"money-hunter.app.real-banner-ads-enabled=false"
})
@AutoConfigureMockMvc
@SpringBootTest
class RealRewardAdSessionTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	void realRewardAdCompletionRequiresSingleUseSessionToken() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.with(user("ad-session-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/player/ads/skill-point/complete")
						.with(user("ad-session-user"))
						.contentType(APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		String sessionResponse = mockMvc.perform(post("/api/player/ads/sessions")
						.with(user("ad-session-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"SKILL_POINT\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionToken", not(blankOrNullString())))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String sessionToken = JsonPath.read(sessionResponse, "$.sessionToken");

		mockMvc.perform(post("/api/player/ads/skill-point/complete")
						.with(user("ad-session-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"adSessionToken\":\"" + sessionToken + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillPoints", is(1)));

		mockMvc.perform(post("/api/player/ads/skill-point/complete")
						.with(user("ad-session-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"adSessionToken\":\"" + sessionToken + "\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void firstJobSelectionGrantsTutorialTimers() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.with(user("tutorial-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"MAGE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tutorialRewardClaimed", is(true)))
				.andExpect(jsonPath("$.autoHuntEndsAt", not(blankOrNullString())))
				.andExpect(jsonPath("$.boostEndsAt", not(blankOrNullString())));

		mockMvc.perform(get("/api/player")
						.with(user("tutorial-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tutorialRewardClaimed", is(true)));
	}

	@Test
	void rookieEventStartsOnlyWhenUserOpensEvent() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.with(user("rookie-event-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.visible", is(true)))
				.andExpect(jsonPath("$.rookieEvent.started", is(false)))
				.andExpect(jsonPath("$.rookieEvent.startable", is(true)))
				.andExpect(jsonPath("$.rookieEvent.active", is(false)))
				.andExpect(jsonPath("$.rookieEvent.daysRemaining", is(10)))
				.andExpect(jsonPath("$.rookieEvent.days.length()", is(0)));

		mockMvc.perform(post("/api/player/rookie-event/start")
						.with(user("rookie-event-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.visible", is(true)))
				.andExpect(jsonPath("$.rookieEvent.started", is(true)))
				.andExpect(jsonPath("$.rookieEvent.startable", is(false)))
				.andExpect(jsonPath("$.rookieEvent.active", is(true)))
				.andExpect(jsonPath("$.rookieEvent.daysRemaining", is(10)))
				.andExpect(jsonPath("$.rookieEvent.completedDays", is(0)))
				.andExpect(jsonPath("$.rookieEvent.currentDay", is(1)))
				.andExpect(jsonPath("$.rookieEvent.eventPetSkillLevel", is(15)))
				.andExpect(jsonPath("$.rookieEvent.rewardDescription", containsString("펫 보유 수 제한 미포함")))
				.andExpect(jsonPath("$.rookieEvent.days[0].title", is("사냥 준비")))
				.andExpect(jsonPath("$.rookieEvent.days[0].rewardLabel", is("SP 1개")))
				.andExpect(jsonPath("$.rookieEvent.days[0].rewardClaimable", is(false)))
				.andExpect(jsonPath("$.rookieEvent.days[0].missions[2].label", is("토스 포인트 수령하기")))
				.andExpect(jsonPath("$.rookieEvent.days[1].rewardLabel", is("자동전투 1시간")))
				.andExpect(jsonPath("$.rookieEvent.days[2].rewardLabel", is("공속버프 1시간")))
				.andExpect(jsonPath("$.rookieEvent.days[5].missions[2].label", is("10레벨 달성하기")));
	}

	@Test
	void rookieEventDailyRewardRequiresClaimButton() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.with(user("rookie-event-reward-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/rookie-event/start")
						.with(user("rookie-event-reward-user")))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey("rookie-event-reward-user").orElseThrow();
			player.addRookieEventCombatProgress(Duration.ofHours(1).toMillis(), 0, 20, 0);
			player.addRookieEventSettlement();
		});

		mockMvc.perform(get("/api/player")
						.with(user("rookie-event-reward-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completedDays", is(1)))
				.andExpect(jsonPath("$.rookieEvent.days[0].rewardClaimed", is(false)))
				.andExpect(jsonPath("$.rookieEvent.days[0].rewardClaimable", is(true)))
				.andExpect(jsonPath("$.skillPoints", is(0)));

		mockMvc.perform(post("/api/player/rookie-event/days/1/reward/claim")
						.with(user("rookie-event-reward-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completedDays", is(1)))
				.andExpect(jsonPath("$.rookieEvent.days[0].rewardClaimed", is(true)))
				.andExpect(jsonPath("$.rookieEvent.days[0].rewardClaimable", is(false)))
				.andExpect(jsonPath("$.skillPoints", is(1)));

		mockMvc.perform(get("/api/player")
						.with(user("rookie-event-reward-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completedDays", is(1)))
				.andExpect(jsonPath("$.rookieEvent.days[0].rewardClaimed", is(true)))
				.andExpect(jsonPath("$.skillPoints", is(1)));
	}

	@Test
	void rookieEventFinalRewardRequiresClaimButton() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.with(user("rookie-event-final-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey("rookie-event-final-user").orElseThrow();
			player.overrideRookieEventForTest(Instant.now(), LocalDate.now(), 7, 7, false, 7);
		});

		mockMvc.perform(get("/api/player")
						.with(user("rookie-event-final-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completed", is(true)))
				.andExpect(jsonPath("$.rookieEvent.rewardClaimed", is(false)));

		mockMvc.perform(post("/api/player/rookie-event/final-reward/claim")
						.with(user("rookie-event-final-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rookieEvent.completed", is(true)))
				.andExpect(jsonPath("$.rookieEvent.rewardClaimed", is(true)))
				.andExpect(jsonPath("$.rookieEvent.rewardActive", is(true)))
				.andExpect(jsonPath("$.rookieEvent.rewardDaysRemaining", is(30)));
	}

	@Test
	void skillPointRewardSessionIsBlockedWhenAllSpendableSkillsAreMaxed() throws Exception {
		mockMvc.perform(post("/api/player/job")
						.with(user("skill-max-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"ARCHER\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey("skill-max-user").orElseThrow();
			for (SkillType type : SkillType.values()) {
				if (type != SkillType.AUTO_HUNT_EFFICIENCY) {
					player.getOrCreateSkill(type).setLevel(PlayerSkill.MAX_LEVEL);
				}
			}
		});

		mockMvc.perform(get("/api/player")
						.with(user("skill-max-user")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillPointRewardsAvailable", is(false)));

		mockMvc.perform(post("/api/player/ads/sessions")
						.with(user("skill-max-user"))
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"SKILL_POINT\"}"))
				.andExpect(status().isConflict());
	}
}
