package com.money_hunter.presentation.dto.response;

import com.money_hunter.domain.SkillType;

public record SkillResponse(
		SkillType type,
		int level,
		int effectTier
) {
}
