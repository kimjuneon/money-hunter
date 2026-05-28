package com.money_hunter.application;

import com.money_hunter.application.LoginSessionService.IssuedLoginSession;
import com.money_hunter.infrastructure.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TossLoginService {
	private static final String REVIEW_USER_KEY = "test-player";

	private final AppProperties appProperties;
	private final TossLoginClient tossLoginClient;
	private final LoginSessionService loginSessionService;

	public TossLoginService(
			AppProperties appProperties,
			TossLoginClient tossLoginClient,
			LoginSessionService loginSessionService
	) {
		this.appProperties = appProperties;
		this.tossLoginClient = tossLoginClient;
		this.loginSessionService = loginSessionService;
	}

	public IssuedLoginSession login(String authorizationCode, String referrer) {
		if (appProperties.mockMonetizationEnabled()) {
			return loginSessionService.issue(REVIEW_USER_KEY);
		}
		if (!appProperties.tossLoginEnabled()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "토스 로그인이 아직 활성화되지 않았어요.");
		}
		if (authorizationCode == null || authorizationCode.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토스 로그인 인가 코드가 필요해요.");
		}
		TossLoginClient.TossLoginUser user = tossLoginClient.login(authorizationCode.trim(), referrer);
		return loginSessionService.issue(user.userKey());
	}
}
