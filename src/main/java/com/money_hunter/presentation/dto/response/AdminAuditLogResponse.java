package com.money_hunter.presentation.dto.response;

import java.time.Instant;

import com.money_hunter.domain.AdminAuditLog;

public record AdminAuditLogResponse(
		Long id,
		String action,
		String target,
		String actorFingerprint,
		String beforeValue,
		String afterValue,
		String reason,
		String clientIp,
		String userAgent,
		Instant createdAt
) {
	public static AdminAuditLogResponse from(AdminAuditLog log) {
		return new AdminAuditLogResponse(
				log.getId(),
				log.getAction(),
				log.getTarget(),
				log.getActorFingerprint(),
				log.getBeforeValue(),
				log.getAfterValue(),
				log.getReason(),
				log.getClientIp(),
				log.getUserAgent(),
				log.getCreatedAt()
		);
	}
}
