package com.money_hunter.presentation.controller;

import java.security.Principal;

import com.money_hunter.application.PlayerService;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.application.dto.response.BossRaidRewardResponse;
import com.money_hunter.presentation.dto.request.AdCompletionRequest;
import com.money_hunter.presentation.dto.request.ChooseJobRequest;
import com.money_hunter.presentation.dto.request.ClaimRewardRequest;
import com.money_hunter.presentation.dto.request.FriendInviteRewardClaimRequest;
import com.money_hunter.presentation.dto.request.GameProfileRequest;
import com.money_hunter.presentation.dto.request.GameProfileSyncEventRequest;
import com.money_hunter.presentation.dto.request.IapGrantRequest;
import com.money_hunter.presentation.dto.request.PetSkinEquipRequest;
import com.money_hunter.presentation.dto.request.PunchKingScoreRequest;
import com.money_hunter.presentation.dto.request.StartAdRewardSessionRequest;
import com.money_hunter.application.dto.response.AdRewardSessionResponse;
import com.money_hunter.application.dto.response.DungeonCouponRewardResponse;
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

	@PostMapping("/ads/dungeon-additional/complete")
	public DungeonCouponRewardResponse completeDungeonAdditionalEntryAd(
			Principal principal,
			@RequestBody(required = false) AdCompletionRequest request
	) {
		String userKey = userKey(principal);
		requireRewardAdMode();
		return requiresRealRewardSession()
				? playerService.runAdditionalDungeonAfterAd(userKey, adSessionToken(request))
				: playerService.runAdditionalDungeonAfterAd(userKey);
	}

	@PostMapping("/combat/hit")
	public PlayerStateResponse hitMonster(Principal principal) {
		return playerService.hitMonster(userKey(principal));
	}

	@PostMapping("/tutorial/feature/complete")
	public PlayerStateResponse completeFeatureTutorial(Principal principal) {
		return playerService.completeFeatureTutorial(userKey(principal));
	}

	@PostMapping("/rookie-event/start")
	public PlayerStateResponse startRookieEvent(Principal principal) {
		return playerService.startRookieEvent(userKey(principal));
	}

	@PostMapping("/rookie-event/home-shortcut-return")
	public PlayerStateResponse completeRookieEventHomeShortcutMission(Principal principal) {
		return playerService.completeRookieEventHomeShortcutMission(userKey(principal));
	}

	@PostMapping("/rookie-event/skill-point-help")
	public PlayerStateResponse claimRookieEventSkillPointHelp(Principal principal) {
		return playerService.claimRookieEventSkillPointHelp(userKey(principal));
	}

	@PostMapping("/rookie-event/mission-notifications/agreement")
	public PlayerStateResponse markRookieEventMissionNotificationAgreed(Principal principal) {
		return playerService.markRookieEventMissionNotificationAgreed(userKey(principal));
	}

	@PostMapping("/game-profile")
	public PlayerStateResponse updateGameProfile(
			Principal principal,
			@Valid @RequestBody GameProfileRequest request
	) {
		return playerService.updateGameProfile(userKey(principal), request.nickname());
	}

	@PostMapping("/game-profile/sync-events")
	public void logGameProfileSyncEvent(
			Principal principal,
			@Valid @RequestBody GameProfileSyncEventRequest request
	) {
		playerService.logGameProfileSyncEvent(
				userKey(principal),
				request.statusCode(),
				request.source(),
				request.runtime(),
				request.hostname(),
				request.appName(),
				request.webViewType(),
				request.sdkAvailable(),
				request.message());
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

	@PostMapping("/shop/pet-skins/{skinKey}/purchase")
	public PlayerStateResponse purchasePetSkin(Principal principal, @PathVariable String skinKey) {
		return playerService.purchasePetSkin(userKey(principal), skinKey);
	}

	@PostMapping("/shop/pet-skins/{skinKey}/equip")
	public PlayerStateResponse equipPetSkin(
			Principal principal,
			@PathVariable String skinKey,
			@Valid @RequestBody PetSkinEquipRequest request
	) {
		return playerService.equipPetSkin(userKey(principal), skinKey, request.slot());
	}

	@PostMapping("/shop/pet-skins/easter-eggs/unlock")
	public PlayerStateResponse unlockEasterEggPetSkins(Principal principal) {
		return playerService.unlockEasterEggPetSkins(userKey(principal));
	}

	@PostMapping("/shop/iap/grant")
	public PlayerStateResponse grantIapProduct(Principal principal, @Valid @RequestBody IapGrantRequest request) {
		String userKey = userKey(principal);
		requirePaymentMode();
		return playerService.grantIapProduct(userKey, request.orderId(), request.productId());
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
	public PlayerStateResponse claimFriendInviteReward(
			Principal principal,
			@Valid @RequestBody(required = false) FriendInviteRewardClaimRequest request
	) {
		String userKey = userKey(principal);
		requireShareRewardMode();
		return playerService.claimFriendInviteReward(userKey, friendInviteCount(request));
	}

	@PostMapping("/dungeon-coupon/use")
	public DungeonCouponRewardResponse useDungeonCoupon(Principal principal) {
		return playerService.useDungeonCoupon(userKey(principal));
	}

	@PostMapping("/dungeon/run")
	public DungeonCouponRewardResponse runDungeon(Principal principal) {
		return playerService.runDungeon(userKey(principal));
	}

	@PostMapping("/boss/raid")
	public BossRaidRewardResponse raidBoss(Principal principal) {
		return playerService.raidBoss(userKey(principal));
	}

	@PostMapping("/adventures/mini-game/start")
	public PlayerStateResponse startAdventureMiniGame(Principal principal) {
		return playerService.startAdventureMiniGame(userKey(principal));
	}

	@PostMapping("/adventures/mini-game/clear")
	public PlayerStateResponse clearAdventureMiniGame(Principal principal) {
		return playerService.clearAdventureMiniGame(userKey(principal));
	}

	@PostMapping("/adventures/punch-king/submit")
	public PlayerStateResponse submitWeeklyPunchKingScore(
			Principal principal,
			@Valid @RequestBody PunchKingScoreRequest request
	) {
		return playerService.submitWeeklyPunchKingScore(userKey(principal), request.score());
	}

	@PostMapping("/rookie-event/days/{day}/reward/claim")
	public PlayerStateResponse claimRookieEventDailyReward(Principal principal, @PathVariable int day) {
		return playerService.claimRookieEventDailyReward(userKey(principal), day);
	}

	@PostMapping("/rookie-event/final-reward/claim")
	public PlayerStateResponse claimRookieEventFinalReward(Principal principal) {
		return playerService.claimRookieEventFinalReward(userKey(principal));
	}

	@PostMapping("/event-rewards/{rewardId}/claim")
	public PlayerStateResponse claimEventReward(Principal principal, @PathVariable Long rewardId) {
		return playerService.claimEventReward(userKey(principal), rewardId);
	}

	@PostMapping("/onestore/auto-hunt/claim")
	public PlayerStateResponse claimOneStoreAutoHunt(Principal principal) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.completeAutoHuntAd(userKey);
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
	public PlayerStateResponse claimOneStoreFriendInviteReward(
			Principal principal,
			@Valid @RequestBody(required = false) FriendInviteRewardClaimRequest request
	) {
		String userKey = userKey(principal);
		requireGameRewardMode();
		return playerService.claimFriendInviteReward(userKey, friendInviteCount(request));
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

	private void requirePaymentMode() {
		if (!appProperties.mockMonetizationEnabled() && !appProperties.realPaymentsEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "인앱 결제 연동이 활성화되어 있지 않아요.");
		}
	}

	private void requireShareRewardMode() {
		if (!appProperties.mockMonetizationEnabled() && !appProperties.realShareRewardEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공유 리워드 연동이 활성화되어 있지 않아요.");
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

	private int friendInviteCount(FriendInviteRewardClaimRequest request) {
		return request == null ? 1 : request.normalizedCompletedInvites();
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
