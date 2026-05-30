package com.money_hunter.presentation.controller;

import java.util.List;

import com.money_hunter.application.AdminAccessGuard;
import com.money_hunter.application.AdminAuditService;
import com.money_hunter.application.AdminMonitoringService;
import com.money_hunter.application.AdminPlayerService;
import com.money_hunter.application.RuntimeEconomyService;
import com.money_hunter.application.RuntimeEconomyService.PolicyChangeResult;
import com.money_hunter.application.RuntimeEconomyService.PolicyDefinition;
import com.money_hunter.application.dto.response.AdminPlayerResetResponse;
import com.money_hunter.application.dto.response.AdminPlayerResponse;
import com.money_hunter.domain.AdminAuditLog;
import com.money_hunter.presentation.dto.request.AdminPlayerActionRequest;
import com.money_hunter.presentation.dto.request.AdminPolicyUpdateRequest;
import com.money_hunter.presentation.dto.request.AdminRevenueCalibrationRequest;
import com.money_hunter.application.dto.response.AdminAuditLogResponse;
import com.money_hunter.application.dto.response.AdminAuditPageResponse;
import com.money_hunter.application.dto.response.AdminPolicyResponse;
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
	private static final long FULLY_BOOSTED_GOLD_PER_HOUR = 6_000L;
	private static final long FULL_RATE_REQUIRED_ADS_PER_HOUR = 2L;

	private final AdminAccessGuard adminAccessGuard;
	private final AdminMonitoringService monitoringService;
	private final RuntimeEconomyService economyService;
	private final AdminAuditService adminAuditService;
	private final AdminPlayerService adminPlayerService;

	public AdminController(
			AdminAccessGuard adminAccessGuard,
			AdminMonitoringService monitoringService,
			RuntimeEconomyService economyService,
			AdminAuditService adminAuditService,
			AdminPlayerService adminPlayerService
	) {
		this.adminAccessGuard = adminAccessGuard;
		this.monitoringService = monitoringService;
		this.economyService = economyService;
		this.adminAuditService = adminAuditService;
		this.adminPlayerService = adminPlayerService;
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

	@GetMapping("/player-growth")
	public AdminMonitoringService.AdminPlayerGrowthReport playerGrowth(
			@RequestParam(defaultValue = "30") int days,
			HttpServletRequest request
	) {
		adminAccessGuard.require(request);
		return monitoringService.playerGrowth(days);
	}

	@GetMapping("/players")
	public List<AdminPlayerResponse> players(
			@RequestParam(defaultValue = "") String query,
			@RequestParam(defaultValue = "30") int limit,
			HttpServletRequest request
	) {
		adminAccessGuard.require(request);
		return adminPlayerService.search(query, limit);
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
				"6,000G/h, 광고 2회 기준 자동 환산",
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

	private long deriveGoldPerTossPoint(long adRevenuePerRewardAdWon) {
		long hourlyRevenueBasis = Math.multiplyExact(adRevenuePerRewardAdWon, FULL_RATE_REQUIRED_ADS_PER_HOUR);
		return Math.max(1, ceilDiv(FULLY_BOOSTED_GOLD_PER_HOUR, hourlyRevenueBasis));
	}

	private long ceilDiv(long dividend, long divisor) {
		return (dividend + divisor - 1) / divisor;
	}
}
