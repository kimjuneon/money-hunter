package com.money_hunter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.money_hunter.application.PlayerService;
import com.money_hunter.application.TossSmartMessageClient;
import com.money_hunter.domain.JobType;
import com.money_hunter.domain.NotificationType;
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

@Import({TestcontainersConfiguration.class, AutoHuntSmartMessageRetryTest.FakeSmartMessageConfig.class})
@ActiveProfiles("review")
@TestPropertySource(properties = {
		"money-hunter.app.real-smart-message-enabled=true",
		"money-hunter.smart-message.auto-hunt-ended-template-set-code=gold-hunter-money_hunter_auto_hunt_ended",
		"money-hunter.smart-message.auto-hunt-ending-soon-enabled=true",
		"money-hunter.smart-message.auto-hunt-ending-soon-template-set-code=gold-hunter-AUTO_HUNT_ENDING_SOON_V1"
})
@AutoConfigureMockMvc
@SpringBootTest
class AutoHuntSmartMessageRetryTest {
	private static final String USER_KEY = "auto-hunt-smart-message-retry-user";
	private static final String ENDING_SOON_TEMPLATE_SET_CODE = "gold-hunter-AUTO_HUNT_ENDING_SOON_V1";

	@Autowired
	private PlayerService playerService;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private NotificationEventRepository notificationEventRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private FakeTossSmartMessageClient smartMessageClient;

	@BeforeEach
	void setUp() {
		notificationEventRepository.deleteAll();
		playerRepository.deleteAll();
		smartMessageClient.reset();
	}

	@Test
	void autoHuntEndedSmartMessageFailuresAreRetriedWithoutDuplicatingInAppNotifications() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
			Instant autoHuntEndsAt = Instant.now().minusSeconds(60);
			player.setLastSettledAt(autoHuntEndsAt.minusSeconds(300));
			player.setAutoHuntEndsAt(autoHuntEndsAt);
			player.clearAutoHuntEndNotification();
		});

		smartMessageClient.failSends(true);
		assertThat(playerService.publishAutoHuntEndNotifications()).isEqualTo(1);
		assertThat(smartMessageClient.attempts()).isEqualTo(1);

		var failedPlayer = playerRepository.findByUserKey(USER_KEY).orElseThrow();
		assertThat(failedPlayer.getAutoHuntEndNotifiedAt()).isNull();
		assertThat(failedPlayer.getAutoHuntEndSmartMessageAttemptedAt()).isNotNull();
		var failedNotifications = notificationEventRepository.findByPlayerAndTypeAndReadAtIsNull(failedPlayer, NotificationType.AUTO_HUNT_ENDED);
		assertThat(failedNotifications).hasSize(1);
		assertThat(failedNotifications.get(0).getSettledGold()).isPositive();

		assertThat(playerService.publishAutoHuntEndNotifications()).isZero();
		assertThat(smartMessageClient.attempts()).isEqualTo(1);

		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
			player.markAutoHuntEndSmartMessageAttempted(Instant.now().minus(Duration.ofMinutes(6)));
		});
		smartMessageClient.failSends(false);

		assertThat(playerService.publishAutoHuntEndNotifications()).isEqualTo(1);
		assertThat(smartMessageClient.attempts()).isEqualTo(2);

		var sentPlayer = playerRepository.findByUserKey(USER_KEY).orElseThrow();
		assertThat(sentPlayer.getAutoHuntEndNotifiedAt()).isNotNull();
		assertThat(notificationEventRepository.findByPlayerAndTypeAndReadAtIsNull(sentPlayer, NotificationType.AUTO_HUNT_ENDED)).hasSize(1);
	}

	@Test
	void autoHuntEndingSoonSmartMessageSendsOnceForThirtyMinuteWindow() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
			player.setAutoHuntEndsAt(Instant.now().plus(Duration.ofMinutes(29)));
			player.markAutoHuntAdClaimed(Instant.now().minus(Duration.ofMinutes(10)));
		});

		assertThat(playerService.publishAutoHuntEndingSoonNotifications()).isEqualTo(1);
		assertThat(playerService.publishAutoHuntEndingSoonNotifications()).isZero();
		assertThat(smartMessageClient.calls()).containsExactly(new SmartMessageCall(
				USER_KEY,
				ENDING_SOON_TEMPLATE_SET_CODE,
				Map.of(
						"title", "사냥종료 임박",
						"message", "자동 사냥이 30분 남았어요.",
						"landingUrl", "intoss://gold-hunter")));
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
		private final AtomicInteger attempts = new AtomicInteger();
		private final List<SmartMessageCall> calls = new ArrayList<>();
		private boolean failSends;

		@Override
		public SmartMessageSendResult sendMessage(String userKey, String templateSetCode, Map<String, String> context) {
			attempts.incrementAndGet();
			calls.add(new SmartMessageCall(userKey, templateSetCode, Map.copyOf(context)));
			if (failSends) {
				throw new IllegalStateException("simulated smart message failure");
			}
			return new SmartMessageSendResult(1, 1, 0, "");
		}

		void failSends(boolean failSends) {
			this.failSends = failSends;
		}

		int attempts() {
			return attempts.get();
		}

		List<SmartMessageCall> calls() {
			return List.copyOf(calls);
		}

		void reset() {
			attempts.set(0);
			calls.clear();
			failSends = false;
		}
	}
}
