package com.money_hunter.presentation.controller;

import java.security.Principal;
import java.util.Optional;
import java.util.regex.Pattern;

import com.money_hunter.infrastructure.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserKeyResolver {
	private static final String GUEST_USER_KEY = "test-player";
	private static final String GUEST_USER_KEY_HEADER = "X-Money-Hunter-Guest-Key";
	private static final Pattern GUEST_USER_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,120}");

	private final AppProperties appProperties;

	public UserKeyResolver(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public String resolve(Principal principal) {
		if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
			return principal.getName();
		}
		if (appProperties.guestUserEnabled()) {
			Optional<String> guestUserKey = guestUserKeyFromHeader();
			if (guestUserKey.isPresent()) {
				return guestUserKey.get();
			}
			return GUEST_USER_KEY;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required.");
	}

	private Optional<String> guestUserKeyFromHeader() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return Optional.empty();
		}
		HttpServletRequest request = attributes.getRequest();
		String rawGuestUserKey = request.getHeader(GUEST_USER_KEY_HEADER);
		if (rawGuestUserKey == null || rawGuestUserKey.isBlank()) {
			return Optional.empty();
		}
		String guestUserKey = rawGuestUserKey.trim();
		if (!GUEST_USER_KEY_PATTERN.matcher(guestUserKey).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid guest user key.");
		}
		return Optional.of(guestUserKey);
	}
}
