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

import com.money_hunter.domain.AdminSession;
import com.money_hunter.infrastructure.config.AdminProperties;
import com.money_hunter.infrastructure.persistence.AdminSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {
	private static final int TOKEN_BYTES = 32;
	private static final int SESSION_HOURS = 12;

	private final AdminProperties properties;
	private final AdminSessionRepository adminSessionRepository;
	private final Clock clock = Clock.systemUTC();
	private final SecureRandom secureRandom = new SecureRandom();
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public AdminAuthService(AdminProperties properties, AdminSessionRepository adminSessionRepository) {
		this.properties = properties;
		this.adminSessionRepository = adminSessionRepository;
	}

	@Transactional
	public IssuedAdminSession login(String loginId, String password) {
		if (!properties.hasCredential()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관리자 계정이 아직 설정되지 않았어요.");
		}
		if (!matchesUsername(loginId) || !matchesPassword(password)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않아요.");
		}
		Instant now = clock.instant();
		Instant expiresAt = now.plus(SESSION_HOURS, ChronoUnit.HOURS);
		String token = newToken();
		String tokenHash = hash(token);
		adminSessionRepository.deleteByExpiresAtBefore(now.minus(1, ChronoUnit.DAYS));
		adminSessionRepository.save(new AdminSession(tokenHash, properties.username().trim(), now, expiresAt));
		return new IssuedAdminSession(token, properties.username().trim(), expiresAt);
	}

	@Transactional(readOnly = true)
	public Optional<AdminAccessGuard.AdminContext> resolve(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		String tokenHash = hash(token.trim());
		return adminSessionRepository.findByTokenHash(tokenHash)
				.filter(session -> session.isActive(clock.instant()))
				.map(session -> new AdminAccessGuard.AdminContext(
						session.getUsername(),
						tokenHash.substring(0, 12),
						session.getExpiresAt()));
	}

	@Transactional
	public void logout(String token) {
		if (token == null || token.isBlank()) {
			return;
		}
		adminSessionRepository.findByTokenHash(hash(token.trim()))
				.filter(session -> session.isActive(clock.instant()))
				.ifPresent(session -> session.revoke(clock.instant()));
	}

	private boolean matchesUsername(String loginId) {
		if (loginId == null) {
			return false;
		}
		return MessageDigest.isEqual(
				properties.username().trim().getBytes(StandardCharsets.UTF_8),
				loginId.trim().getBytes(StandardCharsets.UTF_8));
	}

	private boolean matchesPassword(String password) {
		if (password == null) {
			return false;
		}
		if (properties.passwordBcrypt() != null && !properties.passwordBcrypt().isBlank()) {
			try {
				return passwordEncoder.matches(password, properties.passwordBcrypt().trim());
			} catch (IllegalArgumentException exception) {
				throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "관리자 비밀번호 설정이 올바르지 않아요.");
			}
		}
		if (properties.allowPlainPassword() && properties.password() != null && !properties.password().isBlank()) {
			return MessageDigest.isEqual(
					properties.password().getBytes(StandardCharsets.UTF_8),
					password.getBytes(StandardCharsets.UTF_8));
		}
		return false;
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
			throw new IllegalStateException("Admin session token hashing is unavailable.", exception);
		}
	}

	public record IssuedAdminSession(String accessToken, String username, Instant expiresAt) {
	}
}
