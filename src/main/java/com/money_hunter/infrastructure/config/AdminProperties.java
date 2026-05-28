package com.money_hunter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.admin")
public record AdminProperties(
		String username,
		String passwordBcrypt,
		String password,
		boolean allowPlainPassword
) {
	public boolean hasCredential() {
		return username != null
				&& !username.isBlank()
				&& (hasBcryptPassword() || hasPlainPassword());
	}

	public boolean hasBcryptPassword() {
		return passwordBcrypt != null && !passwordBcrypt.isBlank();
	}

	public boolean hasPlainPassword() {
		return allowPlainPassword && password != null && !password.isBlank();
	}
}
