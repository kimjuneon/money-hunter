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

import com.money_hunter.domain.RewardClaimStatus;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import com.money_hunter.infrastructure.persistence.RewardClaimRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AdminMonitoringService {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final int ANOMALY_LIMIT_PER_RULE = 20;
	private static final long AD_EVENTS_PER_HOUR_WARNING = 20;
	private static final long REWARD_CLAIMS_PER_DAY_WARNING = 3;
	private static final int SKILL_POINTS_WARNING = 100;
	private static final long TIMER_GRACE_SECONDS = 120;
	private static final long GOLD_THRESHOLD_MULTIPLIER = 3;

	private final PlayerRepository playerRepository;
	private final AdEventRepository adEventRepository;
	private final RewardClaimRepository rewardClaimRepository;
	private final RuntimeEconomyService economy;
	private final AppProperties appProperties;
	private final Clock clock = Clock.systemUTC();

	public AdminMonitoringService(
			PlayerRepository playerRepository,
			AdEventRepository adEventRepository,
			RewardClaimRepository rewardClaimRepository,
			RuntimeEconomyService economy,
			AppProperties appProperties
	) {
		this.playerRepository = playerRepository;
		this.adEventRepository = adEventRepository;
		this.rewardClaimRepository = rewardClaimRepository;
		this.economy = economy;
		this.appProperties = appProperties;
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
				economy.snapshot()
		);
	}

	public AdminAnomalyReport anomalies() {
		Instant now = Instant.now(clock);
		Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
		Instant today = LocalDate.now(SEOUL).atStartOfDay(SEOUL).toInstant();
		var limit = PageRequest.of(0, ANOMALY_LIMIT_PER_RULE);
		long suspiciousGold = multiplyCapped(economy.rewardGoldThreshold(), GOLD_THRESHOLD_MULTIPLIER);
		Instant maxAutoHuntEnd = now.plusSeconds(economy.maxAdSeconds() + TIMER_GRACE_SECONDS);
		Instant maxBoostEnd = now.plusSeconds(economy.maxAdSeconds() + TIMER_GRACE_SECONDS);
		List<AdminAnomaly> anomalies = new ArrayList<>();

		adEventRepository.findPlayerEventCountsSince(oneHourAgo, AD_EVENTS_PER_HOUR_WARNING, limit)
				.forEach(row -> anomalies.add(new AdminAnomaly(
						"WARNING",
						"HIGH_AD_EVENTS",
						row.getUserKey(),
						"1시간 광고 이벤트 과다",
						"최근 1시간 동안 광고성 보상 이벤트가 " + row.getEventCount()
								+ "회 발생했어요. 자동 호출이나 반복 클릭을 확인하세요.",
						row.getEventCount(),
						AD_EVENTS_PER_HOUR_WARNING,
						row.getLastOccurredAt())));

		rewardClaimRepository.findPlayerClaimCountsSince(today, REWARD_CLAIMS_PER_DAY_WARNING, limit)
				.forEach(row -> anomalies.add(new AdminAnomaly(
						"CRITICAL",
						"HIGH_REWARD_CLAIMS",
						row.getUserKey(),
						"일일 보상 수령 과다",
						"오늘 보상 수령이 " + row.getClaimCount() + "건, 총 " + row.getPointAmount()
								+ "P로 집계됐어요. 실제 포인트 지급 전 검토가 필요해요.",
						row.getClaimCount(),
						REWARD_CLAIMS_PER_DAY_WARNING,
						row.getLastClaimedAt())));

		playerRepository.findPlayersWithGoldAtLeast(suspiciousGold, limit)
				.forEach(row -> anomalies.add(new AdminAnomaly(
						"INFO",
						"HIGH_GOLD_BALANCE",
						row.getUserKey(),
						"보유 골드 과다",
						"현재 골드가 " + row.getGold() + "G예요. 정상 장기 미수령일 수 있지만 보상 기준의 "
								+ GOLD_THRESHOLD_MULTIPLIER + "배를 넘어서 관찰 대상으로 표시했어요.",
						row.getGold(),
						suspiciousGold,
						row.getUpdatedAt())));

		playerRepository.findPlayersWithSkillPointsAtLeast(SKILL_POINTS_WARNING, limit)
				.forEach(row -> anomalies.add(new AdminAnomaly(
						"INFO",
						"HIGH_SKILL_POINTS",
						row.getUserKey(),
						"미사용 스킬 포인트 과다",
						"미사용 SP가 " + row.getSkillPoints() + "개예요. 결제/초대/레벨업 누적이 정상인지 확인하세요.",
						row.getSkillPoints(),
						SKILL_POINTS_WARNING,
						row.getUpdatedAt())));

		playerRepository.findPlayersWithTimersBeyond(maxAutoHuntEnd, maxBoostEnd, limit)
				.forEach(row -> anomalies.add(timerAnomaly(now, row)));

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
						AD_EVENTS_PER_HOUR_WARNING,
						REWARD_CLAIMS_PER_DAY_WARNING,
						suspiciousGold,
						SKILL_POINTS_WARNING,
						economy.maxAdSeconds(),
						TIMER_GRACE_SECONDS));
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

	private AdminAnomaly timerAnomaly(Instant now, PlayerRepository.PlayerTimerSnapshot row) {
		long autoHuntSeconds = secondsAfterNow(now, row.getAutoHuntEndsAt());
		long boostSeconds = secondsAfterNow(now, row.getBoostEndsAt());
		long observedSeconds = Math.max(autoHuntSeconds, boostSeconds);
		String timerName = autoHuntSeconds >= boostSeconds ? "자동사냥" : "공속버프";
		return new AdminAnomaly(
				"CRITICAL",
				"TIMER_OVER_CAP",
				row.getUserKey(),
				timerName + " 시간 상한 초과",
				timerName + " 남은 시간이 " + observedSeconds + "초로, 정책 상한 "
						+ economy.maxAdSeconds() + "초와 유예 " + TIMER_GRACE_SECONDS + "초를 초과했어요.",
				observedSeconds,
				economy.maxAdSeconds() + TIMER_GRACE_SECONDS,
				row.getUpdatedAt());
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
			EconomyPolicySnapshot economy
	) {
	}

	public record AdminAnomalyReport(
			Instant generatedAt,
			int totalCount,
			long criticalCount,
			long warningCount,
			long infoCount,
			List<AdminAnomaly> anomalies,
			AdminAnomalyThresholds thresholds
	) {
	}

	public record AdminAnomaly(
			String severity,
			String category,
			String userKey,
			String title,
			String detail,
			long observedValue,
			long thresholdValue,
			Instant detectedAt
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
}
