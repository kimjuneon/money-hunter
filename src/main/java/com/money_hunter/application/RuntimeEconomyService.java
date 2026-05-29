package com.money_hunter.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.money_hunter.domain.GameEconomyPolicy;
import com.money_hunter.infrastructure.config.EconomyProperties;
import com.money_hunter.infrastructure.persistence.GameEconomyPolicyRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeEconomyService {
	private static final List<PolicyDefinition> DEFINITIONS = List.of(
			new PolicyDefinition("adRevenuePerRewardAdWon", "리워드 광고 1회 예상 매출", "원", 1, 10_000),
			new PolicyDefinition("goldPerTossPoint", "토스포인트 1P당 골드", "골드", 1, 1_000_000),
			new PolicyDefinition("companionPriceWon", "동료 펫 1마리 가격", "원", 0, 1_000_000),
			new PolicyDefinition("skillPointPackPriceWon", "SP 10팩 가격", "원", 0, 1_000_000),
			new PolicyDefinition("skillPointPackAmount", "스킬 포인트 팩 지급량", "SP", 1, 1_000),
			new PolicyDefinition("friendInviteRewardSkillPoints", "친구 초대 보상", "SP", 0, 1_000),
			new PolicyDefinition("friendInviteLimit", "친구 초대 보상 제한", "명", 0, 100),
			new PolicyDefinition("maxCharacterSlots", "최대 캐릭터 슬롯", "개", 1, 3),
			new PolicyDefinition("autoHuntAdSeconds", "자동사냥 광고 보상 시간", "초", 60, 86_400),
			new PolicyDefinition("boostAdSeconds", "공속버프 광고 보상 시간", "초", 60, 86_400),
			new PolicyDefinition("maxAdSeconds", "광고 보상 최대 누적 시간", "초", 3_600, 86_400),
			new PolicyDefinition("rewardPointAmount", "보상 수령 포인트 기준", "P", 1, 1_000_000)
	);

	private final EconomyProperties defaults;
	private final GameEconomyPolicyRepository repository;
	private final Clock clock = Clock.systemUTC();
	private final AtomicReference<EconomyPolicySnapshot> cache = new AtomicReference<>();
	private volatile Instant cacheLoadedAt = Instant.EPOCH;

	public RuntimeEconomyService(EconomyProperties defaults, GameEconomyPolicyRepository repository) {
		this.defaults = defaults;
		this.repository = repository;
	}

	@PostConstruct
	void init() {
		reload();
	}

	public EconomyPolicySnapshot snapshot() {
		EconomyPolicySnapshot snapshot = cache.get();
		if (snapshot == null || isCacheStale()) {
			reload();
			return cache.get();
		}
		return snapshot;
	}

	public List<PolicyDefinition> definitions() {
		return DEFINITIONS;
	}

	@Transactional(readOnly = true)
	public void reload() {
		EconomyPolicySnapshot merged = repository.findById(GameEconomyPolicy.SINGLETON_ID)
				.map(this::merge)
				.orElseGet(this::defaultsSnapshot);
		merged.validate();
		cache.set(merged);
		cacheLoadedAt = Instant.now(clock);
	}

	@Transactional
	public PolicyChangeResult update(String key, Number value) {
		requireKnownKey(key);
		GameEconomyPolicy row = ensureRow();
		EconomyPolicySnapshot before = snapshot();
		row.update(key, value, clock.instant());
		EconomyPolicySnapshot after = merge(row);
		after.validate();
		repository.save(row);
		cache.set(after);
		cacheLoadedAt = Instant.now(clock);
		return new PolicyChangeResult(key, valueOf(before, key), valueOf(after, key), false);
	}

	@Transactional
	public PolicyChangeResult reset(String key) {
		requireKnownKey(key);
		GameEconomyPolicy row = ensureRow();
		EconomyPolicySnapshot before = snapshot();
		row.reset(key, clock.instant());
		EconomyPolicySnapshot after = merge(row);
		after.validate();
		repository.save(row);
		cache.set(after);
		cacheLoadedAt = Instant.now(clock);
		return new PolicyChangeResult(key, valueOf(before, key), valueOf(after, key), true);
	}

	private boolean isCacheStale() {
		return cacheLoadedAt.plus(Duration.ofSeconds(15)).isBefore(Instant.now(clock));
	}

	private GameEconomyPolicy ensureRow() {
		return repository.findById(GameEconomyPolicy.SINGLETON_ID)
				.orElseGet(() -> repository.save(new GameEconomyPolicy(GameEconomyPolicy.SINGLETON_ID, Instant.now(clock))));
	}

	private EconomyPolicySnapshot merge(GameEconomyPolicy row) {
		int goldPerTossPoint = nvl(row.getGoldPerTossPoint(), defaults.goldPerTossPoint());
		int rewardPointAmount = nvl(row.getRewardPointAmount(), defaults.rewardPointAmount());
		return new EconomyPolicySnapshot(
				nvl(row.getAdRevenuePerRewardAdWon(), defaults.adRevenuePerRewardAdWon()),
				goldPerTossPoint,
				nvl(row.getCompanionPriceWon(), defaults.companionPriceWon()),
				nvl(row.getSkillPointPackPriceWon(), defaults.skillPointPackPriceWon()),
				nvl(row.getSkillPointPackAmount(), defaults.skillPointPackAmount()),
				nvl(row.getFriendInviteRewardSkillPoints(), defaults.friendInviteRewardSkillPoints()),
				nvl(row.getFriendInviteLimit(), defaults.friendInviteLimit()),
				nvl(row.getMaxCharacterSlots(), defaults.maxCharacterSlots()),
				nvl(row.getAutoHuntAdSeconds(), defaults.autoHuntAdSeconds()),
				nvl(row.getBoostAdSeconds(), defaults.boostAdSeconds()),
				nvl(row.getMaxAdSeconds(), defaults.maxAdSeconds()),
				rewardGoldThreshold(goldPerTossPoint, rewardPointAmount),
				rewardPointAmount
		);
	}

	private EconomyPolicySnapshot defaultsSnapshot() {
		long rewardGoldThreshold = rewardGoldThreshold(defaults.goldPerTossPoint(), defaults.rewardPointAmount());
		return new EconomyPolicySnapshot(
				defaults.adRevenuePerRewardAdWon(),
				defaults.goldPerTossPoint(),
				defaults.companionPriceWon(),
				defaults.skillPointPackPriceWon(),
				defaults.skillPointPackAmount(),
				defaults.friendInviteRewardSkillPoints(),
				defaults.friendInviteLimit(),
				defaults.maxCharacterSlots(),
				defaults.autoHuntAdSeconds(),
				defaults.boostAdSeconds(),
				defaults.maxAdSeconds(),
				rewardGoldThreshold,
				defaults.rewardPointAmount()
		);
	}

	private long rewardGoldThreshold(int goldPerTossPoint, int rewardPointAmount) {
		return (long) goldPerTossPoint * rewardPointAmount;
	}

	private int nvl(Integer value, int fallback) {
		return value == null ? fallback : value;
	}

	private long nvl(Long value, long fallback) {
		return value == null ? fallback : value;
	}

	public int adRevenuePerRewardAdWon() {
		return snapshot().adRevenuePerRewardAdWon();
	}

	public int goldPerTossPoint() {
		return snapshot().goldPerTossPoint();
	}

	public int companionPriceWon() {
		return snapshot().companionPriceWon();
	}

	public int skillPointPackPriceWon() {
		return snapshot().skillPointPackPriceWon();
	}

	public int skillPointPackAmount() {
		return snapshot().skillPointPackAmount();
	}

	public int friendInviteRewardSkillPoints() {
		return snapshot().friendInviteRewardSkillPoints();
	}

	public int friendInviteLimit() {
		return snapshot().friendInviteLimit();
	}

	public int maxCharacterSlots() {
		return snapshot().maxCharacterSlots();
	}

	public long autoHuntAdSeconds() {
		return snapshot().autoHuntAdSeconds();
	}

	public long boostAdSeconds() {
		return snapshot().boostAdSeconds();
	}

	public long maxAdSeconds() {
		return snapshot().maxAdSeconds();
	}

	public long rewardGoldThreshold() {
		return snapshot().rewardGoldThreshold();
	}

	public int rewardPointAmount() {
		return snapshot().rewardPointAmount();
	}

	public Number valueOf(EconomyPolicySnapshot snapshot, String key) {
		return switch (key) {
			case "adRevenuePerRewardAdWon" -> snapshot.adRevenuePerRewardAdWon();
			case "goldPerTossPoint" -> snapshot.goldPerTossPoint();
			case "companionPriceWon" -> snapshot.companionPriceWon();
			case "skillPointPackPriceWon" -> snapshot.skillPointPackPriceWon();
			case "skillPointPackAmount" -> snapshot.skillPointPackAmount();
			case "friendInviteRewardSkillPoints" -> snapshot.friendInviteRewardSkillPoints();
			case "friendInviteLimit" -> snapshot.friendInviteLimit();
			case "maxCharacterSlots" -> snapshot.maxCharacterSlots();
			case "autoHuntAdSeconds" -> snapshot.autoHuntAdSeconds();
			case "boostAdSeconds" -> snapshot.boostAdSeconds();
			case "maxAdSeconds" -> snapshot.maxAdSeconds();
			case "rewardGoldThreshold" -> snapshot.rewardGoldThreshold();
			case "rewardPointAmount" -> snapshot.rewardPointAmount();
			default -> throw new IllegalArgumentException("Unknown policy key.");
		};
	}

	private void requireKnownKey(String key) {
		if (DEFINITIONS.stream().noneMatch(definition -> definition.key().equals(key))) {
			throw new IllegalArgumentException("Unknown policy key.");
		}
	}

	public record PolicyDefinition(String key, String label, String unit, long min, long max) {
	}

	public record PolicyChangeResult(String key, Number beforeValue, Number afterValue, boolean resetToDefault) {
	}
}
