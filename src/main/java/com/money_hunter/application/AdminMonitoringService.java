package com.money_hunter.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.money_hunter.domain.AdminAnomalyCase;
import com.money_hunter.domain.AdminAnomalyStatus;
import com.money_hunter.domain.RewardClaimStatus;
import com.money_hunter.infrastructure.config.AdProperties;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.config.PromotionProperties;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.AdminAnomalyActionRepository;
import com.money_hunter.infrastructure.persistence.AdminAnomalyCaseRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import com.money_hunter.infrastructure.persistence.RewardClaimRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AdminMonitoringService {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final String MODE_LIVE = "LIVE";
	private static final String MODE_TEST = "TEST";
	private static final String MODE_OFF = "OFF";
	private static final String MODE_CHECK = "CHECK";

	private final PlayerRepository playerRepository;
	private final AdEventRepository adEventRepository;
	private final RewardClaimRepository rewardClaimRepository;
	private final AdminAnomalyCaseRepository anomalyCaseRepository;
	private final AdminAnomalyActionRepository anomalyActionRepository;
	private final RuntimeEconomyService economy;
	private final AppProperties appProperties;
	private final AdProperties adProperties;
	private final PromotionProperties promotionProperties;
	private final Clock clock = Clock.systemUTC();

	public AdminMonitoringService(
			PlayerRepository playerRepository,
			AdEventRepository adEventRepository,
			RewardClaimRepository rewardClaimRepository,
			AdminAnomalyCaseRepository anomalyCaseRepository,
			AdminAnomalyActionRepository anomalyActionRepository,
			RuntimeEconomyService economy,
			AppProperties appProperties,
			AdProperties adProperties,
			PromotionProperties promotionProperties
	) {
		this.playerRepository = playerRepository;
		this.adEventRepository = adEventRepository;
		this.rewardClaimRepository = rewardClaimRepository;
		this.anomalyCaseRepository = anomalyCaseRepository;
		this.anomalyActionRepository = anomalyActionRepository;
		this.economy = economy;
		this.appProperties = appProperties;
		this.adProperties = adProperties;
		this.promotionProperties = promotionProperties;
	}

	public AdminOverview overview() {
		Instant now = Instant.now(clock);
		Instant today = LocalDate.now(SEOUL).atStartOfDay(SEOUL).toInstant();
		long adEventsToday = adEventRepository.countByOccurredAtAfter(today);
		long rewardAdEventsToday = adEventsToday;
		long rewardClaimsToday = rewardClaimRepository.countByCreatedAtAfter(today);
		long rewardPointsToday = rewardClaimRepository.sumPointAmountSince(today);
		long pendingRewardClaims = rewardClaimRepository.countByStatus(RewardClaimStatus.PENDING_PROMOTION_GRANT);
		long pendingRewardPoints = rewardClaimRepository.sumPointAmountByStatus(RewardClaimStatus.PENDING_PROMOTION_GRANT);
		long estimatedAdRevenueWonToday = rewardAdEventsToday * economy.adRevenuePerRewardAdWon();
		long estimatedRewardCostWonToday = rewardPointsToday;

		return new AdminOverview(
				now,
				appProperties.integrationMode(),
				appProperties.distributionTarget(),
				appProperties.tossReleaseReady(),
				appProperties.releaseBlockers(),
				playerRepository.count(),
				playerRepository.countByJobIsNotNull(),
				playerRepository.countBySuspendedAtIsNotNull(),
				playerRepository.countByCreatedAtAfter(today),
				playerRepository.countByAutoHuntEndsAtAfter(now),
				playerRepository.countByBoostEndsAtAfter(now),
				playerRepository.totalGold(),
				rewardAdEventsToday,
				rewardClaimsToday,
				rewardPointsToday,
				pendingRewardClaims,
				pendingRewardPoints,
				estimatedAdRevenueWonToday,
				estimatedRewardCostWonToday,
				estimatedAdRevenueWonToday - estimatedRewardCostWonToday,
				economy.snapshot(),
				runtimeStatusItems()
		);
	}

	public AdminAnomalyReport anomalies() {
		Instant now = Instant.now(clock);
		Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
		Instant today = LocalDate.now(SEOUL).atStartOfDay(SEOUL).toInstant();
		var limit = PageRequest.of(0, economy.anomalyLimitPerRule());
		long adEventsPerHourWarning = economy.anomalyAdEventsPerHourWarning();
		long rewardClaimsPerDayWarning = economy.anomalyRewardClaimsPerDayWarning();
		long goldThresholdMultiplier = economy.anomalyGoldThresholdMultiplier();
		int skillPointsWarning = economy.anomalySkillPointsWarning();
		long timerGraceSeconds = economy.anomalyTimerGraceSeconds();
		long suspiciousGold = multiplyCapped(economy.rewardGoldThreshold(), goldThresholdMultiplier);
		Instant maxAutoHuntEnd = now.plusSeconds(economy.maxAdSeconds() + timerGraceSeconds);
		Instant maxBoostEnd = now.plusSeconds(economy.maxAdSeconds() + timerGraceSeconds);
		List<AdminAnomaly> detectedAnomalies = new ArrayList<>();

		adEventRepository.findPlayerEventCountsSince(oneHourAgo, adEventsPerHourWarning, limit)
				.forEach(row -> detectedAnomalies.add(anomaly(
						"WARNING",
						"HIGH_AD_EVENTS",
						row.getUserKey(),
						"1시간 광고 이벤트 과다",
						"최근 1시간 동안 광고성 보상 이벤트가 " + row.getEventCount()
								+ "회 발생했어요. 자동 호출이나 반복 클릭을 확인하세요.",
						row.getEventCount(),
						adEventsPerHourWarning,
						row.getLastOccurredAt())));

		rewardClaimRepository.findPlayerClaimCountsSince(today, rewardClaimsPerDayWarning, limit)
				.forEach(row -> detectedAnomalies.add(anomaly(
						"CRITICAL",
						"HIGH_REWARD_CLAIMS",
						row.getUserKey(),
						"일일 보상 수령 과다",
						"오늘 보상 수령이 " + row.getClaimCount() + "건, 총 " + row.getPointAmount()
								+ "P로 집계됐어요. 실제 포인트 지급 전 검토가 필요해요.",
						row.getClaimCount(),
						rewardClaimsPerDayWarning,
						row.getLastClaimedAt())));

		playerRepository.findPlayersWithGoldAtLeast(suspiciousGold, limit)
				.forEach(row -> detectedAnomalies.add(anomaly(
						"INFO",
						"HIGH_GOLD_BALANCE",
						row.getUserKey(),
						"보유 골드 과다",
						"현재 골드가 " + row.getGold() + "G예요. 정상 장기 미수령일 수 있지만 보상 기준의 "
								+ goldThresholdMultiplier + "배를 넘어서 관찰 대상으로 표시했어요.",
						row.getGold(),
						suspiciousGold,
						row.getUpdatedAt())));

		playerRepository.findPlayersWithSkillPointsAtLeast(skillPointsWarning, limit)
				.forEach(row -> detectedAnomalies.add(anomaly(
						"INFO",
						"HIGH_SKILL_POINTS",
						row.getUserKey(),
						"미사용 스킬 포인트 과다",
						"미사용 SP가 " + row.getSkillPoints() + "개예요. 결제/초대/레벨업 누적이 정상인지 확인하세요.",
						row.getSkillPoints(),
						skillPointsWarning,
						row.getUpdatedAt())));

		playerRepository.findPlayersWithTimersBeyond(maxAutoHuntEnd, maxBoostEnd, limit)
				.forEach(row -> detectedAnomalies.add(timerAnomaly(now, row, timerGraceSeconds)));

		List<AdminAnomaly> anomalies = new ArrayList<>(enrichAnomalyCases(detectedAnomalies));

		anomalies.sort(Comparator
				.comparingInt((AdminAnomaly anomaly) -> severityRank(anomaly.severity()))
				.thenComparing(AdminAnomaly::detectedAt, Comparator.nullsLast(Comparator.reverseOrder())));

		return new AdminAnomalyReport(
				now,
				anomalies.size(),
				countSeverity(anomalies, "CRITICAL"),
				countSeverity(anomalies, "WARNING"),
				countSeverity(anomalies, "INFO"),
				anomalies,
				new AdminAnomalyThresholds(
						adEventsPerHourWarning,
						rewardClaimsPerDayWarning,
						suspiciousGold,
						skillPointsWarning,
						economy.maxAdSeconds(),
						timerGraceSeconds),
				List.of(
						new AdminAnomalyRule(
								"HIGH_AD_EVENTS",
								"1시간 광고 이벤트 과다",
								"최근 1시간 동안 유저별 광고 보상 이벤트 수가 기준 이상이면 표시해요.",
								"WARNING",
								adEventsPerHourWarning,
								"회",
								true,
								"anomalyAdEventsPerHourWarning",
								1,
								10_000),
						new AdminAnomalyRule(
								"HIGH_REWARD_CLAIMS",
								"일일 보상 수령 과다",
								"하루 보상 수령 건수가 기준 이상이면 포인트 지급 전 확인 대상으로 표시해요.",
								"CRITICAL",
								rewardClaimsPerDayWarning,
								"건",
								true,
								"anomalyRewardClaimsPerDayWarning",
								1,
								1_000),
						new AdminAnomalyRule(
								"HIGH_GOLD_BALANCE",
								"보유 골드 과다",
								"보상 신청 기준 골드에 배수를 곱한 값 이상 보유하면 장기 미수령/비정상 누적 확인 대상으로 표시해요.",
								"INFO",
								goldThresholdMultiplier,
								"배",
								true,
								"anomalyGoldThresholdMultiplier",
								1,
								1_000),
						new AdminAnomalyRule(
								"HIGH_SKILL_POINTS",
								"미사용 스킬 포인트 과다",
								"미사용 SP가 기준 이상이면 결제/초대/레벨업 누적이 정상인지 확인해요.",
								"INFO",
								skillPointsWarning,
								"SP",
								true,
								"anomalySkillPointsWarning",
								1,
								100_000),
						new AdminAnomalyRule(
								"TIMER_OVER_CAP",
								"광고 시간 상한 초과",
								"자동사냥/공속버프 남은 시간이 최대 누적 시간과 유예 시간을 초과하면 표시해요.",
								"CRITICAL",
								timerGraceSeconds,
								"초 유예",
								true,
								"anomalyTimerGraceSeconds",
								0,
								86_400),
						new AdminAnomalyRule(
								"LIMIT_PER_RULE",
								"룰별 최대 표시 수",
								"각 이상징후 항목에서 한 번에 조회할 최대 유저 수예요.",
								"INFO",
								economy.anomalyLimitPerRule(),
								"건",
								true,
								"anomalyLimitPerRule",
								1,
								200)));
	}

	public AdminPlayerGrowthReport playerGrowth(int days) {
		int safeDays = Math.max(7, Math.min(days, 90));
		LocalDate today = LocalDate.now(SEOUL);
		LocalDate firstDay = today.minusDays(safeDays - 1L);
		Instant firstDayStart = firstDay.atStartOfDay(SEOUL).toInstant();
		long cumulative = playerRepository.countByCreatedAtBefore(firstDayStart);
		List<AdminPlayerGrowthPoint> points = new ArrayList<>();
		for (int offset = 0; offset < safeDays; offset++) {
			LocalDate day = firstDay.plusDays(offset);
			Instant startedAt = day.atStartOfDay(SEOUL).toInstant();
			Instant endedAt = day.plusDays(1).atStartOfDay(SEOUL).toInstant();
			long newPlayers = playerRepository.countByCreatedAtBetween(startedAt, endedAt);
			cumulative += newPlayers;
			points.add(new AdminPlayerGrowthPoint(day.toString(), newPlayers, cumulative));
		}
		return new AdminPlayerGrowthReport(Instant.now(clock), safeDays, points);
	}

	public AdminServerMetrics serverMetrics() {
		Runtime runtime = Runtime.getRuntime();
		java.lang.management.MemoryMXBean memory = java.lang.management.ManagementFactory.getMemoryMXBean();
		java.lang.management.ThreadMXBean threads = java.lang.management.ManagementFactory.getThreadMXBean();
		java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
		java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		return new AdminServerMetrics(
				Instant.now(clock),
				runtimeMxBean.getUptime(),
				memory.getHeapMemoryUsage().getUsed(),
				memory.getHeapMemoryUsage().getMax(),
				memory.getNonHeapMemoryUsage().getUsed(),
				threads.getThreadCount(),
				threads.getDaemonThreadCount(),
				runtime.availableProcessors(),
				os.getSystemLoadAverage(),
				runtime.freeMemory(),
				runtime.totalMemory(),
				runtime.maxMemory());
	}

	private AdminAnomaly timerAnomaly(Instant now, PlayerRepository.PlayerTimerSnapshot row, long timerGraceSeconds) {
		long autoHuntSeconds = secondsAfterNow(now, row.getAutoHuntEndsAt());
		long boostSeconds = secondsAfterNow(now, row.getBoostEndsAt());
		long observedSeconds = Math.max(autoHuntSeconds, boostSeconds);
		String timerName = autoHuntSeconds >= boostSeconds ? "자동사냥" : "공속버프";
		return anomaly(
				"CRITICAL",
				"TIMER_OVER_CAP",
				row.getUserKey(),
				timerName + " 시간 상한 초과",
				timerName + " 남은 시간이 " + observedSeconds + "초로, 정책 상한 "
						+ economy.maxAdSeconds() + "초와 유예 " + timerGraceSeconds + "초를 초과했어요.",
				observedSeconds,
				economy.maxAdSeconds() + timerGraceSeconds,
				row.getUpdatedAt());
	}

	private AdminAnomaly anomaly(
			String severity,
			String category,
			String userKey,
			String title,
			String detail,
			long observedValue,
			long thresholdValue,
			Instant detectedAt
	) {
		String anomalyKey = anomalyKey(category, userKey);
		return new AdminAnomaly(
				anomalyKey,
				severity,
				category,
				userKey,
				title,
				detail,
				observedValue,
				thresholdValue,
				detectedAt,
				AdminAnomalyStatus.OPEN,
				null,
				null,
				List.of());
	}

	private List<AdminAnomaly> enrichAnomalyCases(List<AdminAnomaly> anomalies) {
		if (anomalies.isEmpty()) {
			return anomalies;
		}
		Map<String, AdminAnomalyCase> cases = anomalyCaseRepository
				.findByAnomalyKeyIn(anomalies.stream().map(AdminAnomaly::anomalyKey).toList())
				.stream()
				.collect(Collectors.toMap(AdminAnomalyCase::getAnomalyKey, Function.identity()));
		return anomalies.stream()
				.map(anomaly -> {
					AdminAnomalyCase anomalyCase = cases.get(anomaly.anomalyKey());
					if (anomalyCase == null) {
						return anomaly;
					}
					List<AdminAnomalyActionSummary> actions = anomalyActionRepository
							.findByAnomalyCaseOrderByCreatedAtDesc(anomalyCase)
							.stream()
							.map(action -> new AdminAnomalyActionSummary(
									action.getStatus(),
									action.getNote(),
									action.getActorFingerprint(),
									action.getCreatedAt()))
							.toList();
					return new AdminAnomaly(
							anomaly.anomalyKey(),
							anomaly.severity(),
							anomaly.category(),
							anomaly.userKey(),
							anomaly.title(),
							anomaly.detail(),
							anomaly.observedValue(),
							anomaly.thresholdValue(),
							anomaly.detectedAt(),
							anomalyCase.getStatus(),
							anomalyCase.getNote(),
							anomalyCase.getUpdatedAt(),
							actions);
				})
				.toList();
	}

	private String anomalyKey(String category, String userKey) {
		return category + ":" + userKey;
	}

	private List<AdminRuntimeStatusItem> runtimeStatusItems() {
		OperationalStatus rewardAds = adStatus(appProperties.realRewardAdsEnabled(), "리워드");
		OperationalStatus bannerAds = adStatus(appProperties.realBannerAdsEnabled(), "배너");
		OperationalStatus pointRewards = promotionStatus();
		return List.of(
				statusItem(
						"review-tools",
						"심사용 테스트 도구",
						!appProperties.reviewToolsEnabled(),
						appProperties.reviewToolsEnabled() ? "테스트 도구 활성" : "비활성",
						appProperties.reviewToolsEnabled() ? MODE_TEST : MODE_OFF),
				statusItem(
						"guest-user",
						"게스트 접속",
						!appProperties.guestUserEnabled(),
						appProperties.guestUserEnabled() ? "게스트 허용" : "비활성",
						appProperties.guestUserEnabled() ? MODE_TEST : MODE_OFF),
				statusItem(
						"mock-monetization",
						"모의 수익화",
						!appProperties.mockMonetizationEnabled(),
						appProperties.mockMonetizationEnabled() ? "테스트 모드" : "비활성",
						appProperties.mockMonetizationEnabled() ? MODE_TEST : MODE_OFF),
				statusItem(
						"toss-identity",
						"토스 사용자 식별",
						appProperties.tossLoginEnabled() || appProperties.tossUserKeyEnabled(),
						appProperties.tossUserKeyEnabled() ? "userKey 사용" : "로그인/식별 확인 필요",
						appProperties.tossUserKeyEnabled() ? MODE_LIVE : MODE_CHECK),
				statusItem("reward-ads", "리워드 광고", rewardAds.healthy(), rewardAds.detail(), rewardAds.mode()),
				statusItem("banner-ads", "배너 광고", bannerAds.healthy(), bannerAds.detail(), bannerAds.mode()),
				statusItem(
						"payments",
						"인앱 결제",
						appProperties.realPaymentsEnabled(),
						appProperties.realPaymentsEnabled() ? "운영 결제" : "비활성",
						appProperties.realPaymentsEnabled() ? MODE_LIVE : MODE_OFF),
				statusItem("point-rewards", "토스포인트 지급", pointRewards.healthy(), pointRewards.detail(), pointRewards.mode()),
				statusItem(
						"smart-message",
						"스마트 발송",
						appProperties.realSmartMessageEnabled(),
						appProperties.realSmartMessageEnabled() ? "운영 발송" : "비활성",
						appProperties.realSmartMessageEnabled() ? MODE_LIVE : MODE_OFF),
				statusItem(
						"share-reward",
						"공유 리워드",
						appProperties.realShareRewardEnabled(),
						appProperties.realShareRewardEnabled() ? "운영 리워드" : "비활성",
						appProperties.realShareRewardEnabled() ? MODE_LIVE : MODE_OFF));
	}

	private OperationalStatus adStatus(boolean enabled, String label) {
		if (!enabled) {
			return new OperationalStatus(false, "비활성", MODE_OFF);
		}
		if ("live".equals(adProperties.normalizedMode())) {
			return new OperationalStatus(true, label + " 운영 광고 ID 사용", MODE_LIVE);
		}
		return new OperationalStatus(false, label + " 테스트 광고 ID 사용", MODE_TEST);
	}

	private OperationalStatus promotionStatus() {
		if (!appProperties.realTossPointRewardsEnabled()) {
			return new OperationalStatus(false, "비활성", MODE_OFF);
		}
		String code = promotionProperties.normalizedRewardClaimCode();
		if (code.isBlank()) {
			return new OperationalStatus(false, "프로모션 코드 없음", MODE_CHECK);
		}
		if (code.startsWith("TEST_")) {
			return new OperationalStatus(false, "테스트 프로모션 코드", MODE_TEST);
		}
		return new OperationalStatus(true, "운영 프로모션 코드", MODE_LIVE);
	}

	private AdminRuntimeStatusItem statusItem(String key, String label, boolean healthy, String detail, String mode) {
		return new AdminRuntimeStatusItem(key, label, healthy ? "OK" : "BLOCKED", healthy, detail, mode);
	}

	private long secondsAfterNow(Instant now, Instant target) {
		if (target == null || !target.isAfter(now)) {
			return 0;
		}
		return Duration.between(now, target).toSeconds();
	}

	private long multiplyCapped(long value, long multiplier) {
		if (value > Long.MAX_VALUE / multiplier) {
			return Long.MAX_VALUE;
		}
		return value * multiplier;
	}

	private long countSeverity(List<AdminAnomaly> anomalies, String severity) {
		return anomalies.stream()
				.filter(anomaly -> severity.equals(anomaly.severity()))
				.count();
	}

	private int severityRank(String severity) {
		return switch (severity) {
			case "CRITICAL" -> 0;
			case "WARNING" -> 1;
			default -> 2;
		};
	}

	public record AdminOverview(
			Instant generatedAt,
			String integrationMode,
			String distributionTarget,
			boolean tossReleaseReady,
			java.util.List<String> releaseBlockers,
			long totalPlayers,
			long onboardedPlayers,
			long suspendedPlayers,
			long newPlayersToday,
			long activeAutoHuntPlayers,
			long activeBoostPlayers,
			long totalGoldInCirculation,
			long rewardAdEventsToday,
			long rewardClaimsToday,
			long rewardPointsToday,
			long pendingRewardClaims,
			long pendingRewardPoints,
			long estimatedAdRevenueWonToday,
			long estimatedRewardCostWonToday,
			long estimatedNetWonToday,
			EconomyPolicySnapshot economy,
			List<AdminRuntimeStatusItem> runtimeStatusItems
	) {
	}

	public record AdminAnomalyReport(
			Instant generatedAt,
			int totalCount,
			long criticalCount,
			long warningCount,
			long infoCount,
			List<AdminAnomaly> anomalies,
			AdminAnomalyThresholds thresholds,
			List<AdminAnomalyRule> rules
	) {
	}

	public record AdminAnomaly(
			String anomalyKey,
			String severity,
			String category,
			String userKey,
			String title,
			String detail,
			long observedValue,
			long thresholdValue,
			Instant detectedAt,
			AdminAnomalyStatus status,
			String note,
			Instant statusUpdatedAt,
			List<AdminAnomalyActionSummary> actions
	) {
	}

	public record AdminAnomalyRule(
			String category,
			String title,
			String description,
			String severity,
			long thresholdValue,
			String unit,
			boolean editable,
			String policyKey,
			long min,
			long max
	) {
	}

	public record AdminAnomalyActionSummary(
			AdminAnomalyStatus status,
			String note,
			String actorFingerprint,
			Instant createdAt
	) {
	}

	public record AdminAnomalyThresholds(
			long adEventsPerHourWarning,
			long rewardClaimsPerDayWarning,
			long goldBalanceWarning,
			int skillPointsWarning,
			long maxAdSeconds,
			long timerGraceSeconds
	) {
	}

	public record AdminPlayerGrowthReport(
			Instant generatedAt,
			int days,
			List<AdminPlayerGrowthPoint> points
	) {
	}

	public record AdminPlayerGrowthPoint(
			String date,
			long newPlayers,
			long totalPlayers
	) {
	}

	public record AdminRuntimeStatusItem(
			String key,
			String label,
			String status,
			boolean healthy,
			String detail,
			String mode
	) {
	}

	private record OperationalStatus(
			boolean healthy,
			String detail,
			String mode
	) {
	}

	public record AdminServerMetrics(
			Instant generatedAt,
			long uptimeMillis,
			long heapUsedBytes,
			long heapMaxBytes,
			long nonHeapUsedBytes,
			int threadCount,
			int daemonThreadCount,
			int availableProcessors,
			double systemLoadAverage,
			long freeMemoryBytes,
			long totalMemoryBytes,
			long maxMemoryBytes
	) {
	}
}
