package com.money_hunter.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RookieEventMissionNotificationScheduler {
	private final PlayerService playerService;

	public RookieEventMissionNotificationScheduler(PlayerService playerService) {
		this.playerService = playerService;
	}

	@Scheduled(fixedDelayString = "${money-hunter.notifications.rookie-event-mission-check-ms:3600000}")
	public void publishRookieEventMissionNotifications() {
		playerService.publishRookieEventMissionNotifications();
	}
}
