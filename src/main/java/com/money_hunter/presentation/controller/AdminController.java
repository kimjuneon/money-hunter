package com.money_hunter.presentation.controller;

import java.util.List;

import com.money_hunter.application.AdminAnomalyCaseService;
import com.money_hunter.application.AdminAccessGuard;
import com.money_hunter.application.AdminAuditService;
import com.money_hunter.application.AdminMonitoringService;
import com.money_hunter.application.AdminPaymentService;
import com.money_hunter.application.AdminPlayerService;
import com.money_hunter.application.EconomyPolicySnapshot;
import com.money_hunter.application.PlayerService;
import com.money_hunter.application.RookieEventSettingsService;
import com.money_hunter.application.RookieEventSettingsService.EventSettingsChangeResult;
import com.money_hunter.application.RuntimeEconomyService;
import com.money_hunter.application.RuntimeEconomyService.PolicyChangeResult;
import com.money_hunter.application.RuntimeEconomyService.PolicyDefinition;
import com.money_hunter.application.dto.response.AdminRookieEventSettingsResponse;
import com.money_hunter.application.dto.response.AdminPaymentPageResponse;
import com.money_hunter.application.dto.response.AdminPlayerPageResponse;
import com.money_hunter.application.dto.response.AdminPlayerResetResponse;
import com.money_hunter.application.dto.response.AdminPlayerResponse;
import com.money_hunter.domain.AdminAnomalyStatus;
import com.money_hunter.domain.AdminAuditLog;
import com.money_hunter.presentation.dto.request.AdminAnomalyActionRequest;
import com.money_hunter.presentation.dto.request.AdminAppInTossAdMetricRequest;
import com.money_hunter.presentation.dto.request.AdminPlayerActionRequest;
import com.money_hunter.presentation.dto.request.AdminPlayerFavoriteRequest;
import com.money_hunter.presentation.dto.request.AdminPlayerNicknameRequest;
import com.money_hunter.presentation.dto.request.AdminPlayerPetActionRequest;
import com.money_hunter.presentation.dto.request.AdminPlayerResourceAdjustRequest;
import com.money_hunter.presentation.dto.request.AdminPolicyUpdateRequest;
import com.money_hunter.presentation.dto.request.AdminRevenueCalibrationRequest;
import com.money_hunter.presentation.dto.request.AdminRookieEventSettingsRequest;
import com.money_hunter.application.dto.response.AdminAuditLogResponse;
import com.money_hunter.application.dto.response.AdminAuditPageResponse;
import com.money_hunter.application.dto.response.AdminPolicyResponse;
import com.money_hunter.application.dto.response.PlayerStateResponse;
import com.money_hunter.presentation.dto.request.AdminRookieEventTestRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
	private static final long MAX_GOLD_PER_HOUR = 6_000L;
	private static final long DUNGEON_ENTRY_HUNT_REQUIREMENT_SECONDS = 3_600L;
	private static final long DAY_SECONDS = 86_400L;
	private static final List<RewardPlan> FULL_POWER_DUNGEON_REWARDS = List.of(
			new RewardPlan(RewardValueType.GOLD, 2_500, 4_000, 35),
			new RewardPlan(RewardValueType.SKILL_POINT, 4, 4, 30),
			new RewardPlan(RewardValueType.AUTO_HUNT_SECONDS, 9_000, 9_000, 25),
			new RewardPlan(RewardValueType.BOSS_TICKET, 1, 1, 10)
	);
	private static final List<RewardPlan> FULL_POWER_BOSS_REWARDS = List.of(
			new RewardPlan(RewardValueType.GOLD, 10_000, 12_000, 35),
			new RewardPlan(RewardValueType.SKILL_POINT, 3, 3, 40),
			new RewardPlan(RewardValueType.AUTO_HUNT_SECONDS, 28_800, 28_800, 25)
	);

	private final AdminAccessGuard adminAccessGuard;
	private final AdminMonitoringService monitoringService;
	private final RuntimeEconomyService economyService;
	private final AdminAuditService adminAuditService;
	private final AdminPlayerService adminPlayerService;
	private final AdminPaymentService adminPaymentService;
	private final AdminAnomalyCaseService anomalyCaseService;
	private final PlayerService playerService;
	private final RookieEventSettingsService rookieEventSettingsService;

	public AdminController(
			AdminAccessGuard adminAccessGuard,
			AdminMonitoringService monitoringService,
			RuntimeEconomyService economyService,
			AdminAuditService adminAuditService,
			AdminPlayerService adminPlayerService,
			AdminPaymentService adminPaymentService,
			AdminAnomalyCaseService anomalyCaseService,
			PlayerService playerService,
			RookieEventSettingsService rookieEventSettingsService
	) {
		this.adminAccessGuard = adminAccessGuard;
		this.monitoringService = monitoringService;
		this.economyService = economyService;
		this.adminAuditService = adminAuditService;
		this.adminPlayerService = adminPlayerService;
		this.adminPaymentService = adminPaymentService;
		this.anomalyCaseService = anomalyCaseService;
		this.playerService = playerService;
		this.rookieEventSettingsService = rookieEventSettingsService;
	}

	@GetMapping("/overview")
	public AdminMonitoringService.AdminOverview overview(HttpServletRequest request) {
		adminAccessGuard.require(request);
		return monitoringService.overview();
	}

	@GetMapping("/anomalies")
	public AdminMonitoringService.AdminAnomalyReport anomalies(HttpServletRequest request) {
		adminAccessGuard.require(request);
		return monitoringService.anomalies();
	}

	@GetMapping("/payments")
	public AdminPaymentPageResponse payments(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "30") int size,
			HttpServletRequest request
	) {
		adminAccessGuard.require(request);
		return AdminPaymentPageResponse.from(adminPaymentService.search(page, size));
	}

	@PostMapping("/anomalies/actions")
	public AdminAnomalyCaseService.AdminAnomalyCaseResponse updateAnomalyAction(
			@Valid @RequestBody AdminAnomalyActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminAnomalyStatus status = parseAnomalyStatus(requestBody.status());
		AdminAnomalyCaseService.AdminAnomalyCaseResponse response = anomalyCaseService.update(
				requestBody.anomalyKey(),
				requestBody.category(),
				requestBody.userKey(),
				status,
				requestBody.note(),
				admin);
		adminAuditService.record(
				admin,
				"ANOMALY_ACTION",
				requestBody.anomalyKey(),
				null,
				status.name(),
				optionalReason(requestBody.note()),
				request);
		return response;
	}

	@GetMapping("/player-growth")
	public AdminMonitoringService.AdminPlayerGrowthReport playerGrowth(
			@RequestParam(defaultValue = "30") int days,
			HttpServletRequest request
	) {
		adminAccessGuard.require(request);
		return monitoringService.playerGrowth(days);
	}

	@GetMapping("/revenue")
	public AdminMonitoringService.AdminRevenueReport revenue(
			@RequestParam(defaultValue = "30") int days,
			HttpServletRequest request
	) {
		adminAccessGuard.require(request);
		return monitoringService.revenue(days);
	}

	@PostMapping("/revenue/app-in-toss-metrics")
	public AdminMonitoringService.AdminAppInTossAdMetric saveAppInTossAdMetric(
			@Valid @RequestBody AdminAppInTossAdMetricRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminMonitoringService.AdminAppInTossAdMetric response = monitoringService.saveAppInTossAdMetric(
				requestBody.date(),
				requestBody.adImpressions(),
				requestBody.adWatchRatePercent(),
				requestBody.ecpmWon(),
				requestBody.note());
		adminAuditService.record(
				admin,
				"APP_IN_TOSS_AD_METRIC",
				response.date(),
				null,
				response.adImpressions() + " impressions, eCPM " + response.ecpmWon(),
				"앱인토스 일별 광고 핵심 지표 저장",
				request);
		return response;
	}

	@GetMapping("/server-metrics")
	public AdminMonitoringService.AdminServerMetrics serverMetrics(HttpServletRequest request) {
		adminAccessGuard.require(request);
		return monitoringService.serverMetrics();
	}

	@GetMapping("/players")
	public AdminPlayerPageResponse players(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "30") int size,
			@RequestParam(defaultValue = "") String favoriteMode,
			@RequestParam(defaultValue = "false") boolean favoritesOnly,
			@RequestParam(defaultValue = "ALL") String status,
			@RequestParam(defaultValue = "ALL") String progress,
			@RequestParam(defaultValue = "false") boolean hiddenSkinsOnly,
			@RequestParam(defaultValue = "false") boolean activeAutoHuntOnly,
			@RequestParam(defaultValue = "lastAccessedAt:desc") String sort,
			HttpServletRequest request
	) {
		adminAccessGuard.require(request);
		String resolvedFavoriteMode = favoriteMode == null || favoriteMode.isBlank()
				? (favoritesOnly ? "FAVORITE" : "ALL")
				: favoriteMode;
		return AdminPlayerPageResponse.from(adminPlayerService.search(
				query,
				page,
				size,
				resolvedFavoriteMode,
				status,
				progress,
				hiddenSkinsOnly,
				activeAutoHuntOnly,
				sort));
	}

	@PostMapping("/players/{userKey}/suspend")
	public AdminPlayerResponse suspendPlayer(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		String reason = optionalReason(requestBody.reason());
		AdminPlayerResponse player = adminPlayerService.suspend(userKey, reason);
		adminAuditService.record(
				admin,
				"USER_SUSPEND",
				player.userKey(),
				"ACTIVE",
				"SUSPENDED",
				reason,
				request);
		return player;
	}

	@PostMapping("/players/{userKey}/resume")
	public AdminPlayerResponse resumePlayer(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		String reason = optionalReason(requestBody.reason());
		AdminPlayerResponse player = adminPlayerService.resume(userKey);
		adminAuditService.record(
				admin,
				"USER_RESUME",
				player.userKey(),
				"SUSPENDED",
				"ACTIVE",
				reason,
				request);
		return player;
	}

	@PostMapping("/players/{userKey}/reset")
	public AdminPlayerResetResponse resetPlayer(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		String reason = optionalReason(requestBody.reason());
		AdminPlayerResetResponse result = adminPlayerService.resetFromLogin(userKey);
		adminAuditService.record(
				admin,
				"USER_RESET",
				result.userKey(),
				"EXISTS",
				result.playerDeleted() ? "DELETED" : "NO_PLAYER",
				reason,
				request);
		return result;
	}

	@PostMapping("/players/{userKey}/favorite")
	public AdminPlayerResponse favoritePlayer(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerFavoriteRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminPlayerResponse player = adminPlayerService.setFavorite(userKey, requestBody.favorite());
		adminAuditService.record(
				admin,
				"USER_FAVORITE",
				player.userKey(),
				null,
				String.valueOf(player.adminFavorite()),
				optionalReason(requestBody.reason()),
				request);
		return player;
	}

	@PostMapping("/players/{userKey}/admin-nickname")
	public AdminPlayerResponse updatePlayerAdminNickname(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerNicknameRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminPlayerResponse before = adminPlayerService.get(userKey);
		AdminPlayerResponse player = adminPlayerService.updateAdminNickname(userKey, requestBody.nickname());
		adminAuditService.record(
				admin,
				"USER_ADMIN_NICKNAME",
				player.userKey(),
				before.adminNickname(),
				player.adminNickname(),
				optionalReason(requestBody.reason()),
				request);
		return player;
	}

	@PostMapping("/players/{userKey}/cs/gold")
	public AdminPlayerResponse adjustPlayerGold(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerResourceAdjustRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminPlayerResponse player = adminPlayerService.adjustGold(userKey, requestBody.mode(), requestBody.amount());
		adminAuditService.record(
				admin,
				"USER_CS_GOLD",
				player.userKey(),
				requestBody.mode(),
				String.valueOf(player.gold()),
				optionalReason(requestBody.reason()),
				request);
		return player;
	}

	@PostMapping("/players/{userKey}/cs/skill-points")
	public AdminPlayerResponse adjustPlayerSkillPoints(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerResourceAdjustRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminPlayerResponse player = adminPlayerService.adjustSkillPoints(userKey, requestBody.mode(), requestBody.amount());
		adminAuditService.record(
				admin,
				"USER_CS_SKILL_POINTS",
				player.userKey(),
				requestBody.mode(),
				String.valueOf(player.skillPoints()),
				optionalReason(requestBody.reason()),
				request);
		return player;
	}

	@PostMapping("/players/{userKey}/cs/pet")
	public AdminPlayerResponse adjustPlayerPet(
			@PathVariable String userKey,
			@Valid @RequestBody AdminPlayerPetActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		String action = normalizePetAction(requestBody.action());
		AdminPlayerResponse player = "REMOVE".equals(action)
				? adminPlayerService.removePet(userKey)
				: adminPlayerService.grantPet(userKey, economyService.maxCharacterSlots());
		adminAuditService.record(
				admin,
				"REMOVE".equals(action) ? "USER_CS_PET_REMOVE" : "USER_CS_PET_GRANT",
				player.userKey(),
				action,
				String.valueOf(player.characterSlots()),
				optionalReason(requestBody.reason()),
				request);
		return player;
	}

	@GetMapping("/test-tools/rookie-event/{userKey}")
	public PlayerStateResponse rookieEventTestState(@PathVariable String userKey, HttpServletRequest request) {
		adminAccessGuard.require(request);
		return playerService.getState(userKey);
	}

	@PostMapping("/test-tools/rookie-event/{userKey}/reset")
	public PlayerStateResponse resetRookieEventForTest(
			@PathVariable String userKey,
			@Valid @RequestBody(required = false) AdminRookieEventTestRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminPlayerResponse player = adminPlayerService.resetRookieEventForTest(userKey);
		adminAuditService.record(
				admin,
				"ROOKIE_EVENT_TEST_RESET",
				player.userKey(),
				null,
				"completedDays=0,rewardedDays=0,finalRewardClaimed=false",
				optionalReason(requestBody == null ? null : requestBody.reason()),
				request);
		return playerService.getState(player.userKey());
	}

	@PostMapping("/test-tools/rookie-event/{userKey}/complete-next-day")
	public PlayerStateResponse completeNextRookieEventDayForTest(
			@PathVariable String userKey,
			@Valid @RequestBody(required = false) AdminRookieEventTestRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminPlayerResponse player = adminPlayerService.completeNextRookieEventDayForTest(userKey);
		adminAuditService.record(
				admin,
				"ROOKIE_EVENT_TEST_COMPLETE_DAY",
				player.userKey(),
				null,
				"completedDays+1",
				optionalReason(requestBody == null ? null : requestBody.reason()),
				request);
		return playerService.getState(player.userKey());
	}

	@PostMapping("/test-tools/rookie-event/{userKey}/advance-day")
	public PlayerStateResponse advanceRookieEventDayForTest(
			@PathVariable String userKey,
			@Valid @RequestBody(required = false) AdminRookieEventTestRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		AdminPlayerResponse player = adminPlayerService.advanceRookieEventDayForTest(userKey);
		adminAuditService.record(
				admin,
				"ROOKIE_EVENT_TEST_ADVANCE_DAY",
				player.userKey(),
				null,
				"eventDay+1",
				optionalReason(requestBody == null ? null : requestBody.reason()),
				request);
		return playerService.getState(player.userKey());
	}

	@PostMapping("/test-tools/rookie-event/{userKey}/state")
	public PlayerStateResponse overrideRookieEventForTest(
			@PathVariable String userKey,
			@Valid @RequestBody AdminRookieEventTestRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		int completedDays = requestBody.completedDays() == null ? 0 : requestBody.completedDays();
		int rewardedDays = requestBody.rewardedDays() == null ? 0 : requestBody.rewardedDays();
		boolean finalRewardClaimed = Boolean.TRUE.equals(requestBody.finalRewardClaimed());
		AdminPlayerResponse player = adminPlayerService.overrideRookieEventForTest(
				userKey,
				completedDays,
				rewardedDays,
				finalRewardClaimed);
		adminAuditService.record(
				admin,
				"ROOKIE_EVENT_TEST_STATE",
				player.userKey(),
				null,
				"completedDays=" + completedDays + ",rewardedDays=" + rewardedDays + ",finalRewardClaimed=" + finalRewardClaimed,
				optionalReason(requestBody.reason()),
				request);
		return playerService.getState(player.userKey());
	}

	@GetMapping("/events/rookie-event")
	public AdminRookieEventSettingsResponse rookieEventSettings(HttpServletRequest request) {
		adminAccessGuard.require(request);
		return rookieEventSettingsService.response();
	}

	@PatchMapping("/events/rookie-event")
	public AdminRookieEventSettingsResponse updateRookieEventSettings(
			@Valid @RequestBody AdminRookieEventSettingsRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		EventSettingsChangeResult result = rookieEventSettingsService.setRookieEventEnabled(requestBody.enabled());
		adminAuditService.record(
				admin,
				"ROOKIE_EVENT_STATUS",
				"rookie-event",
				String.valueOf(result.beforeEnabled()),
				String.valueOf(result.afterEnabled()),
				optionalReason(requestBody.reason()),
				request);
		return rookieEventSettingsService.response();
	}

	@GetMapping("/policies")
	public List<AdminPolicyResponse> policies(HttpServletRequest request) {
		adminAccessGuard.require(request);
		var snapshot = economyService.snapshot();
		return economyService.definitions().stream()
				.map(definition -> toResponse(definition, economyService.valueOf(snapshot, definition.key())))
				.toList();
	}

	@PatchMapping("/policies")
	public List<AdminPolicyResponse> updatePolicy(
			@RequestBody AdminPolicyUpdateRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		String reason = optionalReason(requestBody.reason());
		PolicyChangeResult result = requestBody.resetToDefault()
				? economyService.reset(requestBody.key())
				: economyService.update(requestBody.key(), requireValue(requestBody.value()));
		adminAuditService.record(
				admin,
				requestBody.resetToDefault() ? "POLICY_RESET" : "POLICY_UPDATE",
				result.key(),
				String.valueOf(result.beforeValue()),
				String.valueOf(result.afterValue()),
				reason,
				request);
		return policies(request);
	}

	@PatchMapping("/economy/revenue-calibration")
	public AdminMonitoringService.AdminOverview calibrateRevenue(
			@Valid @RequestBody AdminRevenueCalibrationRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		long adRevenuePerRewardAdWon = requestBody.adRevenuePerRewardAdWon();
		long goldPerTossPoint = deriveGoldPerTossPoint(adRevenuePerRewardAdWon);
		PolicyChangeResult adRevenueResult = economyService.update("adRevenuePerRewardAdWon", adRevenuePerRewardAdWon);
		PolicyChangeResult goldPerPointResult = economyService.update("goldPerTossPoint", goldPerTossPoint);
		adminAuditService.record(
				admin,
				"REVENUE_CALIBRATION",
				"adRevenuePerRewardAdWon",
				String.valueOf(adRevenueResult.beforeValue()),
				String.valueOf(adRevenueResult.afterValue()),
				"광고 단가 기준 자동 환산",
				request);
		adminAuditService.record(
				admin,
				"REVENUE_CALIBRATION",
				"goldPerTossPoint",
				String.valueOf(goldPerPointResult.beforeValue()),
				String.valueOf(goldPerPointResult.afterValue()),
				"풀강 유저 광고 1회 기대 골드 기준 자동 환산",
				request);
		return monitoringService.overview();
	}

	@GetMapping("/audits")
	public AdminAuditPageResponse audits(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "30") int size,
			HttpServletRequest request
	) {
		adminAccessGuard.require(request);
		Page<AdminAuditLog> logs = adminAuditService.recent(page, size);
		return new AdminAuditPageResponse(
				logs.getContent().stream().map(AdminAuditLogResponse::from).toList(),
				logs.getNumber(),
				logs.getSize(),
				logs.getTotalElements(),
				logs.getTotalPages());
	}

	@GetMapping("/health")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void health(HttpServletRequest request) {
		adminAccessGuard.require(request);
	}

	private AdminPolicyResponse toResponse(PolicyDefinition definition, Number value) {
		return new AdminPolicyResponse(
				definition.key(),
				definition.label(),
				definition.unit(),
				definition.min(),
				definition.max(),
				value);
	}

	private Number requireValue(Long value) {
		if (value == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Policy value is required.");
		}
		return value;
	}

	private String optionalReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}
		return reason.trim();
	}

	private String normalizePetAction(String action) {
		String normalized = action == null ? "" : action.trim().toUpperCase();
		if ("GRANT".equals(normalized) || "REMOVE".equals(normalized)) {
			return normalized;
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 펫 처리 유형이에요.");
	}

	private AdminAnomalyStatus parseAnomalyStatus(String status) {
		try {
			return AdminAnomalyStatus.valueOf(status);
		} catch (RuntimeException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이상징후 처리 상태예요.");
		}
	}

	private long deriveGoldPerTossPoint(long adRevenuePerRewardAdWon) {
		EconomyPolicySnapshot policy = economyService.snapshot();
		double expectedGold = expectedFullPowerGoldPerRewardAd(policy);
		long derived = (long) Math.ceil(expectedGold / Math.max(1L, adRevenuePerRewardAdWon));
		return Math.max(1L, Math.min(1_000_000L, derived));
	}

	private double expectedFullPowerGoldPerRewardAd(EconomyPolicySnapshot policy) {
		double autoHuntGold = secondsToFullPowerGold(policy.autoHuntAdSeconds());
		double bossExpectedGold = expectedRewardGold(FULL_POWER_BOSS_REWARDS, 0);
		double dungeonExpectedGold = expectedRewardGold(FULL_POWER_DUNGEON_REWARDS, bossExpectedGold);
		double dungeonOpportunities = dungeonOpportunitiesUnlockedByRewardAd(policy);
		return autoHuntGold + dungeonExpectedGold * dungeonOpportunities;
	}

	private double dungeonOpportunitiesUnlockedByRewardAd(EconomyPolicySnapshot policy) {
		int freeDailyLimit = Math.max(0, policy.dungeonFreeDailyLimit());
		long autoHuntSeconds = Math.max(0, policy.autoHuntAdSeconds());
		if (freeDailyLimit == 0 || autoHuntSeconds == 0) {
			return 0;
		}
		double gateContribution = Math.min(1.0, autoHuntSeconds / (double) DUNGEON_ENTRY_HUNT_REQUIREMENT_SECONDS);
		long cooldownSeconds = Math.max(0, policy.dungeonReentryCooldownSeconds());
		if (cooldownSeconds == 0) {
			return freeDailyLimit * gateContribution;
		}
		long availableSecondsAfterGate = Math.max(0, DAY_SECONDS - DUNGEON_ENTRY_HUNT_REQUIREMENT_SECONDS);
		long dailyRunsPossible = 1 + availableSecondsAfterGate / cooldownSeconds;
		return Math.min(freeDailyLimit, Math.max(1, dailyRunsPossible)) * gateContribution;
	}

	private double expectedRewardGold(List<RewardPlan> rewards, double bossExpectedGold) {
		int totalWeight = rewards.stream().mapToInt(RewardPlan::weight).sum();
		if (totalWeight <= 0) {
			return 0;
		}
		double expectedGold = 0;
		for (RewardPlan reward : rewards) {
			double probability = reward.weight() / (double) totalWeight;
			expectedGold += probability * rewardGoldValue(reward, bossExpectedGold);
		}
		return expectedGold;
	}

	private double rewardGoldValue(RewardPlan reward, double bossExpectedGold) {
		return switch (reward.type()) {
			case GOLD -> (reward.minAmount() + reward.maxAmount()) / 2.0;
			case AUTO_HUNT_SECONDS -> secondsToFullPowerGold((reward.minAmount() + reward.maxAmount()) / 2.0);
			case BOSS_TICKET -> bossExpectedGold * ((reward.minAmount() + reward.maxAmount()) / 2.0);
			case SKILL_POINT -> 0;
		};
	}

	private double secondsToFullPowerGold(double seconds) {
		return MAX_GOLD_PER_HOUR * Math.max(0, seconds) / 3_600.0;
	}

	private enum RewardValueType {
		GOLD,
		SKILL_POINT,
		AUTO_HUNT_SECONDS,
		BOSS_TICKET
	}

	private record RewardPlan(RewardValueType type, long minAmount, long maxAmount, int weight) {
	}
}
