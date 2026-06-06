package com.money_hunter.infrastructure.persistence;

import java.util.Optional;

import com.money_hunter.domain.IapOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IapOrderRepository extends JpaRepository<IapOrder, Long> {
	Optional<IapOrder> findByOrderId(String orderId);

	Page<IapOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
