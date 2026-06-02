package com.money_hunter.application.dto.response;

public record RookieEventMissionResponse(
		String key,
		String label,
		String progressText,
		int progressPercent,
		boolean completed
) {
}
