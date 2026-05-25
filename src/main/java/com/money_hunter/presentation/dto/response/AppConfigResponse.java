package com.money_hunter.presentation.dto.response;

public record AppConfigResponse(
		boolean reviewToolsEnabled,
		boolean guestUserEnabled,
		String environment
) {
}
