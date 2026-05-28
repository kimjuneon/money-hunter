package com.money_hunter.infrastructure.config;

import java.util.Arrays;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AdminCredentialStartupValidator {
	private final AdminProperties properties;
	private final Environment environment;

	public AdminCredentialStartupValidator(AdminProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	@PostConstruct
	void validate() {
		if (!properties.hasCredential()) {
			throw new IllegalStateException(
					"Admin credentials are required. Set MONEY_HUNTER_ADMIN_USERNAME and "
							+ "MONEY_HUNTER_ADMIN_PASSWORD_BCRYPT, or enable plain password only for local development.");
		}
		if (!isLocalProfile() && properties.hasPlainPassword()) {
			throw new IllegalStateException("Plain admin password is allowed only with the local profile.");
		}
	}

	private boolean isLocalProfile() {
		return Arrays.stream(environment.getActiveProfiles()).anyMatch("local"::equals)
				|| Arrays.stream(environment.getDefaultProfiles()).anyMatch("local"::equals);
	}
}
