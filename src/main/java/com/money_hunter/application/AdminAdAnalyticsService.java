package com.money_hunter.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
	private static final int DEFAULT_PLAYER_PAGE_SIZE = 30;
	private static final int MAX_PLAYER_PAGE_SIZE = 100;
	private static final List<AdEventType> ANALYTICS_AD_TYPES = List.of(
			AdEventType.AUTO_HUNT,
			AdEventType.DUNGEON_ADDITIONAL_ENTRY,
			AdEventType.MINI_GAME_CONTINUE,
			AdEventType.REWARD_CLAIM);

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
	public AdminAdAnalyticsReport report(int page, int size, String sort) {
		LocalDate today = LocalDate.now(SEOUL);
		LocalDate weekStartedAt = today.minusDays(6);
		Instant periodStartedAt = weekStartedAt.atStartOfDay(SEOUL).toInstant();
		Instant periodEndedAt = today.plusDays(1).atStartOfDay(SEOUL).toInstant();
		long accessDayCount = playerDailyAccessRepository.countAccessDatesBetween(weekStartedAt, today);
		long activeUserCount = playerDailyAccessRepository.countActivePlayersBetween(weekStartedAt, today);
		List<AdminAdDailyAverage> dailyAverages = dailyAverages(
				periodStartedAt,
				periodEndedAt,
				accessDayCount,
				activeUserCount);
		AdminPlayerAdPlaybackPage playerPlaybackPage = playerPlaybackPage(page, size, sort);
		AdminAdAnalyticsSummary summary = summary(dailyAverages, accessDayCount, activeUserCount);
		return new AdminAdAnalyticsReport(
				Instant.now(clock),
				weekStartedAt.toString(),
				today.toString(),
				summary,
				dailyAverages,
				playerPlaybackPage);
	}

	private List<AdminAdDailyAverage> dailyAverages(
			Instant periodStartedAt,
			Instant periodEndedAt,
			long accessDayCount,
			long activeUserCount
	) {
		Map<AdEventType, Long> serverPlayedCounts = new EnumMap<>(AdEventType.class);
		adEventRepository.findTypeCountsBetween(periodStartedAt, periodEndedAt, ANALYTICS_AD_TYPES)
				.forEach(row -> serverPlayedCounts.put(row.getType(), row.getEventCount()));

		Map<AdEventType, EnumMap<AdClientEventType, Long>> clientCounts = new EnumMap<>(AdEventType.class);
		adClientEventRepository.findTypeEventCountsBetween(periodStartedAt, periodEndedAt, ANALYTICS_AD_TYPES)
				.forEach(row -> clientCounts(row.getType(), clientCounts).put(row.getEventType(), row.getEventCount()));

		long dayDenominator = Math.max(1L, accessDayCount);
		long activeUserDenominator = Math.max(1L, activeUserCount);
		return ANALYTICS_AD_TYPES.stream()
				.map(type -> {
					EnumMap<AdClientEventType, Long> counts = clientCounts(type, clientCounts);
					long clientPlayedCount = counts.getOrDefault(AdClientEventType.PLAYED, 0L);
					long playedCount = Math.max(serverPlayedCounts.getOrDefault(type, 0L), clientPlayedCount);
					return new AdminAdDailyAverage(
							type.name(),
							adLabel(type),
							adShortLabel(type),
							playedCount,
							roundTwoDecimals((double) playedCount / dayDenominator),
							roundTwoDecimals((double) playedCount / activeUserDenominator),
							counts.getOrDefault(AdClientEventType.ATTEMPTED, 0L),
							counts.getOrDefault(AdClientEventType.LOAD_FAILED, 0L),
							counts.getOrDefault(AdClientEventType.SHOW_FAILED, 0L),
							clientPlayedCount);
				})
				.toList();
	}

	private AdminPlayerAdPlaybackPage playerPlaybackPage(int page, int size, String sort) {
		int safePage = Math.max(0, page);
		int safeSize = Math.max(1, Math.min(size <= 0 ? DEFAULT_PLAYER_PAGE_SIZE : size, MAX_PLAYER_PAGE_SIZE));
		String normalizedSort = normalizedSort(sort);
		Map<String, PlayerAdAccumulator> rows = new LinkedHashMap<>();

		adEventRepository.findPlayerTypeCounts(ANALYTICS_AD_TYPES)
				.forEach(row -> accumulator(row.getUserKey(), rows)
						.recordPlayed(
								row.getAdminNickname(),
								row.getLevel(),
								row.getLastAccessedAt(),
								row.getType(),
								row.getEventCount(),
								row.getLastOccurredAt()));
		adClientEventRepository.findPlayerEventCountsByType(ANALYTICS_AD_TYPES)
				.forEach(row -> accumulator(row.getUserKey(), rows)
						.recordClientEvent(
								row.getAdminNickname(),
								row.getLevel(),
								row.getLastAccessedAt(),
								row.getType(),
								row.getEventType(),
								row.getEventCount(),
								row.getLastOccurredAt()));

		List<PlayerAdAccumulator> sortedRows = new ArrayList<>(rows.values());
		sortedRows.sort(playerComparator(normalizedSort)
				.thenComparing(PlayerAdAccumulator::userKey));

		int totalElements = sortedRows.size();
		int totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / safeSize);
		int currentPage = Math.min(safePage, totalPages - 1);
		int fromIndex = Math.min(totalElements, currentPage * safeSize);
		int toIndex = Math.min(totalElements, fromIndex + safeSize);
		List<AdminPlayerAdPlayback> content = sortedRows.subList(fromIndex, toIndex)
				.stream()
				.map(PlayerAdAccumulator::toResponse)
				.toList();
		return new AdminPlayerAdPlaybackPage(
				content,
				currentPage,
				safeSize,
				totalElements,
				totalPages,
				normalizedSort);
	}

	private AdminAdAnalyticsSummary summary(
			List<AdminAdDailyAverage> averages,
			long accessDayCount,
			long activeUserCount
	) {
		long totalPlayed = averages.stream().mapToLong(AdminAdDailyAverage::playedCount).sum();
		long totalAttempted = averages.stream().mapToLong(AdminAdDailyAverage::attemptedCount).sum();
		long totalLoadFailed = averages.stream().mapToLong(AdminAdDailyAverage::loadFailureCount).sum();
		long totalShowFailed = averages.stream().mapToLong(AdminAdDailyAverage::showFailureCount).sum();
		long totalClientPlayed = averages.stream().mapToLong(AdminAdDailyAverage::clientPlayedCount).sum();
		long dayDenominator = Math.max(1L, accessDayCount);
		long activeUserDenominator = Math.max(1L, activeUserCount);
		return new AdminAdAnalyticsSummary(
				totalPlayed,
				roundTwoDecimals((double) totalPlayed / dayDenominator),
				roundTwoDecimals((double) totalPlayed / activeUserDenominator),
				totalAttempted,
				totalLoadFailed,
				totalShowFailed,
				totalClientPlayed,
				accessDayCount,
				activeUserCount);
	}

	private PlayerAdAccumulator accumulator(String userKey, Map<String, PlayerAdAccumulator> rows) {
		return rows.computeIfAbsent(userKey, PlayerAdAccumulator::new);
	}

	private EnumMap<AdClientEventType, Long> clientCounts(
			AdEventType type,
			Map<AdEventType, EnumMap<AdClientEventType, Long>> counts
	) {
		return counts.computeIfAbsent(type, ignored -> new EnumMap<>(AdClientEventType.class));
	}

	private Comparator<PlayerAdAccumulator> playerComparator(String sort) {
		return switch (sort) {
			case "played:desc" -> Comparator
					.comparingLong(PlayerAdAccumulator::totalPlayedCount)
					.reversed()
					.thenComparing(instantComparator(PlayerAdAccumulator::lastActivityAt));
			case "failures:desc" -> Comparator
					.comparingLong(PlayerAdAccumulator::totalFailureCount)
					.reversed()
					.thenComparing(Comparator.comparingLong(PlayerAdAccumulator::totalPlayedCount).reversed());
			case "attempted:desc" -> Comparator
					.comparingLong(PlayerAdAccumulator::totalAttemptedCount)
					.reversed()
					.thenComparing(Comparator.comparingLong(PlayerAdAccumulator::totalPlayedCount).reversed());
			case "recentActivity:desc" -> instantComparator(PlayerAdAccumulator::lastActivityAt)
					.thenComparing(Comparator.comparingLong(PlayerAdAccumulator::totalPlayedCount).reversed());
			default -> instantComparator(PlayerAdAccumulator::lastAccessedAt)
					.thenComparing(Comparator.comparingLong(PlayerAdAccumulator::totalPlayedCount).reversed());
		};
	}

	private Comparator<PlayerAdAccumulator> instantComparator(Function<PlayerAdAccumulator, Instant> extractor) {
		return (left, right) -> compareInstantDesc(extractor.apply(left), extractor.apply(right));
	}

	private int compareInstantDesc(Instant left, Instant right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		return right.compareTo(left);
	}

	private String normalizedSort(String sort) {
		return switch (sort == null ? "" : sort.trim()) {
			case "played:desc", "failures:desc", "attempted:desc", "recentActivity:desc", "lastAccessedAt:desc" -> sort.trim();
			default -> "lastAccessedAt:desc";
		};
	}

	private double roundTwoDecimals(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private String adLabel(AdEventType type) {
		return switch (type) {
			case AUTO_HUNT -> "자동사냥 광고";
			case DUNGEON_ADDITIONAL_ENTRY -> "던전 추가 입장 광고";
			case MINI_GAME_CONTINUE -> "순발력 훈련장 광고";
			case REWARD_CLAIM -> "토스포인트 환급 전 광고";
			case SKILL_POINT -> "SP 광고";
			default -> type == null ? "알 수 없는 광고" : type.name();
		};
	}

	private String adShortLabel(AdEventType type) {
		return switch (type) {
			case AUTO_HUNT -> "자동사냥";
			case DUNGEON_ADDITIONAL_ENTRY -> "던전추가입장";
			case MINI_GAME_CONTINUE -> "훈련장";
			case REWARD_CLAIM -> "토스환급";
			default -> adLabel(type);
		};
	}

	public record AdminAdAnalyticsReport(
			Instant generatedAt,
			String weekStartedAt,
			String weekEndedAt,
			AdminAdAnalyticsSummary summary,
			List<AdminAdDailyAverage> dailyAverages,
			AdminPlayerAdPlaybackPage playerPlaybackPage
	) {
	}

	public record AdminAdAnalyticsSummary(
			long playedCount,
			double averageDailyPlayedCount,
			double averageDailyPlayedPerActiveUserCount,
			long attemptedCount,
			long loadFailureCount,
			long showFailureCount,
			long clientPlayedCount,
			long accessDayCount,
			long activeUserCount
	) {
	}

	public record AdminAdDailyAverage(
			String type,
			String label,
			String shortLabel,
			long playedCount,
			double averageDailyPlayedCount,
			double averageDailyPlayedPerActiveUserCount,
			long attemptedCount,
			long loadFailureCount,
			long showFailureCount,
			long clientPlayedCount
	) {
	}

	public record AdminPlayerAdPlaybackPage(
			List<AdminPlayerAdPlayback> content,
			int page,
			int size,
			int totalElements,
			int totalPages,
			String sort
	) {
	}

	public record AdminPlayerAdPlayback(
			String userKey,
			String adminNickname,
			int level,
			Instant lastAccessedAt,
			long totalPlayedCount,
			long attemptedCount,
			long loadFailureCount,
			long showFailureCount,
			long clientPlayedCount,
			Instant lastPlayedAt,
			Instant lastClientEventAt,
			List<AdminPlayerAdTypePlayback> ads
	) {
	}

	public record AdminPlayerAdTypePlayback(
			String type,
			String label,
			String shortLabel,
			long totalPlayedCount,
			long attemptedCount,
			long loadFailureCount,
			long showFailureCount,
			long clientPlayedCount,
			Instant lastPlayedAt,
			Instant lastClientEventAt
	) {
	}

	private final class PlayerAdAccumulator {
		private final String userKey;
		private final EnumMap<AdEventType, AdTypeAccumulator> ads = new EnumMap<>(AdEventType.class);
		private String adminNickname;
		private int level;
		private Instant lastAccessedAt;

		private PlayerAdAccumulator(String userKey) {
			this.userKey = userKey;
			ANALYTICS_AD_TYPES.forEach(type -> ads.put(type, new AdTypeAccumulator(type)));
		}

		private void recordPlayed(
				String nickname,
				int level,
				Instant lastAccessedAt,
				AdEventType type,
				long count,
				Instant lastOccurredAt
		) {
			rememberPlayer(nickname, level, lastAccessedAt);
			ad(type).recordPlayed(count, lastOccurredAt);
		}

		private void recordClientEvent(
				String nickname,
				int level,
				Instant lastAccessedAt,
				AdEventType type,
				AdClientEventType eventType,
				long count,
				Instant lastOccurredAt
		) {
			rememberPlayer(nickname, level, lastAccessedAt);
			ad(type).recordClientEvent(eventType, count, lastOccurredAt);
		}

		private void rememberPlayer(String nickname, int level, Instant lastAccessedAt) {
			if (this.adminNickname == null || this.adminNickname.isBlank()) {
				this.adminNickname = nickname;
			}
			this.level = Math.max(this.level, level);
			this.lastAccessedAt = latest(this.lastAccessedAt, lastAccessedAt);
		}

		private AdTypeAccumulator ad(AdEventType type) {
			return ads.computeIfAbsent(type, AdTypeAccumulator::new);
		}

		private AdminPlayerAdPlayback toResponse() {
			List<AdminPlayerAdTypePlayback> adResponses = ANALYTICS_AD_TYPES.stream()
					.map(type -> ads.getOrDefault(type, new AdTypeAccumulator(type)).toResponse())
					.toList();
			return new AdminPlayerAdPlayback(
					userKey,
					adminNickname,
					level,
					lastAccessedAt,
					totalPlayedCount(),
					totalAttemptedCount(),
					totalLoadFailureCount(),
					totalShowFailureCount(),
					totalClientPlayedCount(),
					lastPlayedAt(),
					lastClientEventAt(),
					adResponses);
		}

		private String userKey() {
			return userKey;
		}

		private Instant lastAccessedAt() {
			return lastAccessedAt;
		}

		private long totalPlayedCount() {
			return ads.values().stream().mapToLong(AdTypeAccumulator::playedCount).sum();
		}

		private long totalAttemptedCount() {
			return ads.values().stream().mapToLong(AdTypeAccumulator::attemptedCount).sum();
		}

		private long totalFailureCount() {
			return totalLoadFailureCount() + totalShowFailureCount();
		}

		private long totalLoadFailureCount() {
			return ads.values().stream().mapToLong(AdTypeAccumulator::loadFailureCount).sum();
		}

		private long totalShowFailureCount() {
			return ads.values().stream().mapToLong(AdTypeAccumulator::showFailureCount).sum();
		}

		private long totalClientPlayedCount() {
			return ads.values().stream().mapToLong(AdTypeAccumulator::clientPlayedCount).sum();
		}

		private Instant lastPlayedAt() {
			return ads.values().stream()
					.map(AdTypeAccumulator::lastPlayedAt)
					.reduce(null, this::latest);
		}

		private Instant lastClientEventAt() {
			return ads.values().stream()
					.map(AdTypeAccumulator::lastClientEventAt)
					.reduce(null, this::latest);
		}

		private Instant lastActivityAt() {
			return latest(lastPlayedAt(), lastClientEventAt());
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

	private final class AdTypeAccumulator {
		private final AdEventType type;
		private long serverPlayedCount;
		private long attemptedCount;
		private long loadFailureCount;
		private long showFailureCount;
		private long clientPlayedCount;
		private Instant lastPlayedAt;
		private Instant lastClientEventAt;

		private AdTypeAccumulator(AdEventType type) {
			this.type = type;
		}

		private void recordPlayed(long count, Instant lastOccurredAt) {
			this.serverPlayedCount += count;
			this.lastPlayedAt = latest(this.lastPlayedAt, lastOccurredAt);
		}

		private void recordClientEvent(AdClientEventType eventType, long count, Instant lastOccurredAt) {
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

		private AdminPlayerAdTypePlayback toResponse() {
			return new AdminPlayerAdTypePlayback(
					type.name(),
					adLabel(type),
					adShortLabel(type),
					playedCount(),
					attemptedCount,
					loadFailureCount,
					showFailureCount,
					clientPlayedCount,
					lastPlayedAt,
					lastClientEventAt);
		}

		private long playedCount() {
			return Math.max(serverPlayedCount, clientPlayedCount);
		}

		private long attemptedCount() {
			return attemptedCount;
		}

		private long loadFailureCount() {
			return loadFailureCount;
		}

		private long showFailureCount() {
			return showFailureCount;
		}

		private long clientPlayedCount() {
			return clientPlayedCount;
		}

		private Instant lastPlayedAt() {
			return lastPlayedAt;
		}

		private Instant lastClientEventAt() {
			return lastClientEventAt;
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
