package com.money_hunter.presentation.controller;

import java.util.List;

import com.money_hunter.application.AdminAccessGuard;
import com.money_hunter.application.AdminAuditService;
import com.money_hunter.application.PlayerService;
import com.money_hunter.application.dto.response.PlayerStateResponse;
import com.money_hunter.application.dto.response.RewardClaimResponse;
import com.money_hunter.domain.JobType;
import com.money_hunter.infrastructure.toss.LocalRecordingTossPromotionClient;
import com.money_hunter.presentation.dto.request.AdminPlayerActionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test-tools/promotion")
public class AdminPromotionTestController {
	private final AdminAccessGuard adminAccessGuard;
	private final AdminAuditService adminAuditService;
	private final PlayerService playerService;
	private final ObjectProvider<LocalRecordingTossPromotionClient> tossPromotionClientProvider;

	public AdminPromotionTestController(
			AdminAccessGuard adminAccessGuard,
			AdminAuditService adminAuditService,
			PlayerService playerService,
			ObjectProvider<LocalRecordingTossPromotionClient> tossPromotionClientProvider
	) {
		this.adminAccessGuard = adminAccessGuard;
		this.adminAuditService = adminAuditService;
		this.playerService = playerService;
		this.tossPromotionClientProvider = tossPromotionClientProvider;
	}

	@GetMapping("/{userKey}")
	public AdminPromotionTestResponse state(@PathVariable String userKey, HttpServletRequest request) {
		adminAccessGuard.require(request);
		return response(playerService.getState(userKey), null);
	}

	@PostMapping("/{userKey}/prepare")
	public AdminPromotionTestResponse prepare(
			@PathVariable String userKey,
			@Valid @RequestBody(required = false) AdminPlayerActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		playerService.resetForTest(userKey);
		PlayerStateResponse player = playerService.chooseJobForTest(userKey, JobType.WARRIOR);
		clearRecordedPromotions(userKey);
		adminAuditService.record(
				admin,
				"PROMOTION_TEST_PREPARE",
				player.userKey(),
				null,
				"job=WARRIOR,promotionExecutions=0",
				optionalReason(requestBody),
				request);
		return response(player, null);
	}

	@PostMapping("/{userKey}/benefit-tab-entry")
	public AdminPromotionTestResponse markBenefitTabEntry(
			@PathVariable String userKey,
			@Valid @RequestBody(required = false) AdminPlayerActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		PlayerStateResponse player = playerService.markBenefitTabNewUserEntryForTest(userKey);
		adminAuditService.record(
				admin,
				"PROMOTION_TEST_BENEFIT_TAB_ENTRY",
				player.userKey(),
				null,
				"benefitTabNewUserEntry=true",
				optionalReason(requestBody),
				request);
		return response(player, null);
	}

	@PostMapping("/{userKey}/claim-reward")
	public AdminPromotionTestResponse claimReward(
			@PathVariable String userKey,
			@Valid @RequestBody(required = false) AdminPlayerActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		RewardClaimResponse rewardClaim = playerService.claimRewardForTest(userKey);
		adminAuditService.record(
				admin,
				"PROMOTION_TEST_CLAIM_REWARD",
				rewardClaim.state().userKey(),
				null,
				"claimId=" + rewardClaim.claimId() + ",status=" + rewardClaim.status(),
				optionalReason(requestBody),
				request);
		return response(rewardClaim.state(), rewardClaim);
	}

	@PostMapping("/{userKey}/executions/clear")
	public AdminPromotionTestResponse clearExecutions(
			@PathVariable String userKey,
			@Valid @RequestBody(required = false) AdminPlayerActionRequest requestBody,
			HttpServletRequest request
	) {
		AdminAccessGuard.AdminContext admin = adminAccessGuard.require(request);
		clearRecordedPromotions(userKey);
		PlayerStateResponse player = playerService.getState(userKey);
		adminAuditService.record(
				admin,
				"PROMOTION_TEST_EXECUTIONS_CLEAR",
				player.userKey(),
				null,
				"promotionExecutions=0",
				optionalReason(requestBody),
				request);
		return response(player, null);
	}

	private AdminPromotionTestResponse response(PlayerStateResponse player, RewardClaimResponse rewardClaim) {
		return new AdminPromotionTestResponse(
				player,
				rewardClaim,
				recordedPromotions(player.userKey()),
				mockExecutionLogAvailable());
	}

	private List<LocalRecordingTossPromotionClient.RecordedPromotion> recordedPromotions(String userKey) {
		LocalRecordingTossPromotionClient client = tossPromotionClientProvider.getIfAvailable();
		if (client == null) {
			return List.of();
		}
		return client.executionsFor(userKey);
	}

	private void clearRecordedPromotions(String userKey) {
		LocalRecordingTossPromotionClient client = tossPromotionClientProvider.getIfAvailable();
		if (client != null) {
			client.clearFor(userKey);
		}
	}

	private boolean mockExecutionLogAvailable() {
		return tossPromotionClientProvider.getIfAvailable() != null;
	}

	private String optionalReason(AdminPlayerActionRequest requestBody) {
		if (requestBody == null || requestBody.reason() == null || requestBody.reason().isBlank()) {
			return null;
		}
		return requestBody.reason().trim();
	}

	public record AdminPromotionTestResponse(
			PlayerStateResponse player,
			RewardClaimResponse rewardClaim,
			List<LocalRecordingTossPromotionClient.RecordedPromotion> executions,
			boolean mockExecutionLogAvailable
	) {
	}
}
