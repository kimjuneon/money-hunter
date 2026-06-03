package com.money_hunter.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.money_hunter.application.dto.response.AdminRookieEventSettingsResponse;
import com.money_hunter.domain.RookieEventSettings;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import com.money_hunter.infrastructure.persistence.RookieEventSettingsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RookieEventSettingsService {
	private static final Logger log = LoggerFactory.getLogger(RookieEventSettingsService.class);
	private static final int ROOKIE_EVENT_DAYS = 7;
	private static final int ROOKIE_EVENT_PLAYER_DAYS = 10;
	private static final int ROOKIE_EVENT_REWARD_DURATION_DAYS = 30;
	private static final int ROOKIE_EVENT_PET_SKILL_LEVEL = 15;

	private final RookieEventSettingsRepository settingsRepository;
	private final PlayerRepository playerRepository;
	private final Clock clock = Clock.systemUTC();
	private final AtomicReference<EventSettingsSnapshot> cache = new AtomicReference<>();
	private volatile Instant cacheLoadedAt = Instant.EPOCH;

	public RookieEventSettingsService(
			RookieEventSettingsRepository settingsRepository,
			PlayerRepository playerRepository
	) {
		this.settingsRepository = settingsRepository;
		this.playerRepository = playerRepository;
	}

	@PostConstruct
	void init() {
		reload();
	}

	public boolean rookieEventEnabled() {
		EventSettingsSnapshot snapshot = snapshot();
		return snapshot.enabled();
	}

	@Transactional(readOnly = true)
	public AdminRookieEventSettingsResponse response() {
		EventSettingsSnapshot snapshot = snapshot();
		return new AdminRookieEventSettingsResponse(
				snapshot.enabled(),
				true,
				ROOKIE_EVENT_DAYS,
				ROOKIE_EVENT_PLAYER_DAYS,
				ROOKIE_EVENT_REWARD_DURATION_DAYS,
				ROOKIE_EVENT_PET_SKILL_LEVEL,
				playerRepository.countByRookieEventStartedAtIsNotNull(),
				playerRepository.countByRookieEventCompletedDaysGreaterThanEqual(ROOKIE_EVENT_DAYS),
				playerRepository.countByRookieEventRewardClaimedAtIsNotNull(),
				playerRepository.countByJobIsNotNullAndRookieEventStartedAtIsNull(),
				snapshot.updatedAt());
	}

	@Transactional
	public EventSettingsChangeResult setRookieEventEnabled(boolean enabled) {
		RookieEventSettings row = ensureRow();
		boolean before = row.isEnabled();
		row.setEnabled(enabled, clock.instant());
		settingsRepository.save(row);
		EventSettingsSnapshot after = new EventSettingsSnapshot(row.isEnabled(), row.getUpdatedAt());
		cache.set(after);
		cacheLoadedAt = Instant.now(clock);
		log.warn("신규 이벤트 운영 상태 변경: before={}, after={}", before, enabled);
		return new EventSettingsChangeResult(before, enabled);
	}

	@Transactional(readOnly = true)
	public void reload() {
		RookieEventSettings row = settingsRepository.findById(RookieEventSettings.SINGLETON_ID)
				.orElseGet(() -> new RookieEventSettings(RookieEventSettings.SINGLETON_ID, true, Instant.now(clock)));
		cache.set(new EventSettingsSnapshot(row.isEnabled(), row.getUpdatedAt()));
		cacheLoadedAt = Instant.now(clock);
	}

	private EventSettingsSnapshot snapshot() {
		EventSettingsSnapshot snapshot = cache.get();
		if (snapshot == null || cacheLoadedAt.plus(Duration.ofSeconds(15)).isBefore(Instant.now(clock))) {
			reload();
			return cache.get();
		}
		return snapshot;
	}

	private RookieEventSettings ensureRow() {
		return settingsRepository.findById(RookieEventSettings.SINGLETON_ID)
				.orElseGet(() -> settingsRepository.save(new RookieEventSettings(
						RookieEventSettings.SINGLETON_ID,
						true,
						Instant.now(clock))));
	}

	private record EventSettingsSnapshot(boolean enabled, Instant updatedAt) {
	}

	public record EventSettingsChangeResult(boolean beforeEnabled, boolean afterEnabled) {
	}
}
