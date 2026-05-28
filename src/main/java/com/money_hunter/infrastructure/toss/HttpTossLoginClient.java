package com.money_hunter.infrastructure.toss;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.money_hunter.application.TossLoginClient;
import com.money_hunter.infrastructure.config.TossApiProperties;
import org.springframework.stereotype.Component;

@Component
public class HttpTossLoginClient implements TossLoginClient {
	private static final String GENERATE_TOKEN_PATH = "/api-partner/v1/apps-in-toss/user/oauth2/generate-token";
	private static final String LOGIN_ME_PATH = "/api-partner/v1/apps-in-toss/user/oauth2/login-me";

	private final TossApiProperties properties;
	private final ObjectMapper objectMapper;
	private final TossMtlsHttpClientFactory httpClientFactory;

	public HttpTossLoginClient(TossApiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClientFactory = new TossMtlsHttpClientFactory(properties);
	}

	@Override
	public TossLoginUser login(String authorizationCode, String referrer) {
		String accessToken = generateToken(authorizationCode, referrer);
		return loginMe(accessToken);
	}

	private String generateToken(String authorizationCode, String referrer) {
		Map<String, String> body = Map.of(
				"authorizationCode", authorizationCode,
				"referrer", referrer == null || referrer.isBlank() ? "DEFAULT" : referrer
		);
		JsonNode success = post(GENERATE_TOKEN_PATH, body);
		String accessToken = success.path("accessToken").asText("");
		if (accessToken.isBlank()) {
			throw new IllegalStateException("토스 AccessToken 응답이 올바르지 않아요.");
		}
		return accessToken;
	}

	private TossLoginUser loginMe(String accessToken) {
		HttpRequest request = HttpRequest.newBuilder(uri(LOGIN_ME_PATH))
				.timeout(httpClientFactory.requestTimeout())
				.header("Authorization", "Bearer " + accessToken)
				.header("Accept", "application/json")
				.GET()
				.build();
		JsonNode success = send(request);
		String userKey = success.path("userKey").asText("");
		if (userKey.isBlank()) {
			throw new IllegalStateException("토스 로그인 사용자 식별키를 찾지 못했어요.");
		}
		return new TossLoginUser(userKey);
	}

	private JsonNode post(String path, Object body) {
		try {
			HttpRequest request = HttpRequest.newBuilder(uri(path))
					.timeout(httpClientFactory.requestTimeout())
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
					.build();
			return send(request);
		} catch (IOException exception) {
			throw new IllegalStateException("토스 로그인 요청을 만들지 못했어요.", exception);
		}
	}

	private JsonNode send(HttpRequest request) {
		try {
			HttpResponse<String> response = httpClientFactory.get()
					.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode root = objectMapper.readTree(response.body());
			if (response.statusCode() >= 400) {
				throw new IllegalStateException(tossErrorMessage(root));
			}
			if ("SUCCESS".equals(root.path("resultType").asText())) {
				return root.path("success");
			}
			if (root.has("error")) {
				throw new IllegalStateException(tossErrorMessage(root));
			}
			throw new IllegalStateException("토스 로그인 응답이 올바르지 않아요.");
		} catch (IOException exception) {
			throw new IllegalStateException("토스 로그인 서버와 통신하지 못했어요.", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("토스 로그인 요청이 중단됐어요.", exception);
		}
	}

	private String tossErrorMessage(JsonNode root) {
		if (root.path("error").isTextual()) {
			return "토스 로그인이 만료됐거나 다시 사용할 수 없는 요청이에요.";
		}
		String reason = root.path("error").path("reason").asText("");
		return reason.isBlank() ? "토스 로그인 처리에 실패했어요." : reason;
	}

	private URI uri(String path) {
		return URI.create(properties.normalizedBaseUrl() + path);
	}
}
