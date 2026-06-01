package com.money_hunter.application;

public interface TossPromotionClient {
	String issueExecutionKey(String userKey);

	void executePromotion(String userKey, String promotionCode, String executionKey, int amount);

	String getExecutionResult(String userKey, String promotionCode, String executionKey);
}
