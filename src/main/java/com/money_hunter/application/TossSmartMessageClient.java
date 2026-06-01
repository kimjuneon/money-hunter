package com.money_hunter.application;

import java.util.Map;

public interface TossSmartMessageClient {
	SmartMessageSendResult sendMessage(String userKey, String templateSetCode, Map<String, String> context);

	record SmartMessageSendResult(
			int msgCount,
			int sentPushCount,
			int sentInboxCount,
			String failureSummary
	) {
	}
}
