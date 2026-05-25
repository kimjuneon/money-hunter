package com.money_hunter.infrastructure.persistence;

import com.money_hunter.domain.AdEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdEventRepository extends JpaRepository<AdEvent, Long> {
}
