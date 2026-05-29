package com.money_hunter.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.money_hunter.application.dto.response.AdminPlayerResetResponse;
import com.money_hunter.application.dto.response.AdminPlayerResponse;
import com.money_hunter.domain.Player;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.AdRewardSessionRepository;
import com.money_hunter.infrastructure.persistence.LoginSessionRepository;
import com.money_hunter.infrastructure.persistence.NotificationEventRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import com.money_hunter.infrastructure.persistence.RewardClaimRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPlayerService {
	private static final int MAX_SEARCH_LIMIT = 100;

	private final PlayerRepository playerRepository;
	private final LoginSessionRepository loginSessionRepository;
	private final AdRewardSessionRepository adRewardSessionRepository;
	private final NotificationEventRepository notificationEventRepository;
	private final RewardClaimRepository rewardClaimRepository;
	private final AdEventRepository adEventRepository;
	private final Clock clock = Clock.systemUTC();

	public AdminPlayerService(
			PlayerRepository playerRepository,
			LoginSessionRepository loginSessionRepository,
			AdRewardSessionRepository adRewardSessionRepository,
			NotificationEventRepository notificationEventRepository,
			RewardClaimRepository rewardClaimRepository,
			AdEventRepository adEventRepository
	) {
		this.playerRepository = playerRepository;
		this.loginSessionRepository = loginSessionRepository;
		this.adRewardSessionRepository = adRewardSessionRepository;
		this.notificationEventRepository = notificationEventRepository;
		this.rewardClaimRepository = rewardClaimRepository;
		this.adEventRepository = adEventRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminPlayerResponse> search(String query, int limit) {
		int safeLimit = Math.max(1, Math.min(limit, MAX_SEARCH_LIMIT));
		return playerRepository.searchPlayers(normalize(query), PageRequest.of(0, safeLimit)).stream()
				.map(AdminPlayerResponse::from)
				.toList();
	}

	@Transactional
	public AdminPlayerResponse suspend(String userKey, String reason) {
		Player player = player(userKey);
		player.suspend(reason, clock.instant());
		loginSessionRepository.deleteByUserKey(userKey);
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse resume(String userKey) {
		Player player = player(userKey);
		player.resume();
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResetResponse resetFromLogin(String userKey) {
		String targetUserKey = requiredUserKey(userKey);
		long loginSessionsDeleted = loginSessionRepository.deleteByUserKey(targetUserKey);
		boolean playerDeleted = false;
		long adRewardSessionsDeleted = 0;
		long notificationsDeleted = 0;
		long rewardClaimsDeleted = 0;
		long adEventsDeleted = 0;
		Player player = playerRepository.findByUserKey(targetUserKey).orElse(null);
		if (player != null) {
			adRewardSessionsDeleted = adRewardSessionRepository.deleteByPlayerUserKey(targetUserKey);
			notificationsDeleted = notificationEventRepository.deleteByPlayerUserKey(targetUserKey);
			rewardClaimsDeleted = rewardClaimRepository.deleteByPlayerUserKey(targetUserKey);
			adEventsDeleted = adEventRepository.deleteByPlayerUserKey(targetUserKey);
			playerRepository.delete(player);
			playerDeleted = true;
		}
		return new AdminPlayerResetResponse(
				targetUserKey,
				playerDeleted,
				loginSessionsDeleted,
				adRewardSessionsDeleted,
				notificationsDeleted,
				rewardClaimsDeleted,
				adEventsDeleted);
	}

	private Player player(String userKey) {
		return playerRepository.findByUserKey(requiredUserKey(userKey))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저를 찾지 못했어요."));
	}

	private String requiredUserKey(String userKey) {
		if (userKey == null || userKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유저 키가 필요해요.");
		}
		return userKey.trim();
	}

	private String normalize(String query) {
		if (query == null || query.isBlank()) {
			return "";
		}
		return query.trim();
	}
}
