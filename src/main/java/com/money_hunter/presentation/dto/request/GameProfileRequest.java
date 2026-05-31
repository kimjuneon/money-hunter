package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record GameProfileRequest(
		@Size(max = 80, message = "게임 프로필 닉네임은 80자 이하로 입력해 주세요.")
		String nickname
) {
}
