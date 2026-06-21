package com.money_hunter.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.money_hunter.domain.AdClientEventType;
import com.money_hunter.domain.AdEventType;
import com.money_hunter.infrastructure.persistence.AdClientEventRepository;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.PlayerDailyAccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAdAnalyticsService {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final int MAX_PLAYER_ROWS = 1_000;
	private static final List<AdEventType> REWARD_AD_TYPES = List.of(
			AdEventType.AUTO_HUNT,
			AdEventType.SKILL_POINT,
			AdEventType.REWARD_CLAIM,
			AdEventType.DUNGEON_ADDITIONAL_ENTRY);

	private final AdEventRepository adEventRepository;
	private final AdClientEventRepository adClientEventRepository;
	private final PlayerDailyAccessRepository playerDailyAccessRepository;
	private final Clock clock = Clock.systemUTC();

	public AdminAdAnalyticsService(
			AdEventRepository adEventRepository,
			AdClientEventRepository adClientEventRepository,
			PlayerDailyAccessRepository playerDailyAccessRepository
	) {
		this.adEventRepository = adEventRepository;
		this.adClientEventRepository = adClientEventRepository;
		this.playerDailyAccessRepository = playerDailyAccessRepository;
	}

	@Transactional(readOnly = true)
	public AdminAdAnalyticsReport report() {
		LocalDate today = LocalDate.now(SEOUL);
		LocalDate weekStartedAt = today.minusDays(6);
		Instant periodStartedAt = weekStartedAt.atStartOfDay(SEOUL).toInstant();
		Instant periodEndedAt = today.plusDays(1).atStartOfDay(SEOUL).toInstant();
		long accessDayCount = playerDailyAccessRepository.countAccessDatesBetween(weekStartedAt, today);
		long activeUserDays = playerDailyAccessRepository.countActiveUserDaysBetween(weekStartedAt, today);
		List<AdminAdDailyAverage> dailyAverages = dailyAverages(periodStartedAt, periodEndedAt, accessDayCount);
		List<AdminPlayerAdPlayback> playerPlaybacks = playerPlaybacks();
		AdminAdAnalyticsSummary summary = summary(dailyAverages, accessDayCount, activeUserDays);
		return new AdminAdAnalyticsReport(
				Instant.now(clock),
				weekStartedAt.toString(),
				today.toString(),
				summary,
				dailyAverages,
				playerPlaybacks);
	}

	private List<AdminAdDailyAverage> dailyAverages(
			Instant periodStartedAt,
			Instant periodEndedAt,
			long accessDayCount
	) {
		Map<AdEventType, Long> playedCounts = new EnumMap<>(AdEventType.class);
		adEventRepository.findTypeCountsBetween(periodStartedAt, periodEndedAt, REWARD_AD_TYPES)
				.forEach(row -> playedCounts.put(row.getType(), row.getEventCount()));

		Map<AdEventType, EnumMap<AdClientEventType, Long>> clientCounts = new EnumMap<>(AdEventType.class);
		adClientEventRepository.findTypeEventCountsBetween(periodStartedAt, periodEndedAt, REWARD_AD_TYPES)
				.forEach(row -> clientCounts(row.getType(), clientCounts).put(row.getEventType(), row.getEventCount()));

		long denominator = Math.max(1L, accessDayCount);
		return REWARD_AD_TYPES.stream()
				.map(type -> {
					EnumMap<AdClientEventType, Long> counts = clientCounts(type, clientCounts);
					long playedCount = playedCounts.getOrDefault(type, 0L);
					return new AdminAdDailyAverage(
							type.name(),
							adLabel(type),
							playedCount,
							roundTwoDecimals((double) playedCount / denominator),
							counts.getOrDefault(AdClientEventType.ATTEMPTED, 0L),
							counts.getOrDefault(AdClientEventType.LOAD_FAILED, 0L),
							counts.getOrDefault(AdClientEventType.SHOW_FAILED, 0L),
							counts.getOrDefault(AdClientEventType.PLAYED, 0L));
				})
				.toList();
	}

	private List<AdminPlayerAdPlayback> playerPlaybacks() {
		Map<PlayerAdKey, PlayerAdAccumulator> rows = new LinkedHashMap<>();
		adEventRepository.findPlayerTypeCounts(REWARD_AD_TYPES)
				.forEach(row -> accumulator(row.getUserKey(), row.getType(), rows)
						.recordPlayed(row.getAdminNickname(), row.getLevel(), row.getEventCount(), row.getLastOccurredAt()));
		adClientEventRepository.findPlayerEventCountsByType(REWARD_AD_TYPES)
				.forEach(row -> accumulator(row.getUserKey(), row.getType(), rows)
						.recordClientEvent(
								row.getAdminNickname(),
								row.getLevel(),
								row.getEventType(),
								row.getEventCount(),
								row.getLastOccurredAt()));

		return rows.values().stream()
				.sorted(Comparator
						.comparingLong(PlayerAdAccumulator::totalPlayedCount).reversed()
						.thenComparing(Comparator.comparingLong(PlayerAdAccumulator::attemptedCount).reversed())
						.thenComparing(PlayerAdAccumulator::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(PlayerAdAccumulator::userKey)
						.thenComparing(PlayerAdAccumulator::typeName))
				.limit(MAX_PLAYER_ROWS)
				.map(PlayerAdAccumulator::toResponse)
				.toList();
	}

	private AdminAdAnalyticsSummary summary(
			List<AdminAdDailyAverage> averages,
			long accessDayCount,
			long activeUserDays
	) {
		long totalPlayed = averages.stream().mapToLong(AdminAdDailyAverage::playedCount).sum();
		long totalAttempted = averages.stream().mapToLong(AdminAdDailyAverage::attemptedCount).sum();
		long totalLoadFailed = averages.stream().mapToLong(AdminAdDailyAverage::loadFailureCount).sum();
		long totalShowFailed = averages.stream().mapToLong(AdminAdDailyAverage::showFailureCount).sum();
		long totalClientPlayed = averages.stream().mapToLong(AdminAdDailyAverage::clientPlayedCount).sum();
		long denominator = Math.max(1L, accessDayCount);
		return new AdminAdAnalyticsSummary(
				totalPlayed,
				roundTwoDecimals((double) totalPlayed / denominator),
				totalAttempted,
				totalLoadFailed,
				totalShowFailed,
				totalClientPlayed,
				accessDayCount,
				activeUserDays);
	}

	private PlayerAdAccumulator accumulator(
			String userKey,
			AdEventType type,
			Map<PlayerAdKey, PlayerAdAccumulator> rows
	) {
		PlayerAdKey key = new PlayerAdKey(userKey, type);
		return rows.computeIfAbsent(key, ignored -> new PlayerAdAccumulator(userKey, type));
	}

	private EnumMap<AdClientEventType, Long> clientCounts(
			AdEventType type,
			Map<AdEventType, EnumMap<AdClientEventType, Long>> counts
	) {
		return counts.computeIfAbsent(type, ignored -> new EnumMap<>(AdClientEventType.class));
	}

	private double roundTwoDecimals(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private String adLabel(AdEventType type) {
		return switch (type) {
			case AUTO_HUNT -> "자동사냥 광고";
			case SKILL_POINT -> "SP 광고";
			case REWARD_CLAIM -> "토스포인트 환급 전 광고";
			case DUNGEON_ADDITIONAL_ENTRY -> "던전 추가 입장 광고";
			default -> type == null ? "알 수 없는 광고" : type.name();
		};
	}

	public record AdminAdAnalyticsReport(
			Instant generatedAt,
			String weekStartedAt,
			String weekEndedAt,
			AdminAdAnalyticsSummary summary,
			List<AdminAdDailyAverage> dailyAverages,
			List<AdminPlayerAdPlayback> playerPlaybacks
	) {
	}

	public record AdminAdAnalyticsSummary(
			long playedCount,
			double averageDailyPlayedCount,
			long attemptedCount,
			long loadFailureCount,
			long showFailureCount,
			long clientPlayedCount,
			long accessDayCount,
			long activeUserDays
	) {
	}

	public record AdminAdDailyAverage(
			String type,
			String label,
			long playedCount,
			double averageDailyPlayedCount,
			long attemptedCount,
			long loadFailureCount,
			long showFailureCount,
			long clientPlayedCount
	) {
	}

	public record AdminPlayerAdPlayback(
			String userKey,
			String adminNickname,
			int level,
			String type,
			String label,
			long totalPlayedCount,
			long attemptedCount,
			long loadFailureCount,
			long showFailureCount,
			long clientPlayedCount,
			Instant lastPlayedAt,
			Instant lastClientEventAt
	) {
	}

	private record PlayerAdKey(String userKey, AdEventType type) {
	}

	private final class PlayerAdAccumulator {
		private final String userKey;
		private final AdEventType type;
		private String adminNickname;
		private int level;
		private long totalPlayedCount;
		private long attemptedCount;
		private long loadFailureCount;
		private long showFailureCount;
		private long clientPlayedCount;
		private Instant lastPlayedAt;
		private Instant lastClientEventAt;

		private PlayerAdAccumulator(String userKey, AdEventType type) {
			this.userKey = userKey;
			this.type = type;
		}

		private void recordPlayed(String nickname, int level, long count, Instant lastOccurredAt) {
			rememberPlayer(nickname, level);
			this.totalPlayedCount += count;
			this.lastPlayedAt = latest(this.lastPlayedAt, lastOccurredAt);
		}

		private void recordClientEvent(
				String nickname,
				int level,
				AdClientEventType eventType,
				long count,
				Instant lastOccurredAt
		) {
			rememberPlayer(nickname, level);
			if (eventType == AdClientEventType.ATTEMPTED) {
				this.attemptedCount += count;
			} else if (eventType == AdClientEventType.LOAD_FAILED) {
				this.loadFailureCount += count;
			} else if (eventType == AdClientEventType.SHOW_FAILED) {
				this.showFailureCount += count;
			} else if (eventType == AdClientEventType.PLAYED) {
				this.clientPlayedCount += count;
			}
			this.lastClientEventAt = latest(this.lastClientEventAt, lastOccurredAt);
		}

		private void rememberPlayer(String nickname, int level) {
			if (this.adminNickname == null || this.adminNickname.isBlank()) {
				this.adminNickname = nickname;
			}
			this.level = Math.max(this.level, level);
		}

		private AdminPlayerAdPlayback toResponse() {
			return new AdminPlayerAdPlayback(
					userKey,
					adminNickname,
					level,
					type.name(),
					adLabel(type),
					totalPlayedCount,
					attemptedCount,
					loadFailureCount,
					showFailureCount,
					clientPlayedCount,
					lastPlayedAt,
					lastClientEventAt);
		}

		private String userKey() {
			return userKey;
		}

		private String typeName() {
			return type.name();
		}

		private long totalPlayedCount() {
			return totalPlayedCount;
		}

		private long attemptedCount() {
			return attemptedCount;
		}

		private Instant lastActivityAt() {
			return latest(lastPlayedAt, lastClientEventAt);
		}

		private Instant latest(Instant current, Instant candidate) {
			if (candidate == null) {
				return current;
			}
			if (current == null || candidate.isAfter(current)) {
				return candidate;
			}
			return current;
		}
	}
}
