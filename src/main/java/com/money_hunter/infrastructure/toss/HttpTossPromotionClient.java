package com.money_hunter.infrastructure.toss;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.money_hunter.application.TossPromotionClient;
import com.money_hunter.infrastructure.config.TossApiProperties;
import org.springframework.stereotype.Component;

@Component
public class HttpTossPromotionClient implements TossPromotionClient {
	private static final String GET_KEY_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion/get-key";
	private static final String EXECUTE_PATH = "/api-partner/v1/apps-in-toss/promotion/execute-promotion";
	private static final String EXECUTION_RESULT_PATH = "/api-partner/v1/apps-in-toss/promotion/execution-result";

	private final TossApiProperties properties;
	private final ObjectMapper objectMapper;
	private final TossMtlsHttpClientFactory httpClientFactory;

	public HttpTossPromotionClient(TossApiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClientFactory = new TossMtlsHttpClientFactory(properties);
	}

	@Override
	public String issueExecutionKey(String userKey) {
		JsonNode success = send(HttpRequest.newBuilder(uri(GET_KEY_PATH))
				.timeout(httpClientFactory.requestTimeout())
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("x-toss-user-key", userKey)
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.build());
		String key = success.path("key").asText("");
		if (key.isBlank()) {
			throw new IllegalStateException("토스 포인트 지급 키가 비어 있어요.");
		}
		return key;
	}

	@Override
	public void executePromotion(String userKey, String promotionCode, String executionKey, int amount) {
		Map<String, Object> body = Map.of(
				"promotionCode", promotionCode,
				"key", executionKey,
				"amount", amount
		);
		try {
			send(HttpRequest.newBuilder(uri(EXECUTE_PATH))
					.timeout(httpClientFactory.requestTimeout())
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.header("x-toss-user-key", userKey)
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
					.build());
		} catch (IOException exception) {
			throw new IllegalStateException("토스 포인트 지급 요청을 만들지 못했어요.", exception);
		}
	}

	@Override
	public String getExecutionResult(String userKey, String promotionCode, String executionKey) {
		Map<String, Object> body = Map.of(
				"promotionCode", promotionCode,
				"key", executionKey
		);
		try {
			JsonNode success = send(HttpRequest.newBuilder(uri(EXECUTION_RESULT_PATH))
					.timeout(httpClientFactory.requestTimeout())
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.header("x-toss-user-key", userKey)
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
					.build());
			return success.asText("PENDING");
		} catch (IOException exception) {
			throw new IllegalStateException("토스 포인트 지급 결과 조회 요청을 만들지 못했어요.", exception);
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
			throw new IllegalStateException(tossErrorMessage(root));
		} catch (IOException exception) {
			throw new IllegalStateException("토스 포인트 서버와 통신하지 못했어요.", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("토스 포인트 요청이 중단됐어요.", exception);
		}
	}

	private String tossErrorMessage(JsonNode root) {
		String reason = root.path("error").path("reason").asText("");
		return reason.isBlank() ? "토스 포인트 처리에 실패했어요." : reason;
	}

	private URI uri(String path) {
		return URI.create(properties.normalizedBaseUrl() + path);
	}
}
