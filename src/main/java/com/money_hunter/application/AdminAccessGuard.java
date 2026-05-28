package com.money_hunter.application;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminAccessGuard {
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final AdminAuthService adminAuthService;

	public AdminAccessGuard(AdminAuthService adminAuthService) {
		this.adminAuthService = adminAuthService;
	}

	public AdminContext require(HttpServletRequest request) {
		String token = tokenFrom(request);
		if (token == null || token.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 로그인이 필요해요.");
		}
		return adminAuthService.resolve(token)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 세션이 만료되었어요."));
	}

	public String tokenFrom(HttpServletRequest request) {
		String authorization = request.getHeader(AUTHORIZATION_HEADER);
		if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
			return authorization.substring(BEARER_PREFIX.length());
		}
		return null;
	}

	public record AdminContext(String username, String actorFingerprint, Instant expiresAt) {
	}
}
