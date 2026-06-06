package com.money_hunter.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdventureAndBattleReadyNotificationScheduler {
	private final PlayerService playerService;

	public AdventureAndBattleReadyNotificationScheduler(PlayerService playerService) {
		this.playerService = playerService;
	}

	@Scheduled(fixedDelayString = "${money-hunter.notifications.dungeon-explore-available-check-ms:60000}")
	public void publishDungeonExploreAvailableNotifications() {
		playerService.publishDungeonExploreAvailableNotifications();
	}

	@Scheduled(fixedDelayString = "${money-hunter.notifications.battle-ready-daily-check-ms:3600000}")
	public void publishBattleReadyDailyNotifications() {
		playerService.publishBattleReadyDailyNotifications();
	}
}
