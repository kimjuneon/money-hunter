package com.money_hunter.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record AdminPlayerNicknameRequest(
		@Size(max = 80, message = "관리자 별명은 80자 이하로 입력해 주세요.")
		String nickname,
		String reason
) {
}
