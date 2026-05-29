package com.money_hunter.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.iap")
public record IapProperties(
		String flarePetProductId,
		String aquaPetProductId,
		String skillPointPackProductId
) {
	public Map<String, String> clientProductIds() {
		return Map.of(
				"flarePet", value(flarePetProductId),
				"aquaPet", value(aquaPetProductId),
				"skillPointPack", value(skillPointPackProductId)
		);
	}

	public boolean isKnownProduct(String productId) {
		return !value(productType(productId)).isBlank();
	}

	public String productType(String productId) {
		String normalized = value(productId);
		if (normalized.equals(value(flarePetProductId))) {
			return "FLARE_PET";
		}
		if (normalized.equals(value(aquaPetProductId))) {
			return "AQUA_PET";
		}
		if (normalized.equals(value(skillPointPackProductId))) {
			return "SKILL_POINT_PACK";
		}
		return "";
	}

	private String value(String raw) {
		return raw == null ? "" : raw.trim();
	}
}
