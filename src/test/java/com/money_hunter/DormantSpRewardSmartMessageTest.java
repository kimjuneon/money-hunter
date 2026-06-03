package com.money_hunter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.money_hunter.application.PlayerService;
import com.money_hunter.application.TossSmartMessageClient;
import com.money_hunter.domain.AdEventType;
import com.money_hunter.domain.JobType;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
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

@Import({TestcontainersConfiguration.class, DormantSpRewardSmartMessageTest.FakeSmartMessageConfig.class})
@ActiveProfiles("review")
@TestPropertySource(properties = {
		"money-hunter.app.real-smart-message-enabled=true",
		"money-hunter.smart-message.dormant-sp-reward-enabled=true",
		"money-hunter.smart-message.dormant-sp-reward-template-set-code=gold-hunter-GOLD_HUNTER_DORMANT_SP_GRANTED_V1"
})
@AutoConfigureMockMvc
@SpringBootTest
class DormantSpRewardSmartMessageTest {
	private static final String USER_KEY = "dormant-sp-reward-user";
	private static final String TEMPLATE_SET_CODE = "gold-hunter-GOLD_HUNTER_DORMANT_SP_GRANTED_V1";

	@Autowired
	private PlayerService playerService;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private AdEventRepository adEventRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private FakeTossSmartMessageClient smartMessageClient;

	@BeforeEach
	void setUp() {
		adEventRepository.deleteAll();
		playerRepository.deleteAll();
		smartMessageClient.reset();
	}

	@Test
	void dormantSpRewardSendsAndGrantsOnceAtThreeInactiveDays() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		makeDormantForDays(USER_KEY, 3);

		assertThat(playerService.publishDormantSpRewardNotifications()).isEqualTo(1);

		assertThat(smartMessageClient.calls()).containsExactly(new SmartMessageCall(
				USER_KEY,
				TEMPLATE_SET_CODE,
				Map.of()));
		var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
		assertThat(player.getSkillPoints()).isEqualTo(1);
		assertThat(player.getDormantSpRewardSentStage()).isEqualTo(1);
		assertThat(player.getDormantSpRewardStreakAccessedAt()).isEqualTo(player.getLastAccessedAt());
		assertThat(adEventRepository.countByTypeAndOccurredAtAfter(AdEventType.DORMANT_SP_REWARD, Instant.now().minus(Duration.ofMinutes(1))))
				.isEqualTo(1);

		assertThat(playerService.publishDormantSpRewardNotifications()).isZero();
		assertThat(smartMessageClient.calls()).hasSize(1);
	}

	@Test
	void dormantSpRewardRepeatsEveryTwoDaysUpToNextStages() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		setDormantRewardStage(USER_KEY, 5, 1, Duration.ofDays(2).plusMinutes(1));

		assertThat(playerService.publishDormantSpRewardNotifications()).isEqualTo(1);

		var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
		assertThat(player.getSkillPoints()).isEqualTo(1);
		assertThat(player.getDormantSpRewardSentStage()).isEqualTo(2);
		assertThat(smartMessageClient.calls()).hasSize(1);
	}

	@Test
	void dormantSpRewardStartsNewStreakAfterUserReturnsAndLeavesAgain() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
			Instant oldAccessedAt = Instant.now().minus(Duration.ofDays(10));
			player.markAccessed(oldAccessedAt);
			player.markDormantSpRewardSent(oldAccessedAt, 4, Instant.now().minus(Duration.ofDays(1)));
			player.markAccessed(Instant.now().minus(Duration.ofDays(3)).minusSeconds(60));
		});

		assertThat(playerService.publishDormantSpRewardNotifications()).isEqualTo(1);

		var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
		assertThat(player.getDormantSpRewardSentStage()).isEqualTo(1);
		assertThat(player.getSkillPoints()).isEqualTo(1);
	}

	private void makeDormantForDays(String userKey, long days) {
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(userKey).orElseThrow();
			player.markAccessed(Instant.now().minus(Duration.ofDays(days)).minusSeconds(60));
		});
	}

	private void setDormantRewardStage(String userKey, long inactiveDays, int stage, Duration sentAgo) {
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(userKey).orElseThrow();
			Instant accessedAt = Instant.now().minus(Duration.ofDays(inactiveDays)).minusSeconds(60);
			player.markAccessed(accessedAt);
			player.markDormantSpRewardSent(accessedAt, stage, Instant.now().minus(sentAgo));
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
