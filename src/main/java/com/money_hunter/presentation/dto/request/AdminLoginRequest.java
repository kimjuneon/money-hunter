package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
		@NotBlank String loginId,
		@NotBlank String password
) {
}
