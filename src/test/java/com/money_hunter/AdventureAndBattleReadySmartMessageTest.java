package com.money_hunter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.money_hunter.application.PlayerService;
import com.money_hunter.application.TossSmartMessageClient;
import com.money_hunter.domain.JobType;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.NotificationEventRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

@Import({TestcontainersConfiguration.class, AdventureAndBattleReadySmartMessageTest.FakeSmartMessageConfig.class})
@ActiveProfiles("review")
@TestPropertySource(properties = {
		"money-hunter.app.real-smart-message-enabled=true",
		"money-hunter.app.dungeon-coupon-enabled=true",
		"money-hunter.economy.dungeon-reentry-cooldown-seconds=0",
		"money-hunter.smart-message.dungeon-explore-available-enabled=true",
		"money-hunter.smart-message.dungeon-explore-available-template-set-code=gold-hunter-DUNGEON_EXPLORE_AVAILABLE",
		"money-hunter.smart-message.battle-ready-daily-enabled=true",
		"money-hunter.smart-message.battle-ready-daily-template-set-code=gold-hunter-BATTLE_READY_DAILY"
})
@AutoConfigureMockMvc
@SpringBootTest
class AdventureAndBattleReadySmartMessageTest {
	private static final String USER_KEY = "adventure-battle-ready-message-user";
	private static final String DUNGEON_TEMPLATE_SET_CODE = "gold-hunter-DUNGEON_EXPLORE_AVAILABLE";
	private static final String BATTLE_TEMPLATE_SET_CODE = "gold-hunter-BATTLE_READY_DAILY";
	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	@Autowired
	private PlayerService playerService;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private AdEventRepository adEventRepository;

	@Autowired
	private NotificationEventRepository notificationEventRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private FakeTossSmartMessageClient smartMessageClient;

	@BeforeEach
	void setUp() {
		notificationEventRepository.deleteAll();
		adEventRepository.deleteAll();
		playerRepository.deleteAll();
		smartMessageClient.reset();
	}

	@Test
	void dungeonExploreAvailableSendsOnceUntilExploredThenSendsForNextAvailableRun() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		makeDungeonReady(USER_KEY);

		assertThat(playerService.publishDungeonExploreAvailableNotifications()).isEqualTo(1);
		assertThat(playerService.publishDungeonExploreAvailableNotifications()).isZero();
		assertThat(smartMessageClient.calls()).containsExactly(new SmartMessageCall(
				USER_KEY,
				DUNGEON_TEMPLATE_SET_CODE,
				Map.of(
						"title", "던전 탐험 가능",
						"message", "던전 탐험 준비가 완료됐어요.",
						"landingUrl", "intoss://gold-hunter")));

		playerService.runDungeon(USER_KEY);

		assertThat(playerService.publishDungeonExploreAvailableNotifications()).isEqualTo(1);
		assertThat(smartMessageClient.calls()).hasSize(2);
		assertThat(smartMessageClient.calls().get(1).templateSetCode()).isEqualTo(DUNGEON_TEMPLATE_SET_CODE);
	}

	@Test
	void dungeonExploreAvailableDoesNotSendAfterDailyRunsAreExhausted() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		exhaustDungeonRuns(USER_KEY);

		assertThat(playerService.publishDungeonExploreAvailableNotifications()).isZero();
		assertThat(smartMessageClient.calls()).isEmpty();
	}

	@Test
	void battleReadyDailySendsFromFirstInactiveDayButStopsAfterFiveDays() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		makeInactiveFor(USER_KEY, Duration.ofDays(1).plusMinutes(1));

		assertThat(playerService.publishBattleReadyDailyNotifications()).isEqualTo(1);
		assertThat(playerService.publishBattleReadyDailyNotifications()).isZero();
		assertThat(smartMessageClient.calls()).containsExactly(new SmartMessageCall(
				USER_KEY,
				BATTLE_TEMPLATE_SET_CODE,
				Map.of(
						"title", "전투 준비 완료",
						"message", "자동 전투 보상이 기다리고 있어요.",
						"landingUrl", "intoss://gold-hunter",
						"day", "1")));

		smartMessageClient.reset();
		makeInactiveFor(USER_KEY, Duration.ofDays(6));

		assertThat(playerService.publishBattleReadyDailyNotifications()).isZero();
		assertThat(smartMessageClient.calls()).isEmpty();
	}

	private void makeDungeonReady(String userKey) {
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(userKey).orElseThrow();
			player.resetDungeonDailyLimitForTest(LocalDate.now(SEOUL_ZONE));
			player.addDungeonEntryHuntProgress(3_600_000L, 3_600_000L);
		});
	}

	private void makeInactiveFor(String userKey, Duration duration) {
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(userKey).orElseThrow();
			player.markAccessed(Instant.now().minus(duration));
		});
	}

	private void exhaustDungeonRuns(String userKey) {
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(userKey).orElseThrow();
			LocalDate today = LocalDate.now(SEOUL_ZONE);
			Instant now = Instant.now();
			player.resetDungeonDailyLimitForTest(today);
			player.addDungeonEntryHuntProgress(3_600_000L, 3_600_000L);
			for (int i = 0; i < 5; i++) {
				player.enterDungeon(now.plusSeconds(i), today, 5, Duration.ZERO);
			}
		});
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeSmartMessageConfig {
		@Bean
		@Primary
		FakeTossSmartMessageClient fakeTossSmartMessageClient() {
			return new FakeTossSmartMessageClient();
		}
	}

	record SmartMessageCall(String userKey, String templateSetCode, Map<String, String> context) {
	}

	static class FakeTossSmartMessageClient implements TossSmartMessageClient {
		private final List<SmartMessageCall> calls = new ArrayList<>();

		@Override
		public SmartMessageSendResult sendMessage(String userKey, String templateSetCode, Map<String, String> context) {
			calls.add(new SmartMessageCall(userKey, templateSetCode, Map.copyOf(context)));
			return new SmartMessageSendResult(1, 1, 0, "");
		}

		List<SmartMessageCall> calls() {
			return List.copyOf(calls);
		}

		void reset() {
			calls.clear();
		}
	}
}
