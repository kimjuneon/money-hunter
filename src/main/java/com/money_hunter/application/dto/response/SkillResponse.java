package com.money_hunter.application.dto.response;

import com.money_hunter.domain.SkillType;

public record SkillResponse(
		SkillType type,
		int level,
		int effectTier
) {
}
