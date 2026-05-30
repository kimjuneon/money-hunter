package com.money_hunter.presentation.controller;

import java.security.Principal;

import com.money_hunter.application.PlayerService;
import com.money_hunter.application.dto.response.PlayerStateResponse;
import com.money_hunter.application.dto.response.RewardClaimResponse;
import com.money_hunter.presentation.dto.request.ChooseJobRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

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
		return playerService.completeAutoHuntForTest(userKey(principal));
	}

	@PostMapping("/boost")
	public PlayerStateResponse completeBoostForTest(Principal principal) {
		return playerService.completeBoostForTest(userKey(principal));
	}

	@PostMapping("/skill-point")
	public PlayerStateResponse completeSkillPointForTest(Principal principal) {
		return playerService.grantSkillPointForTest(userKey(principal));
	}

	@PostMapping("/claim-reward")
	public RewardClaimResponse claimRewardForTest(Principal principal) {
		return playerService.claimRewardForTest(userKey(principal));
	}

	@PostMapping("/job")
	public PlayerStateResponse chooseJobForTest(
			Principal principal,
			@Valid @RequestBody ChooseJobRequest request
	) {
		return playerService.chooseJobForTest(userKey(principal), request.job());
	}

	@PostMapping("/companions/unlock-all")
	public PlayerStateResponse unlockAllCompanionsForTest(Principal principal) {
		return playerService.unlockAllCompanionsForTest(userKey(principal));
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
