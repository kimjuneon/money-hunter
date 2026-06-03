package com.money_hunter.infrastructure.persistence;

import com.money_hunter.domain.RookieEventSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RookieEventSettingsRepository extends JpaRepository<RookieEventSettings, Long> {
}
