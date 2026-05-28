package com.money_hunter.presentation.dto.request;

public record AdminPolicyUpdateRequest(
		String key,
		Long value,
		boolean resetToDefault,
		String reason
) {
}
