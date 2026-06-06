package com.money_hunter.application.dto.response;

import java.time.Instant;

import com.money_hunter.domain.IapOrder;

public record AdminPaymentResponse(
		String orderId,
		String userKey,
		String productId,
		String productType,
		String productLabel,
		boolean granted,
		Instant grantedAt,
		Instant createdAt
) {
	public static AdminPaymentResponse from(IapOrder order) {
		return new AdminPaymentResponse(
				order.getOrderId(),
				order.getUserKey(),
				order.getProductId(),
				order.getProductType(),
				productLabel(order.getProductType()),
				order.isGranted(),
				order.getGrantedAt(),
				order.getCreatedAt());
	}

	private static String productLabel(String productType) {
		return switch (productType == null ? "" : productType) {
			case "FLARE_PET", "AQUA_PET" -> "동료 펫";
			case "SKILL_POINT_PACK" -> "SP 패키지";
			default -> productType == null || productType.isBlank() ? "알 수 없는 상품" : productType;
		};
	}
}
