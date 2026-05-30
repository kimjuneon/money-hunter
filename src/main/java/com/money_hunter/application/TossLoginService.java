package com.money_hunter.application;

import com.money_hunter.application.LoginSessionService.IssuedLoginSession;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TossLoginService {
	private static final Logger log = LoggerFactory.getLogger(TossLoginService.class);
	private static final String REVIEW_USER_KEY = "test-player";

	private final AppProperties appProperties;
	private final TossLoginClient tossLoginClient;
	private final LoginSessionService loginSessionService;
	private final PlayerRepository playerRepository;

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

	public IssuedLoginSession login(String authorizationCode, String referrer) {
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
		requireNotSuspended(user.userKey());
		log.info("토스 로그인 성공: userKey={}, referrer={}", mask(user.userKey()), safeReferrer(referrer));
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
