package com.money_hunter.application;

public record TossIapOrderStatus(
		String orderId,
		String productId,
		String status,
		String reason
) {
	public boolean isPaymentCompletedForGrant() {
		return "PAYMENT_COMPLETED".equals(status()) || "PURCHASED".equals(status());
	}
}
