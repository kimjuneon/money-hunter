package com.money_hunter.presentation.dto.request;

import com.money_hunter.domain.SkillType;

import jakarta.validation.constraints.NotNull;

public record UpgradeSkillRequest(@NotNull SkillType type) {
}
