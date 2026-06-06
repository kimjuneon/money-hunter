package com.money_hunter.presentation.controller;

import com.money_hunter.application.LoginSessionService.IssuedLoginSession;
import com.money_hunter.application.TossLoginService;
import com.money_hunter.presentation.dto.request.TossLoginRequest;
import com.money_hunter.application.dto.response.LoginSessionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final TossLoginService tossLoginService;

	public AuthController(TossLoginService tossLoginService) {
		this.tossLoginService = tossLoginService;
	}

	@PostMapping("/toss/login")
	public LoginSessionResponse login(@Valid @RequestBody TossLoginRequest request) {
		IssuedLoginSession session = tossLoginService.login(request.authorizationCode(), request.referrer(), request.entryPath());
		return new LoginSessionResponse(session.token(), "Bearer", session.userKey(), session.expiresAt());
	}
}
