package com.money_hunter.presentation.controller;

import java.security.Principal;

import com.money_hunter.application.PlayerService;
import com.money_hunter.application.dto.response.PlayerStateResponse;
import com.money_hunter.application.dto.response.RewardClaimResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "review"})
@RestController
@RequestMapping("/api/player/test")
public class ReviewTestController {
	private final PlayerService playerService;
	private final UserKeyResolver userKeyResolver;

	public ReviewTestController(PlayerService playerService, UserKeyResolver userKeyResolver) {
		this.playerService = playerService;
		this.userKeyResolver = userKeyResolver;
	}

	@PostMapping("/fill-reward-gauge")
	public PlayerStateResponse fillRewardGaugeForTest(Principal principal) {
		return playerService.fillRewardGaugeForTest(userKey(principal));
	}

	@PostMapping("/auto-hunt")
	public PlayerStateResponse completeAutoHuntForTest(Principal principal) {
		return playerService.completeAutoHuntAd(userKey(principal));
	}

	@PostMapping("/boost")
	public PlayerStateResponse completeBoostForTest(Principal principal) {
		return playerService.completeBoostAd(userKey(principal));
	}

	@PostMapping("/skill-point")
	public PlayerStateResponse completeSkillPointForTest(Principal principal) {
		return playerService.completeSkillPointAd(userKey(principal));
	}

	@PostMapping("/claim-reward")
	public RewardClaimResponse claimRewardForTest(Principal principal) {
		return playerService.claimRewardAfterAd(userKey(principal), "review-test-" + System.nanoTime());
	}

	@PostMapping("/reset")
	public PlayerStateResponse resetForTest(Principal principal) {
		return playerService.resetForTest(userKey(principal));
	}

	@PostMapping("/end-auto-hunt")
	public PlayerStateResponse endAutoHuntForTest(Principal principal) {
		return playerService.endAutoHuntForTest(userKey(principal));
	}

	private String userKey(Principal principal) {
		return userKeyResolver.resolve(principal);
	}
}
