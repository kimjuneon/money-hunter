package com.money_hunter.application.dto.response;

public record EventSummaryResponse(
		String key,
		String title,
		String description,
		String status,
		String progressText,
		String rewardText,
		boolean visible,
		boolean active,
		boolean completed,
		boolean claimable
) {
}
