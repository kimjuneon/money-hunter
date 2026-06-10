package com.money_hunter.domain;

public record EventRewardGrant(
		long goldAmount,
		int skillPointAmount,
		long autoHuntSeconds,
		int dungeonCouponAmount,
		int bossRaidTicketAmount,
		boolean rookieEventPetReward,
		boolean vipBadgeReward,
		boolean petSkinUnlockReward
) {
	public static EventRewardGrant gold(long amount) {
		return new EventRewardGrant(amount, 0, 0, 0, 0, false, false, false);
	}

	public static EventRewardGrant skillPoint(int amount) {
		return new EventRewardGrant(0, amount, 0, 0, 0, false, false, false);
	}

	public static EventRewardGrant autoHunt(long seconds) {
		return new EventRewardGrant(0, 0, seconds, 0, 0, false, false, false);
	}

	public static EventRewardGrant rookieEventPet() {
		return new EventRewardGrant(0, 0, 0, 0, 0, true, false, false);
	}

	public static EventRewardGrant dailyMissionFinal() {
		return new EventRewardGrant(1_000, 1, 0, 0, 0, false, false, false);
	}

	public static EventRewardGrant vipDaily() {
		return new EventRewardGrant(0, 1, 3_600, 3, 1, false, true, true);
	}
}
