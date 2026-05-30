package com.money_hunter.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.money_hunter.domain.AdminAnomalyAction;
import com.money_hunter.domain.AdminAnomalyCase;
import com.money_hunter.domain.AdminAnomalyStatus;
import com.money_hunter.infrastructure.persistence.AdminAnomalyActionRepository;
import com.money_hunter.infrastructure.persistence.AdminAnomalyCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAnomalyCaseService {
	private static final Logger log = LoggerFactory.getLogger(AdminAnomalyCaseService.class);

	private final AdminAnomalyCaseRepository caseRepository;
	private final AdminAnomalyActionRepository actionRepository;
	private final Clock clock = Clock.systemUTC();

	public AdminAnomalyCaseService(
			AdminAnomalyCaseRepository caseRepository,
			AdminAnomalyActionRepository actionRepository
	) {
		this.caseRepository = caseRepository;
		this.actionRepository = actionRepository;
	}

	@Transactional
	public AdminAnomalyCaseResponse update(
			String anomalyKey,
			String category,
			String userKey,
			AdminAnomalyStatus status,
			String note,
			AdminAccessGuard.AdminContext admin
	) {
		Instant now = Instant.now(clock);
		AdminAnomalyCase anomalyCase = caseRepository.findByAnomalyKey(anomalyKey)
				.orElseGet(() -> new AdminAnomalyCase(anomalyKey, category, userKey, now));
		anomalyCase.update(status, normalize(note), now);
		AdminAnomalyCase savedCase = caseRepository.save(anomalyCase);
		AdminAnomalyAction action = actionRepository.save(new AdminAnomalyAction(
				savedCase,
				status,
				normalize(note),
				admin.actorFingerprint(),
				now));
		log.info(
				"이상징후 조치 저장: anomalyKey={}, category={}, userKey={}, status={}, actor={}",
				anomalyKey,
				category,
				userKey,
				status,
				admin.actorFingerprint());
		return AdminAnomalyCaseResponse.from(savedCase, List.of(action));
	}

	private String normalize(String note) {
		if (note == null || note.isBlank()) {
			return null;
		}
		return note.trim();
	}

	public record AdminAnomalyCaseResponse(
			String anomalyKey,
			String category,
			String userKey,
			AdminAnomalyStatus status,
			String note,
			Instant updatedAt,
			Instant resolvedAt,
			List<AdminMonitoringService.AdminAnomalyActionSummary> actions
	) {
		public static AdminAnomalyCaseResponse from(AdminAnomalyCase anomalyCase, List<AdminAnomalyAction> actions) {
			return new AdminAnomalyCaseResponse(
					anomalyCase.getAnomalyKey(),
					anomalyCase.getCategory(),
					anomalyCase.getUserKey(),
					anomalyCase.getStatus(),
					anomalyCase.getNote(),
					anomalyCase.getUpdatedAt(),
					anomalyCase.getResolvedAt(),
					actions.stream()
							.map(action -> new AdminMonitoringService.AdminAnomalyActionSummary(
									action.getStatus(),
									action.getNote(),
									action.getActorFingerprint(),
									action.getCreatedAt()))
							.toList());
		}
	}
}
