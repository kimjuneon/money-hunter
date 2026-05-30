package com.money_hunter.infrastructure.persistence;

import java.util.List;

import com.money_hunter.domain.AdminAnomalyAction;
import com.money_hunter.domain.AdminAnomalyCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAnomalyActionRepository extends JpaRepository<AdminAnomalyAction, Long> {
	List<AdminAnomalyAction> findByAnomalyCaseOrderByCreatedAtDesc(AdminAnomalyCase anomalyCase);
}
