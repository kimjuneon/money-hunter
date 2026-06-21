package com.money_hunter.presentation.dto.request;

import com.money_hunter.domain.AdClientEventType;
import com.money_hunter.domain.AdEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdClientEventRequest(
		@NotNull AdEventType type,
		@NotNull AdClientEventType eventType,
		@Size(max = 60) String adGroupKey,
		@Size(max = 120) String adGroupId,
		@Size(max = 120) String sessionToken,
		@Size(max = 500) String errorMessage
) {
}
