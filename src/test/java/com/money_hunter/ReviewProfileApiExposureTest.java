package com.money_hunter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import com.money_hunter.application.AdminMonitoringService;
import com.money_hunter.application.AdminPlayerService;
import com.money_hunter.application.RuntimeEconomyService;
import com.money_hunter.domain.AdEvent;
import com.money_hunter.domain.AdEventType;
import com.money_hunter.domain.Player;
import com.money_hunter.domain.PlayerSkill;
import com.money_hunter.domain.SkillType;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

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
	private AdEventRepository adEventRepository;

	@Autowired
	private AdminMonitoringService adminMonitoringService;

	@Autowired
	private AdminPlayerService adminPlayerService;

	@Autowired
	private RuntimeEconomyService economy;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@AfterEach
	void cleanReviewPlayer() {
		adminPlayerService.resetFromLogin("test-player");
	}

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
	void suspendedPlayerCannotUseExistingSessionOrLoginAgain() throws Exception {
		String response = mockMvc.perform(post("/api/auth/toss/login")
						.contentType(APPLICATION_JSON)
						.content("{\"authorizationCode\":\"mock-code\",\"referrer\":\"SANDBOX\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String token = JsonPath.read(response, "$.accessToken");

		mockMvc.perform(post("/api/player/job")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		adminPlayerService.suspend("test-player", "테스트 정지");

		mockMvc.perform(get("/api/player")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/auth/toss/login")
						.contentType(APPLICATION_JSON)
						.content("{\"authorizationCode\":\"mock-code\",\"referrer\":\"SANDBOX\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminResetDeletesPlayerAndLoginSessions() throws Exception {
		String response = mockMvc.perform(post("/api/auth/toss/login")
						.contentType(APPLICATION_JSON)
						.content("{\"authorizationCode\":\"mock-code\",\"referrer\":\"SANDBOX\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String token = JsonPath.read(response, "$.accessToken");

		mockMvc.perform(post("/api/player/job")
						.header("Authorization", "Bearer " + token)
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"ARCHER\"}"))
				.andExpect(status().isOk());
		adminPlayerService.resetFromLogin("test-player");

		assertFalse(playerRepository.findByUserKey("test-player").isPresent());
		mockMvc.perform(get("/api/player")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
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
	void goldRatesIncreaseWithSharedSkillPetUnlockAndPetSkill() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		String chosen = mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"MAGE\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long baseGoldPerHour = readLong(chosen, "$.baseGoldPerHour");
		long boostedGoldPerHour = readLong(chosen, "$.boostedGoldPerHour");
		assertTrue(boostedGoldPerHour > baseGoldPerHour);

		mockMvc.perform(post("/api/player/shop/skill-points/purchase"))
				.andExpect(status().isOk());
		String statUpgraded = mockMvc.perform(post("/api/player/skills/upgrade")
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"INTELLIGENCE\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long afterStatGoldPerHour = readLong(statUpgraded, "$.baseGoldPerHour");
		assertTrue(afterStatGoldPerHour > baseGoldPerHour);

		String petUnlocked = mockMvc.perform(post("/api/player/shop/companions/purchase"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long afterPetGoldPerHour = readLong(petUnlocked, "$.baseGoldPerHour");
		assertTrue(afterPetGoldPerHour > afterStatGoldPerHour);

		String petSkillUpgraded = mockMvc.perform(post("/api/player/skills/upgrade")
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"PET_FLARE_ATTACK\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long afterPetSkillGoldPerHour = readLong(petSkillUpgraded, "$.baseGoldPerHour");
		assertTrue(afterPetSkillGoldPerHour > afterPetGoldPerHour);
		assertTrue(readLong(petSkillUpgraded, "$.boostedGoldPerHour") > afterPetSkillGoldPerHour);
	}

	@Test
	void goldPerTossPointChangesRewardConversionButNotGoldRates() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		String before = mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.goldPerTossPoint", is(100)))
				.andExpect(jsonPath("$.rewardGoldThreshold", is(1000)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		long baseGoldPerHour = readLong(before, "$.baseGoldPerHour");
		long boostedGoldPerHour = readLong(before, "$.boostedGoldPerHour");

		try {
			economy.update("goldPerTossPoint", 200);

			mockMvc.perform(get("/api/player"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.goldPerTossPoint", is(200)))
					.andExpect(jsonPath("$.rewardGoldThreshold", is(2000)))
					.andExpect(jsonPath("$.baseGoldPerHour", is((int) baseGoldPerHour)))
					.andExpect(jsonPath("$.boostedGoldPerHour", is((int) boostedGoldPerHour)));
		} finally {
			economy.reset("goldPerTossPoint");
		}
	}

	@Test
	void adRevenuePolicyDoesNotChangeGoldRates() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		String before = mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"ARCHER\"}"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		long baseGoldPerHour = readLong(before, "$.baseGoldPerHour");
		long boostedGoldPerHour = readLong(before, "$.boostedGoldPerHour");

		try {
			economy.update("adRevenuePerRewardAdWon", 120);

			mockMvc.perform(get("/api/player"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.baseGoldPerHour", is((int) baseGoldPerHour)))
					.andExpect(jsonPath("$.boostedGoldPerHour", is((int) boostedGoldPerHour)));
		} finally {
			economy.reset("adRevenuePerRewardAdWon");
		}
	}

	@Test
	void fullyUpgradedBoostedGoldRateIsCappedAtDefaultSixThousand() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			while (player.getCharacterSlots() < 3) {
				player.purchaseCharacterSlot(3);
			}
			for (SkillType type : SkillType.values()) {
				if (type != SkillType.AUTO_HUNT_EFFICIENCY) {
					player.getOrCreateSkill(type).setLevel(PlayerSkill.MAX_LEVEL);
				}
			}
			Instant now = Instant.now();
			player.setAutoHuntEndsAt(now.plusSeconds(3600));
			player.setBoostEndsAt(now.plusSeconds(3600));
		});

		mockMvc.perform(get("/api/player"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.baseGoldPerHour", is(4000)))
				.andExpect(jsonPath("$.boostedGoldPerHour", is(6000)))
				.andExpect(jsonPath("$.goldPerHour", is(6000)))
				.andExpect(jsonPath("$.payoutRatePercent", is(100)));
	}

	@Test
	void skillUpgradeContinuesPastTwentyUntilThirty() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			player.getOrCreateSkill(SkillType.STRENGTH).setLevel(20);
			player.addSkillPoints(1);
		});

		mockMvc.perform(post("/api/player/skills/upgrade")
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"STRENGTH\"}"))
				.andExpect(status().isOk());

		int upgradedLevel = transactionTemplate.execute(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			return player.getOrCreateSkill(SkillType.STRENGTH).getLevel();
		});
		assertEquals(21, upgradedLevel);
	}

	@Test
	void friendInviteRewardClaimsMultipleCompletedInvitesAtOnce() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/player/reward/friend-invite/claim")
						.contentType(APPLICATION_JSON)
						.content("{\"completedInvites\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.friendInviteRewardCount", is(2)))
				.andExpect(jsonPath("$.skillPoints", is(10)));

		mockMvc.perform(post("/api/player/reward/friend-invite/claim")
						.contentType(APPLICATION_JSON)
						.content("{\"completedInvites\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.friendInviteRewardCount", is(3)))
				.andExpect(jsonPath("$.skillPoints", is(15)));
	}

	@Test
	void reviewTestToolsBypassNormalRewardLimits() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/test/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"ROGUE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.job", is("ROGUE")));

		mockMvc.perform(post("/api/player/test/skill-point"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillPoints", is(1)));
		mockMvc.perform(post("/api/player/test/skill-point"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillPoints", is(2)));

		mockMvc.perform(post("/api/player/test/companions/unlock-all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.characterSlots", is(3)));
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
	void offlineSettlementUsesUpdatedSkillAndPetGoldRate() throws Exception {
		long baseGold = settleFiveMinutesAndReadGold(false);
		long upgradedGold = settleFiveMinutesAndReadGold(true);

		assertTrue(upgradedGold > baseGold);
	}

	@Test
	void rewardClaimUsesAllAvailableConvertedPoints() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"MAGE\"}"))
				.andExpect(status().isOk());

		Player player = playerRepository.findByUserKey("test-player").orElseThrow();
		Instant now = Instant.now();
		player.setAutoHuntEndsAt(null);
		player.setBoostEndsAt(null);
		player.setLastSettledAt(now);
		player.addGold(1_800);
		playerRepository.saveAndFlush(player);

		mockMvc.perform(post("/api/player/reward/claim-after-ad")
						.contentType(APPLICATION_JSON)
						.content("{\"idempotencyKey\":\"claim-all-available-points\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pointAmount", is(18)))
				.andExpect(jsonPath("$.state.gold", is(0)));
	}

	@Test
	void adminAnomalyReportFlagsExcessiveAdRewardEvents() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		Player player = playerRepository.findByUserKey("test-player").orElseThrow();
		for (int i = 0; i < economy.anomalyAdEventsPerHourWarning() + 1; i++) {
			adEventRepository.save(new AdEvent(player, AdEventType.AUTO_HUNT, 3600, Instant.now()));
		}

		AdminMonitoringService.AdminAnomalyReport report = adminMonitoringService.anomalies();
		assertTrue(report.anomalies().stream().anyMatch(anomaly ->
				"HIGH_AD_EVENTS".equals(anomaly.category())
						&& "test-player".equals(anomaly.userKey())));
	}

	private long readLong(String json, String path) {
		Number number = JsonPath.read(json, path);
		return number.longValue();
	}

	private long settleFiveMinutesAndReadGold(boolean upgraded) throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"MAGE\"}"))
				.andExpect(status().isOk());
		if (upgraded) {
			mockMvc.perform(post("/api/player/shop/skill-points/purchase"))
					.andExpect(status().isOk());
			mockMvc.perform(post("/api/player/skills/upgrade")
							.contentType(APPLICATION_JSON)
							.content("{\"type\":\"INTELLIGENCE\"}"))
					.andExpect(status().isOk());
			mockMvc.perform(post("/api/player/shop/companions/purchase"))
					.andExpect(status().isOk());
			mockMvc.perform(post("/api/player/skills/upgrade")
							.contentType(APPLICATION_JSON)
							.content("{\"type\":\"PET_FLARE_ATTACK\"}"))
					.andExpect(status().isOk());
		}

		Player player = playerRepository.findByUserKey("test-player").orElseThrow();
		Instant now = Instant.now();
		player.setLastSettledAt(now.minusSeconds(300));
		player.setAutoHuntEndsAt(now.plusSeconds(300));
		player.setBoostEndsAt(null);
		playerRepository.saveAndFlush(player);

		String settled = mockMvc.perform(get("/api/player"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return readLong(settled, "$.gold");
	}
}
