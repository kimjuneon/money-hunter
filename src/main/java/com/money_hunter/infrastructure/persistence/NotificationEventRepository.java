package com.money_hunter.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.money_hunter.domain.NotificationEvent;
import com.money_hunter.domain.NotificationType;
import com.money_hunter.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
	Optional<NotificationEvent> findTopByPlayerAndReadAtIsNullOrderByCreatedAtDesc(Player player);

	List<NotificationEvent> findByPlayerAndReadAtIsNull(Player player);

	List<NotificationEvent> findByPlayerAndTypeAndReadAtIsNull(Player player, NotificationType type);

	Optional<NotificationEvent> findByIdAndPlayerUserKey(Long id, String userKey);
}
