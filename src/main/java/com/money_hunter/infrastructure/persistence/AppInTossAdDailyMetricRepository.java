package com.money_hunter.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.money_hunter.domain.AppInTossAdDailyMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppInTossAdDailyMetricRepository extends JpaRepository<AppInTossAdDailyMetric, Long> {
	Optional<AppInTossAdDailyMetric> findByMetricDate(LocalDate metricDate);

	List<AppInTossAdDailyMetric> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate startedAt, LocalDate endedAt);
}
