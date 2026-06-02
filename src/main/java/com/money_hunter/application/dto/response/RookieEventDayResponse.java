package com.money_hunter.application.dto.response;

import java.util.List;

public record RookieEventDayResponse(
		int day,
		String title,
		boolean current,
		boolean completed,
		boolean locked,
		String rewardLabel,
		boolean rewardClaimed,
		List<RookieEventMissionResponse> missions
) {
}
