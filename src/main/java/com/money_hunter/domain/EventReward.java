package com.money_hunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_rewards")
public class EventReward {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Column(nullable = false, length = 160)
	private String rewardKey;

	@Column(nullable = false, length = 80)
	private String sourceEventKey;

	@Column(nullable = false, length = 80)
	private String sourceEventName;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(nullable = false, length = 240)
	private String description;

	@Column(nullable = false, length = 120)
	private String rewardLabel;

	@Column(nullable = false)
	private long goldAmount = 0;

	@Column(nullable = false)
	private int skillPointAmount = 0;

	@Column(nullable = false)
	private long autoHuntSeconds = 0;

	@Column(nullable = false)
	private int dungeonCouponAmount = 0;

	@Column(nullable = false)
	private int bossRaidTicketAmount = 0;

	@Column(nullable = false)
	private int dailyMissionSkipTicketAmount = 0;

	@Column(nullable = false)
	private boolean rookieEventPetReward = false;

	@Column(nullable = false)
	private boolean vipBadgeReward = false;

	@Column(nullable = false)
	private boolean petSkinUnlockReward = false;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant claimedAt;

	protected EventReward() {
	}

	public EventReward(
			Player player,
			String rewardKey,
			String sourceEventKey,
			String sourceEventName,
			String title,
			String description,
			String rewardLabel,
			EventRewardGrant grant,
			Instant createdAt,
			Instant expiresAt
	) {
		this.player = player;
		this.rewardKey = rewardKey;
		this.sourceEventKey = sourceEventKey;
		this.sourceEventName = sourceEventName;
		this.title = title;
		this.description = description;
		this.rewardLabel = rewardLabel;
		this.goldAmount = grant.goldAmount();
		this.skillPointAmount = grant.skillPointAmount();
		this.autoHuntSeconds = grant.autoHuntSeconds();
		this.dungeonCouponAmount = grant.dungeonCouponAmount();
		this.bossRaidTicketAmount = grant.bossRaidTicketAmount();
		this.dailyMissionSkipTicketAmount = grant.dailyMissionSkipTicketAmount();
		this.rookieEventPetReward = grant.rookieEventPetReward();
		this.vipBadgeReward = grant.vipBadgeReward();
		this.petSkinUnlockReward = grant.petSkinUnlockReward();
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public Long getId() {
		return id;
	}

	public Player getPlayer() {
		return player;
	}

	public String getRewardKey() {
		return rewardKey;
	}

	public String getSourceEventKey() {
		return sourceEventKey;
	}

	public String getSourceEventName() {
		return sourceEventName;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getRewardLabel() {
		return rewardLabel;
	}

	public long getGoldAmount() {
		return goldAmount;
	}

	public int getSkillPointAmount() {
		return skillPointAmount;
	}

	public long getAutoHuntSeconds() {
		return autoHuntSeconds;
	}

	public int getDungeonCouponAmount() {
		return dungeonCouponAmount;
	}

	public int getBossRaidTicketAmount() {
		return bossRaidTicketAmount;
	}

	public int getDailyMissionSkipTicketAmount() {
		return dailyMissionSkipTicketAmount;
	}

	public boolean isRookieEventPetReward() {
		return rookieEventPetReward;
	}

	public boolean isVipBadgeReward() {
		return vipBadgeReward;
	}

	public boolean isPetSkinUnlockReward() {
		return petSkinUnlockReward;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getClaimedAt() {
		return claimedAt;
	}

	public boolean isClaimed() {
		return claimedAt != null;
	}

	public boolean isExpired(Instant now) {
		return expiresAt != null && !expiresAt.isAfter(now);
	}

	public void markClaimed(Instant claimedAt) {
		if (this.claimedAt == null) {
			this.claimedAt = claimedAt;
		}
	}
}
