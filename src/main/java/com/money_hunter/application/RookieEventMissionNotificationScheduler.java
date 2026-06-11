package com.money_hunter.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RookieEventMissionNotificationScheduler {
	private final PlayerService playerService;

	public RookieEventMissionNotificationScheduler(PlayerService playerService) {
		this.playerService = playerService;
	}

	@Scheduled(cron = "${money-hunter.notifications.rookie-event-mission-cron:0 0 19 * * *}", zone = "Asia/Seoul")
	public void publishRookieEventMissionNotifications() {
		playerService.publishRookieEventMissionNotifications();
	}
}
