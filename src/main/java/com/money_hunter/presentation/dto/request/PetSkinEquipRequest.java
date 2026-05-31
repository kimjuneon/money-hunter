package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PetSkinEquipRequest(
		@Min(1) @Max(2) int slot
) {
}
