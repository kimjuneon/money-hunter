package com.money_hunter.application;

import java.time.Clock;
import java.time.Instant;

import com.money_hunter.domain.AdminAuditLog;
import com.money_hunter.infrastructure.persistence.AdminAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {
	private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

	private final AdminAuditLogRepository repository;
	private final Clock clock = Clock.systemUTC();

	public AdminAuditService(AdminAuditLogRepository repository) {
		this.repository = repository;
	}

	public void record(
			AdminAccessGuard.AdminContext admin,
			String action,
			String target,
			String beforeValue,
			String afterValue,
			String reason,
			HttpServletRequest request
	) {
		repository.save(new AdminAuditLog(
				action,
				target,
				admin.actorFingerprint(),
				truncate(beforeValue, 4000),
				truncate(afterValue, 4000),
				truncate(reason, 500),
				truncate(clientIp(request), 80),
				truncate(request.getHeader("User-Agent"), 300),
				Instant.now(clock)));
		log.info("관리자 감사 로그 기록: action={}, target={}, actor={}", action, target, admin.actorFingerprint());
	}

	public Page<AdminAuditLog> recent(int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.min(100, Math.max(1, size));
		return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(safePage, safeSize));
	}

	private String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
