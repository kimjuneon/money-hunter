package com.money_hunter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
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
@TestPropertySource(properties = "money-hunter.app.real-smart-message-enabled=true")
@AutoConfigureMockMvc
@SpringBootTest
class AutoHuntSmartMessageRetryTest {
	private static final String USER_KEY = "auto-hunt-smart-message-retry-user";

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
			player.setAutoHuntEndsAt(Instant.now().minusSeconds(60));
			player.clearAutoHuntEndNotification();
		});

		smartMessageClient.failSends(true);
		assertThat(playerService.publishAutoHuntEndNotifications()).isEqualTo(1);
		assertThat(smartMessageClient.attempts()).isEqualTo(1);

		var failedPlayer = playerRepository.findByUserKey(USER_KEY).orElseThrow();
		assertThat(failedPlayer.getAutoHuntEndNotifiedAt()).isNull();
		assertThat(failedPlayer.getAutoHuntEndSmartMessageAttemptedAt()).isNotNull();
		assertThat(notificationEventRepository.findByPlayerAndTypeAndReadAtIsNull(failedPlayer, NotificationType.AUTO_HUNT_ENDED)).hasSize(1);

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

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeSmartMessageConfig {
		@Bean
		@Primary
		FakeTossSmartMessageClient fakeTossSmartMessageClient() {
			return new FakeTossSmartMessageClient();
		}
	}

	static class FakeTossSmartMessageClient implements TossSmartMessageClient {
		private final AtomicInteger attempts = new AtomicInteger();
		private boolean failSends;

		@Override
		public SmartMessageSendResult sendMessage(String userKey, String templateSetCode, Map<String, String> context) {
			attempts.incrementAndGet();
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

		void reset() {
			attempts.set(0);
			failSends = false;
		}
	}
}
