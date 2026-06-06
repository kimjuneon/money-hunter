package com.money_hunter.infrastructure.toss;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.money_hunter.application.TossPromotionClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Profile({"local", "review"})
@Component
public class LocalRecordingTossPromotionClient implements TossPromotionClient {
	private final CopyOnWriteArrayList<RecordedPromotion> executions = new CopyOnWriteArrayList<>();
	private final AtomicInteger keySequence = new AtomicInteger();

	@Override
	public String issueExecutionKey(String userKey) {
		return "local-promotion-key-" + keySequence.incrementAndGet();
	}

	@Override
	public void executePromotion(String userKey, String promotionCode, String executionKey, int amount) {
		executions.add(new RecordedPromotion(
				userKey,
				promotionCode,
				executionKey,
				amount,
				"SUCCESS",
				Instant.now()));
	}

	@Override
	public String getExecutionResult(String userKey, String promotionCode, String executionKey) {
		return executions.stream()
				.filter(execution -> execution.userKey().equals(userKey))
				.filter(execution -> execution.promotionCode().equals(promotionCode))
				.filter(execution -> execution.executionKey().equals(executionKey))
				.findFirst()
				.map(RecordedPromotion::result)
				.orElse("SUCCESS");
	}

	public List<RecordedPromotion> executionsFor(String userKey) {
		return executions.stream()
				.filter(execution -> execution.userKey().equals(userKey))
				.toList();
	}

	public void clearFor(String userKey) {
		executions.removeIf(execution -> execution.userKey().equals(userKey));
	}

	public record RecordedPromotion(
			String userKey,
			String promotionCode,
			String executionKey,
			int amount,
			String result,
			Instant executedAt
	) {
	}
}
