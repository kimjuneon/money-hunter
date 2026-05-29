package com.money_hunter;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
