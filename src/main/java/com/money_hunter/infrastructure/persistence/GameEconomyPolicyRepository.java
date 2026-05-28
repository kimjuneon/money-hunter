package com.money_hunter.infrastructure.persistence;

import com.money_hunter.domain.GameEconomyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameEconomyPolicyRepository extends JpaRepository<GameEconomyPolicy, Long> {
}
