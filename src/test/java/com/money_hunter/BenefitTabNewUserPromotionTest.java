package com.money_hunter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.money_hunter.application.PlayerService;
import com.money_hunter.application.dto.response.RewardClaimResponse;
import com.money_hunter.application.TossLoginClient;
import com.money_hunter.application.TossPromotionClient;
import com.money_hunter.domain.JobType;
import com.money_hunter.domain.RewardClaimStatus;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@Import({TestcontainersConfiguration.class, BenefitTabNewUserPromotionTest.FakeTossClients.class})
@ActiveProfiles("prod")
@TestPropertySource(properties = {
		"money-hunter.app.toss-login-enabled=true",
		"money-hunter.app.toss-user-key-enabled=true",
		"money-hunter.app.real-toss-point-rewards-enabled=true",
		"money-hunter.promotion.reward-claim-code=MAIN_PROMOTION",
		"money-hunter.promotion.benefit-tab-new-user-code=BENEFIT_NEW_USER_PROMOTION",
		"money-hunter.promotion.benefit-tab-new-user-amount=30"
})
@AutoConfigureMockMvc
@SpringBootTest
class BenefitTabNewUserPromotionTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PlayerService playerService;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private RecordingTossPromotionClient tossPromotionClient;

	@BeforeEach
	void setUp() {
		tossPromotionClient.clear();
	}

	@Test
	void benefitPathFirstTossLoginReceivesAdditionalPromotionOnce() throws Exception {
		login("benefit-first-user", "/benefit");
		playerService.chooseJob("benefit-first-user", JobType.WARRIOR);

		addGold("benefit-first-user", 1_000);
		playerService.claimRewardAfterAd("benefit-first-user", "benefit-claim-1");

		assertThat(tossPromotionClient.executions())
				.extracting(ExecutedPromotion::promotionCode)
				.containsExactly("MAIN_PROMOTION", "BENEFIT_NEW_USER_PROMOTION");
		assertThat(tossPromotionClient.executions())
				.extracting(ExecutedPromotion::amount)
				.containsExactly(10, 30);

		addGold("benefit-first-user", 1_000);
		playerService.claimRewardAfterAd("benefit-first-user", "benefit-claim-2");

		assertThat(tossPromotionClient.executions())
				.extracting(ExecutedPromotion::promotionCode)
				.containsExactly("MAIN_PROMOTION", "BENEFIT_NEW_USER_PROMOTION", "MAIN_PROMOTION");
	}

	@Test
	void benefitPathDoesNotQualifyExistingTossLoginUser() throws Exception {
		login("existing-user", "");
		playerService.chooseJob("existing-user", JobType.WARRIOR);
		login("existing-user", "/benefit");

		addGold("existing-user", 1_000);
		playerService.claimRewardAfterAd("existing-user", "existing-claim-1");

		assertThat(tossPromotionClient.executions())
				.extracting(ExecutedPromotion::promotionCode)
				.containsExactly("MAIN_PROMOTION");
	}

	@Test
	void benefitPromotionFailureDoesNotBlockMainRewardClaim() throws Exception {
		login("benefit-failure-user", "/benefit");
		playerService.chooseJob("benefit-failure-user", JobType.WARRIOR);
		tossPromotionClient.failPromotionCode("BENEFIT_NEW_USER_PROMOTION");

		addGold("benefit-failure-user", 1_000);
		RewardClaimResponse response = playerService.claimRewardAfterAd("benefit-failure-user", "benefit-failure-claim");

		assertThat(response.status()).isEqualTo(RewardClaimStatus.GRANTED);
		assertThat(tossPromotionClient.executions())
				.extracting(ExecutedPromotion::promotionCode)
				.containsExactly("MAIN_PROMOTION");
	}

	private void addGold(String userKey, long amount) {
		transactionTemplate.executeWithoutResult(status -> {
			var player = playerRepository.findByUserKey(userKey).orElseThrow();
			player.addGold(amount);
		});
	}

	private String login(String authorizationCode, String entryPath) throws Exception {
		String body = """
				{"authorizationCode":"%s","referrer":"test","entryPath":"%s"}
				""".formatted(authorizationCode, entryPath);
		String response = mockMvc.perform(post("/api/auth/toss/login")
						.contentType(APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}

	@TestConfiguration
	static class FakeTossClients {
		@Bean
		@Primary
		TossLoginClient tossLoginClient() {
			return (authorizationCode, referrer) -> new TossLoginClient.TossLoginUser(authorizationCode);
		}

		@Bean
		@Primary
		RecordingTossPromotionClient tossPromotionClient() {
			return new RecordingTossPromotionClient();
		}
	}

	record ExecutedPromotion(String userKey, String promotionCode, String executionKey, int amount) {
	}

	static class RecordingTossPromotionClient implements TossPromotionClient {
		private final List<ExecutedPromotion> executions = new ArrayList<>();
		private final List<String> failedPromotionCodes = new ArrayList<>();
		private int keySequence = 0;

		@Override
		public String issueExecutionKey(String userKey) {
			keySequence += 1;
			return "execution-key-" + keySequence;
		}

		@Override
		public void executePromotion(String userKey, String promotionCode, String executionKey, int amount) {
			if (failedPromotionCodes.contains(promotionCode)) {
				throw new IllegalStateException("simulated promotion failure");
			}
			executions.add(new ExecutedPromotion(userKey, promotionCode, executionKey, amount));
		}

		@Override
		public String getExecutionResult(String userKey, String promotionCode, String executionKey) {
			return "SUCCESS";
		}

		List<ExecutedPromotion> executions() {
			return List.copyOf(executions);
		}

		void clear() {
			executions.clear();
			failedPromotionCodes.clear();
			keySequence = 0;
		}

		void failPromotionCode(String promotionCode) {
			failedPromotionCodes.add(promotionCode);
		}
	}
}
