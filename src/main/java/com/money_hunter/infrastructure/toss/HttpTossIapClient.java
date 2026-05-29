package com.money_hunter.infrastructure.toss;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.money_hunter.application.TossIapClient;
import com.money_hunter.application.TossIapOrderStatus;
import com.money_hunter.infrastructure.config.TossApiProperties;
import org.springframework.stereotype.Component;

@Component
public class HttpTossIapClient implements TossIapClient {
	private static final String ORDER_STATUS_PATH = "/api-partner/v1/apps-in-toss/order/get-order-status";

	private final TossApiProperties properties;
	private final ObjectMapper objectMapper;
	private final TossMtlsHttpClientFactory httpClientFactory;

	public HttpTossIapClient(TossApiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClientFactory = new TossMtlsHttpClientFactory(properties);
	}

	@Override
	public TossIapOrderStatus getOrderStatus(String userKey, String orderId) {
		try {
			HttpRequest request = HttpRequest.newBuilder(uri(ORDER_STATUS_PATH))
					.timeout(httpClientFactory.requestTimeout())
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.header("x-toss-user-key", userKey)
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("orderId", orderId))))
					.build();
			JsonNode success = send(request);
			return new TossIapOrderStatus(
					success.path("orderId").asText(""),
					success.path("sku").asText(""),
					success.path("status").asText(""),
					success.path("reason").asText(""));
		} catch (IOException exception) {
			throw new IllegalStateException("토스 인앱결제 조회 요청을 만들지 못했어요.", exception);
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
			throw new IllegalStateException("토스 인앱결제 서버와 통신하지 못했어요.", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("토스 인앱결제 조회 요청이 중단됐어요.", exception);
		}
	}

	private String tossErrorMessage(JsonNode root) {
		String reason = root.path("error").path("reason").asText("");
		return reason.isBlank() ? "토스 인앱결제 조회에 실패했어요." : reason;
	}

	private URI uri(String path) {
		return URI.create(properties.normalizedBaseUrl() + path);
	}
}
