package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.money_hunter.domain.IapOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IapOrderRepository extends JpaRepository<IapOrder, Long> {
	Optional<IapOrder> findByOrderId(String orderId);

	Page<IapOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startedAt, Instant endedAt);

	@Query("""
			select o.productType as productType,
				count(o.id) as orderCount,
				coalesce(sum(case when o.grantedAt is not null then 1 else 0 end), 0) as grantedCount
			from IapOrder o
			where o.createdAt >= :startedAt
				and o.createdAt < :endedAt
			group by o.productType
			order by count(o.id) desc
			""")
	List<IapProductCount> findProductCountsBetween(
			@Param("startedAt") Instant startedAt,
			@Param("endedAt") Instant endedAt);

	interface IapProductCount {
		String getProductType();

		long getOrderCount();

		long getGrantedCount();
	}
}
