package com.money_hunter.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.money_hunter.domain.AdEvent;
import com.money_hunter.domain.AdEventType;
import com.money_hunter.domain.AdRewardSession;
import com.money_hunter.domain.IapOrder;
import com.money_hunter.domain.JobType;
import com.money_hunter.domain.NotificationEvent;
import com.money_hunter.domain.NotificationType;
import com.money_hunter.domain.Player;
import com.money_hunter.domain.PlayerSkill;
import com.money_hunter.domain.RewardClaim;
import com.money_hunter.domain.SkillType;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.AdRewardSessionRepository;
import com.money_hunter.infrastructure.persistence.IapOrderRepository;
import com.money_hunter.infrastructure.persistence.NotificationEventRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import com.money_hunter.infrastructure.persistence.RewardClaimRepository;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.config.IapProperties;
import com.money_hunter.infrastructure.config.SmartMessageProperties;
import com.money_hunter.application.dto.response.AdRewardSessionResponse;
import com.money_hunter.application.dto.response.NotificationResponse;
import com.money_hunter.application.dto.response.PlayerStateResponse;
import com.money_hunter.application.dto.response.MonsterResponse;
import com.money_hunter.application.dto.response.RewardClaimResponse;
import com.money_hunter.application.dto.response.SkillResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerService {
	private static final long GOLD_MICROS = 1_000_000L;
	private static final long HOUR_MILLIS = 3_600_000L;
	private static final double BOOST_REWARD_MULTIPLIER = 1.5;
	private static final double MAX_PREBOOST_PAYOUT_RATE = 1.0 / BOOST_REWARD_MULTIPLIER;
	private static final double BASE_PAYOUT_RATE = 0.20;
	private static final double MAX_SKILL_PAYOUT_RATE = 0.5333333333;
	private static final double PET_UNLOCK_PAYOUT_RATE = 0.04;
	private static final double PET_SKILL_PAYOUT_RATE = (MAX_PREBOOST_PAYOUT_RATE
			- MAX_SKILL_PAYOUT_RATE
			- PET_UNLOCK_PAYOUT_RATE * 2) / 2;
	private static final int HIT_REWARD_PERCENT = 75;
	private static final int BASE_ATTACK_INTERVAL_MILLIS = 1_500;
	private static final int BOOSTED_ATTACK_INTERVAL_MILLIS = 750;
	private static final int MIN_ATTACK_INTERVAL_MILLIS = 750;
	private static final int RAPID_ATTACK_INTERVAL_REDUCTION_MILLIS = 45;
	private static final Duration AD_SESSION_TTL = Duration.ofMinutes(10);
	private static final String[] MONSTER_KEYS = {"BOSS_ROCK", "BOSS_FROST", "BOSS_TREANT"};
	private static final Map<String, Integer> MONSTER_BASE_HP = Map.of(
			"BOSS_ROCK", 120,
			"BOSS_FROST", 135,
			"BOSS_TREANT", 150
	);
	private static final Set<SkillType> STAT_SKILLS = EnumSet.of(
			SkillType.STRENGTH,
			SkillType.DEXTERITY,
			SkillType.INTELLIGENCE,
			SkillType.LUCK
	);
	private static final Set<SkillType> SP_SPENDABLE_SKILLS = EnumSet.of(
			SkillType.STRENGTH,
			SkillType.DEXTERITY,
			SkillType.INTELLIGENCE,
			SkillType.LUCK,
			SkillType.MINING_MASTERY,
			SkillType.RAPID_ATTACK,
			SkillType.REWARD_AMPLIFIER,
			SkillType.PET_FLARE_ATTACK,
			SkillType.PET_AQUA_ATTACK
	);
	private static final Set<AdEventType> REWARD_AD_TYPES = EnumSet.of(
			AdEventType.AUTO_HUNT,
			AdEventType.BOOST,
			AdEventType.SKILL_POINT,
			AdEventType.REWARD_CLAIM
	);

	private final PlayerRepository playerRepository;
	private final AdEventRepository adEventRepository;
	private final AdRewardSessionRepository adRewardSessionRepository;
	private final NotificationEventRepository notificationEventRepository;
	private final RewardClaimRepository rewardClaimRepository;
	private final IapOrderRepository iapOrderRepository;
	private final RuntimeEconomyService economy;
	private final AppProperties appProperties;
	private final IapProperties iapProperties;
	private final SmartMessageProperties smartMessageProperties;
	private final TossIapClient tossIapClient;
	private final TossSmartMessageClient tossSmartMessageClient;
	private final Clock clock;

	public PlayerService(
			PlayerRepository playerRepository,
			AdEventRepository adEventRepository,
			AdRewardSessionRepository adRewardSessionRepository,
			NotificationEventRepository notificationEventRepository,
			RewardClaimRepository rewardClaimRepository,
			IapOrderRepository iapOrderRepository,
			RuntimeEconomyService economy,
			AppProperties appProperties,
			IapProperties iapProperties,
			SmartMessageProperties smartMessageProperties,
			TossIapClient tossIapClient,
			TossSmartMessageClient tossSmartMessageClient
	) {
		this.playerRepository = playerRepository;
		this.adEventRepository = adEventRepository;
		this.adRewardSessionRepository = adRewardSessionRepository;
		this.notificationEventRepository = notificationEventRepository;
		this.rewardClaimRepository = rewardClaimRepository;
		this.iapOrderRepository = iapOrderRepository;
		this.economy = economy;
		this.appProperties = appProperties;
		this.iapProperties = iapProperties;
		this.smartMessageProperties = smartMessageProperties;
		this.tossIapClient = tossIapClient;
		this.tossSmartMessageClient = tossSmartMessageClient;
		this.clock = Clock.systemUTC();
	}

	@Transactional
	public PlayerStateResponse getState(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		settle(player);
		publishAutoHuntEndNotificationIfDue(player, clock.instant());
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse chooseJob(String userKey, JobType job) {
		Player player = getOrCreatePlayer(userKey);
		boolean firstJob = !player.hasChosenJob();
		player.chooseJob(job);
		for (SkillType type : SkillType.values()) {
			player.getOrCreateSkill(type);
		}
		if (firstJob) {
			claimTutorialStarterReward(player, clock.instant());
		}
		return toState(player);
	}

	@Transactional
	public AdRewardSessionResponse startRewardAdSession(String userKey, AdEventType type) {
		requireRewardAdType(type);
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		requireRewardAvailable(player, type);
		Instant now = clock.instant();
		AdRewardSession session = adRewardSessionRepository.save(new AdRewardSession(
				player,
				type,
				UUID.randomUUID().toString(),
				now,
				now.plus(AD_SESSION_TTL)));
		return new AdRewardSessionResponse(type, session.getSessionToken(), now.plus(AD_SESSION_TTL));
	}

	@Transactional
	public PlayerStateResponse completeAutoHuntAd(String userKey) {
		return completeAutoHuntAd(userKey, null, false);
	}

	@Transactional
	public PlayerStateResponse completeAutoHuntAd(String userKey, String adSessionToken) {
		return completeAutoHuntAd(userKey, adSessionToken, true);
	}

	private PlayerStateResponse completeAutoHuntAd(String userKey, String adSessionToken, boolean requireAdSession) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		Instant now = clock.instant();
		completeRewardAdSessionIfRequired(player, AdEventType.AUTO_HUNT, adSessionToken, requireAdSession, now);
		player.setAutoHuntEndsAt(addCappedTime(player.getAutoHuntEndsAt(), economy.autoHuntAdSeconds(), now));
		player.clearAutoHuntEndNotification();
		clearUnreadAutoHuntEndNotifications(player, now);
		adEventRepository.save(new AdEvent(player, AdEventType.AUTO_HUNT, (int) economy.autoHuntAdSeconds(), now));
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse completeBoostAd(String userKey) {
		return completeBoostAd(userKey, null, false);
	}

	@Transactional
	public PlayerStateResponse completeBoostAd(String userKey, String adSessionToken) {
		return completeBoostAd(userKey, adSessionToken, true);
	}

	private PlayerStateResponse completeBoostAd(String userKey, String adSessionToken, boolean requireAdSession) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		Instant now = clock.instant();
		completeRewardAdSessionIfRequired(player, AdEventType.BOOST, adSessionToken, requireAdSession, now);
		player.setBoostEndsAt(addCappedTime(player.getBoostEndsAt(), economy.boostAdSeconds(), now));
		adEventRepository.save(new AdEvent(player, AdEventType.BOOST, (int) economy.boostAdSeconds(), now));
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse completeSkillPointAd(String userKey) {
		return completeSkillPointAd(userKey, null, false);
	}

	@Transactional
	public PlayerStateResponse completeSkillPointAd(String userKey, String adSessionToken) {
		return completeSkillPointAd(userKey, adSessionToken, true);
	}

	private PlayerStateResponse completeSkillPointAd(String userKey, String adSessionToken, boolean requireAdSession) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		Instant now = clock.instant();
		requireSkillPointRewardAvailable(player);
		completeRewardAdSessionIfRequired(player, AdEventType.SKILL_POINT, adSessionToken, requireAdSession, now);
		player.addSkillPoint();
		adEventRepository.save(new AdEvent(player, AdEventType.SKILL_POINT, 1, now));
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse hitMonster(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		publishAutoHuntEndNotificationIfDue(player, clock.instant());
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse completeFeatureTutorial(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		player.completeFeatureTutorial(clock.instant());
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse upgradeSkill(String userKey, SkillType type) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		requirePetUnlocked(player, type);
		if (isStatSkill(type)) {
			return upgradeSharedStatSkill(player);
		}
		PlayerSkill skill = player.getOrCreateSkill(type);
		if (skill.isMaxLevel()) {
			throw new IllegalStateException("Skill is already at max level.");
		}
		player.spendSkillPoint();
		skill.levelUp();
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse purchaseCompanion(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		player.purchaseCharacterSlot(economy.maxCharacterSlots());
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse purchaseSkillPointPack(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		requireSkillPointRewardAvailable(player);
		player.addSkillPoints(economy.skillPointPackAmount());
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse claimFriendInviteReward(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		player.claimFriendInviteReward(economy.friendInviteLimit(), economy.friendInviteRewardSkillPoints());
		adEventRepository.save(new AdEvent(
				player,
				AdEventType.FRIEND_INVITE_REWARD,
				economy.friendInviteRewardSkillPoints(),
				clock.instant()));
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse grantIapProduct(String userKey, String orderId, String productId) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		Instant now = clock.instant();
		String normalizedProductId = normalize(productId);
		String normalizedOrderId = normalize(orderId);
		String productType = iapProperties.productType(normalizedProductId);
		if (productType.isBlank()) {
			throw new IllegalArgumentException("지원하지 않는 인앱 상품이에요.");
		}

		IapOrder order = iapOrderRepository.findByOrderId(normalizedOrderId)
				.orElseGet(() -> {
					verifyIapOrderIfRequired(userKey, normalizedOrderId, normalizedProductId);
					return iapOrderRepository.save(new IapOrder(
							normalizedOrderId,
							userKey,
							normalizedProductId,
							productType,
							now));
				});
		if (!order.getUserKey().equals(userKey) || !order.getProductId().equals(normalizedProductId)) {
			throw new IllegalStateException("인앱 결제 주문 정보가 일치하지 않아요.");
		}
		if (order.isGranted()) {
			return toState(player);
		}

		switch (productType) {
			case "FLARE_PET", "AQUA_PET" -> player.purchaseCharacterSlot(economy.maxCharacterSlots());
			case "SKILL_POINT_PACK" -> {
				requireSkillPointRewardAvailable(player);
				player.addSkillPoints(economy.skillPointPackAmount());
			}
			default -> throw new IllegalArgumentException("지원하지 않는 인앱 상품이에요.");
		}
		order.markGranted(now);
		return toState(player);
	}

	@Transactional
	public RewardClaimResponse claimRewardAfterAd(String userKey, String idempotencyKey) {
		return claimRewardAfterAd(userKey, idempotencyKey, null, false);
	}

	@Transactional
	public RewardClaimResponse claimRewardAfterAd(String userKey, String idempotencyKey, String adSessionToken) {
		return claimRewardAfterAd(userKey, idempotencyKey, adSessionToken, true);
	}

	private RewardClaimResponse claimRewardAfterAd(
			String userKey,
			String idempotencyKey,
			String adSessionToken,
			boolean requireAdSession
	) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		completeRewardAdSessionIfRequired(player, AdEventType.REWARD_CLAIM, adSessionToken, requireAdSession, clock.instant());
		RewardClaim claim = rewardClaimRepository.findByPlayerAndIdempotencyKey(player, idempotencyKey)
				.orElseGet(() -> createRewardClaim(player, idempotencyKey));
		adEventRepository.save(new AdEvent(player, AdEventType.REWARD_CLAIM, claim.getPointAmount(), clock.instant()));
		return new RewardClaimResponse(
				claim.getId(),
				claim.getPointAmount(),
				claim.getStatus(),
				claim.getIdempotencyKey(),
				toState(player));
	}

	@Transactional
	public PlayerStateResponse claimOneStoreGameReward(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		if (player.getGold() < economy.rewardGoldThreshold()) {
			throw new IllegalStateException("Reward gauge is not full.");
		}
		player.spendGold(economy.rewardGoldThreshold());
		player.addSkillPoints(economy.friendInviteRewardSkillPoints());
		adEventRepository.save(new AdEvent(
				player,
				AdEventType.REWARD_CLAIM,
				economy.friendInviteRewardSkillPoints(),
				clock.instant()));
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse fillRewardGaugeForTest(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		settle(player);
		long missingGold = Math.max(0, economy.rewardGoldThreshold() - player.getGold());
		if (missingGold > 0) {
			player.addGold(missingGold);
		}
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse resetForTest(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		Instant now = clock.instant();
		player.resetForTest(now);
		notificationEventRepository.findByPlayerAndReadAtIsNull(player)
				.forEach(notification -> notification.markRead(now));
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse endAutoHuntForTest(String userKey) {
		Player player = getOrCreatePlayer(userKey);
		requireOnboarded(player);
		Instant now = clock.instant();
		player.setAutoHuntEndsAt(now.minusMillis(1));
		player.clearAutoHuntEndNotification();
		settle(player);
		publishAutoHuntEndNotificationIfDue(player, now);
		return toState(player);
	}

	@Transactional
	public PlayerStateResponse markNotificationRead(String userKey, Long notificationId) {
		NotificationEvent notification = notificationEventRepository.findByIdAndPlayerUserKey(notificationId, userKey)
				.orElseThrow(() -> new IllegalArgumentException("Notification not found."));
		notification.markRead(clock.instant());
		Player player = getOrCreatePlayer(userKey);
		settle(player);
		return toState(player);
	}

	@Transactional
	public int publishAutoHuntEndNotifications() {
		Instant now = clock.instant();
		List<Player> players = playerRepository.findAutoHuntEndedNotificationTargets(now);
		players.forEach(player -> {
			settle(player);
			publishAutoHuntEndNotificationIfDue(player, now);
		});
		return players.size();
	}

	private RewardClaim createRewardClaim(Player player, String idempotencyKey) {
		if (player.getGold() < economy.rewardGoldThreshold()) {
			throw new IllegalStateException("Reward gauge is not full.");
		}
		int claimPointAmount = availableRewardPointAmount(player);
		long claimGoldAmount = rewardGoldAmount(claimPointAmount);
		player.spendGold(claimGoldAmount);
		return rewardClaimRepository.save(new RewardClaim(
				player,
				claimGoldAmount,
				claimPointAmount,
				idempotencyKey,
				clock.instant()));
	}

	private int availableRewardPointAmount(Player player) {
		return (int) Math.min(Integer.MAX_VALUE, player.getGold() / economy.goldPerTossPoint());
	}

	private long rewardGoldAmount(int pointAmount) {
		return (long) pointAmount * economy.goldPerTossPoint();
	}

	private Player getOrCreatePlayer(String userKey) {
		Player player = playerRepository.findByUserKey(userKey)
				.orElseGet(() -> {
					playerRepository.insertIfAbsent(userKey, clock.instant());
					return playerRepository.findByUserKey(userKey).orElseThrow();
				});
		requireActivePlayer(player);
		return player;
	}

	private void verifyIapOrderIfRequired(String userKey, String orderId, String productId) {
		if (appProperties.mockMonetizationEnabled() || !appProperties.realIapOrderVerificationEnabled()) {
			return;
		}
		TossIapOrderStatus status = tossIapClient.getOrderStatus(userKey, orderId);
		if (!orderId.equals(status.orderId()) || !productId.equals(status.productId())) {
			throw new IllegalStateException("토스 인앱결제 주문과 상품 정보가 일치하지 않아요.");
		}
		if (!status.isPaymentCompletedForGrant()) {
			throw new IllegalStateException("토스 인앱결제가 완료된 주문이 아니에요.");
		}
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private void requireActivePlayer(Player player) {
		if (player.isSuspended()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 유저예요. 관리자에게 문의해 주세요.");
		}
	}

	private void settle(Player player) {
		Instant now = clock.instant();
		Instant huntEnd = player.getAutoHuntEndsAt();
		if (huntEnd == null || !huntEnd.isAfter(player.getLastSettledAt())) {
			player.setLastSettledAt(now);
			return;
		}

		Instant settlementEnd = min(now, huntEnd);
		if (!settlementEnd.isAfter(player.getLastSettledAt())) {
			return;
		}

		resolveAutoCombat(player, settlementEnd);
		if (!huntEnd.isAfter(now) && player.getLastSettledAt().isBefore(settlementEnd)) {
			player.setLastSettledAt(settlementEnd);
		}
	}

	private void resolveAutoCombat(Player player, Instant settlementEnd) {
		Instant settlementStart = player.getLastSettledAt();
		long millis = Duration.between(settlementStart, settlementEnd).toMillis();
		long boostedMillis = boostedMillis(player, settlementStart, settlementEnd);
		long normalMillis = millis - boostedMillis;
		AttackCounts attackCounts = attackCounts(player, normalMillis, boostedMillis);
		long totalAttacks = attackCounts.total();
		if (totalAttacks <= 0) {
			return;
		}

		long rewardMicros = combatRewardMicros(player, normalMillis, boostedMillis);
		player.reserveDefeatGold(defeatRewardMicros(rewardMicros));
		player.collectHitGold(hitRewardMicros(rewardMicros));
		resolveAggregateDamage(player, attackCounts);
		player.setLastSettledAt(settlementEnd);
	}

	private void resolveAggregateDamage(Player player, AttackCounts attackCounts) {
		long totalDamage = attackCounts.normal() * damage(player, false)
				+ attackCounts.boosted() * damage(player, true);
		while (totalDamage > 0) {
			int currentHp = player.getCurrentMonsterHp();
			if (totalDamage < currentHp) {
				player.damageMonster((int) totalDamage);
				return;
			}
			totalDamage -= currentHp;
			player.damageMonster(currentHp);
			player.collectDefeatGold();
			player.recordMonsterDefeat(defeatExperience(player));
			String nextMonsterKey = nextMonsterKey(player.getCurrentMonsterKey());
			player.replaceMonster(nextMonsterKey, maxMonsterHp(player.getDefeatedMonsters(), nextMonsterKey));
		}
	}

	private int damage(Player player, boolean boosted) {
		int rapidLevel = player.getOrCreateSkill(SkillType.RAPID_ATTACK).getLevel();
		int statLevel = currentJobStatLevel(player);
		int damage = 16 + player.getLevel() * 2 + rapidLevel * 2 + statLevel * 3 + petDamage(player);
		if (boosted) {
			damage = Math.round(damage * 1.35f);
		}
		return damage;
	}

	private long combatRewardMicros(Player player, long normalMillis, long boostedMillis) {
		long normalGoldPerHour = baseGoldPerHour(player);
		long boostedGoldPerHour = boostedGoldPerHour(player);
		long elapsedMillis = normalMillis + boostedMillis;
		long rewardMicros = ((normalGoldPerHour * normalMillis)
				+ (boostedGoldPerHour * boostedMillis)) * GOLD_MICROS / HOUR_MILLIS;
		return Math.min(rewardMicros, maxRewardMicros(elapsedMillis));
	}

	private long maxRewardMicros(long elapsedMillis) {
		return maxGoldPerHour() * elapsedMillis * GOLD_MICROS / HOUR_MILLIS;
	}

	private long hitRewardMicros(long rewardMicros) {
		return rewardMicros * HIT_REWARD_PERCENT / 100;
	}

	private long defeatRewardMicros(long rewardMicros) {
		return rewardMicros - hitRewardMicros(rewardMicros);
	}

	private long boostedMillis(Player player, Instant from, Instant to) {
		Instant boostEnd = player.getBoostEndsAt();
		if (boostEnd == null || !boostEnd.isAfter(from)) {
			return 0;
		}
		Instant boostedTo = min(to, boostEnd);
		return Math.max(0, Duration.between(from, boostedTo).toMillis());
	}

	private void claimTutorialStarterReward(Player player, Instant now) {
		if (player.hasClaimedTutorialReward()) {
			return;
		}
		player.setAutoHuntEndsAt(addCappedTime(player.getAutoHuntEndsAt(), economy.autoHuntAdSeconds(), now));
		player.setBoostEndsAt(addCappedTime(player.getBoostEndsAt(), economy.boostAdSeconds(), now));
		player.clearAutoHuntEndNotification();
		clearUnreadAutoHuntEndNotifications(player, now);
		player.claimTutorialReward(now);
	}

	private void requireRewardAdType(AdEventType type) {
		if (!REWARD_AD_TYPES.contains(type)) {
			throw new IllegalArgumentException("지원하지 않는 광고 보상 타입이에요.");
		}
	}

	private void requireRewardAvailable(Player player, AdEventType type) {
		if (type == AdEventType.SKILL_POINT) {
			requireSkillPointRewardAvailable(player);
		}
		if (type == AdEventType.REWARD_CLAIM && player.getGold() < economy.rewardGoldThreshold()) {
			throw new IllegalStateException("Reward gauge is not full.");
		}
	}

	private void requireSkillPointRewardAvailable(Player player) {
		if (allSkillsMaxed(player)) {
			throw new IllegalStateException("모든 스킬 강화가 완료되어 SP를 더 받을 수 없어요.");
		}
	}

	private boolean allSkillsMaxed(Player player) {
		return SP_SPENDABLE_SKILLS.stream()
				.map(player::getOrCreateSkill)
				.allMatch(PlayerSkill::isMaxLevel);
	}

	private void completeRewardAdSessionIfRequired(
			Player player,
			AdEventType type,
			String adSessionToken,
			boolean requireAdSession,
			Instant now
	) {
		if (!requireAdSession) {
			return;
		}
		if (adSessionToken == null || adSessionToken.isBlank()) {
			throw new IllegalArgumentException("광고 세션이 필요해요.");
		}
		AdRewardSession session = adRewardSessionRepository.findBySessionToken(adSessionToken.trim())
				.orElseThrow(() -> new IllegalStateException("광고 세션을 찾을 수 없어요."));
		if (!session.getPlayer().getId().equals(player.getId()) || session.getType() != type) {
			throw new IllegalStateException("광고 세션 정보가 일치하지 않아요.");
		}
		if (session.isCompleted()) {
			throw new IllegalStateException("이미 사용한 광고 세션이에요.");
		}
		if (session.isExpired(now)) {
			throw new IllegalStateException("광고 세션이 만료됐어요. 다시 시청해 주세요.");
		}
		session.complete(now);
	}

	private Instant addCappedTime(Instant currentEnd, long secondsToAdd, Instant now) {
		Instant base = currentEnd != null && currentEnd.isAfter(now) ? currentEnd : now;
		Instant cappedEnd = now.plusSeconds(economy.maxAdSeconds());
		Instant requestedEnd = base.plusSeconds(secondsToAdd);
		return min(requestedEnd, cappedEnd);
	}

	private PlayerStateResponse toState(Player player) {
		ensureMonster(player);
		syncStatSkills(player);
		List<SkillResponse> skills = Arrays.stream(SkillType.values())
				.map(player::getOrCreateSkill)
				.map(skill -> new SkillResponse(skill.getType(), skill.getLevel(), effectTier(skill)))
				.toList();
		long threshold = economy.rewardGoldThreshold();
		return new PlayerStateResponse(
				player.getUserKey(),
				player.getJob(),
				!player.hasChosenJob(),
				player.hasClaimedTutorialReward(),
				player.hasChosenJob() && !player.hasCompletedFeatureTutorial(),
				player.getCharacterSlots(),
				economy.maxCharacterSlots(),
				economy.companionPriceWon(),
				economy.skillPointPackPriceWon(),
				economy.skillPointPackAmount(),
				player.getGold(),
				threshold,
				economy.rewardPointAmount(),
				economy.adRevenuePerRewardAdWon(),
				economy.goldPerTossPoint(),
				payoutRatePercent(player),
				(int) Math.min(100, player.getGold() * 100 / threshold),
				player.getGold() >= threshold,
				player.getSkillPoints(),
				!allSkillsMaxed(player),
				player.getFriendInviteRewardCount(),
				economy.friendInviteLimit(),
				economy.friendInviteRewardSkillPoints(),
				player.getLevel(),
				player.getExperience(),
				player.getNextLevelExperience(),
				currentGoldPerHour(player),
				attackIntervalMillis(player),
				player.getAutoHuntEndsAt(),
				player.getBoostEndsAt(),
				monsterResponse(player),
				skills,
				latestNotification(player));
	}

	private NotificationResponse latestNotification(Player player) {
		return notificationEventRepository.findTopByPlayerAndReadAtIsNullOrderByCreatedAtDesc(player)
				.map(notification -> new NotificationResponse(
						notification.getId(),
						notification.getType().name(),
						notification.getTitle(),
						notification.getBody(),
						notification.getSentAt()))
				.orElse(null);
	}

	private void publishAutoHuntEndNotificationIfDue(Player player, Instant now) {
		Instant autoHuntEndsAt = player.getAutoHuntEndsAt();
		if (autoHuntEndsAt == null || autoHuntEndsAt.isAfter(now) || player.getAutoHuntEndNotifiedAt() != null) {
			return;
		}
		notificationEventRepository.save(new NotificationEvent(
				player,
				NotificationType.AUTO_HUNT_ENDED,
				"자동사냥이 종료됐어요",
				"광고를 보고 자동사냥 시간을 다시 충전할 수 있어요.",
				now));
		sendAutoHuntEndedSmartMessage(player);
		player.markAutoHuntEndNotified(now);
	}

	private void sendAutoHuntEndedSmartMessage(Player player) {
		String templateSetCode = smartMessageProperties.normalizedAutoHuntEndedTemplateSetCode();
		if (!appProperties.realSmartMessageEnabled() || templateSetCode.isBlank()) {
			return;
		}
		try {
			tossSmartMessageClient.sendMessage(
					player.getUserKey(),
					templateSetCode,
					smartMessageProperties.autoHuntEndedContext());
		} catch (RuntimeException ignored) {
			// The in-app notification is still persisted; the scheduler will not spam retries for this hunt session.
		}
	}

	private void clearUnreadAutoHuntEndNotifications(Player player, Instant now) {
		notificationEventRepository.findByPlayerAndTypeAndReadAtIsNull(player, NotificationType.AUTO_HUNT_ENDED)
				.forEach(notification -> notification.markRead(now));
	}

	private MonsterResponse monsterResponse(Player player) {
		return new MonsterResponse(
				player.getCurrentMonsterKey(),
				player.getCurrentMonsterHp(),
				maxMonsterHp(player.getDefeatedMonsters(), player.getCurrentMonsterKey()),
				defeatGold(player),
				player.getDefeatedMonsters());
	}

	private long currentGoldPerHour(Player player) {
		return isActive(player.getBoostEndsAt()) ? boostedGoldPerHour(player) : baseGoldPerHour(player);
	}

	private long baseGoldPerHour(Player player) {
		return Math.round(maxGoldPerHour() * basePayoutRate(player));
	}

	private long boostedGoldPerHour(Player player) {
		return Math.min(maxGoldPerHour(), Math.round(baseGoldPerHour(player) * BOOST_REWARD_MULTIPLIER));
	}

	private int attackIntervalMillis(Player player) {
		return isActive(player.getBoostEndsAt()) ? boostedAttackIntervalMillis(player) : normalAttackIntervalMillis(player);
	}

	private int boostedAttackIntervalMillis(Player player) {
		return attackIntervalMillis(player, BOOSTED_ATTACK_INTERVAL_MILLIS);
	}

	private int normalAttackIntervalMillis(Player player) {
		return attackIntervalMillis(player, BASE_ATTACK_INTERVAL_MILLIS);
	}

	private int attackIntervalMillis(Player player, int baseIntervalMillis) {
		int rapidLevel = player.getOrCreateSkill(SkillType.RAPID_ATTACK).getLevel();
		return Math.max(
				MIN_ATTACK_INTERVAL_MILLIS,
				baseIntervalMillis - rapidLevel * RAPID_ATTACK_INTERVAL_REDUCTION_MILLIS);
	}

	private AttackCounts attackCounts(Player player, long normalMillis, long boostedMillis) {
		return new AttackCounts(
				normalMillis / normalAttackIntervalMillis(player),
				boostedMillis / boostedAttackIntervalMillis(player));
	}

	private long maxGoldPerHour() {
		return (long) economy.adRevenuePerRewardAdWon() * economy.goldPerTossPoint();
	}

	private int payoutRatePercent(Player player) {
		if (maxGoldPerHour() <= 0) {
			return 0;
		}
		return (int) Math.round(currentGoldPerHour(player) * 100.0 / maxGoldPerHour());
	}

	private double basePayoutRate(Player player) {
		double skillProgress = skillProgress(player);
		double skillRate = BASE_PAYOUT_RATE + (MAX_SKILL_PAYOUT_RATE - BASE_PAYOUT_RATE) * skillProgress;
		return Math.min(MAX_PREBOOST_PAYOUT_RATE, skillRate + petPayoutRate(player));
	}

	private double skillProgress(Player player) {
		int masteryLevel = player.getOrCreateSkill(SkillType.MINING_MASTERY).getLevel();
		int rapidLevel = player.getOrCreateSkill(SkillType.RAPID_ATTACK).getLevel();
		int rewardLevel = player.getOrCreateSkill(SkillType.REWARD_AMPLIFIER).getLevel();
		int statLevel = currentJobStatLevel(player);
		int totalLevel = masteryLevel + rapidLevel + rewardLevel + statLevel;
		return Math.min(1.0, totalLevel / (PlayerSkill.MAX_LEVEL * 4.0));
	}

	private double petPayoutRate(Player player) {
		int pets = unlockedPetCount(player);
		double unlockRate = pets * PET_UNLOCK_PAYOUT_RATE;
		double skillRate = petSkillProgress(player, SkillType.PET_FLARE_ATTACK, 1) * PET_SKILL_PAYOUT_RATE
				+ petSkillProgress(player, SkillType.PET_AQUA_ATTACK, 2) * PET_SKILL_PAYOUT_RATE;
		return unlockRate + skillRate;
	}

	private double petSkillProgress(Player player, SkillType type, int requiredPetCount) {
		if (unlockedPetCount(player) < requiredPetCount) {
			return 0;
		}
		return player.getOrCreateSkill(type).getLevel() / (double) PlayerSkill.MAX_LEVEL;
	}

	private int petDamage(Player player) {
		int pets = unlockedPetCount(player);
		int damage = pets * 4;
		if (pets >= 1) {
			damage += player.getOrCreateSkill(SkillType.PET_FLARE_ATTACK).getLevel() * 2;
		}
		if (pets >= 2) {
			damage += player.getOrCreateSkill(SkillType.PET_AQUA_ATTACK).getLevel() * 2;
		}
		return damage;
	}

	private int unlockedPetCount(Player player) {
		return Math.max(0, player.getCharacterSlots() - 1);
	}

	private void requirePetUnlocked(Player player, SkillType type) {
		if (type == SkillType.PET_FLARE_ATTACK && unlockedPetCount(player) < 1) {
			throw new IllegalStateException("첫 번째 동료 펫을 먼저 잠금 해제해야 해요.");
		}
		if (type == SkillType.PET_AQUA_ATTACK && unlockedPetCount(player) < 2) {
			throw new IllegalStateException("두 번째 동료 펫을 먼저 잠금 해제해야 해요.");
		}
	}

	private int effectTier(PlayerSkill skill) {
		if (skill.getLevel() <= 0) {
			return 0;
		}
		return Math.min(4, ((skill.getLevel() - 1) / 5) + 1);
	}

	private int currentJobStatLevel(Player player) {
		if (player.getJob() == null) {
			return 0;
		}
		return sharedStatLevel(player);
	}

	private SkillType statSkillFor(JobType job) {
		if (job == null) {
			return null;
		}
		return switch (job) {
			case WARRIOR -> SkillType.STRENGTH;
			case ARCHER -> SkillType.DEXTERITY;
			case MAGE -> SkillType.INTELLIGENCE;
			case ROGUE -> SkillType.LUCK;
		};
	}

	private PlayerStateResponse upgradeSharedStatSkill(Player player) {
		int level = sharedStatLevel(player);
		if (level >= PlayerSkill.MAX_LEVEL) {
			throw new IllegalStateException("Skill is already at max level.");
		}
		player.spendSkillPoint();
		setSharedStatLevel(player, level + 1);
		return toState(player);
	}

	private boolean isStatSkill(SkillType type) {
		return STAT_SKILLS.contains(type);
	}

	private int sharedStatLevel(Player player) {
		return STAT_SKILLS.stream()
				.map(player::getOrCreateSkill)
				.mapToInt(PlayerSkill::getLevel)
				.max()
				.orElse(0);
	}

	private void syncStatSkills(Player player) {
		setSharedStatLevel(player, sharedStatLevel(player));
	}

	private void setSharedStatLevel(Player player, int level) {
		STAT_SKILLS.forEach(type -> player.getOrCreateSkill(type).setLevel(level));
	}

	private void ensureMonster(Player player) {
		if (player.getCurrentMonsterHp() <= 0) {
			player.replaceMonster(player.getCurrentMonsterKey(), maxMonsterHp(player.getDefeatedMonsters(), player.getCurrentMonsterKey()));
		}
	}

	private int maxMonsterHp(int defeatedMonsters, String key) {
		return MONSTER_BASE_HP.getOrDefault(key, 120) + defeatedMonsters * 18;
	}

	private long defeatGold(Player player) {
		return Math.max(1, currentGoldPerHour(player) * (100 - HIT_REWARD_PERCENT) / 100 / 60);
	}

	private long defeatExperience(Player player) {
		return 8L + player.getDefeatedMonsters() / 2L;
	}

	private String nextMonsterKey(String currentKey) {
		if (MONSTER_KEYS.length == 1) {
			return MONSTER_KEYS[0];
		}
		String next = currentKey;
		while (next.equals(currentKey)) {
			next = MONSTER_KEYS[ThreadLocalRandom.current().nextInt(MONSTER_KEYS.length)];
		}
		return next;
	}

	private void requireOnboarded(Player player) {
		if (!player.hasChosenJob()) {
			throw new IllegalStateException("Choose a job before playing Money Hunter.");
		}
	}

	private Instant min(Instant first, Instant second) {
		return first.isBefore(second) ? first : second;
	}

	private boolean isActive(Instant value) {
		return isActiveAt(value, clock.instant());
	}

	private boolean isActiveAt(Instant value, Instant at) {
		return value != null && value.isAfter(at);
	}

	private record AttackCounts(long normal, long boosted) {
		long total() {
			return normal + boosted;
		}
	}
}
