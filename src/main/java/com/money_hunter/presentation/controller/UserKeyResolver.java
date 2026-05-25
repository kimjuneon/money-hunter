package com.money_hunter.presentation.controller;

import java.security.Principal;

import com.money_hunter.infrastructure.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserKeyResolver {
	private static final String GUEST_USER_KEY = "test-player";

	private final AppProperties appProperties;

	public UserKeyResolver(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public String resolve(Principal principal) {
		if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
			return principal.getName();
		}
		if (appProperties.guestUserEnabled()) {
			return GUEST_USER_KEY;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login is required.");
	}
}
