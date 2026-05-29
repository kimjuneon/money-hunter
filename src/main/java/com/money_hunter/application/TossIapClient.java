package com.money_hunter.application;

public interface TossIapClient {
	TossIapOrderStatus getOrderStatus(String userKey, String orderId);
}
