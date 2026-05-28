package com.money_hunter.presentation.controller;

import java.util.List;

import com.money_hunter.application.AdminAccessGuard;
import com.money_hunter.application.AdminAuditService;
import com.money_hunter.application.AdminMonitoringService;
import com.money_hunter.application.RuntimeEconomyService;
import com.money_hunter.application.RuntimeEconomyService.PolicyChangeResult;
import com.money_hunter.application.RuntimeEconomyService.PolicyDefinition;
import com.money_hunter.domain.AdminAuditLog;
import com.money_hunter.presentation.dto.request.AdminPolicyUpdateRequest;
import com.money_hunter.application.dto.response.AdminAuditLogResponse;
import com.money_hunter.application.dto.response.AdminAuditPageResponse;
import com.money_hunter.application.dto.response.AdminPolicyResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
	private final AdminAccessGuard adminAccessGuard;
	private final AdminMonitoringService monitoringService;
	private final RuntimeEconomyService economyService;
	private final AdminAuditService adminAuditService;

	public AdminController(
			AdminAccessGuard adminAccessGuard,
			AdminMonitoringService monitoringService,
			RuntimeEconomyService economyService,
			AdminAuditService adminAuditService
	) {
		this.adminAccessGuard = adminAccessGuard;
		this.monitoringService = monitoringService;
		this.economyService = economyService;
		this.adminAuditService = adminAuditService;
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
		requireReason(requestBody.reason());
		PolicyChangeResult result = requestBody.resetToDefault()
				? economyService.reset(requestBody.key())
				: economyService.update(requestBody.key(), requireValue(requestBody.value()));
		adminAuditService.record(
				admin,
				requestBody.resetToDefault() ? "POLICY_RESET" : "POLICY_UPDATE",
				result.key(),
				String.valueOf(result.beforeValue()),
				String.valueOf(result.afterValue()),
				requestBody.reason(),
				request);
		return policies(request);
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

	private void requireReason(String reason) {
		if (reason == null || reason.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경 사유를 입력해야 해요.");
		}
	}
}
