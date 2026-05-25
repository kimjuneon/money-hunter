package com.money_hunter.presentation.controller;

import java.security.Principal;

import com.money_hunter.application.PlayerService;
import com.money_hunter.presentation.dto.request.ChooseJobRequest;
import com.money_hunter.presentation.dto.request.ClaimRewardRequest;
import com.money_hunter.presentation.dto.response.PlayerStateResponse;
import com.money_hunter.presentation.dto.response.RewardClaimResponse;
import com.money_hunter.presentation.dto.request.UpgradeSkillRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/player")
public class PlayerController {
	private final PlayerService playerService;
	private final UserKeyResolver userKeyResolver;

	public PlayerController(PlayerService playerService, UserKeyResolver userKeyResolver) {
		this.playerService = playerService;
		this.userKeyResolver = userKeyResolver;
	}

	@GetMapping
	public PlayerStateResponse getState(Principal principal) {
		return playerService.getState(userKey(principal));
	}

	@PostMapping("/job")
	public PlayerStateResponse chooseJob(Principal principal, @Valid @RequestBody ChooseJobRequest request) {
		return playerService.chooseJob(userKey(principal), request.job());
	}

	@PostMapping("/ads/auto-hunt/complete")
	public PlayerStateResponse completeAutoHuntAd(Principal principal) {
		return playerService.completeAutoHuntAd(userKey(principal));
	}

	@PostMapping("/ads/boost/complete")
	public PlayerStateResponse completeBoostAd(Principal principal) {
		return playerService.completeBoostAd(userKey(principal));
	}

	@PostMapping("/ads/skill-point/complete")
	public PlayerStateResponse completeSkillPointAd(Principal principal) {
		return playerService.completeSkillPointAd(userKey(principal));
	}

	@PostMapping("/combat/hit")
	public PlayerStateResponse hitMonster(Principal principal) {
		return playerService.hitMonster(userKey(principal));
	}

	@PostMapping("/skills/upgrade")
	public PlayerStateResponse upgradeSkill(Principal principal, @Valid @RequestBody UpgradeSkillRequest request) {
		return playerService.upgradeSkill(userKey(principal), request.type());
	}

	@PostMapping("/shop/companions/purchase")
	public PlayerStateResponse purchaseCompanion(Principal principal) {
		return playerService.purchaseCompanion(userKey(principal));
	}

	@PostMapping("/shop/skill-points/purchase")
	public PlayerStateResponse purchaseSkillPointPack(Principal principal) {
		return playerService.purchaseSkillPointPack(userKey(principal));
	}

	@PostMapping("/reward/claim-after-ad")
	public RewardClaimResponse claimRewardAfterAd(
			Principal principal,
			@Valid @RequestBody ClaimRewardRequest request
	) {
		return playerService.claimRewardAfterAd(userKey(principal), request.idempotencyKey());
	}

	@PostMapping("/reward/friend-invite/claim")
	public PlayerStateResponse claimFriendInviteReward(Principal principal) {
		return playerService.claimFriendInviteReward(userKey(principal));
	}

	@PostMapping("/notifications/{notificationId}/read")
	public PlayerStateResponse markNotificationRead(Principal principal, @PathVariable Long notificationId) {
		return playerService.markNotificationRead(userKey(principal), notificationId);
	}

	private String userKey(Principal principal) {
		return userKeyResolver.resolve(principal);
	}
}
