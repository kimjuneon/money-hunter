package com.money_hunter.presentation.dto.response;

public record AdminPolicyResponse(
		String key,
		String label,
		String unit,
		long min,
		long max,
		Number value
) {
}
