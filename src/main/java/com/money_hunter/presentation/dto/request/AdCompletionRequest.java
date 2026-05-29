package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record AdCompletionRequest(@Size(max = 120) String adSessionToken) {
}
