package com.money_hunter.infrastructure.toss;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.money_hunter.application.TossSmartMessageClient;
import com.money_hunter.application.TossSmartMessageClient.SmartMessageSendResult;
import com.money_hunter.infrastructure.config.TossApiProperties;
import org.springframework.stereotype.Component;

@Component
public class HttpTossSmartMessageClient implements TossSmartMessageClient {
	private static final String SEND_MESSAGE_PATH = "/api-partner/v1/apps-in-toss/messenger/send-message";

	private final TossApiProperties properties;
	private final ObjectMapper objectMapper;
	private final TossMtlsHttpClientFactory httpClientFactory;

	public HttpTossSmartMessageClient(TossApiProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClientFactory = new TossMtlsHttpClientFactory(properties);
	}

	@Override
	public SmartMessageSendResult sendMessage(String userKey, String templateSetCode, Map<String, String> context) {
		Map<String, Object> body = Map.of(
				"templateSetCode", templateSetCode,
				"context", context
		);
		try {
			HttpRequest request = HttpRequest.newBuilder(uri(SEND_MESSAGE_PATH))
					.timeout(httpClientFactory.requestTimeout())
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.header("x-toss-user-key", userKey)
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
					.build();
			return send(request);
		} catch (IOException exception) {
			throw new IllegalStateException("토스 알림 요청을 만들지 못했어요.", exception);
		}
	}

	private SmartMessageSendResult send(HttpRequest request) {
		try {
			HttpResponse<String> response = httpClientFactory.get()
					.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode root = responseBody(response);
			if (response.statusCode() >= 400 || !"SUCCESS".equals(root.path("resultType").asText())) {
				throw new IllegalStateException(tossErrorMessage(response, root));
			}
			return sendResult(root.path("success"));
		} catch (IOException exception) {
			throw new IllegalStateException("토스 알림 서버와 통신하지 못했어요.", exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("토스 알림 요청이 중단됐어요.", exception);
		}
	}

	private JsonNode responseBody(HttpResponse<String> response) throws IOException {
		String body = response.body() == null ? "" : response.body();
		if (body.isBlank()) {
			throw new IllegalStateException("토스 알림 응답이 비어 있어요. status=" + response.statusCode());
		}
		try {
			return objectMapper.readTree(body);
		} catch (IOException exception) {
			throw new IllegalStateException("토스 알림 응답을 해석하지 못했어요. status="
					+ response.statusCode() + ", body=" + abbreviate(body), exception);
		}
	}

	private SmartMessageSendResult sendResult(JsonNode success) {
		return new SmartMessageSendResult(
				success.path("msgCount").asInt(0),
				success.path("sentPushCount").asInt(0),
				success.path("sentInboxCount").asInt(0),
				failureSummary(success.path("fail")));
	}

	private String failureSummary(JsonNode fail) {
		if (fail.isMissingNode() || fail.isNull()) {
			return "";
		}
		String summary = fail.toString();
		return "{}".equals(summary) ? "" : abbreviate(summary);
	}

	private String tossErrorMessage(HttpResponse<String> response, JsonNode root) {
		String reason = root.path("error").path("reason").asText("");
		String errorCode = root.path("error").path("errorCode").asText("");
		String detail = reason.isBlank() ? "토스 알림 발송에 실패했어요." : reason;
		return "status=" + response.statusCode()
				+ (errorCode.isBlank() ? "" : ", errorCode=" + errorCode)
				+ ", reason=" + detail;
	}

	private String abbreviate(String body) {
		String normalized = body.replaceAll("\\s+", " ").trim();
		return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
	}

	private URI uri(String path) {
		return URI.create(properties.normalizedBaseUrl() + path);
	}
}
