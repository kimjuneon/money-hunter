package com.money_hunter.presentation.dto.response;

public record MonsterResponse(
		String key,
		int hp,
		int maxHp,
		long defeatGold,
		int defeatedMonsters
) {
}
