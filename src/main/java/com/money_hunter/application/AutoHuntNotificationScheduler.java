package com.money_hunter.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoHuntNotificationScheduler {
	private final PlayerService playerService;

	public AutoHuntNotificationScheduler(PlayerService playerService) {
		this.playerService = playerService;
	}

	@Scheduled(fixedDelayString = "${money-hunter.notifications.auto-hunt-ended-check-ms:5000}")
	public void publishAutoHuntEndedNotifications() {
		playerService.publishAutoHuntEndNotifications();
	}
}
