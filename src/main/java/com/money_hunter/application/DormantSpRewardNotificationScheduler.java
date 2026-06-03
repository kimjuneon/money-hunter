package com.money_hunter.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DormantSpRewardNotificationScheduler {
	private final PlayerService playerService;

	public DormantSpRewardNotificationScheduler(PlayerService playerService) {
		this.playerService = playerService;
	}

	@Scheduled(fixedDelayString = "${money-hunter.notifications.dormant-sp-reward-check-ms:600000}")
	public void publishDormantSpRewardNotifications() {
		playerService.publishDormantSpRewardNotifications();
	}
}
