package com.money_hunter.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.money_hunter.domain.AdminAnomalyCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAnomalyCaseRepository extends JpaRepository<AdminAnomalyCase, Long> {
	List<AdminAnomalyCase> findByAnomalyKeyIn(Collection<String> anomalyKeys);

	Optional<AdminAnomalyCase> findByAnomalyKey(String anomalyKey);
}
