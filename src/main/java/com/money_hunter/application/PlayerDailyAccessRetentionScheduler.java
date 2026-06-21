package com.money_hunter.application;

import java.time.LocalDate;
import java.time.ZoneId;

import com.money_hunter.infrastructure.persistence.PlayerDailyAccessRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlayerDailyAccessRetentionScheduler {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final PlayerDailyAccessRepository playerDailyAccessRepository;

	public PlayerDailyAccessRetentionScheduler(PlayerDailyAccessRepository playerDailyAccessRepository) {
		this.playerDailyAccessRepository = playerDailyAccessRepository;
	}

	@Transactional
	@Scheduled(cron = "${money-hunter.analytics.daily-access-cleanup-cron:0 10 4 * * *}", zone = "Asia/Seoul")
	public void deleteOldDailyAccesses() {
		LocalDate retentionStartDate = LocalDate.now(SEOUL).minusDays(6);
		playerDailyAccessRepository.deleteBefore(retentionStartDate);
	}
}
