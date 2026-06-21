package com.money_hunter.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.money_hunter.domain.AdEventType;
import com.money_hunter.domain.AppInTossAdDailyMetric;
import com.money_hunter.domain.RewardClaimStatus;
import com.money_hunter.infrastructure.config.AdProperties;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.config.PromotionProperties;
import com.money_hunter.infrastructure.persistence.AdRewardSessionRepository;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.AppInTossAdDailyMetricRepository;
import com.money_hunter.infrastructure.persistence.AdminAnomalyActionRepository;
import com.money_hunter.infrastructure.persistence.AdminAnomalyCaseRepository;
import com.money_hunter.infrastructure.persistence.IapOrderRepository;
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
	private static final long VIP_MONTHLY_PRICE_WON = 9_900L;
	private static final int LOYAL_MIN_LEVEL = 3;
	private static final long LOYAL_MIN_REWARD_AD_EVENTS = 10L;
	private static final List<AdEventType> LOYAL_REWARD_AD_TYPES = List.of(
			AdEventType.AUTO_HUNT,
			AdEventType.SKILL_POINT,
			AdEventType.REWARD_CLAIM,
			AdEventType.DUNGEON_ADDITIONAL_ENTRY);

	private final PlayerRepository playerRepository;
	private final AdEventRepository adEventRepository;
	private final AdRewardSessionRepository adRewardSessionRepository;
	private final AppInTossAdDailyMetricRepository appInTossAdDailyMetricRepository;
	private final RewardClaimRepository rewardClaimRepository;
	private final IapOrderRepository iapOrderRepository;
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
			AdRewardSessionRepository adRewardSessionRepository,
			AppInTossAdDailyMetricRepository appInTossAdDailyMetricRepository,
			RewardClaimRepository rewardClaimRepository,
			IapOrderRepository iapOrderRepository,
			AdminAnomalyCaseRepository anomalyCaseRepository,
			AdminAnomalyActionRepository anomalyActionRepository,
			RuntimeEconomyService economy,
			AppProperties appProperties,
			AdProperties adProperties,
			PromotionProperties promotionProperties
	) {
		this.playerRepository = playerRepository;
		this.adEventRepository = adEventRepository;
		this.adRewardSessionRepository = adRewardSessionRepository;
		this.appInTossAdDailyMetricRepository = appInTossAdDailyMetricRepository;
		this.rewardClaimRepository = rewardClaimRepository;
		this.iapOrderRepository = iapOrderRepository;
		this.anomalyCaseRepository = anomalyCaseRepository;
		this.anomalyActionRepository = anomalyActionRepository;
		this.economy = economy;
		this.appProperties = appProperties;
		this.adProperties = adProperties;
		this.promotionProperties = promotionProperties;
	}

	public AdminOverview overview() {
		Instant now = Instant.now(clock);
		LocalDate todayDate = LocalDate.now(SEOUL);
		Instant today = startOfDay(todayDate);
		Instant tomorrow = startOfDay(todayDate.plusDays(1));
		long adEventsToday = adEventRepository.countByOccurredAtAfter(today);
		long rewardAdEventsToday = adEventsToday;
		long rewardClaimsToday = rewardClaimRepository.countByCreatedAtAfter(today);
		long rewardPointsToday = rewardClaimRepository.sumPointAmountSince(today);
		long pendingRewardClaims = rewardClaimRepository.countByStatus(RewardClaimStatus.PENDING_PROMOTION_GRANT);
		long pendingRewardPoints = rewardClaimRepository.sumPointAmountByStatus(RewardClaimStatus.PENDING_PROMOTION_GRANT);
		long estimatedAdRevenueWonToday = rewardAdEventsToday * economy.adRevenuePerRewardAdWon();
		long estimatedRewardCostWonToday = rewardPointsToday;
		long appEnteredUsersToday = playerRepository.countByLastAccessedAtGreaterThanEqual(today);
		long onboardedEnteredUsersToday = playerRepository
				.countByJobIsNotNullAndLastAccessedAtGreaterThanEqualAndLastAccessedAtLessThan(today, tomorrow);
		long activeUsersToday = countLoyalActiveUsersForReferenceDate(todayDate);
		long visitorOnlyUsersToday = countNonLoyalVisitorsForReferenceDate(todayDate, today, tomorrow);

		return new AdminOverview(
				now,
				appProperties.integrationMode(),
				appProperties.distributionTarget(),
				appProperties.tossReleaseReady(),
				appProperties.releaseBlockers(),
				playerRepository.count(),
				playerRepository.countByJobIsNotNull(),
					playerRepository.countBySuspendedAtIsNotNull(),
					playerRepository.countByCreatedAtGreaterThanEqual(today),
					playerRepository.countByAutoHuntEndsAtAfter(now),
					playerRepository.totalGold(),
				appEnteredUsersToday,
				onboardedEnteredUsersToday,
				activeUsersToday,
				visitorOnlyUsersToday,
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

		playerRepository.findPlayersWithTimersBeyond(maxAutoHuntEnd, limit)
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
								"자동사냥 남은 시간이 최대 누적 시간과 유예 시간을 초과하면 표시해요.",
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
			long newPlayers = playerRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startedAt, endedAt);
			cumulative += newPlayers;
			long appEnteredUsers = playerRepository.countByLastAccessedAtGreaterThanEqualAndLastAccessedAtLessThan(startedAt, endedAt);
			long onboardedEnteredUsers = playerRepository
					.countByJobIsNotNullAndLastAccessedAtGreaterThanEqualAndLastAccessedAtLessThan(startedAt, endedAt);
			long activeUsers = countLoyalActiveUsersForReferenceDate(day);
			long visitorOnlyUsers = countNonLoyalVisitorsForReferenceDate(day, startedAt, endedAt);
			points.add(new AdminPlayerGrowthPoint(
					day.toString(),
					newPlayers,
					cumulative,
					appEnteredUsers,
					onboardedEnteredUsers,
					activeUsers,
					visitorOnlyUsers));
		}
		return new AdminPlayerGrowthReport(Instant.now(clock), safeDays, points);
	}

	public AdminRevenueReport revenue(int days) {
		int safeDays = Math.max(7, Math.min(days, 90));
		LocalDate today = LocalDate.now(SEOUL);
		LocalDate firstDay = today.minusDays(safeDays - 1L);
		Instant periodStartedAt = firstDay.atStartOfDay(SEOUL).toInstant();
		Instant periodEndedAt = today.plusDays(1).atStartOfDay(SEOUL).toInstant();
		Map<LocalDate, AppInTossAdDailyMetric> appInTossMetrics = appInTossAdDailyMetricRepository
				.findByMetricDateBetweenOrderByMetricDateAsc(firstDay, today)
				.stream()
				.collect(Collectors.toMap(AppInTossAdDailyMetric::getMetricDate, Function.identity()));
		List<AdminRevenuePoint> points = new ArrayList<>();

		for (int offset = 0; offset < safeDays; offset++) {
			LocalDate day = firstDay.plusDays(offset);
			Instant startedAt = day.atStartOfDay(SEOUL).toInstant();
			Instant endedAt = day.plusDays(1).atStartOfDay(SEOUL).toInstant();
			points.add(revenuePoint(day, startedAt, endedAt, appInTossMetrics.get(day)));
		}

		AdminRevenueSummary todaySummary = points.isEmpty()
				? emptyRevenueSummary()
				: summaryFromPoint(points.get(points.size() - 1));
		AdminRevenueSummary periodSummary = summaryFromPoints(points);
		return new AdminRevenueReport(
				Instant.now(clock),
				safeDays,
				todaySummary,
				periodSummary,
				points,
				iapProductBreakdown(periodStartedAt, periodEndedAt),
				adEventBreakdown(periodStartedAt, periodEndedAt),
				List.of(
						new AdminRevenueReferenceMetric(
								"실제 eCPM",
								"앱인토스 콘솔의 일별 노출, 광고 시청률, eCPM 3개 값만 입력하면 실제 콘솔 기준 변동을 함께 볼 수 있어요."),
						new AdminRevenueReferenceMetric(
								"광고 세션 완료율",
								"앱에서 광고 세션을 만든 뒤 보상 완료 API까지 도달한 비율이에요. 콘솔 광고 시청률 하락 원인을 확인하는 내부 퍼널 지표예요."),
						new AdminRevenueReferenceMetric(
								"충성 활성 사용자",
								loyalActiveUserDefinition()),
						new AdminRevenueReferenceMetric(
								"트래픽 품질",
								"방문/저관여 비율, 충성 활성당 광고, 보상 전환을 같이 보면 유저 믹스 변화와 광고 위치 문제를 나눠 볼 수 있어요.")));
	}

	public AdminAppInTossAdMetric saveAppInTossAdMetric(
			LocalDate date,
			long adImpressions,
			BigDecimal adWatchRatePercent,
			BigDecimal ecpmWon,
			String note
	) {
		LocalDate today = LocalDate.now(SEOUL);
		if (date.isAfter(today)) {
			throw new IllegalArgumentException("미래 날짜의 앱인토스 지표는 입력할 수 없어요.");
		}
		Instant now = Instant.now(clock);
		AppInTossAdDailyMetric metric = appInTossAdDailyMetricRepository.findByMetricDate(date)
				.orElseGet(() -> new AppInTossAdDailyMetric(date, now));
		metric.update(
				adImpressions,
				normalizeDecimal(adWatchRatePercent, 2),
				normalizeDecimal(ecpmWon, 2),
				note,
				now);
		return toAppInTossAdMetric(appInTossAdDailyMetricRepository.save(metric));
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

	private AdminRevenuePoint revenuePoint(
			LocalDate date,
			Instant startedAt,
			Instant endedAt,
			AppInTossAdDailyMetric appInTossMetric
	) {
		long adEventCount = adEventRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(startedAt, endedAt);
		long adSessionStartedCount = adRewardSessionRepository
				.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startedAt, endedAt);
		long adSessionCompletedCount = adRewardSessionRepository
				.countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndCompletedAtIsNotNull(startedAt, endedAt);
		long rewardClaimCount = rewardClaimRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startedAt, endedAt);
		long rewardCostWon = rewardClaimRepository.sumPointAmountBetween(startedAt, endedAt);
		long iapOrderCount = iapOrderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startedAt, endedAt);
		long estimatedAdRevenueWon = adEventCount * economy.adRevenuePerRewardAdWon();
		long estimatedIapRevenueWon = estimatedIapRevenue(startedAt, endedAt);
		long estimatedGrossRevenueWon = estimatedAdRevenueWon + estimatedIapRevenueWon;
		long appEnteredUsers = playerRepository.countByLastAccessedAtGreaterThanEqualAndLastAccessedAtLessThan(startedAt, endedAt);
		long activeUsers = countLoyalActiveUsersForReferenceDate(date);
		long visitorOnlyUsers = countNonLoyalVisitorsForReferenceDate(date, startedAt, endedAt);
		return new AdminRevenuePoint(
				date.toString(),
				estimatedAdRevenueWon,
				estimatedIapRevenueWon,
				estimatedGrossRevenueWon,
				rewardCostWon,
				estimatedGrossRevenueWon - rewardCostWon,
				adEventCount,
				adSessionStartedCount,
				adSessionCompletedCount,
				rewardClaimCount,
				iapOrderCount,
				appEnteredUsers,
				activeUsers,
				visitorOnlyUsers,
				appInTossMetric == null ? null : appInTossMetric.getAdImpressions(),
				appInTossMetric == null ? null : appInTossMetric.getAdWatchRatePercent(),
				appInTossMetric == null ? null : appInTossMetric.getEcpmWon(),
				appInTossEstimatedRevenueWon(appInTossMetric));
	}

	private long countLoyalActiveUsersForReferenceDate(LocalDate referenceDate) {
		LocalDate firstAccessDay = referenceDate.minusDays(1);
		LocalDate secondAccessDay = referenceDate.minusDays(2);
		LocalDate thirdAccessDay = referenceDate.minusDays(3);
		return playerRepository.countLoyalActiveUsersByReferenceDay(
				firstAccessDay,
				secondAccessDay,
				thirdAccessDay,
				LOYAL_MIN_LEVEL,
				LOYAL_MIN_REWARD_AD_EVENTS,
				LOYAL_REWARD_AD_TYPES,
				AdEventType.DUNGEON_RUN);
	}

	private long countNonLoyalVisitorsForReferenceDate(LocalDate referenceDate, Instant visitedAtStartedAt, Instant visitedAtEndedAt) {
		LocalDate firstAccessDay = referenceDate.minusDays(1);
		LocalDate secondAccessDay = referenceDate.minusDays(2);
		LocalDate thirdAccessDay = referenceDate.minusDays(3);
		return playerRepository.countNonLoyalVisitorsByReferenceDay(
				visitedAtStartedAt,
				visitedAtEndedAt,
				firstAccessDay,
				secondAccessDay,
				thirdAccessDay,
				LOYAL_MIN_LEVEL,
				LOYAL_MIN_REWARD_AD_EVENTS,
				LOYAL_REWARD_AD_TYPES,
				AdEventType.DUNGEON_RUN);
	}

	private String loyalActiveUserDefinition() {
		return "기준일인 오늘은 제외하고 직전 3일 동안 매일 접속했고, 레벨 " + LOYAL_MIN_LEVEL
				+ " 이상이며, 토스포인트 보상 수령 이력, 리워드 광고 이벤트 " + LOYAL_MIN_REWARD_AD_EVENTS
				+ "회 이상, 주간 펀치킹 점수, 던전 탐험 이력을 모두 가진 유저예요. 접속은 일별 접속 기록 기준이에요.";
	}

	private Instant startOfDay(LocalDate day) {
		return day.atStartOfDay(SEOUL).toInstant();
	}

	private BigDecimal normalizeDecimal(BigDecimal value, int scale) {
		return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO).setScale(scale, RoundingMode.HALF_UP);
	}

	private Long appInTossEstimatedRevenueWon(AppInTossAdDailyMetric metric) {
		if (metric == null) {
			return null;
		}
		return metric.getEcpmWon()
				.multiply(BigDecimal.valueOf(metric.getAdImpressions()))
				.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP)
				.longValue();
	}

	private AdminAppInTossAdMetric toAppInTossAdMetric(AppInTossAdDailyMetric metric) {
		return new AdminAppInTossAdMetric(
				metric.getMetricDate().toString(),
				metric.getAdImpressions(),
				metric.getAdWatchRatePercent(),
				metric.getEcpmWon(),
				appInTossEstimatedRevenueWon(metric),
				metric.getNote(),
				metric.getUpdatedAt());
	}

	private long estimatedIapRevenue(Instant startedAt, Instant endedAt) {
		return iapOrderRepository.findProductCountsBetween(startedAt, endedAt)
				.stream()
				.mapToLong(row -> row.getOrderCount() * iapProductPrice(row.getProductType()))
				.sum();
	}

	private AdminRevenueSummary summaryFromPoint(AdminRevenuePoint point) {
		return new AdminRevenueSummary(
				point.estimatedAdRevenueWon(),
				point.estimatedIapRevenueWon(),
				point.estimatedGrossRevenueWon(),
				point.rewardCostWon(),
				point.estimatedNetRevenueWon(),
				point.adEventCount(),
				point.adSessionStartedCount(),
				point.adSessionCompletedCount(),
				point.rewardClaimCount(),
				point.iapOrderCount(),
				point.appEnteredUsers(),
				point.activeUsers(),
				point.visitorOnlyUsers());
	}

	private AdminRevenueSummary summaryFromPoints(List<AdminRevenuePoint> points) {
		return new AdminRevenueSummary(
				points.stream().mapToLong(AdminRevenuePoint::estimatedAdRevenueWon).sum(),
				points.stream().mapToLong(AdminRevenuePoint::estimatedIapRevenueWon).sum(),
				points.stream().mapToLong(AdminRevenuePoint::estimatedGrossRevenueWon).sum(),
				points.stream().mapToLong(AdminRevenuePoint::rewardCostWon).sum(),
				points.stream().mapToLong(AdminRevenuePoint::estimatedNetRevenueWon).sum(),
				points.stream().mapToLong(AdminRevenuePoint::adEventCount).sum(),
				points.stream().mapToLong(AdminRevenuePoint::adSessionStartedCount).sum(),
				points.stream().mapToLong(AdminRevenuePoint::adSessionCompletedCount).sum(),
				points.stream().mapToLong(AdminRevenuePoint::rewardClaimCount).sum(),
				points.stream().mapToLong(AdminRevenuePoint::iapOrderCount).sum(),
				points.stream().mapToLong(AdminRevenuePoint::appEnteredUsers).sum(),
				points.stream().mapToLong(AdminRevenuePoint::activeUsers).sum(),
				points.stream().mapToLong(AdminRevenuePoint::visitorOnlyUsers).sum());
	}

	private AdminRevenueSummary emptyRevenueSummary() {
		return new AdminRevenueSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	private List<AdminIapProductRevenue> iapProductBreakdown(Instant startedAt, Instant endedAt) {
		return iapOrderRepository.findProductCountsBetween(startedAt, endedAt)
				.stream()
				.map(row -> new AdminIapProductRevenue(
						row.getProductType(),
						iapProductLabel(row.getProductType()),
						iapProductPrice(row.getProductType()),
						row.getOrderCount(),
						row.getGrantedCount(),
						row.getOrderCount() * iapProductPrice(row.getProductType())))
				.toList();
	}

	private List<AdminAdEventRevenue> adEventBreakdown(Instant startedAt, Instant endedAt) {
		return adEventRepository.findTypeCountsBetween(startedAt, endedAt)
				.stream()
				.map(row -> new AdminAdEventRevenue(
						row.getType() == null ? "" : row.getType().name(),
						adEventLabel(row.getType()),
						row.getEventCount(),
						row.getEventCount() * economy.adRevenuePerRewardAdWon()))
				.toList();
	}

	private long iapProductPrice(String productType) {
		return switch (productType == null ? "" : productType) {
			case "FLARE_PET", "AQUA_PET" -> economy.companionPriceWon();
			case "SKILL_POINT_PACK" -> economy.skillPointPackPriceWon();
			case "VIP_MONTHLY" -> VIP_MONTHLY_PRICE_WON;
			default -> 0L;
		};
	}

	private String iapProductLabel(String productType) {
		return switch (productType == null ? "" : productType) {
			case "FLARE_PET", "AQUA_PET" -> "동료 펫";
			case "SKILL_POINT_PACK" -> "SP 패키지";
			case "VIP_MONTHLY" -> "VIP 멤버십";
			default -> productType == null || productType.isBlank() ? "알 수 없는 상품" : productType;
		};
	}

	private String adEventLabel(AdEventType type) {
		if (type == null) {
			return "알 수 없는 이벤트";
		}
		return switch (type) {
			case AUTO_HUNT -> "1시간 자동사냥";
			case SKILL_POINT -> "SP 1개";
			case REWARD_CLAIM -> "토스포인트 환급 전 광고";
			case MINI_GAME_CONTINUE -> "순발력 훈련장 이어하기";
			case DUNGEON_ADDITIONAL_ENTRY -> "던전 추가 입장권";
			case DUNGEON_COUPON -> "던전 입장권";
			case DUNGEON_RUN -> "던전 실행";
			case BOSS_RAID -> "보스 토벌";
			case FRIEND_INVITE_REWARD -> "친구 초대 보상";
			case DORMANT_SP_REWARD -> "휴면 복귀 SP 보상";
		};
	}

	private AdminAnomaly timerAnomaly(Instant now, PlayerRepository.PlayerTimerSnapshot row, long timerGraceSeconds) {
		long autoHuntSeconds = secondsAfterNow(now, row.getAutoHuntEndsAt());
		return anomaly(
				"CRITICAL",
				"TIMER_OVER_CAP",
				row.getUserKey(),
				"자동사냥 시간 상한 초과",
				"자동사냥 남은 시간이 " + autoHuntSeconds + "초로, 정책 상한 "
						+ economy.maxAdSeconds() + "초와 유예 " + timerGraceSeconds + "초를 초과했어요.",
				autoHuntSeconds,
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
		OperationalStatus bannerAds = bannerAdStatus();
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

	private OperationalStatus bannerAdStatus() {
		if (!appProperties.realBannerAdsEnabled()) {
			return new OperationalStatus(true, "미사용", MODE_OFF);
		}
		return adStatus(true, "배너");
	}

	private OperationalStatus promotionStatus() {
		if (!appProperties.realTossPointRewardsEnabled()) {
			return new OperationalStatus(false, "비활성", MODE_OFF);
		}
		String rewardClaimCode = promotionProperties.normalizedRewardClaimCode();
		String benefitTabNewUserCode = promotionProperties.normalizedBenefitTabNewUserCode();
		if (rewardClaimCode.isBlank()) {
			return new OperationalStatus(false, "프로모션 코드 없음", MODE_CHECK);
		}
		if (rewardClaimCode.startsWith("TEST_") || benefitTabNewUserCode.startsWith("TEST_")) {
			List<String> testTargets = new ArrayList<>();
			if (rewardClaimCode.startsWith("TEST_")) {
				testTargets.add("보상 수령");
			}
			if (benefitTabNewUserCode.startsWith("TEST_")) {
				testTargets.add("혜택 탭");
			}
			return new OperationalStatus(false, String.join(", ", testTargets) + " 테스트 프로모션 코드", MODE_TEST);
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
				long totalGoldInCirculation,
			long appEnteredUsersToday,
			long onboardedEnteredUsersToday,
			long activeUsersToday,
			long visitorOnlyUsersToday,
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
			long totalPlayers,
			long appEnteredUsers,
			long onboardedEnteredUsers,
			long activeUsers,
			long visitorOnlyUsers
	) {
	}

	public record AdminRevenueReport(
			Instant generatedAt,
			int days,
			AdminRevenueSummary today,
			AdminRevenueSummary period,
			List<AdminRevenuePoint> points,
			List<AdminIapProductRevenue> iapProducts,
			List<AdminAdEventRevenue> adEvents,
			List<AdminRevenueReferenceMetric> referenceMetrics
	) {
	}

	public record AdminRevenueSummary(
			long estimatedAdRevenueWon,
			long estimatedIapRevenueWon,
			long estimatedGrossRevenueWon,
			long rewardCostWon,
			long estimatedNetRevenueWon,
			long adEventCount,
			long adSessionStartedCount,
			long adSessionCompletedCount,
			long rewardClaimCount,
			long iapOrderCount,
			long appEnteredUsers,
			long activeUsers,
			long visitorOnlyUsers
	) {
	}

	public record AdminAppInTossAdMetric(
			String date,
			long adImpressions,
			BigDecimal adWatchRatePercent,
			BigDecimal ecpmWon,
			Long estimatedRevenueWon,
			String note,
			Instant updatedAt
	) {
	}

	public record AdminRevenuePoint(
			String date,
			long estimatedAdRevenueWon,
			long estimatedIapRevenueWon,
			long estimatedGrossRevenueWon,
			long rewardCostWon,
			long estimatedNetRevenueWon,
			long adEventCount,
			long adSessionStartedCount,
			long adSessionCompletedCount,
			long rewardClaimCount,
			long iapOrderCount,
			long appEnteredUsers,
			long activeUsers,
			long visitorOnlyUsers,
			Long appInTossAdImpressions,
			BigDecimal appInTossAdWatchRatePercent,
			BigDecimal appInTossEcpmWon,
			Long appInTossEstimatedRevenueWon
	) {
	}

	public record AdminIapProductRevenue(
			String productType,
			String productLabel,
			long unitPriceWon,
			long orderCount,
			long grantedCount,
			long estimatedRevenueWon
	) {
	}

	public record AdminAdEventRevenue(
			String type,
			String label,
			long eventCount,
			long estimatedRevenueWon
	) {
	}

	public record AdminRevenueReferenceMetric(
			String label,
			String description
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
