package com.money_hunter.application.dto.response;

import java.util.List;

public record AdminAuditPageResponse(
		List<AdminAuditLogResponse> logs,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
