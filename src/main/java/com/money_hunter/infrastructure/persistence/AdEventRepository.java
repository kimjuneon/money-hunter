package com.money_hunter.infrastructure.persistence;

import java.time.Instant;

import com.money_hunter.domain.AdEvent;
import com.money_hunter.domain.AdEventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdEventRepository extends JpaRepository<AdEvent, Long> {
	long countByOccurredAtAfter(Instant occurredAt);

	long countByTypeAndOccurredAtAfter(AdEventType type, Instant occurredAt);
}
