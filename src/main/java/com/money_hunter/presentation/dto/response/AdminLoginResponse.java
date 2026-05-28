package com.money_hunter.presentation.dto.response;

import java.time.Instant;

import com.money_hunter.application.AdminAuthService.IssuedAdminSession;

public record AdminLoginResponse(
		String accessToken,
		String tokenType,
		String username,
		Instant expiresAt
) {
	public static AdminLoginResponse from(IssuedAdminSession session) {
		return new AdminLoginResponse(session.accessToken(), "Bearer", session.username(), session.expiresAt());
	}
}
