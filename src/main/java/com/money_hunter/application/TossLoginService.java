package com.money_hunter.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.money_hunter.application.LoginSessionService.IssuedLoginSession;
import com.money_hunter.domain.Player;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TossLoginService {
	private static final Logger log = LoggerFactory.getLogger(TossLoginService.class);
	private static final String REVIEW_USER_KEY = "test-player";
	private static final String BENEFIT_TAB_ENTRY_PATH = "/benefit";

	private final AppProperties appProperties;
	private final TossLoginClient tossLoginClient;
	private final LoginSessionService loginSessionService;
	private final PlayerRepository playerRepository;
	private final Clock clock = Clock.systemUTC();

	public TossLoginService(
			AppProperties appProperties,
			TossLoginClient tossLoginClient,
			LoginSessionService loginSessionService,
			PlayerRepository playerRepository
	) {
		this.appProperties = appProperties;
		this.tossLoginClient = tossLoginClient;
		this.loginSessionService = loginSessionService;
		this.playerRepository = playerRepository;
	}

	@Transactional
	public IssuedLoginSession login(String authorizationCode, String referrer, String entryPath) {
		if (appProperties.mockMonetizationEnabled()) {
			requireNotSuspended(REVIEW_USER_KEY);
			log.info("리뷰/테스트 로그인 세션 발급: userKey={}", mask(REVIEW_USER_KEY));
			return loginSessionService.issue(REVIEW_USER_KEY);
		}
		if (!appProperties.tossLoginEnabled()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "토스 로그인이 아직 활성화되지 않았어요.");
		}
		if (authorizationCode == null || authorizationCode.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토스 로그인 인가 코드가 필요해요.");
		}
		TossLoginClient.TossLoginUser user = tossLoginClient.login(authorizationCode.trim(), referrer);
		Optional<Player> existingPlayer = playerRepository.findByUserKey(user.userKey());
		existingPlayer.filter(Player::isSuspended)
				.ifPresent(player -> {
					throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 유저예요. 관리자에게 문의해 주세요.");
				});
		if (existingPlayer.isEmpty() && isBenefitTabEntryPath(entryPath)) {
			Instant now = clock.instant();
			Player player = new Player(user.userKey(), now);
			player.markBenefitTabNewUserEntry(now);
			playerRepository.save(player);
			log.info("혜택 탭 신규 유저 진입 기록: userKey={}, entryPath={}", mask(user.userKey()), safeReferrer(entryPath));
		}
		log.info("토스 로그인 성공: userKey={}, referrer={}, entryPath={}",
				mask(user.userKey()),
				safeReferrer(referrer),
				safeReferrer(entryPath));
		return loginSessionService.issue(user.userKey());
	}

	private void requireNotSuspended(String userKey) {
		playerRepository.findByUserKey(userKey)
				.filter(player -> player.isSuspended())
				.ifPresent(player -> {
					throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 유저예요. 관리자에게 문의해 주세요.");
					});
	}

	private String safeReferrer(String referrer) {
		if (referrer == null || referrer.isBlank()) {
			return "";
		}
		String normalized = referrer.trim();
		return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
	}

	private boolean isBenefitTabEntryPath(String entryPath) {
		String normalized = normalizeEntryPath(entryPath);
		return BENEFIT_TAB_ENTRY_PATH.equals(normalized);
	}

	private String normalizeEntryPath(String entryPath) {
		if (entryPath == null || entryPath.isBlank()) {
			return "";
		}
		String normalized = entryPath.trim();
		int queryIndex = normalized.indexOf('?');
		if (queryIndex >= 0) {
			normalized = normalized.substring(0, queryIndex);
		}
		int hashIndex = normalized.indexOf('#');
		if (hashIndex >= 0) {
			normalized = normalized.substring(0, hashIndex);
		}
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		while (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
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
