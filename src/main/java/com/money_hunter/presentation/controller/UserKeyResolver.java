package com.money_hunter.presentation.controller;

import java.security.Principal;
import java.util.Optional;
import java.util.regex.Pattern;

import com.money_hunter.application.LoginSessionService;
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
	private static final String DISTRIBUTION_TARGET_HEADER = "X-Money-Hunter-Distribution-Target";
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String ONESTORE_DISTRIBUTION_TARGET = "ONESTORE";
	private static final Pattern GUEST_USER_KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,120}");

	private final AppProperties appProperties;
	private final LoginSessionService loginSessionService;

	public UserKeyResolver(AppProperties appProperties, LoginSessionService loginSessionService) {
		this.appProperties = appProperties;
		this.loginSessionService = loginSessionService;
	}

	public String resolve(Principal principal) {
		if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
			return principal.getName();
		}
		Optional<String> sessionUserKey = sessionUserKeyFromHeader();
		if (sessionUserKey.isPresent()) {
			return sessionUserKey.get();
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

	private Optional<String> sessionUserKeyFromHeader() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return Optional.empty();
		}
		String authorization = attributes.getRequest().getHeader(AUTHORIZATION_HEADER);
		if (authorization == null || authorization.isBlank()) {
			return Optional.empty();
		}
		if (!authorization.startsWith(BEARER_PREFIX)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login session.");
		}
		return Optional.of(loginSessionService.resolveUserKey(authorization.substring(BEARER_PREFIX.length()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login session expired.")));
	}

	private Optional<String> guestUserKeyFromHeader() {
		if (!appProperties.oneStoreTarget() && !isOneStoreRequest()) {
			return Optional.empty();
		}
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

	private boolean isOneStoreRequest() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return false;
		}
		String distributionTarget = attributes.getRequest().getHeader(DISTRIBUTION_TARGET_HEADER);
		return ONESTORE_DISTRIBUTION_TARGET.equalsIgnoreCase(distributionTarget);
	}
}
