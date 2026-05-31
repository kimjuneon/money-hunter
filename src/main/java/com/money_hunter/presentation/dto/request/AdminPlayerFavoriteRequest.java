package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminPlayerFavoriteRequest(
		@NotNull(message = "즐겨찾기 여부가 필요해요.")
		Boolean favorite,
		String reason
) {
}
