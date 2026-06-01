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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPlayerService {
	private static final Logger log = LoggerFactory.getLogger(AdminPlayerService.class);
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
	public List<AdminPlayerResponse> search(String query, int limit, boolean favoritesOnly, boolean hiddenSkinsOnly) {
		int safeLimit = Math.max(1, Math.min(limit, MAX_SEARCH_LIMIT));
		return playerRepository.searchPlayers(normalize(query), favoritesOnly, hiddenSkinsOnly, PageRequest.of(0, safeLimit)).stream()
				.map(AdminPlayerResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminPlayerResponse get(String userKey) {
		return AdminPlayerResponse.from(player(userKey));
	}

	@Transactional
	public AdminPlayerResponse suspend(String userKey, String reason) {
		Player player = player(userKey);
		player.suspend(reason, clock.instant());
		loginSessionRepository.deleteByUserKey(userKey);
		log.warn("관리자 유저 정지 처리: userKey={}, reason={}", mask(userKey), truncate(reason, 120));
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse resume(String userKey) {
		Player player = player(userKey);
		player.resume();
		log.info("관리자 유저 정지 해제: userKey={}", mask(userKey));
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse setFavorite(String userKey, boolean favorite) {
		Player player = player(userKey);
		player.setAdminFavorite(favorite);
		log.info("관리자 유저 즐겨찾기 변경: userKey={}, favorite={}", mask(userKey), favorite);
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse updateAdminNickname(String userKey, String nickname) {
		Player player = player(userKey);
		player.updateAdminNickname(nickname);
		log.info("관리자 유저 별명 변경: userKey={}, nickname={}", mask(userKey), truncate(player.getAdminNickname(), 80));
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse adjustGold(String userKey, String mode, long amount) {
		Player player = player(userKey);
		if (isSetMode(mode)) {
			player.setGold(amount);
		} else {
			player.adjustGold(amount);
		}
		log.warn("관리자 골드 조정: userKey={}, mode={}, amount={}, result={}", mask(userKey), normalizedMode(mode), amount, player.getGold());
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse adjustSkillPoints(String userKey, String mode, long amount) {
		Player player = player(userKey);
		int safeAmount = toIntAmount(amount);
		if (isSetMode(mode)) {
			player.setSkillPoints(safeAmount);
		} else {
			player.adjustSkillPoints(safeAmount);
		}
		log.warn("관리자 SP 조정: userKey={}, mode={}, amount={}, result={}", mask(userKey), normalizedMode(mode), safeAmount, player.getSkillPoints());
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse grantPet(String userKey, int maxCharacterSlots) {
		Player player = player(userKey);
		player.purchaseCharacterSlot(maxCharacterSlots);
		log.warn("관리자 펫 지급: userKey={}, characterSlots={}", mask(userKey), player.getCharacterSlots());
		return AdminPlayerResponse.from(player);
	}

	@Transactional
	public AdminPlayerResponse removePet(String userKey) {
		Player player = player(userKey);
		int refundedSkillPoints = player.removePetAndRefundSkillPoints();
		log.warn(
				"관리자 펫 제거: userKey={}, characterSlots={}, refundedSkillPoints={}",
				mask(userKey),
				player.getCharacterSlots(),
				refundedSkillPoints);
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
		log.warn(
				"관리자 유저 초기화 처리: userKey={}, playerDeleted={}, loginSessionsDeleted={}, adRewardSessionsDeleted={}, notificationsDeleted={}, rewardClaimsDeleted={}, adEventsDeleted={}",
				mask(targetUserKey),
				playerDeleted,
				loginSessionsDeleted,
				adRewardSessionsDeleted,
				notificationsDeleted,
				rewardClaimsDeleted,
				adEventsDeleted);
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

	private boolean isSetMode(String mode) {
		return "SET".equals(normalizedMode(mode));
	}

	private String normalizedMode(String mode) {
		String normalized = mode == null ? "" : mode.trim().toUpperCase();
		return "SET".equals(normalized) ? "SET" : "ADD";
	}

	private int toIntAmount(long amount) {
		if (amount > Integer.MAX_VALUE || amount < Integer.MIN_VALUE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SP 조정값 범위가 너무 커요.");
		}
		return (int) amount;
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private String mask(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String normalized = value.trim();
		if (normalized.length() <= 8) {
			return "***" + normalized.charAt(normalized.length() - 1);
		}
		return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 4);
	}
}
