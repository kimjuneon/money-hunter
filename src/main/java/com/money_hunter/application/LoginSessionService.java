package com.money_hunter.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import com.money_hunter.domain.LoginSession;
import com.money_hunter.infrastructure.persistence.LoginSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginSessionService {
	private static final int TOKEN_BYTES = 32;
	private static final int SESSION_DAYS = 30;

	private final LoginSessionRepository loginSessionRepository;
	private final Clock clock = Clock.systemUTC();
	private final SecureRandom secureRandom = new SecureRandom();

	public LoginSessionService(LoginSessionRepository loginSessionRepository) {
		this.loginSessionRepository = loginSessionRepository;
	}

	@Transactional
	public IssuedLoginSession issue(String userKey) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(SESSION_DAYS, ChronoUnit.DAYS);
		String token = newToken();
		loginSessionRepository.save(new LoginSession(hash(token), userKey, now, expiresAt));
		return new IssuedLoginSession(token, userKey, expiresAt);
	}

	@Transactional(readOnly = true)
	public Optional<String> resolveUserKey(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		return loginSessionRepository.findByTokenHash(hash(token.trim()))
				.filter(session -> !session.isExpired(clock.instant()))
				.map(LoginSession::getUserKey);
	}

	private String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Session token hashing is unavailable.", exception);
		}
	}

	public record IssuedLoginSession(String token, String userKey, Instant expiresAt) {
	}
}
