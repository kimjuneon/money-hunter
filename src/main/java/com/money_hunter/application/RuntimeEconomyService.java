package com.money_hunter.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.money_hunter.domain.GameEconomyPolicy;
import com.money_hunter.infrastructure.config.EconomyProperties;
import com.money_hunter.infrastructure.persistence.GameEconomyPolicyRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeEconomyService {
	private static final Logger log = LoggerFactory.getLogger(RuntimeEconomyService.class);
	private static final String GOLD_PER_TOSS_POINT_KEY = "goldPerTossPoint";
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
			new PolicyDefinition("autoHuntAdCooldownSeconds", "자동사냥 광고 보상 쿨타임", "초", 0, 86_400),
			new PolicyDefinition("maxAdSeconds", "광고 보상 최대 누적 시간", "초", 3_600, 86_400),
			new PolicyDefinition("dungeonFreeDailyLimit", "던전 기본 제공 횟수", "회", 0, 20),
			new PolicyDefinition("dungeonAdditionalDailyLimit", "던전 광고 추가 횟수", "회", 0, 20),
			new PolicyDefinition("dungeonReentryCooldownSeconds", "던전 재입장 대기 시간", "초", 0, 86_400),
			new PolicyDefinition("skillPointAdCooldownSeconds", "SP 광고 보상 쿨타임", "초", 0, 86_400),
			new PolicyDefinition("weeklyPunchKingMaxGoldReward", "주간 펀치킹 최대 골드", "G", 0, 10_000_000_000L),
			new PolicyDefinition("weeklyPunchKingGoldRewardScoreScale", "주간 펀치킹 최대 골드 점수 기준", "점", 1, 100_000_000_000L),
			new PolicyDefinition("weeklyPunchKingBaseSkillPoints", "주간 펀치킹 기본 SP", "SP", 0, 1_000),
			new PolicyDefinition("weeklyPunchKingSkillPointTier2Score", "주간 펀치킹 SP 2단계 점수", "점", 0, 100_000_000_000L),
			new PolicyDefinition("weeklyPunchKingSkillPointTier2Reward", "주간 펀치킹 SP 2단계 보상", "SP", 0, 1_000),
			new PolicyDefinition("weeklyPunchKingSkillPointTier3Score", "주간 펀치킹 SP 3단계 점수", "점", 0, 100_000_000_000L),
			new PolicyDefinition("weeklyPunchKingSkillPointTier3Reward", "주간 펀치킹 SP 3단계 보상", "SP", 0, 1_000),
			new PolicyDefinition("weeklyPunchKingSkillPointTier4Score", "주간 펀치킹 SP 4단계 점수", "점", 0, 100_000_000_000L),
			new PolicyDefinition("weeklyPunchKingSkillPointTier4Reward", "주간 펀치킹 SP 4단계 보상", "SP", 0, 1_000),
			new PolicyDefinition("rewardPointAmount", "보상 수령 포인트 기준", "P", 1, 1_000_000),
			new PolicyDefinition("anomalyLimitPerRule", "이상징후 룰별 최대 표시", "건", 1, 200),
			new PolicyDefinition("anomalyAdEventsPerHourWarning", "1시간 광고 이벤트 이상징후 기준", "회", 1, 10_000),
			new PolicyDefinition("anomalyRewardClaimsPerDayWarning", "일일 보상 수령 이상징후 기준", "건", 1, 1_000),
			new PolicyDefinition("anomalyGoldThresholdMultiplier", "보유 골드 이상징후 배수", "배", 1, 1_000),
			new PolicyDefinition("anomalySkillPointsWarning", "미사용 SP 이상징후 기준", "SP", 1, 100_000),
			new PolicyDefinition("anomalyTimerGraceSeconds", "자동사냥 시간 이상징후 유예", "초", 0, 86_400)
	);

	private final EconomyProperties defaults;
	private final GameEconomyPolicyRepository repository;
	private final PlayerRepository playerRepository;
	private final Clock clock = Clock.systemUTC();
	private final AtomicReference<EconomyPolicySnapshot> cache = new AtomicReference<>();
	private volatile Instant cacheLoadedAt = Instant.EPOCH;

	public RuntimeEconomyService(
			EconomyProperties defaults,
			GameEconomyPolicyRepository repository,
			PlayerRepository playerRepository
	) {
		this.defaults = defaults;
		this.repository = repository;
		this.playerRepository = playerRepository;
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
			long scaledPlayers = rescalePlayerGoldBalancesIfNeeded(key, before, after);
			repository.save(row);
			cache.set(after);
			cacheLoadedAt = Instant.now(clock);
			log.info("운영 정책 변경: key={}, before={}, after={}, scaledPlayers={}",
					key, valueOf(before, key), valueOf(after, key), scaledPlayers);
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
			long scaledPlayers = rescalePlayerGoldBalancesIfNeeded(key, before, after);
			repository.save(row);
			cache.set(after);
			cacheLoadedAt = Instant.now(clock);
			log.info("운영 정책 기본값 복원: key={}, before={}, after={}, scaledPlayers={}",
					key, valueOf(before, key), valueOf(after, key), scaledPlayers);
			return new PolicyChangeResult(key, valueOf(before, key), valueOf(after, key), true);
		}

	private long rescalePlayerGoldBalancesIfNeeded(String key, EconomyPolicySnapshot before, EconomyPolicySnapshot after) {
		if (!GOLD_PER_TOSS_POINT_KEY.equals(key) || before.goldPerTossPoint() == after.goldPerTossPoint()) {
			return 0;
		}
		long changed = playerRepository.findAll().stream()
				.filter(player -> player.rescaleGoldForTossPointRate(before.goldPerTossPoint(), after.goldPerTossPoint()))
				.count();
		log.info("토스포인트 환산 기준 변경에 따른 골드 잔액 스케일링: before={}, after={}, players={}",
				before.goldPerTossPoint(),
				after.goldPerTossPoint(),
				changed);
		return changed;
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
					nvl(row.getAutoHuntAdCooldownSeconds(), defaults.autoHuntAdCooldownSeconds()),
					nvl(row.getMaxAdSeconds(), defaults.maxAdSeconds()),
				nvl(row.getDungeonFreeDailyLimit(), defaults.dungeonFreeDailyLimit()),
				nvl(row.getDungeonAdditionalDailyLimit(), defaults.dungeonAdditionalDailyLimit()),
				nvl(row.getDungeonReentryCooldownSeconds(), defaults.dungeonReentryCooldownSeconds()),
				nvl(row.getSkillPointAdCooldownSeconds(), defaults.skillPointAdCooldownSeconds()),
				nvl(row.getWeeklyPunchKingMaxGoldReward(), defaults.weeklyPunchKingMaxGoldReward()),
				nvl(row.getWeeklyPunchKingGoldRewardScoreScale(), defaults.weeklyPunchKingGoldRewardScoreScale()),
				nvl(row.getWeeklyPunchKingBaseSkillPoints(), defaults.weeklyPunchKingBaseSkillPoints()),
				nvl(row.getWeeklyPunchKingSkillPointTier2Score(), defaults.weeklyPunchKingSkillPointTier2Score()),
				nvl(row.getWeeklyPunchKingSkillPointTier2Reward(), defaults.weeklyPunchKingSkillPointTier2Reward()),
				nvl(row.getWeeklyPunchKingSkillPointTier3Score(), defaults.weeklyPunchKingSkillPointTier3Score()),
				nvl(row.getWeeklyPunchKingSkillPointTier3Reward(), defaults.weeklyPunchKingSkillPointTier3Reward()),
				nvl(row.getWeeklyPunchKingSkillPointTier4Score(), defaults.weeklyPunchKingSkillPointTier4Score()),
				nvl(row.getWeeklyPunchKingSkillPointTier4Reward(), defaults.weeklyPunchKingSkillPointTier4Reward()),
				rewardGoldThreshold(goldPerTossPoint, rewardPointAmount),
				rewardPointAmount,
				nvl(row.getAnomalyLimitPerRule(), defaults.anomalyLimitPerRule()),
				nvl(row.getAnomalyAdEventsPerHourWarning(), defaults.anomalyAdEventsPerHourWarning()),
				nvl(row.getAnomalyRewardClaimsPerDayWarning(), defaults.anomalyRewardClaimsPerDayWarning()),
				nvl(row.getAnomalyGoldThresholdMultiplier(), defaults.anomalyGoldThresholdMultiplier()),
				nvl(row.getAnomalySkillPointsWarning(), defaults.anomalySkillPointsWarning()),
				nvl(row.getAnomalyTimerGraceSeconds(), defaults.anomalyTimerGraceSeconds())
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
					defaults.autoHuntAdCooldownSeconds(),
					defaults.maxAdSeconds(),
				defaults.dungeonFreeDailyLimit(),
				defaults.dungeonAdditionalDailyLimit(),
				defaults.dungeonReentryCooldownSeconds(),
				defaults.skillPointAdCooldownSeconds(),
				defaults.weeklyPunchKingMaxGoldReward(),
				defaults.weeklyPunchKingGoldRewardScoreScale(),
				defaults.weeklyPunchKingBaseSkillPoints(),
				defaults.weeklyPunchKingSkillPointTier2Score(),
				defaults.weeklyPunchKingSkillPointTier2Reward(),
				defaults.weeklyPunchKingSkillPointTier3Score(),
				defaults.weeklyPunchKingSkillPointTier3Reward(),
				defaults.weeklyPunchKingSkillPointTier4Score(),
				defaults.weeklyPunchKingSkillPointTier4Reward(),
				rewardGoldThreshold,
				defaults.rewardPointAmount(),
				defaults.anomalyLimitPerRule(),
				defaults.anomalyAdEventsPerHourWarning(),
				defaults.anomalyRewardClaimsPerDayWarning(),
				defaults.anomalyGoldThresholdMultiplier(),
				defaults.anomalySkillPointsWarning(),
				defaults.anomalyTimerGraceSeconds()
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

	public long autoHuntAdCooldownSeconds() {
		return snapshot().autoHuntAdCooldownSeconds();
	}

	public long maxAdSeconds() {
		return snapshot().maxAdSeconds();
	}

	public long skillPointAdCooldownSeconds() {
		return snapshot().skillPointAdCooldownSeconds();
	}

	public long weeklyPunchKingMaxGoldReward() {
		return snapshot().weeklyPunchKingMaxGoldReward();
	}

	public long weeklyPunchKingGoldRewardScoreScale() {
		return snapshot().weeklyPunchKingGoldRewardScoreScale();
	}

	public int weeklyPunchKingBaseSkillPoints() {
		return snapshot().weeklyPunchKingBaseSkillPoints();
	}

	public long weeklyPunchKingSkillPointTier2Score() {
		return snapshot().weeklyPunchKingSkillPointTier2Score();
	}

	public int weeklyPunchKingSkillPointTier2Reward() {
		return snapshot().weeklyPunchKingSkillPointTier2Reward();
	}

	public long weeklyPunchKingSkillPointTier3Score() {
		return snapshot().weeklyPunchKingSkillPointTier3Score();
	}

	public int weeklyPunchKingSkillPointTier3Reward() {
		return snapshot().weeklyPunchKingSkillPointTier3Reward();
	}

	public long weeklyPunchKingSkillPointTier4Score() {
		return snapshot().weeklyPunchKingSkillPointTier4Score();
	}

	public int weeklyPunchKingSkillPointTier4Reward() {
		return snapshot().weeklyPunchKingSkillPointTier4Reward();
	}

	public long rewardGoldThreshold() {
		return snapshot().rewardGoldThreshold();
	}

	public int rewardPointAmount() {
		return snapshot().rewardPointAmount();
	}

	public int anomalyLimitPerRule() {
		return snapshot().anomalyLimitPerRule();
	}

	public long anomalyAdEventsPerHourWarning() {
		return snapshot().anomalyAdEventsPerHourWarning();
	}

	public long anomalyRewardClaimsPerDayWarning() {
		return snapshot().anomalyRewardClaimsPerDayWarning();
	}

	public long anomalyGoldThresholdMultiplier() {
		return snapshot().anomalyGoldThresholdMultiplier();
	}

	public int anomalySkillPointsWarning() {
		return snapshot().anomalySkillPointsWarning();
	}

	public long anomalyTimerGraceSeconds() {
		return snapshot().anomalyTimerGraceSeconds();
	}

	public long dungeonReentryCooldownSeconds() {
		return snapshot().dungeonReentryCooldownSeconds();
	}

	public int dungeonFreeDailyLimit() {
		return snapshot().dungeonFreeDailyLimit();
	}

	public int dungeonAdditionalDailyLimit() {
		return snapshot().dungeonAdditionalDailyLimit();
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
				case "autoHuntAdCooldownSeconds" -> snapshot.autoHuntAdCooldownSeconds();
				case "maxAdSeconds" -> snapshot.maxAdSeconds();
			case "dungeonFreeDailyLimit" -> snapshot.dungeonFreeDailyLimit();
			case "dungeonAdditionalDailyLimit" -> snapshot.dungeonAdditionalDailyLimit();
			case "dungeonReentryCooldownSeconds" -> snapshot.dungeonReentryCooldownSeconds();
			case "skillPointAdCooldownSeconds" -> snapshot.skillPointAdCooldownSeconds();
			case "weeklyPunchKingMaxGoldReward" -> snapshot.weeklyPunchKingMaxGoldReward();
			case "weeklyPunchKingGoldRewardScoreScale" -> snapshot.weeklyPunchKingGoldRewardScoreScale();
			case "weeklyPunchKingBaseSkillPoints" -> snapshot.weeklyPunchKingBaseSkillPoints();
			case "weeklyPunchKingSkillPointTier2Score" -> snapshot.weeklyPunchKingSkillPointTier2Score();
			case "weeklyPunchKingSkillPointTier2Reward" -> snapshot.weeklyPunchKingSkillPointTier2Reward();
			case "weeklyPunchKingSkillPointTier3Score" -> snapshot.weeklyPunchKingSkillPointTier3Score();
			case "weeklyPunchKingSkillPointTier3Reward" -> snapshot.weeklyPunchKingSkillPointTier3Reward();
			case "weeklyPunchKingSkillPointTier4Score" -> snapshot.weeklyPunchKingSkillPointTier4Score();
			case "weeklyPunchKingSkillPointTier4Reward" -> snapshot.weeklyPunchKingSkillPointTier4Reward();
			case "rewardGoldThreshold" -> snapshot.rewardGoldThreshold();
			case "rewardPointAmount" -> snapshot.rewardPointAmount();
			case "anomalyLimitPerRule" -> snapshot.anomalyLimitPerRule();
			case "anomalyAdEventsPerHourWarning" -> snapshot.anomalyAdEventsPerHourWarning();
			case "anomalyRewardClaimsPerDayWarning" -> snapshot.anomalyRewardClaimsPerDayWarning();
			case "anomalyGoldThresholdMultiplier" -> snapshot.anomalyGoldThresholdMultiplier();
			case "anomalySkillPointsWarning" -> snapshot.anomalySkillPointsWarning();
			case "anomalyTimerGraceSeconds" -> snapshot.anomalyTimerGraceSeconds();
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
