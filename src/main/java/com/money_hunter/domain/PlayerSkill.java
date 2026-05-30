package com.money_hunter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_skills")
public class PlayerSkill {
	public static final int MAX_LEVEL = 30;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private SkillType type;

	@Column(nullable = false)
	private int level = 0;

	protected PlayerSkill() {
	}

	public PlayerSkill(Player player, SkillType type) {
		this.player = player;
		this.type = type;
	}

	public Long getId() {
		return id;
	}

	public SkillType getType() {
		return type;
	}

	public int getLevel() {
		return level;
	}

	public boolean isMaxLevel() {
		return level >= MAX_LEVEL;
	}

	public void levelUp() {
		if (isMaxLevel()) {
			throw new IllegalStateException("Skill is already at max level.");
		}
		this.level += 1;
	}

	public void setLevel(int level) {
		if (level < 0 || level > MAX_LEVEL) {
			throw new IllegalArgumentException("Skill level must be between 0 and " + MAX_LEVEL + ".");
		}
		this.level = level;
	}

	public void resetLevel() {
		this.level = 0;
	}
}
