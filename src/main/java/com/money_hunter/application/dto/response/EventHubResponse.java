package com.money_hunter.application.dto.response;

import java.util.List;

public record EventHubResponse(
		List<EventSummaryResponse> events,
		List<EventRewardResponse> rewards,
		int participableEventCount,
		int claimableRewardCount
) {
}
