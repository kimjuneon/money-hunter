package com.money_hunter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.money_hunter.application.PlayerService;
import com.money_hunter.application.TossSmartMessageClient;
import com.money_hunter.domain.JobType;
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

@Import({TestcontainersConfiguration.class, RookieEventMissionSmartMessageTest.FakeSmartMessageConfig.class})
@ActiveProfiles("review")
@TestPropertySource(properties = {
		"money-hunter.app.real-smart-message-enabled=true",
		"money-hunter.smart-message.rookie-event-mission-arrived-enabled=true",
		"money-hunter.smart-message.rookie-event-mission-arrived-template-set-code=gold-hunter-GOLD_HUNTER_ROOKIE_MISSION_ARRIVED_V1"
})
@AutoConfigureMockMvc
@SpringBootTest
class RookieEventMissionSmartMessageTest {
	private static final String USER_KEY = "rookie-event-message-user";
	private static final String TEMPLATE_SET_CODE = "gold-hunter-GOLD_HUNTER_ROOKIE_MISSION_ARRIVED_V1";
	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	@Autowired
	private PlayerService playerService;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private FakeTossSmartMessageClient smartMessageClient;

	@BeforeEach
	void setUp() {
		playerRepository.deleteAll();
		smartMessageClient.reset();
	}

	@Test
	void rookieEventMissionMessageSendsCurrentDayOnce() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		startRookieEvent(USER_KEY);

		assertThat(playerService.publishRookieEventMissionNotifications()).isZero();
		assertThat(smartMessageClient.calls()).isEmpty();

		playerService.markRookieEventMissionNotificationAgreed(USER_KEY);
		assertThat(playerService.publishRookieEventMissionNotifications()).isEqualTo(1);

		assertThat(smartMessageClient.calls()).containsExactly(new SmartMessageCall(
				USER_KEY,
				TEMPLATE_SET_CODE,
				Map.of("day", "1")));
		var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
		assertThat(player.getRookieEventMissionMessageSentDate()).isEqualTo(todayInSeoul());
		assertThat(player.getRookieEventMissionMessageSentDay()).isEqualTo(1);

		assertThat(playerService.publishRookieEventMissionNotifications()).isZero();
		assertThat(smartMessageClient.calls()).hasSize(1);
	}

	@Test
	void rookieEventMissionMessageSkipsUserWhoCompletedTodayMission() {
		playerService.chooseJob(USER_KEY, JobType.WARRIOR);
		startRookieEvent(USER_KEY);
		playerService.markRookieEventMissionNotificationAgreed(USER_KEY);
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(USER_KEY).orElseThrow();
			player.completeRookieEventDay(todayInSeoul(), Instant.now(), 7);
		});

		assertThat(playerService.publishRookieEventMissionNotifications()).isZero();
		assertThat(smartMessageClient.calls()).isEmpty();
	}

	private void startRookieEvent(String userKey) {
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(userKey).orElseThrow();
			player.startRookieEvent(Instant.now().minusSeconds(60), todayInSeoul());
		});
	}

	private LocalDate todayInSeoul() {
		return LocalDate.now(SEOUL_ZONE);
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
