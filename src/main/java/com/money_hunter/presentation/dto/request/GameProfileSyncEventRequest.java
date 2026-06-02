package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record GameProfileSyncEventRequest(
		@Size(max = 40)
		String statusCode,
		@Size(max = 40)
		String source,
		@Size(max = 40)
		String runtime,
		@Size(max = 120)
		String hostname,
		@Size(max = 80)
		String appName,
		@Size(max = 40)
		String webViewType,
		Boolean sdkAvailable,
		@Size(max = 160)
		String message
) {
}
