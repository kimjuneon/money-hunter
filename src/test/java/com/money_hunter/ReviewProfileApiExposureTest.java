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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.money_hunter.application.AdminMonitoringService;
import com.money_hunter.application.AdminPlayerService;
import com.money_hunter.application.RuntimeEconomyService;
import com.money_hunter.application.dto.response.AdminPlayerResponse;
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

		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey("test-player").orElseThrow();
			Instant now = Instant.now();
			player.setLastSettledAt(now);
			player.setAutoHuntEndsAt(now.plusSeconds(60));
		});

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
			assertEquals(baseGoldPerHour, readLong(chosen, "$.goldPerHour"));

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
			assertEquals(afterPetSkillGoldPerHour, readLong(petSkillUpgraded, "$.goldPerHour"));
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

			try {
			economy.update("goldPerTossPoint", 200);

			mockMvc.perform(get("/api/player"))
					.andExpect(status().isOk())
						.andExpect(jsonPath("$.goldPerTossPoint", is(200)))
						.andExpect(jsonPath("$.rewardGoldThreshold", is(2000)))
						.andExpect(jsonPath("$.baseGoldPerHour", is((int) baseGoldPerHour)))
						.andExpect(jsonPath("$.goldPerHour", is((int) baseGoldPerHour)));
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

			try {
			economy.update("adRevenuePerRewardAdWon", 120);

				mockMvc.perform(get("/api/player"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.baseGoldPerHour", is((int) baseGoldPerHour)))
						.andExpect(jsonPath("$.goldPerHour", is((int) baseGoldPerHour)));
		} finally {
			economy.reset("adRevenuePerRewardAdWon");
		}
	}

	@Test
	void timeRewardCooldownPoliciesControlNextAdAvailability() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"ARCHER\"}"))
				.andExpect(status().isOk());

			try {
				economy.update("autoHuntAdCooldownSeconds", 600);

				Instant beforeAutoHunt = Instant.now();
			String autoHuntResponse = mockMvc.perform(post("/api/player/ads/auto-hunt/complete"))
					.andExpect(status().isOk())
					.andReturn()
					.getResponse()
					.getContentAsString();
				Instant nextAutoHuntAt = Instant.parse(JsonPath.read(autoHuntResponse, "$.nextAutoHuntAdAvailableAt"));
				assertTrue(Duration.between(beforeAutoHunt, nextAutoHuntAt).getSeconds() >= 590);
			} finally {
				economy.reset("autoHuntAdCooldownSeconds");
			}
		}

		@Test
	void fullyUpgradedGoldRateIsCappedAtDefaultSixThousand() throws Exception {
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
			});

			mockMvc.perform(get("/api/player"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.baseGoldPerHour", is(6000)))
					.andExpect(jsonPath("$.goldPerHour", is(6000)))
					.andExpect(jsonPath("$.payoutRatePercent", is(100)));
	}

	@Test
	void skillUpgradeCostsTwoSkillPointsFromLevelTwenty() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			player.getOrCreateSkill(SkillType.STRENGTH).setLevel(19);
			player.setSkillPoints(3);
		});

		String levelTwenty = mockMvc.perform(post("/api/player/skills/upgrade")
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"STRENGTH\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillPoints", is(2)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertEquals(20, readSkillInt(levelTwenty, SkillType.STRENGTH, "level"));
		assertEquals(2, readSkillInt(levelTwenty, SkillType.STRENGTH, "upgradeCost"));

		String levelTwentyOne = mockMvc.perform(post("/api/player/skills/upgrade")
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"STRENGTH\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillPoints", is(0)))
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertEquals(21, readSkillInt(levelTwentyOne, SkillType.STRENGTH, "level"));
		assertEquals(2, readSkillInt(levelTwentyOne, SkillType.STRENGTH, "upgradeCost"));

		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			player.setSkillPoints(1);
		});

		mockMvc.perform(post("/api/player/skills/upgrade")
						.contentType(APPLICATION_JSON)
						.content("{\"type\":\"STRENGTH\"}"))
				.andExpect(status().isConflict());
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
				.andExpect(jsonPath("$.skillPoints", is(2)));

		mockMvc.perform(post("/api/player/reward/friend-invite/claim")
						.contentType(APPLICATION_JSON)
						.content("{\"completedInvites\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.friendInviteRewardCount", is(4)))
				.andExpect(jsonPath("$.skillPoints", is(4)));

		mockMvc.perform(post("/api/player/reward/friend-invite/claim")
						.contentType(APPLICATION_JSON)
						.content("{\"completedInvites\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.friendInviteRewardCount", is(5)))
				.andExpect(jsonPath("$.skillPoints", is(5)));
	}

	@Test
	void friendInviteRewardLimitDoesNotResetOnNextDay() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			player.claimFriendInviteReward(5, 1, 5);
		});

		mockMvc.perform(get("/api/player"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.friendInviteRewardCount", is(5)));

		mockMvc.perform(post("/api/player/reward/friend-invite/claim")
						.contentType(APPLICATION_JSON)
						.content("{\"completedInvites\":1}"))
				.andExpect(status().isConflict());
	}

	@Test
	void dungeonAllowsThreeFreeRunsAndTwoAdRunsWithReentryCooldown() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			player.addDungeonEntryHuntProgress(Duration.ofHours(1).toMillis(), Duration.ofHours(1).toMillis());
		});

		mockMvc.perform(post("/api/player/dungeon/run"))
				.andExpect(status().isOk())
					.andExpect(jsonPath("$.rewardLabel").exists())
					.andExpect(jsonPath("$.state.dungeonCoupon.enabled", is(true)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonRunsToday", is(1)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonDailyLimit", is(5)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonRemainingRuns", is(4)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonAvailable", is(false)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonCooldownSeconds", greaterThan(0)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonRewards[0].probabilityLabel").exists());

		mockMvc.perform(post("/api/player/dungeon/run"))
				.andExpect(status().isConflict());

		Instant now = Instant.now();
			transactionTemplate.executeWithoutResult(status -> {
				Player player = playerRepository.findByUserKey("test-player").orElseThrow();
				Instant afterCooldown = now.plus(Duration.ofHours(2));
				player.enterDungeon(afterCooldown, today, 5, Duration.ZERO);
				player.enterDungeon(afterCooldown, today, 5, Duration.ZERO);
				player.resetDungeonReentryCooldownForTest();
			});

			mockMvc.perform(get("/api/player"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dungeonCoupon.dungeonRunsToday", is(3)))
					.andExpect(jsonPath("$.dungeonCoupon.dungeonRemainingRuns", is(2)))
					.andExpect(jsonPath("$.dungeonCoupon.dungeonAvailable", is(true)));

			mockMvc.perform(post("/api/player/dungeon/run"))
					.andExpect(status().isConflict());

			mockMvc.perform(post("/api/player/ads/dungeon-additional/complete"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonRunsToday", is(4)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonRemainingRuns", is(1)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonAvailable", is(false)));

			transactionTemplate.executeWithoutResult(status -> {
				Player player = playerRepository.findByUserKey("test-player").orElseThrow();
				player.resetDungeonReentryCooldownForTest();
			});

			mockMvc.perform(post("/api/player/ads/dungeon-additional/complete"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonRunsToday", is(5)))
					.andExpect(jsonPath("$.state.dungeonCoupon.dungeonRemainingRuns", is(0)));

			mockMvc.perform(post("/api/player/ads/dungeon-additional/complete"))
					.andExpect(status().isConflict());
		}

	@Test
	void dailyMissionCreatesInboxRewardsAndFinalRewardStartsNextCycleWhenClaimed() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		String dailyResponse = mockMvc.perform(post("/api/player/test/events/daily-mission/complete"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dailyMission.completedToday", is(true)))
				.andExpect(jsonPath("$.eventHub.rewards[0].sourceEventKey", is("daily_mission")))
				.andExpect(jsonPath("$.eventHub.rewards[0].claimable", is(true)))
				.andExpect(jsonPath("$.eventHub.rewards[0].daysRemaining", is(7)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Number dailyRewardId = firstRewardId(dailyResponse, "daily_mission");
		mockMvc.perform(post("/api/player/event-rewards/" + dailyRewardId + "/claim"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.gold", is(300)))
				.andExpect(jsonPath("$.eventHub.rewards[0].claimed", is(true)));

			String cycleResponse = mockMvc.perform(post("/api/player/test/events/daily-mission/complete-cycle"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.dailyMission.completedDays", is(7)))
					.andReturn()
					.getResponse()
					.getContentAsString();

			assertTrue(rewardClaimable(cycleResponse, "daily-mission:cycle:1:final"));
			Number finalRewardId = firstRewardIdByKey(cycleResponse, "daily-mission:cycle:1:final");
			mockMvc.perform(post("/api/player/event-rewards/" + finalRewardId + "/claim"))
					.andExpect(status().isOk())
				.andExpect(jsonPath("$.gold", is(1300)))
				.andExpect(jsonPath("$.skillPoints", is(1)))
				.andExpect(jsonPath("$.dailyMission.cycle", is(2)))
				.andExpect(jsonPath("$.dailyMission.completedDays", is(0)));
	}

	@Test
	void vipTestToolActivatesMembershipAndDailyInboxReward() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		String vipResponse = mockMvc.perform(post("/api/player/test/events/vip/activate"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vipMembership.active", is(true)))
				.andExpect(jsonPath("$.eventHub.rewards[0].sourceEventKey", is("vip_monthly")))
				.andExpect(jsonPath("$.eventHub.rewards[0].claimable", is(true)))
				.andExpect(jsonPath("$.eventHub.rewards[0].daysRemaining", is(7)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Number vipRewardId = firstRewardId(vipResponse, "vip_monthly");
		mockMvc.perform(post("/api/player/event-rewards/" + vipRewardId + "/claim"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.skillPoints", is(1)))
				.andExpect(jsonPath("$.dungeonCoupon.count", is(3)))
				.andExpect(jsonPath("$.dungeonCoupon.bossTicketCount", is(1)));
	}

	@Test
	void bossRaidConsumesBossTicketAndGrantsReward() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			player.addBossRaidTickets(1);
		});

		mockMvc.perform(post("/api/player/boss/raid"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bossName", is("흑요석 골렘")))
				.andExpect(jsonPath("$.rewardLabel").exists())
				.andExpect(jsonPath("$.state.dungeonCoupon.count", is(0)));
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
	void adminPlayerTotalSettledWonUsesCumulativeGoldConversion() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"MAGE\"}"))
				.andExpect(status().isOk());

		Player player = playerRepository.findByUserKey("test-player").orElseThrow();
		player.collectCombatGold(1_800_000_000L, 0);
		playerRepository.saveAndFlush(player);

		AdminPlayerResponse response = adminPlayerService.get("test-player");
		assertEquals(1_800, response.cumulativeGoldEarned());
		assertEquals(1_800, response.totalSettledGold());
		assertEquals(18, response.totalSettledWon());
	}

	@Test
	void combatPowerCountsSharedStatSkillsOnlyOnce() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());

		transactionTemplate.executeWithoutResult(status -> {
			Player player = playerRepository.findByUserKey("test-player").orElseThrow();
			for (int i = 1; i < 10; i++) {
				player.levelUpForTest();
			}
			player.getOrCreateSkill(SkillType.STRENGTH).setLevel(20);
			player.getOrCreateSkill(SkillType.DEXTERITY).setLevel(20);
			player.getOrCreateSkill(SkillType.INTELLIGENCE).setLevel(20);
			player.getOrCreateSkill(SkillType.LUCK).setLevel(20);
			player.getOrCreateSkill(SkillType.MINING_MASTERY).setLevel(20);
			player.setSkillPoints(0);
		});

		AdminPlayerResponse response = adminPlayerService.get("test-player");

		assertEquals(5_237_827, response.combatPower());
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

	private Number firstRewardId(String json, String sourceEventKey) {
		List<Number> values = JsonPath.read(json, "$.eventHub.rewards[?(@.sourceEventKey == '" + sourceEventKey + "')].id");
		return values.get(0);
	}

	private Number firstRewardIdByKey(String json, String rewardKey) {
		List<Number> values = JsonPath.read(json, "$.eventHub.rewards[?(@.rewardKey == '" + rewardKey + "')].id");
		return values.get(0);
	}

	private boolean rewardClaimable(String json, String rewardKey) {
		List<Boolean> values = JsonPath.read(json, "$.eventHub.rewards[?(@.rewardKey == '" + rewardKey + "')].claimable");
		return !values.isEmpty() && values.get(0);
	}

	private int readSkillInt(String json, SkillType type, String field) {
		List<Number> values = JsonPath.read(json, "$.skills[?(@.type == '" + type.name() + "')]." + field);
		return values.get(0).intValue();
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
			playerRepository.saveAndFlush(player);

		String settled = mockMvc.perform(get("/api/player"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return readLong(settled, "$.gold");
	}
}
