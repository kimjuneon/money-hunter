package com.money_hunter.application;

import java.util.Map;

public interface TossSmartMessageClient {
	void sendMessage(String userKey, String templateSetCode, Map<String, String> context);
}
