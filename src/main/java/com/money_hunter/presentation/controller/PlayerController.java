package com.money_hunter.presentation.controller;

import java.security.Principal;

import com.money_hunter.application.PlayerService;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.presentation.dto.request.AdCompletionRequest;
import com.money_hunter.presentation.dto.request.ChooseJobRequest;
import com.money_hunter.presentation.dto.request.ClaimRewardRequest;
import com.money_hunter.presentation.dto.request.StartAdRewardSessionRequest;
import com.money_hunter.application.dto.response.AdRewardSessionResponse;
import com.money_hunter.application.dto.response.PlayerStateResponse;
import com.money_hunter.application.dto.response.RewardClaimResponse;
import com.money_hunter.presentation.dto.request.UpgradeSkillRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/player")
public class PlayerController {
	private final PlayerService playerService;
	private final UserKeyResolver userKeyResolver;
	private final AppProperties appProperties;

	public PlayerController(PlayerService playerService, UserKeyResolver userKeyResolver, AppProperties appProperties) {
		this.playerService = playerService;
		this.userKeyResolver = userKeyResolver;
		this.appProperties = appProperties;
	}

	@GetMapping
	public PlayerStateResponse getState(Principal principal) {
		return playerService.getState(userKey(principal));
	}

	@PostMapping("/job")
	public PlayerStateResponse chooseJob(Principal principal, @Valid @RequestBody ChooseJobRequest request) {
		return playerService.chooseJob(userKey(principal), request.job());
	}

	@PostMapping("/ads/sessions")
	public AdRewardSessionResponse startAdRewardSession(
			Principal principal,
			@Valid @RequestBody StartAdRewardSessionRequest request
	) {
		String userKey = userKey(principal);
		requireRewardAdMode();
		return playerService.startRewardAdSession(userKey, request.type());
	}

	@PostMapping("/ads/auto-hunt/complete")
	public PlayerStateResponse completeAutoHuntAd(
			Principal principal,
			@RequestBody(required = false) AdCompletionRequest request
	) {
		String userKey = userKey(principal);
		requireRewardAdMode();
		return requiresRealRewardSession()
				? playerService.completeAutoHuntAd(userKey, adSessionToken(request))
				: playerService.completeAutoHuntAd(userKey);
	}

	@PostMapping("/ads/boost/complete")
	public PlayerStateResponse completeBoostAd(
			Principal principal,
			@RequestBody(required = false) AdCompletionRequest request
	) {
		String userKey = userKey(principal);
		requireRewardAdMode();
		return requiresRealRewardSession()
				? playerService.completeBoostAd(userKey, adSessionToken(request))
				: playerService.completeBoostAd(userKey);
	}

	@PostMapping("/ads/skill-point/complete")
	public PlayerStateResponse completeSkillPointAd(
			Principal principal,
			@RequestBody(required = false) AdCompletionRequest request
	) {
		String userKey = userKey(principal);
		requireRewardAdMode();
		return requiresRealRewardSession()
				? playerService.completeSkillPointAd(userKey, adSessionToken(request))
				: playerService.completeSkillPointAd(userKey);
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
		String userKey = userKey(principal);
		requireMockMonetization();
		return playerService.purchaseCompanion(userKey);
	}

	@PostMapping("/shop/skill-points/purchase")
	public PlayerStateResponse purchaseSkillPointPack(Principal principal) {
		String userKey = userKey(principal);
		requireMockMonetization();
		return playerService.purchaseSkillPointPack(userKey);
	}

	@PostMapping("/reward/claim-after-ad")
	public RewardClaimResponse claimRewardAfterAd(
			Principal principal,
			@Valid @RequestBody ClaimRewardRequest request
	) {
		String userKey = userKey(principal);
		requireRewardAdMode();
		return requiresRealRewardSession()
				? playerService.claimRewardAfterAd(userKey, request.idempotencyKey(), request.adSessionToken())
				: playerService.claimRewardAfterAd(userKey, request.idempotencyKey());
	}

	@PostMapping("/reward/friend-invite/claim")
	public PlayerStateResponse claimFriendInviteReward(Principal principal) {
		String userKey = userKey(principal);
		requireMockMonetization();
		return playerService.claimFriendInviteReward(userKey);
	}

	@PostMapping("/onestore/auto-hunt/claim")
	public PlayerStateResponse claimOneStoreAutoHunt(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.completeAutoHuntAd(userKey);
	}

	@PostMapping("/onestore/boost/claim")
	public PlayerStateResponse claimOneStoreBoost(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.completeBoostAd(userKey);
	}

	@PostMapping("/onestore/skill-point/claim")
	public PlayerStateResponse claimOneStoreSkillPoint(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.completeSkillPointAd(userKey);
	}

	@PostMapping("/onestore/reward/claim")
	public PlayerStateResponse claimOneStoreReward(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.claimOneStoreGameReward(userKey);
	}

	@PostMapping("/onestore/friend-invite/claim")
	public PlayerStateResponse claimOneStoreFriendInviteReward(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.claimFriendInviteReward(userKey);
	}

	@PostMapping("/onestore/shop/companions/unlock")
	public PlayerStateResponse unlockOneStoreCompanion(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.purchaseCompanion(userKey);
	}

	@PostMapping("/onestore/shop/skill-points/claim")
	public PlayerStateResponse claimOneStoreSkillPointPack(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.purchaseSkillPointPack(userKey);
	}

	@PostMapping("/notifications/{notificationId}/read")
	public PlayerStateResponse markNotificationRead(Principal principal, @PathVariable Long notificationId) {
		return playerService.markNotificationRead(userKey(principal), notificationId);
	}

	private String userKey(Principal principal) {
		return userKeyResolver.resolve(principal);
	}

	private void requireMockMonetization() {
		if (!appProperties.mockMonetizationEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "토스 광고, 결제, 리워드 연동 전에는 리뷰 환경에서만 사용할 수 있어요.");
		}
	}

	private void requireRewardAdMode() {
		if (!appProperties.mockMonetizationEnabled() && !appProperties.realRewardAdsEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "리워드 광고 연동이 활성화되어 있지 않아요.");
		}
	}

	private boolean requiresRealRewardSession() {
		return !appProperties.mockMonetizationEnabled() && appProperties.realRewardAdsEnabled();
	}

	private String adSessionToken(AdCompletionRequest request) {
		return request == null ? null : request.adSessionToken();
	}

	private void requireGameRewardMode() {
		if (!appProperties.mockMonetizationEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게임 내 보상은 심사용 또는 원스토어용 환경에서만 사용할 수 있어요.");
		}
	}
}
