package com.money_hunter.presentation.dto.request;

import com.money_hunter.domain.JobType;

import jakarta.validation.constraints.NotNull;

public record ChooseJobRequest(@NotNull JobType job) {
}
