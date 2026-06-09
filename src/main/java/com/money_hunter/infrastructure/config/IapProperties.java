package com.money_hunter.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.iap")
public record IapProperties(
		String flarePetProductId,
		String aquaPetProductId,
		String skillPointPackProductId,
		String vipMonthlyProductId
) {
	public Map<String, String> clientProductIds() {
		return Map.of(
				"flarePet", value(flarePetProductId),
				"aquaPet", value(aquaPetProductId),
				"skillPointPack", value(skillPointPackProductId),
				"vipMonthly", value(vipMonthlyProductId)
		);
	}

	public boolean isKnownProduct(String productId) {
		return !value(productType(productId)).isBlank();
	}

	public String productType(String productId) {
		String normalized = value(productId);
		if (normalized.isBlank()) {
			return "";
		}
		if (matches(normalized, flarePetProductId)) {
			return "FLARE_PET";
		}
		if (matches(normalized, aquaPetProductId)) {
			return "AQUA_PET";
		}
		if (matches(normalized, skillPointPackProductId)) {
			return "SKILL_POINT_PACK";
		}
		if (matches(normalized, vipMonthlyProductId)) {
			return "VIP_MONTHLY";
		}
		return "";
	}

	private boolean matches(String normalizedProductId, String configuredProductId) {
		String configured = value(configuredProductId);
		return !configured.isBlank() && normalizedProductId.equals(configured);
	}

	private String value(String raw) {
		return raw == null ? "" : raw.trim();
	}
}
