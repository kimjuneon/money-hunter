package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPlayerPetActionRequest(
		@NotBlank(message = "펫 처리 유형이 필요해요.")
		String action,
		@Size(max = 500) String reason
) {
}
