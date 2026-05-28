package com.money_hunter.application.dto.response;

import java.time.Instant;

import com.money_hunter.application.AdminAccessGuard.AdminContext;

public record AdminMeResponse(
		String username,
		Instant expiresAt
) {
	public static AdminMeResponse from(AdminContext context) {
		return new AdminMeResponse(context.username(), context.expiresAt());
	}
}
