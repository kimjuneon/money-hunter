package com.money_hunter.presentation.controller;

import com.money_hunter.application.AdminAccessGuard;
import com.money_hunter.application.AdminAuthService;
import com.money_hunter.presentation.dto.request.AdminLoginRequest;
import com.money_hunter.application.dto.response.AdminLoginResponse;
import com.money_hunter.application.dto.response.AdminMeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
	private final AdminAuthService adminAuthService;
	private final AdminAccessGuard adminAccessGuard;

	public AdminAuthController(AdminAuthService adminAuthService, AdminAccessGuard adminAccessGuard) {
		this.adminAuthService = adminAuthService;
		this.adminAccessGuard = adminAccessGuard;
	}

	@PostMapping("/login")
	public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
		return AdminLoginResponse.from(adminAuthService.login(request.loginId(), request.password(), clientIp(httpRequest)));
	}

	@GetMapping("/me")
	public AdminMeResponse me(HttpServletRequest request) {
		return AdminMeResponse.from(adminAccessGuard.require(request));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request) {
		adminAuthService.logout(adminAccessGuard.tokenFrom(request));
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
