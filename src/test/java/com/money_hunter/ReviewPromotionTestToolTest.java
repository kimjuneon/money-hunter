package com.money_hunter;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("review")
@TestPropertySource(properties = {
		"money-hunter.app.real-toss-point-rewards-enabled=true",
		"money-hunter.promotion.reward-claim-code=REVIEW_MAIN_PROMOTION",
		"money-hunter.promotion.benefit-tab-new-user-code=REVIEW_BENEFIT_NEW_USER_PROMOTION",
		"money-hunter.promotion.benefit-tab-new-user-amount=30"
})
@AutoConfigureMockMvc
@SpringBootTest
class ReviewPromotionTestToolTest {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void reviewToolCanExerciseBenefitTabPromotionOnce() throws Exception {
		mockMvc.perform(post("/api/player/test/reset"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/test/job")
						.contentType(APPLICATION_JSON)
						.content("{\"job\":\"WARRIOR\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/player/test/benefit-tab-entry"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/player/test/claim-reward"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("GRANTED")));

		mockMvc.perform(get("/api/player/test/promotion-executions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].promotionCode", is("REVIEW_MAIN_PROMOTION")))
				.andExpect(jsonPath("$[0].amount", is(10)))
				.andExpect(jsonPath("$[1].promotionCode", is("REVIEW_BENEFIT_NEW_USER_PROMOTION")))
				.andExpect(jsonPath("$[1].amount", is(30)));

		mockMvc.perform(post("/api/player/test/claim-reward"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("GRANTED")));

		mockMvc.perform(get("/api/player/test/promotion-executions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[2].promotionCode", is("REVIEW_MAIN_PROMOTION")));
	}
}
