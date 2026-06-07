window.addEventListener("error", (event) => {
  showStartupError(event.error || event.message);
  event.preventDefault();
});
window.addEventListener("unhandledrejection", (event) => {
  showStartupError(event.reason);
  event.preventDefault();
});

function showStartupError(error) {
  try {
    const message = typeof error === "string" ? error : error?.message;
    const text = message || "잠시 후 다시 시도해 주세요.";
    const target = document.getElementById("battleTip");
    if (target) {
      target.textContent = text;
    }
  } catch {
    // Keep startup error handling best-effort only.
  }
}

const distributionTargetOverride = new URLSearchParams(window.location.search).get("target");
const pendingIapProductStorageKey = "moneyHunter.pendingIapProducts";
const hiddenPetSkinsStorageKey = "moneyHunter.hideEasterEggSkins";
const rookieHomeShortcutGuideStorageKey = "moneyHunter.rookieHomeShortcutGuide";
const rookieHomeShortcutGuideSessionKey = "moneyHunter.rookieHomeShortcutGuideSession";
const benefitTabEntryStorageKey = "moneyHunter.benefitTabEntry";
const rookieHomeShortcutParam = "rookieHome";
const rookieHomeShortcutValue = "1";
const benefitTabEntryPath = "/benefit";

const state = {
  player: null,
  displayGold: 0,
  selectedJob: "WARRIOR",
  battleBackgroundIndex: 0,
  lastMonsterSignature: "",
  localMonster: null,
  localLevel: null,
  localExperience: null,
  localNextLevelExperience: null,
  lastHitAt: 0,
  hitInFlight: false,
  combatSyncInFlight: false,
  lastCombatSyncAt: 0,
  lastLocalGoldEstimateAt: 0,
  lastServerGold: null,
  lastSettlementGold: 0,
  attackTimer: null,
  adAction: null,
  adTimer: null,
  timeRewardOverflowAction: null,
  paymentAction: null,
  forceJobModal: false,
  dismissedJobModal: false,
  autoHuntWasActive: false,
  autoHuntEndRefreshInFlight: false,
  resumeRefreshInFlight: false,
  shownNotificationId: null,
  noticeMessage: "",
  noticeExpiresAt: 0,
  noticeTimer: null,
  showDummyBanner: false,
  bgmMuted: false,
  bgmStarted: false,
  soundContext: null,
  pageVisible: !document.hidden,
  featureTutorialStarted: false,
  featureTutorialActive: false,
  featureTutorialIndex: 0,
  featureTutorialPositionTimer: null,
  authToken: "",
  reviewToolsEnabled: false,
  mockMonetizationEnabled: false,
  integrationMode: "",
  tossReleaseReady: false,
  tossLoginEnabled: false,
  realRewardAdsEnabled: false,
  realBannerAdsEnabled: false,
  realPaymentsEnabled: false,
  realShareRewardEnabled: false,
  realSmartMessageEnabled: false,
  notificationAgreementTemplateCode: "",
  rookieEventMissionNotificationAgreementTemplateCode: "",
  notificationAgreementInFlight: false,
  adMode: "test",
  adGroupIds: {},
  iapProductIds: {},
  iapProducts: {},
  shareRewardModuleId: "",
  shareRewardMessage: "친구에게 공유하고 SP 1개를 받아요",
  realAdInFlight: false,
  realAdPreloads: new Map(),
  realAdPreloadTimers: new Map(),
  realPaymentInFlight: false,
  tossAdsInitialized: false,
  tossAdsInitializationPromise: null,
  iapProductInfoRequested: false,
  pendingIapRestoreInFlight: false,
  pendingIapProductIdsByOrderId: readPendingIapProductIds(),
	shareRewardInFlight: false,
	adventureActionInFlight: false,
	adventureActionStepTimers: [],
	gameProfileSyncTimer: null,
  gameProfileSyncInFlight: false,
  lastGameProfileSyncAt: 0,
  lastGameProfileMissingAt: 0,
  lastGameProfileSyncEventAt: 0,
  lastGameProfileSyncEventKey: "",
  lastGameProfileEditorOpenedAt: 0,
  gameProfileEditorInFlight: false,
  gameProfileCreationDeferred: false,
  pendingLeaderboardScoreSubmit: false,
  pendingLeaderboardOpen: false,
  leaderboardEntrySubmitTimer: null,
  leaderboardSubmitInFlight: false,
  lastLeaderboardScoreSubmitted: null,
  loginInFlight: false,
  petEasterEggTapCount: 0,
  petEasterEggTapStartedAt: 0,
  petEasterEggPasswordOpen: false,
  petEasterEggSkinsHidden: storedValue(hiddenPetSkinsStorageKey) === "true",
  petSkinModalSlot: 1,
  rookieEventSelectedDay: 1,
  rookieHomeShortcutReturnHandled: false,
  homeShortcutGuideIndex: 0,
  distributionTarget: String(distributionTargetOverride || "").trim().toUpperCase() === "ONESTORE" ? "ONESTORE" : "TOSS",
};

const combatSyncIntervalMs = 10_000;

function applyRuntimeSafeAreaFallback() {
  const ua = navigator.userAgent || "";
  const isIos = /iPhone|iPad|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  if (!isIos) {
    document.documentElement.style.setProperty("--runtime-safe-top", "0px");
    document.documentElement.style.setProperty("--runtime-safe-bottom", "0px");
    return;
  }

  const tallScreen = Math.max(window.innerHeight || 0, window.screen?.height || 0) >= 800;
  document.documentElement.style.setProperty("--runtime-safe-top", `${tallScreen ? 47 : 24}px`);
  document.documentElement.style.setProperty("--runtime-safe-bottom", `${tallScreen ? 20 : 0}px`);
}

function readPendingIapProductIds() {
  try {
    const parsed = JSON.parse(storedValue(pendingIapProductStorageKey) || "{}");
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function savePendingIapProductIds() {
  storeValue(pendingIapProductStorageKey, JSON.stringify(state.pendingIapProductIdsByOrderId));
}

function rememberPendingIapProduct(orderId, productId) {
  if (!orderId || !productId) {
    return;
  }
  state.pendingIapProductIdsByOrderId[orderId] = productId;
  savePendingIapProductIds();
}

function forgetPendingIapProduct(orderId) {
  if (!orderId || !state.pendingIapProductIdsByOrderId[orderId]) {
    return;
  }
  delete state.pendingIapProductIdsByOrderId[orderId];
  savePendingIapProductIds();
}

const emptyElement = {
  textContent: "",
  disabled: false,
  style: {},
  dataset: {},
  classList: { add() {}, remove() {}, toggle() {} },
  appendChild() {},
  replaceChildren() {},
  click() {},
  scrollIntoView() {},
  setAttribute() {},
  querySelector() {
    return emptyElement;
  },
  addEventListener() {},
};

const $ = (id) => document.getElementById(id) || emptyElement;

const jobMeta = {
  WARRIOR: { label: "전사", pick: "전사를", change: "전사로", className: "warrior", weapon: "sword", image: "/assets/body-warrior.png?v=20260525-03", arm: "/assets/arm-warrior.png?v=20260525-03", projectile: "/assets/projectile-sword.png?v=20260525-02", name: "검의 헌터" },
  ARCHER: { label: "궁수", pick: "궁수를", change: "궁수로", className: "archer", weapon: "arrow", image: "/assets/body-archer.png?v=20260525-03", arm: "/assets/arm-archer.png?v=20260525-03", projectile: "/assets/projectile-arrow.png?v=20260525-02", name: "숲의 헌터" },
  MAGE: { label: "마법사", pick: "마법사를", change: "마법사로", className: "mage", weapon: "magic", image: "/assets/body-mage.png?v=20260525-03", arm: "/assets/arm-mage.png?v=20260525-03", projectile: "/assets/projectile-magic.png?v=20260525-02", name: "별빛 헌터" },
  ROGUE: { label: "도적", pick: "도적을", change: "도적으로", className: "rogue", weapon: "shuriken", image: "/assets/body-rogue.png?v=20260525-02", arm: "/assets/arm-rogue.png?v=20260525-02", projectile: "/assets/projectile-shuriken.png?v=20260525-02", name: "그림자 헌터" },
};

const monsterMeta = {
  BOSS_ROCK: { name: "흑요석 골렘", image: "/assets/boss-rock.png" },
  BOSS_FROST: { name: "빙결 발톱수", image: "/assets/boss-frost.png" },
  BOSS_TREANT: { name: "심록의 고목왕", image: "/assets/boss-treant.png" },
};
const bossRaidTiers = [
  { minCombatPower: 0, powerLabel: "0~500만", bossName: "흑요석 골렘", difficultyName: "초급 보스", image: "/assets/boss-rock.png", rewardPreview: "골드 1,000~1,500G · SP 3개 · 자동사냥 30분" },
  { minCombatPower: 5_000_000, powerLabel: "500만~2,000만", bossName: "빙결 발톱수", difficultyName: "숙련 보스", image: "/assets/boss-frost.png", rewardPreview: "골드 1,500~2,000G · SP 3개 · 자동사냥 1시간" },
  { minCombatPower: 20_000_000, powerLabel: "2,000만~6,000만", bossName: "심록의 고목왕", difficultyName: "정예 보스", image: "/assets/boss-treant.png", rewardPreview: "골드 2,000~2,500G · SP 3개 · 자동사냥 1시간 30분" },
  { minCombatPower: 60_000_000, powerLabel: "6,000만 이상", bossName: "별무리 고대룡", difficultyName: "심층 보스", image: "/assets/boss-treant.png", rewardPreview: "골드 2,500~3,000G · SP 3개 · 자동사냥 2시간" },
];
const maxCombatPower = 99_999_999;
const combatPowerLevelScale = 40;
const combatPowerSkillScale = 180;
const dungeonFreeDailyLimit = 3;
const dungeonTiers = [
  { minCombatPower: 0, powerLabel: "0~500만", name: "초급 던전", rewardPreview: "골드 100~300G · SP 1개 · 자동사냥 10분 · 보스 입장권 10%" },
  { minCombatPower: 5_000_000, powerLabel: "500만~2,000만", name: "숙련 던전", rewardPreview: "골드 300~500G · SP 2개 · 자동사냥 20분 · 보스 입장권 10%" },
  { minCombatPower: 20_000_000, powerLabel: "2,000만~6,000만", name: "정예 던전", rewardPreview: "골드 500~700G · SP 3개 · 자동사냥 30분 · 보스 입장권 10%" },
  { minCombatPower: 60_000_000, powerLabel: "6,000만 이상", name: "심층 던전", rewardPreview: "골드 700~1,000G · SP 4개 · 자동사냥 40분 · 보스 입장권 10%" },
];
const adventureAssets = {
  dungeon: "/assets/adventure/dungeon-icon-filled-inside.png?v=20260605-01",
  bossRaid: "/assets/adventure/boss-icon-filled-inside.png?v=20260605-01",
  bossTicket: "/assets/adventure/boss-ticket.png?v=20260604-01",
};
const adventureSceneSpecs = {
  dungeon: {
    fallback: adventureAssets.dungeon,
    running: [
      { key: "dungeon-gate", label: "입구 정찰", body: "입구를 열고 안쪽을 살피는 중이에요.", image: "/assets/adventure/scenes/dungeon-01-gate.png?v=20260604-01" },
      { key: "dungeon-crossroads", label: "갈림길 추적", body: "갈림길에서 흔적을 추적하는 중이에요.", image: "/assets/adventure/scenes/dungeon-02-crossroads.png?v=20260604-01" },
      { key: "dungeon-rune", label: "함정 해제", body: "낡은 룬 함정을 조심스럽게 해제하는 중이에요.", image: "/assets/adventure/scenes/dungeon-03-rune-trap.png?v=20260604-01" },
      { key: "dungeon-treasure", label: "보물 확인", body: "깊은 방의 보물 상자를 확인하는 중이에요.", image: "/assets/adventure/scenes/dungeon-04-treasure.png?v=20260604-01" },
      { key: "dungeon-ticket-clue", label: "단서 탐색", body: "숨겨진 입장권 단서를 찾는 중이에요.", image: "/assets/adventure/scenes/dungeon-05-ticket-clue.png?v=20260604-01" },
    ],
    success: { key: "dungeon-clear", label: "탐험 완료", body: "탐험 기록을 정리하는 중이에요.", image: "/assets/adventure/scenes/dungeon-06-clear.png?v=20260604-01" },
    failure: { key: "dungeon-fail", label: "탐험 중단", body: "입구 근처로 후퇴했어요.", image: "/assets/adventure/scenes/dungeon-07-retreat.png?v=20260604-01" },
  },
  boss: {
    fallback: adventureAssets.bossRaid,
    running: [
      { key: "boss-encounter", label: "조우", body: "보스의 움직임을 살피는 중이에요.", image: "/assets/adventure/scenes/boss-01-encounter.png?v=20260604-01" },
      { key: "boss-pattern", label: "패턴 분석", body: "공격 패턴의 빈틈을 찾는 중이에요.", image: "/assets/adventure/scenes/boss-02-pattern.png?v=20260604-01" },
      { key: "boss-dodge", label: "회피", body: "거대한 공격을 피하며 거리를 좁히는 중이에요.", image: "/assets/adventure/scenes/boss-03-dodge.png?v=20260604-01" },
      { key: "boss-counter", label: "총공세", body: "동료 펫과 함께 공격을 몰아치는 중이에요.", image: "/assets/adventure/scenes/boss-04-counterattack.png?v=20260604-01" },
      { key: "boss-final", label: "마무리", body: "마지막 일격을 준비하는 중이에요.", image: "/assets/adventure/scenes/boss-05-final-strike.png?v=20260604-01" },
    ],
    success: { key: "boss-victory", label: "토벌 완료", body: "토벌 결과를 정산하는 중이에요.", image: "/assets/adventure/scenes/boss-06-victory.png?v=20260604-01" },
    failure: { key: "boss-fail", label: "토벌 실패", body: "전열을 정비하며 물러났어요.", image: "/assets/adventure/scenes/boss-07-retreat.png?v=20260604-01" },
  },
};
const adventureActionStepDurationMs = 1750;
const adventureActionTotalDurationMs = 8800;
const adventureImagePreloads = new Map();
const monsterKeys = ["BOSS_ROCK", "BOSS_FROST", "BOSS_TREANT"];
const monsterBaseHp = {
  BOSS_ROCK: 120,
  BOSS_FROST: 135,
  BOSS_TREANT: 150,
};

const attackMotionMsByJob = {
  WARRIOR: 820,
  ARCHER: 950,
  MAGE: 900,
  ROGUE: 740,
};

const petAssetVersion = "20260601-02";
const eventAssetVersion = "20260602-01";
const rookieEventAssets = {
  pet: `/assets/events/rookie-event-pet.png?v=${eventAssetVersion}`,
  badge: `/assets/events/rookie-event-badge.png?v=${eventAssetVersion}`,
  trail: `/assets/events/rookie-event-trail.png?v=${eventAssetVersion}`,
};
const petSkinMeta = {
  FIRE_FOX: { key: "FIRE_FOX", name: "해빛냥", image: "/assets/pet-flarefox.svg", attack: "/assets/pet-flarefox-attack.svg", copy: "불꽃 발톱으로 추가 공격" },
  ICE: { key: "ICE", name: "물방울토", image: "/assets/pet-aquabun.svg", attack: "/assets/pet-aquabun-attack.svg", copy: "물방울 탄환으로 추가 공격" },
  FIRE_FOX_SKIN: { key: "FIRE_FOX_SKIN", name: "불꽃여우", image: `/assets/pets/pet-fire-fox.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-fire-fox.png?v=${petAssetVersion}`, copy: "화염 꼬리로 추가 공격" },
  ICE_SLIME: { key: "ICE_SLIME", name: "얼음몽", image: `/assets/pets/pet-ice.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-ice.png?v=${petAssetVersion}`, copy: "빙결 결정으로 추가 공격" },
  BIRD: { key: "BIRD", name: "번개삐오", image: `/assets/pets/pet-bird.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-bird.png?v=${petAssetVersion}`, copy: "번개 깃털로 추가 공격" },
  GREEN_TURTLE: { key: "GREEN_TURTLE", name: "초록거북", image: `/assets/pets/pet-green-tutle.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-green-tutle.png?v=${petAssetVersion}`, copy: "숲의 기운으로 추가 공격" },
  EASTER_EGG_JUNEON: { key: "EASTER_EGG_JUNEON", name: "준언", image: `/assets/pets/easter-egg-juneon.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-fire-fox.png?v=${petAssetVersion}`, copy: "숨겨진 불꽃 장난", easterEgg: true },
  EASTER_EGG_EULGIN: { key: "EASTER_EGG_EULGIN", name: "을진", image: `/assets/pets/easter-egg-eulgin.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-fire-fox.png?v=${petAssetVersion}`, copy: "숨겨진 불꽃 장난", easterEgg: true },
  EASTER_EGG_GYUDONG: { key: "EASTER_EGG_GYUDONG", name: "규동", image: `/assets/pets/easter-egg-gyudong.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-fire-fox.png?v=${petAssetVersion}`, copy: "숨겨진 불꽃 장난", easterEgg: true },
  EASTER_EGG_MINGYU: { key: "EASTER_EGG_MINGYU", name: "민규", image: `/assets/pets/easter-egg-mingyu.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-fire-fox.png?v=${petAssetVersion}`, copy: "숨겨진 불꽃 장난", easterEgg: true },
  EASTER_EGG_JAESEO: { key: "EASTER_EGG_JAESEO", name: "재서", image: `/assets/pets/easter-egg-jaeseo.png?v=${petAssetVersion}`, attack: `/assets/pets/pet-skill-fire-fox.png?v=${petAssetVersion}`, copy: "숨겨진 불꽃 장난", easterEgg: true },
};
const petSkinOrder = ["FIRE_FOX", "ICE", "FIRE_FOX_SKIN", "ICE_SLIME", "BIRD", "GREEN_TURTLE", "EASTER_EGG_JUNEON", "EASTER_EGG_EULGIN", "EASTER_EGG_GYUDONG", "EASTER_EGG_MINGYU", "EASTER_EGG_JAESEO"];
const petMeta = [
  { key: "flare", skill: "PET_FLARE_ATTACK", defaultSkin: "FIRE_FOX", imageId: "petFlare", shopImageId: "petOneShopImage", shopNameId: "petOneShopName", shopCopyId: "petOneShopCopy", statusId: "petOneStatus" },
  { key: "aqua", skill: "PET_AQUA_ATTACK", defaultSkin: "ICE", imageId: "petAqua", shopImageId: "petTwoShopImage", shopNameId: "petTwoShopName", shopCopyId: "petTwoShopCopy", statusId: "petTwoStatus" },
];

const effectAssetVersion = "20260605-07";
const goldPerTossPoint = 100;
const companionPriceWon = 4900;
const skillPointPackPriceWon = 999;
const skillPointPackAmount = 10;
const friendInviteRewardSkillPoints = 1;
const friendInviteLimit = 5;
const battleBackgrounds = [
  "/assets/background-options/combat-background-4.png?v=20260605-01",
  "/assets/background-options/combat-background-3.png?v=20260605-01",
  "/assets/background-options/combat-background-1.png?v=20260605-01",
  "/assets/background-options/combat-background-2.png?v=20260605-01",
];
const battleBackgroundStorageKey = "moneyHunterBattleBackgroundIndex";
const monsterSignatureStorageKey = "moneyHunterMonsterSignature";
const dummyBannerStorageKey = "moneyHunterShowDummyBanner";
const bgmMutedStorageKey = "moneyHunterBgmMuted";
const guestUserKeyStorageKey = "moneyHunterGuestUserKey";
const authTokenStorageKey = "moneyHunterAuthToken";
const notificationAgreementStatusStorageKey = "moneyHunter.autoHuntNotificationAgreementStatus";
const rookieEventMissionNotificationAgreementStatusStorageKey = "moneyHunter.rookieEventMissionNotificationAgreementStatus";
const rookieEventButtonSeenDateStoragePrefix = "moneyHunter.rookieEventButtonSeenDate";
const gameProfileWebviewUrl = "servicetoss://game-center/profile";
const appsInTossSdkUrl = "https://cdn.jsdelivr.net/npm/@apps-in-toss/web-framework@2.5.0/+esm";
const appsInTossBridgeUrl = "https://cdn.jsdelivr.net/npm/@apps-in-toss/web-bridge@2.5.0/dist/bridge.js/+esm";
const productionApiBaseUrl = "https://money-hunter-prod-4qddpaimyq-du.a.run.app";
const realFullScreenAdGroupKeys = ["autoHunt", "rewardClaim", "dungeonAdditional", "jobChange"];
const realAdPreloadRetryMs = 30000;
const realAdCooldownPreloadLeadMs = 20000;
const realAdLoadedTtlMs = 120000;
let tossSdkPromise = null;
let tossBridgePromise = null;
let bannerAdHandle = null;
let bannerAdLoading = false;

function apiBaseUrl() {
  const configured = String(window.MONEY_HUNTER_API_BASE_URL || "").trim();
  if (configured) {
    return configured.replace(/\/$/, "");
  }
  if (window.location.hostname.endsWith("tossmini.com")) {
    return productionApiBaseUrl;
  }
  return "";
}

function apiUrl(path) {
  if (/^https?:\/\//.test(path)) {
    return path;
  }
  return `${apiBaseUrl()}${path}`;
}

const statSkillByJob = {
  WARRIOR: "STRENGTH",
  ARCHER: "DEXTERITY",
  MAGE: "INTELLIGENCE",
  ROGUE: "LUCK",
};

const skillMaxLevel = 30;
const skillEffectMaxTier = 5;
const skillEffectTierStartLevels = [0, 6, 11, 16, 21];

const skillMeta = {
  STRENGTH: { label: "STR 전사", short: "전사 공격", step: 80 / skillMaxLevel, max: skillMaxLevel, effectPrefix: "str" },
  DEXTERITY: { label: "DEX 궁수", short: "궁수 집중", step: 80 / skillMaxLevel, max: skillMaxLevel, effectPrefix: "dex" },
  INTELLIGENCE: { label: "INT 마법사", short: "마법 증폭", step: 80 / skillMaxLevel, max: skillMaxLevel, effectPrefix: "int" },
  LUCK: { label: "LUK 도적", short: "행운 타격", step: 80 / skillMaxLevel, max: skillMaxLevel, effectPrefix: "luk" },
  MINING_MASTERY: { label: "채굴 숙련", short: "채굴량", step: 100 / skillMaxLevel, max: skillMaxLevel, description: "골드 획득량이 증가해요" },
  RAPID_ATTACK: { label: "연속 공격", short: "공격 효율", step: 60 / skillMaxLevel, max: skillMaxLevel, description: "공격 간격이 짧아져요" },
  REWARD_AMPLIFIER: { label: "보상 증폭", short: "게이지 효율", step: 40 / skillMaxLevel, max: skillMaxLevel, description: "보상 게이지 효율이 좋아져요" },
  PET_FLARE_ATTACK: { label: "해빛냥 공격", short: "해빛냥 공격력", max: skillMaxLevel, maxDamage: 40, petIndex: 1, description: "해빛냥 공격 피해량이 올라가요" },
  PET_AQUA_ATTACK: { label: "물방울토 공격", short: "물방울토 공격력", max: skillMaxLevel, maxDamage: 40, petIndex: 2, description: "물방울토 공격 피해량이 올라가요" },
};

const featureTutorialSteps = [
  {
    target: ".battle-screen",
    title: "자동 전투",
    body: "사냥은 자동으로 진행돼요. 몬스터를 처치하면 골드와 경험치를 얻고, 충전된 자동사냥 시간은 앱을 꺼도 이어져요.",
  },
  {
    target: ".action-row",
    title: "보상 버튼",
    body: "전투력과 시간당 골드를 확인하고 자동사냥 시간을 충전해요.",
  },
  {
    target: "#skillPanel .upgrade-row:not(.hidden)",
    panel: "skill",
    title: "헌터 성장",
    body: "SP로 능력치를 강화해요. 직업별 핵심 능력치는 공유되고, 전용 스킬을 올리면 공격 이펙트도 더 화려해져요.",
  },
  {
    target: "#shopPanel",
    panel: "shop",
    title: "상점과 동료 펫",
    body: "동료 펫을 데려오면 함께 공격하고 수익률이 올라가요. 헌터 외형 변경도 이 화면에서 할 수 있어요.",
  },
  {
    target: "#adventurePanel",
    panel: "adventure",
    title: "모험",
    body: "던전은 기본 입장과 광고 추가 입장을 사용할 수 있고, 보스 입장권은 던전 보상에서 확률적으로 얻어요.",
  },
  {
    target: "#rewardPanel",
    panel: "reward",
    title: "보상 수령",
    body: "모은 골드는 포인트 기준으로 환산돼요. 수령 가능 상태가 되면 토스포인트로 받을 수 있어요.",
  },
  {
    target: ".panel-tabs",
    title: "하단 메뉴",
    body: "스킬, 모험, 상점, 보상 수령을 아래 메뉴에서 빠르게 오갈 수 있어요.",
  },
  {
    target: "#muteToggle",
    title: "사운드",
    body: "BGM이나 타격음이 부담스러우면 오른쪽 위 버튼으로 바로 끌 수 있어요.",
  },
];

const homeShortcutGuideSteps = [
  {
    title: "... 메뉴 열기",
    body: "토스 화면 오른쪽 위의 ... 메뉴를 눌러요.",
    preview: "menu",
  },
  {
    title: "휴대폰 홈 화면에 추가 선택",
    body: "메뉴에서 휴대폰 홈 화면에 추가를 선택해요.",
    preview: "select",
  },
  {
    title: "홈 화면에서 들어오기",
    body: "추가된 머니헌터 아이콘으로 다시 들어오면 미션이 확인돼요.",
    preview: "home",
  },
];

function activeJob() {
  return state.player?.job || state.selectedJob || "WARRIOR";
}

function activeStatSkill() {
  return statSkillByJob[activeJob()] || "STRENGTH";
}

function storedValue(key) {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function storeValue(key, value) {
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // Local storage can be unavailable in embedded test environments.
  }
}

function removeStoredValue(key) {
  try {
    window.localStorage.removeItem(key);
  } catch {
    // Local storage can be unavailable in embedded test environments.
  }
}

function normalizedEntryPath(path = window.location.pathname) {
  let normalized = String(path || "").trim();
  if (!normalized) {
    return "/";
  }
  const queryIndex = normalized.indexOf("?");
  if (queryIndex >= 0) {
    normalized = normalized.slice(0, queryIndex);
  }
  const hashIndex = normalized.indexOf("#");
  if (hashIndex >= 0) {
    normalized = normalized.slice(0, hashIndex);
  }
  if (!normalized.startsWith("/")) {
    normalized = `/${normalized}`;
  }
  while (normalized.length > 1 && normalized.endsWith("/")) {
    normalized = normalized.slice(0, -1);
  }
  return normalized;
}

function isBenefitTabEntryPath(path = window.location.pathname) {
  return normalizedEntryPath(path) === benefitTabEntryPath;
}

function markBenefitTabEntryIfNeeded() {
  if (isBenefitTabEntryPath()) {
    storeValue(benefitTabEntryStorageKey, "1");
  }
}

function benefitTabEntryPathForLogin() {
  return storedValue(benefitTabEntryStorageKey) === "1" || isBenefitTabEntryPath()
    ? benefitTabEntryPath
    : "";
}

function clearBenefitTabEntryMarker() {
  removeStoredValue(benefitTabEntryStorageKey);
}

function localDateKey(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function createGuestUserKey() {
  return clientRequestId("guest");
}

function clientRequestId(prefix = "client") {
  let randomValue = "";
  try {
    randomValue = window.crypto?.randomUUID?.() || "";
  } catch {
    randomValue = "";
  }
  if (!randomValue) {
    randomValue = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
  }
  return `${prefix}-${randomValue}`;
}

function guestUserKey() {
  const saved = storedValue(guestUserKeyStorageKey);
  if (saved) {
    return saved;
  }
  const next = createGuestUserKey();
  storeValue(guestUserKeyStorageKey, next);
  return next;
}

function guestRequestHeaders() {
  if (!isOneStoreTarget()) {
    return {};
  }
  return {
    "X-Money-Hunter-Distribution-Target": "ONESTORE",
    "X-Money-Hunter-Guest-Key": guestUserKey(),
  };
}

function authRequestHeaders(skipAuth = false) {
  if (skipAuth || isOneStoreTarget()) {
    return {};
  }
  if (!state.authToken) {
    return {};
  }
  return {
    Authorization: `Bearer ${state.authToken}`,
  };
}

function initialBattleBackgroundIndex() {
  const saved = Number(storedValue(battleBackgroundStorageKey));
  if (Number.isInteger(saved) && saved >= 0 && saved < battleBackgrounds.length) {
    return saved;
  }
  const next = Math.floor(Math.random() * battleBackgrounds.length);
  storeValue(battleBackgroundStorageKey, String(next));
  return next;
}

state.battleBackgroundIndex = initialBattleBackgroundIndex();
state.lastMonsterSignature = storedValue(monsterSignatureStorageKey) || "";
state.showDummyBanner = storedValue(dummyBannerStorageKey) === "true";
state.bgmMuted = storedValue(bgmMutedStorageKey) === "true";
state.authToken = storedValue(authTokenStorageKey) || "";

function normalizedDistributionTarget(value) {
  return String(value || "").trim().toUpperCase() === "ONESTORE" ? "ONESTORE" : "TOSS";
}

function isOneStoreTarget() {
  return state.distributionTarget === "ONESTORE";
}

function isTossMiniRuntime() {
  return window.location.hostname.endsWith("tossmini.com")
    || Boolean(window.MoneyHunterTossSdk)
    || Boolean(window.__appsInToss);
}

function rewardGateLabel() {
  return isOneStoreTarget() ? "게임 내 보상" : "광고 시청";
}

function shouldUseTossLogin() {
  return !isOneStoreTarget() && state.tossLoginEnabled && !state.mockMonetizationEnabled;
}

function shouldUseRealFullScreenAds() {
  return !isOneStoreTarget() && state.realRewardAdsEnabled && !state.mockMonetizationEnabled;
}

function shouldUseRealBannerAd() {
  return !isOneStoreTarget() && state.realBannerAdsEnabled && !state.mockMonetizationEnabled;
}

function shouldUseRealPayments() {
  return !isOneStoreTarget() && state.realPaymentsEnabled && !state.mockMonetizationEnabled;
}

function isReviewIapSandboxMode() {
  return state.integrationMode === "review-iap-sandbox";
}

function shouldUseRealShareReward() {
  return !isOneStoreTarget() && state.realShareRewardEnabled && !state.mockMonetizationEnabled;
}

function adGroupId(key) {
  return String(state.adGroupIds?.[key] || "").trim();
}

function configuredRealFullScreenAdGroupIds() {
  return Array.from(new Set(realFullScreenAdGroupKeys.map(adGroupId).filter(Boolean)));
}

function realAdPreloadEntry(groupId) {
  let entry = state.realAdPreloads.get(groupId);
  if (!entry) {
    entry = {
      status: "idle",
      promise: null,
      loadedAt: 0,
      failedAt: 0,
      error: null,
    };
    state.realAdPreloads.set(groupId, entry);
  }
  return entry;
}

function isRealFullScreenAdLoaded(groupId) {
  const entry = realAdPreloadEntry(groupId);
  if (entry.status !== "loaded") {
    return false;
  }
  return Date.now() - entry.loadedAt < realAdLoadedTtlMs;
}

function preloadRealFullScreenAd(groupId) {
  if (!groupId || !shouldUseRealFullScreenAds() || !state.pageVisible) {
    return;
  }
  const entry = realAdPreloadEntry(groupId);
  if (entry.status === "loaded" && Date.now() - entry.loadedAt < realAdLoadedTtlMs) {
    return;
  }
  if (entry.status === "loaded") {
    markRealFullScreenAdConsumed(groupId);
  }
  if (entry.status === "loading") {
    return;
  }
  if (entry.status === "failed" && Date.now() - entry.failedAt < realAdPreloadRetryMs) {
    return;
  }
  loadRealFullScreenAd(groupId).catch(() => {});
}

function clearRealFullScreenAdPreloadTimers() {
  state.realAdPreloadTimers.forEach((timer) => window.clearTimeout(timer));
  state.realAdPreloadTimers.clear();
}

function scheduleRealFullScreenAdPreload(groupId, delayMs = 0) {
  if (!groupId) {
    return;
  }
  const previousTimer = state.realAdPreloadTimers.get(groupId);
  if (previousTimer) {
    window.clearTimeout(previousTimer);
    state.realAdPreloadTimers.delete(groupId);
  }
  if (delayMs <= 0) {
    preloadRealFullScreenAd(groupId);
    return;
  }
  const timer = window.setTimeout(() => {
    state.realAdPreloadTimers.delete(groupId);
    preloadRealFullScreenAd(groupId);
  }, delayMs);
  state.realAdPreloadTimers.set(groupId, timer);
}

function realAdCooldownPreloadDelay(availableAt) {
  if (!availableAt || !isActive(availableAt)) {
    return 0;
  }
  const availableTime = new Date(availableAt).getTime();
  if (Number.isNaN(availableTime)) {
    return 0;
  }
  return Math.max(0, availableTime - Date.now() - realAdCooldownPreloadLeadMs);
}

function scheduleRealFullScreenAdPreloads(player = state.player) {
  clearRealFullScreenAdPreloadTimers();
  if (!shouldUseRealFullScreenAds()) {
    return;
  }
  const scheduledGroupIds = new Set();
  const schedule = (groupKey, availableAt = null, enabled = true) => {
    const groupId = adGroupId(groupKey);
    if (!enabled || !groupId || scheduledGroupIds.has(groupId)) {
      return;
    }
    scheduledGroupIds.add(groupId);
    scheduleRealFullScreenAdPreload(groupId, realAdCooldownPreloadDelay(availableAt));
  };
  schedule("autoHunt", player?.nextAutoHuntAdAvailableAt);
  const dungeon = player?.dungeonCoupon || {};
  const dungeonRunsToday = dungeon.dungeonRunsToday || 0;
  const dungeonFreeLimit = dungeon.dungeonFreeDailyLimit ?? dungeonFreeDailyLimit;
  const dungeonRemainingRuns = Math.max(0, dungeon.dungeonRemainingRuns ?? ((dungeon.dungeonDailyLimit || 5) - dungeonRunsToday));
  const dungeonCooldownSeconds = dungeon.dungeonNextAvailableAt
    ? remainingSecondsFrom(dungeon.dungeonNextAvailableAt)
    : Math.max(0, dungeon.dungeonCooldownSeconds || 0);
  const dungeonAvailable = Boolean(dungeon.dungeonAvailable);
  schedule("dungeonAdditional", null, Boolean(dungeon.enabled) && dungeonRunsToday >= dungeonFreeLimit && dungeonRemainingRuns > 0 && dungeonCooldownSeconds <= 0 && dungeonAvailable);
  schedule("rewardClaim");
  schedule("jobChange");
  configuredRealFullScreenAdGroupIds()
    .filter((groupId) => !scheduledGroupIds.has(groupId))
    .forEach((groupId) => scheduleRealFullScreenAdPreload(groupId));
}

function markRealFullScreenAdConsumed(groupId) {
  const entry = realAdPreloadEntry(groupId);
  entry.status = "idle";
  entry.promise = null;
  entry.loadedAt = 0;
  entry.error = null;
}

function iapProductId(key) {
  return String(state.iapProductIds?.[key] || "").trim();
}

function runRewardFlow(title, description, action) {
  if (isOneStoreTarget()) {
    if (typeof action.afterAd === "function") {
      return action.afterAd(null);
    }
    return run(action.request, action.message);
  }
  if (shouldUseRealFullScreenAds()) {
    return runRealFullScreenAd(title, description, action);
  }
  return showDummyAd(title, description, action);
}

function runPurchaseFlow(action) {
  if (isOneStoreTarget()) {
    return run(action.request, action.message);
  }
  if (shouldUseRealPayments()) {
    return runRealIapPurchase(action);
  }
  return showDummyPayment(action);
}

function setMessage(message, durationMs = 4200) {
  state.noticeMessage = message;
  state.noticeExpiresAt = Date.now() + durationMs;
  renderBattleTip(isActive(state.player?.autoHuntEndsAt));
  window.clearTimeout(state.noticeTimer);
  state.noticeTimer = window.setTimeout(() => {
    if (Date.now() >= state.noticeExpiresAt) {
      state.noticeMessage = "";
      renderBattleTip(isActive(state.player?.autoHuntEndsAt));
    }
  }, durationMs + 80);
}

function maybeStartFeatureTutorial() {
  if (
    state.featureTutorialStarted
    || state.featureTutorialActive
    || state.player?.featureTutorialRequired !== true
    || !state.player?.job
    || state.player.onboardingRequired
  ) {
    return;
  }
  state.featureTutorialStarted = true;
  window.setTimeout(() => startFeatureTutorial(), 520);
}

function startFeatureTutorial(index = 0) {
  if (!state.player?.job || state.player.onboardingRequired || state.player.featureTutorialRequired !== true) {
    return;
  }
  state.featureTutorialActive = true;
  state.featureTutorialIndex = index;
  $("featureTutorial").classList.remove("hidden");
  document.body.classList.add("feature-tutorial-open");
  renderFeatureTutorialStep();
}

async function finishFeatureTutorial() {
  state.featureTutorialActive = false;
  state.featureTutorialStarted = true;
  window.clearTimeout(state.featureTutorialPositionTimer);
  $("featureTutorial").classList.add("hidden");
  $("featureTutorial").classList.remove("is-positioning");
  document.body.classList.remove("feature-tutorial-open");
  showContentPanel("skill");
  try {
    setServerPlayer(await api("/api/player/tutorial/feature/complete", { method: "POST" }));
    render();
    setMessage("튜토리얼을 완료했어요. SP 1개를 받았어요.");
    scheduleGameProfileSync(800);
    window.setTimeout(() => maybeResumeDeferredGameProfileCreation(), 350);
  } catch (error) {
    setMessage(error.message || "튜토리얼 완료 저장에 실패했어요.");
  }
}

function nextFeatureTutorialStep() {
  if (state.featureTutorialIndex >= featureTutorialSteps.length - 1) {
    finishFeatureTutorial();
    return;
  }
  state.featureTutorialIndex += 1;
  renderFeatureTutorialStep();
}

function renderFeatureTutorialStep() {
  if (!state.featureTutorialActive) {
    return;
  }
  const step = featureTutorialSteps[state.featureTutorialIndex];
  if (!step) {
    finishFeatureTutorial();
    return;
  }
  if (step.panel) {
    showContentPanel(step.panel);
  }
  $("tutorialCount").textContent = `${state.featureTutorialIndex + 1} / ${featureTutorialSteps.length}`;
  $("tutorialTitle").textContent = step.title;
  $("tutorialBody").textContent = step.body;
  $("tutorialNext").textContent = state.featureTutorialIndex >= featureTutorialSteps.length - 1 ? "시작하기" : "다음";
  $("featureTutorial").classList.add("is-positioning");
  window.clearTimeout(state.featureTutorialPositionTimer);
  state.featureTutorialPositionTimer = window.setTimeout(positionFeatureTutorial, 90);
}

function positionFeatureTutorial() {
  if (!state.featureTutorialActive) {
    return;
  }
  const step = featureTutorialSteps[state.featureTutorialIndex];
  const target = document.querySelector(step.target);
  if (!target) {
    nextFeatureTutorialStep();
    return;
  }
  const initialRect = target.getBoundingClientRect();
  const alreadyVisible = initialRect.top >= 12 && initialRect.bottom <= window.innerHeight - 12;
  if (!alreadyVisible) {
    target.scrollIntoView({ block: "center", inline: "nearest", behavior: "smooth" });
  }
  window.clearTimeout(state.featureTutorialPositionTimer);
  state.featureTutorialPositionTimer = window.setTimeout(() => {
    const rect = target.getBoundingClientRect();
    if (!rect.width || !rect.height) {
      nextFeatureTutorialStep();
      return;
    }
    const pad = 8;
    const spotlight = $("tutorialSpotlight");
    spotlight.style.left = `${Math.max(8, rect.left - pad)}px`;
    spotlight.style.top = `${Math.max(8, rect.top - pad)}px`;
    spotlight.style.width = `${Math.min(window.innerWidth - 16, rect.width + pad * 2)}px`;
    spotlight.style.height = `${Math.min(window.innerHeight - 16, rect.height + pad * 2)}px`;

    const card = $("tutorialCard");
    const cardWidth = Math.min(360, window.innerWidth - 28);
    card.style.width = `${cardWidth}px`;
    const cardHeight = card.offsetHeight || 190;
    const targetCenter = rect.left + rect.width / 2;
    const left = Math.min(
      Math.max(14, targetCenter - cardWidth / 2),
      Math.max(14, window.innerWidth - cardWidth - 14)
    );
    const belowTop = rect.bottom + 14;
    const aboveTop = rect.top - cardHeight - 14;
    const top = belowTop + cardHeight < window.innerHeight - 14
      ? belowTop
      : Math.max(14, aboveTop);
    card.style.left = `${left}px`;
    card.style.top = `${top}px`;
    $("featureTutorial").classList.remove("is-positioning");
  }, alreadyVisible ? 90 : 280);
}

function syncBgmState() {
  const audio = $("bgmAudio");
  audio.volume = 0.35;
  audio.muted = !canPlaySound();
  $("muteToggle").classList.toggle("is-muted", state.bgmMuted);
  $("muteToggle").textContent = state.bgmMuted ? "♪" : "♫";
  $("muteToggle").setAttribute("aria-label", state.bgmMuted ? "BGM 켜기" : "BGM 음소거");
}

function canPlaySound() {
  return !state.bgmMuted && state.pageVisible;
}

function resumeAudioContext() {
  state.soundContext?.resume?.().catch(() => {});
}

function playBgmWithRetry(audio, retry = false) {
  const playPromise = audio.play?.();
  if (playPromise?.then) {
    playPromise
      .then(() => {
        state.bgmStarted = true;
      })
      .catch(() => {
        if (retry) {
          window.setTimeout(() => audio.play?.().catch?.(() => {}), 220);
        }
      });
    return;
  }
  state.bgmStarted = true;
}

function startBgm(options = {}) {
  syncBgmState();
  if (!canPlaySound()) {
    return;
  }
  const audio = $("bgmAudio");
  if (state.bgmStarted && !audio.paused) {
    return;
  }
  resumeAudioContext();
  playBgmWithRetry(audio, Boolean(options.retry));
}

function toggleBgmMute() {
  state.bgmMuted = !state.bgmMuted;
  storeValue(bgmMutedStorageKey, String(state.bgmMuted));
  syncBgmState();
  if (!state.bgmMuted) {
    state.bgmStarted = false;
    startBgm();
  }
}

function syncPageSoundState(visible = !document.hidden) {
  state.pageVisible = visible;
  syncBgmState();
  const audio = $("bgmAudio");
  if (canPlaySound()) {
    resumeAudioContext();
    if (state.bgmStarted) {
      startBgm({ retry: true });
    }
    return;
  }
  audio.pause?.();
  state.soundContext?.suspend?.().catch(() => {});
}

function handlePageVisibility(visible = !document.hidden) {
  syncPageSoundState(visible);
  if (!visible || !state.player || state.resumeRefreshInFlight) {
    return;
  }
  schedulePendingIapRestoreRetries();
  state.resumeRefreshInFlight = true;
  refresh()
    .catch((error) => setMessage(error.message))
    .finally(() => {
      restoreBattleVisualAssets();
      scheduleRealFullScreenAdPreloads();
      state.resumeRefreshInFlight = false;
    });
}

function audioContext() {
  const AudioContextType = window.AudioContext || window.webkitAudioContext;
  if (!AudioContextType) {
    return null;
  }
  if (!state.soundContext) {
    state.soundContext = new AudioContextType();
  }
  return state.soundContext;
}

function playMonsterHitSound(defeated = false) {
  if (!canPlaySound()) {
    return;
  }
  const ctx = audioContext();
  if (!ctx) {
    return;
  }
  if (ctx.state === "suspended") {
    ctx.resume?.().catch(() => {});
  }
  const now = ctx.currentTime;
  const duration = defeated ? 0.22 : 0.14;
  const master = ctx.createGain();
  master.gain.setValueAtTime(0.001, now);
  master.gain.exponentialRampToValueAtTime(defeated ? 0.22 : 0.14, now + 0.012);
  master.gain.exponentialRampToValueAtTime(0.001, now + duration);
  master.connect(ctx.destination);

  const thump = ctx.createOscillator();
  thump.type = "triangle";
  thump.frequency.setValueAtTime(defeated ? 118 : 176, now);
  thump.frequency.exponentialRampToValueAtTime(defeated ? 56 : 92, now + duration);
  thump.connect(master);
  thump.start(now);
  thump.stop(now + duration);

  const noiseLength = Math.max(1, Math.floor(ctx.sampleRate * duration));
  const buffer = ctx.createBuffer(1, noiseLength, ctx.sampleRate);
  const samples = buffer.getChannelData(0);
  for (let i = 0; i < noiseLength; i += 1) {
    samples[i] = (Math.random() * 2 - 1) * (1 - i / noiseLength);
  }
  const noise = ctx.createBufferSource();
  const filter = ctx.createBiquadFilter();
  filter.type = "bandpass";
  filter.frequency.setValueAtTime(defeated ? 520 : 720, now);
  filter.Q.setValueAtTime(defeated ? 0.9 : 1.5, now);
  noise.buffer = buffer;
  noise.connect(filter);
  filter.connect(master);
  noise.start(now);
  noise.stop(now + duration);
  window.setTimeout(() => master.disconnect(), (duration + 0.08) * 1000);
}

function authErrorMessage(message = "") {
  const normalized = String(message || "").toLowerCase();
  if (normalized.includes("expired")) {
    return "로그인 세션이 만료됐어요. 다시 로그인해 주세요.";
  }
  return "토스 로그인이 필요해요.";
}

async function api(path, options = {}) {
  const { skipAuth = false, ...fetchOptions } = options;
  const response = await fetch(apiUrl(path), {
    ...fetchOptions,
    headers: {
      "Content-Type": "application/json",
      ...guestRequestHeaders(),
      ...authRequestHeaders(skipAuth),
      ...(fetchOptions.headers || {}),
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: "요청에 실패했어요." }));
    if (response.status === 401 && shouldUseTossLogin()) {
      const message = authErrorMessage(body.message);
      clearAuthToken();
      showLoginGate(message);
      const error = new Error(message);
      error.requiresLogin = true;
      throw error;
    }
    throw new Error(body.message || "요청에 실패했어요.");
  }
  return response.json();
}

async function refresh() {
  applyServerPlayer(await api("/api/player"));
  await completeRookieHomeShortcutMissionIfNeeded();
  hideLoginGate();
  if (state.player.job) {
    state.selectedJob = state.player.job;
  }
  render();
  scheduleGameProfileSync();
  scheduleLeaderboardEntryScoreSubmit();
}

function applyServerPlayer(player, { resetDisplayGold = false } = {}) {
  const previousPlayer = state.player;
  const ratesChanged = combatRatesChanged(previousPlayer, player);
  const shouldPromptNotificationAgreement = shouldPromptAutoHuntNotificationAgreement(previousPlayer, player);
  const previousServerGold = state.lastServerGold ?? previousPlayer?.gold ?? player.gold;
  state.player = player;
  const now = Date.now();
  state.lastCombatSyncAt = now;
  state.lastLocalGoldEstimateAt = now;
  const serverGoldGained = Math.max(0, Math.floor(player.gold - previousServerGold));
  state.lastSettlementGold = serverGoldGained;
  state.lastServerGold = player.gold;
  if (ratesChanged && isActiveAt(player.autoHuntEndsAt, now - 1)) {
    state.lastHitAt = Math.min(state.lastHitAt, now - attackIntervalMillis(player));
  }
  state.displayGold = resetDisplayGold ? player.gold : Math.max(state.displayGold, player.gold);
  syncLocalCombatView(player, Boolean(previousPlayer) && !resetDisplayGold);
  if (shouldPromptNotificationAgreement) {
    window.setTimeout(() => requestAutoHuntNotificationAgreementIfNeeded(), 450);
  }
  scheduleRealFullScreenAdPreloads(player);
}

function resetLocalCombatView(player) {
  state.localMonster = cloneMonster(player?.monster);
  state.localLevel = player?.level ?? null;
  state.localExperience = player?.experience ?? null;
  state.localNextLevelExperience = player?.nextLevelExperience ?? null;
}

function syncLocalCombatView(player, preserveAhead = false) {
  if (!preserveAhead) {
    resetLocalCombatView(player);
    return;
  }
  const serverMonster = cloneMonster(player?.monster);
  if (!serverMonster) {
    state.localMonster = null;
  } else if (!state.localMonster) {
    state.localMonster = serverMonster;
  } else if ((state.localMonster.defeatedMonsters || 0) > (serverMonster.defeatedMonsters || 0)) {
    state.localMonster = {
      ...state.localMonster,
      maxHp: Math.max(state.localMonster.maxHp || 0, serverMonster.maxHp || 0),
    };
  } else if (
    state.localMonster.key === serverMonster.key
    && (state.localMonster.defeatedMonsters || 0) === (serverMonster.defeatedMonsters || 0)
  ) {
    state.localMonster = {
      ...serverMonster,
      hp: Math.min(state.localMonster.hp ?? serverMonster.hp, serverMonster.hp),
    };
  } else {
    state.localMonster = serverMonster;
  }

  const serverLevel = player?.level ?? 1;
  const serverExperience = player?.experience ?? 0;
  const serverNext = player?.nextLevelExperience ?? nextLevelExperience(serverLevel);
  if ((state.localLevel ?? 0) > serverLevel) {
    state.localNextLevelExperience = state.localNextLevelExperience ?? nextLevelExperience(state.localLevel);
    return;
  }
  if ((state.localLevel ?? 0) === serverLevel) {
    state.localExperience = Math.max(state.localExperience ?? serverExperience, serverExperience);
    state.localNextLevelExperience = serverNext;
    return;
  }
  state.localLevel = serverLevel;
  state.localExperience = serverExperience;
  state.localNextLevelExperience = serverNext;
}

function cloneMonster(monster) {
  if (!monster) {
    return null;
  }
  return { ...monster };
}

function setServerPlayer(player, options = {}) {
  applyServerPlayer(player, options);
  if (state.player?.job) {
    state.selectedJob = state.player.job;
  }
}

function combatRatesChanged(previousPlayer, nextPlayer) {
  if (!previousPlayer || !nextPlayer) {
    return false;
  }
	  return previousPlayer.baseGoldPerHour !== nextPlayer.baseGoldPerHour
	    || previousPlayer.normalAttackIntervalMillis !== nextPlayer.normalAttackIntervalMillis
	    || previousPlayer.goldPerHour !== nextPlayer.goldPerHour;
}

function setAuthToken(token) {
  state.authToken = token || "";
  if (state.authToken) {
    storeValue(authTokenStorageKey, state.authToken);
  }
}

function clearAuthToken() {
  state.authToken = "";
  removeStoredValue(authTokenStorageKey);
}

function showLoginGate(message = "") {
  if (isOneStoreTarget()) {
    return;
  }
  $("loginModal").classList.remove("hidden");
  $("loginError").textContent = message;
}

function hideLoginGate() {
  $("loginModal").classList.add("hidden");
  $("loginError").textContent = "";
}

async function requestTossAuthorization() {
  if (window.MoneyHunterTossLogin?.appLogin) {
    return window.MoneyHunterTossLogin.appLogin();
  }
  const sdk = await loadTossSdk();
  if (typeof sdk.appLogin !== "function") {
    throw new Error("토스 로그인 SDK를 찾지 못했어요.");
  }
  return sdk.appLogin();
}

async function loadTossSdk() {
  if (window.MoneyHunterTossSdk) {
    return window.MoneyHunterTossSdk;
  }
  if (!tossSdkPromise) {
    tossSdkPromise = import(appsInTossSdkUrl);
  }
  return tossSdkPromise;
}

async function loadTossBridge() {
  if (!tossBridgePromise) {
    tossBridgePromise = import(appsInTossBridgeUrl);
  }
  return tossBridgePromise;
}

function shouldUseRealSmartMessage() {
  return !isOneStoreTarget() && !state.mockMonetizationEnabled && state.realSmartMessageEnabled;
}

function notificationAgreementStatus(storageKey = notificationAgreementStatusStorageKey) {
  return storedValue(storageKey) || "";
}

function shouldPromptAutoHuntNotificationAgreement(previousPlayer, nextPlayer) {
  if (!nextPlayer?.autoHuntEndsAt || !isActiveAt(nextPlayer.autoHuntEndsAt, Date.now() - 1)) {
    return false;
  }
  return previousPlayer?.autoHuntEndsAt !== nextPlayer.autoHuntEndsAt;
}

function shouldRequestAutoHuntNotificationAgreement() {
  if (!shouldUseRealSmartMessage() || !state.notificationAgreementTemplateCode || state.notificationAgreementInFlight) {
    return false;
  }
  const status = notificationAgreementStatus(notificationAgreementStatusStorageKey);
  return status !== "agreed" && status !== "rejected";
}

async function requestAutoHuntNotificationAgreementIfNeeded() {
  if (!shouldRequestAutoHuntNotificationAgreement()) {
    return;
  }
  await requestNotificationAgreement(state.notificationAgreementTemplateCode, notificationAgreementStatusStorageKey);
}

async function requestNotificationAgreement(templateCode, storageKey) {
  if (!shouldUseRealSmartMessage() || !templateCode || state.notificationAgreementInFlight) {
    return false;
  }
  state.notificationAgreementInFlight = true;
  try {
    const sdk = await loadTossSdk();
    if (typeof sdk.requestNotificationAgreement !== "function") {
      return false;
    }
    await new Promise((resolve, reject) => {
      let cleanup = null;
      let settled = false;
      const settle = (callback, value) => {
        if (settled) {
          return;
        }
        settled = true;
        cleanup?.();
        callback(value);
      };
      cleanup = sdk.requestNotificationAgreement({
        options: { templateCode },
        onEvent: (event) => {
          if (event?.type === "newAgreement" || event?.type === "alreadyAgreed") {
            storeValue(storageKey, "agreed");
          }
          if (event?.type === "agreementRejected") {
            storeValue(storageKey, "rejected");
          }
          settle(resolve);
        },
        onError: (error) => {
          settle(reject, error instanceof Error ? error : new Error("알림 동의 요청에 실패했어요."));
        },
      });
    });
    return notificationAgreementStatus(storageKey) === "agreed";
  } catch {
    // 알림 동의는 편의 기능이라 실패해도 자동사냥 흐름을 막지 않아요.
    return false;
  } finally {
    state.notificationAgreementInFlight = false;
  }
}

async function startTossLogin(options = {}) {
  if (state.loginInFlight) {
    return false;
  }
  const { refreshAfterLogin = true } = options;
  state.loginInFlight = true;
  $("loginButton").disabled = true;
  $("loginButton").textContent = "로그인 중...";
  $("loginError").textContent = "";
  try {
    const { authorizationCode, referrer } = await requestTossAuthorization();
    const session = await api("/api/auth/toss/login", {
      method: "POST",
      skipAuth: true,
      body: JSON.stringify({ authorizationCode, referrer, entryPath: benefitTabEntryPathForLogin() }),
    });
    clearBenefitTabEntryMarker();
    setAuthToken(session.accessToken);
    if (refreshAfterLogin) {
      await refresh();
    } else {
      hideLoginGate();
    }
    setMessage("토스 로그인이 완료됐어요.");
    return true;
  } catch (error) {
    $("loginError").textContent = error.message || "토스 로그인에 실패했어요.";
    return false;
  } finally {
    state.loginInFlight = false;
    $("loginButton").disabled = false;
    $("loginButton").textContent = "토스로 로그인";
  }
}

function renderReviewToolsVisibility() {
  $("devPanel").classList.toggle("review-tools-enabled", state.reviewToolsEnabled);
}

async function loadAppConfig() {
  try {
    const config = await api("/api/app/config");
    state.distributionTarget = normalizedDistributionTarget(distributionTargetOverride || config.distributionTarget);
    state.reviewToolsEnabled = Boolean(config.reviewToolsEnabled) && !isOneStoreTarget();
    state.mockMonetizationEnabled = Boolean(config.mockMonetizationEnabled);
    state.integrationMode = config.integrationMode || "";
    state.tossReleaseReady = Boolean(config.tossReleaseReady);
    state.tossLoginEnabled = Boolean(config.tossLoginEnabled);
    state.realRewardAdsEnabled = Boolean(config.realRewardAdsEnabled);
    state.realBannerAdsEnabled = Boolean(config.realBannerAdsEnabled);
    state.realPaymentsEnabled = Boolean(config.realPaymentsEnabled);
    state.realShareRewardEnabled = Boolean(config.realShareRewardEnabled);
    state.realSmartMessageEnabled = Boolean(config.realSmartMessageEnabled);
    state.notificationAgreementTemplateCode = config.autoHuntEndedNotificationAgreementTemplateCode || "";
    state.rookieEventMissionNotificationAgreementTemplateCode = config.rookieEventMissionNotificationAgreementTemplateCode || "";
    state.adMode = config.adMode || "test";
    state.adGroupIds = config.adGroupIds || {};
    state.realAdPreloads.clear();
    clearRealFullScreenAdPreloadTimers();
    state.iapProductIds = config.iapProductIds || {};
    state.shareRewardModuleId = config.shareRewardModuleId || "";
    state.shareRewardMessage = config.shareRewardMessage || state.shareRewardMessage;
  } catch {
    state.distributionTarget = normalizedDistributionTarget(distributionTargetOverride);
    state.reviewToolsEnabled = false;
    state.mockMonetizationEnabled = isOneStoreTarget();
    state.integrationMode = "";
    state.tossReleaseReady = false;
    state.tossLoginEnabled = false;
    state.realRewardAdsEnabled = false;
    state.realBannerAdsEnabled = false;
    state.realPaymentsEnabled = false;
    state.realShareRewardEnabled = false;
    state.realSmartMessageEnabled = false;
    state.notificationAgreementTemplateCode = "";
    state.rookieEventMissionNotificationAgreementTemplateCode = "";
    state.adMode = "test";
    state.adGroupIds = {};
    state.realAdPreloads.clear();
    clearRealFullScreenAdPreloadTimers();
    state.iapProductIds = {};
    state.iapProducts = {};
    state.shareRewardModuleId = "";
  }
  document.body.classList.toggle("target-onestore", isOneStoreTarget());
  renderReviewToolsVisibility();
}

async function loadIapProductInfo() {
  if (isOneStoreTarget() || state.mockMonetizationEnabled || !state.realPaymentsEnabled || state.iapProductInfoRequested) {
    return;
  }
  state.iapProductInfoRequested = true;
  try {
    const sdk = await loadTossSdk();
    if (typeof sdk?.IAP?.getProductItemList !== "function") {
      return;
    }
    const timeout = new Promise((_, reject) => {
      window.setTimeout(() => reject(new Error("상품 목록 조회 시간이 초과됐어요.")), 3500);
    });
    const response = await Promise.race([sdk.IAP.getProductItemList(), timeout]);
    const products = Array.isArray(response?.products) ? response.products : [];
    state.iapProducts = products.reduce((acc, product) => {
      if (product?.sku) {
        acc[product.sku] = product;
      }
      return acc;
    }, {});
    render();
  } catch {
    state.iapProducts = {};
  }
}

function render() {
  const player = state.player;
  if (!player) {
    return;
  }

  const onboard = player.onboardingRequired;
  $("jobModal").classList.toggle("hidden", state.dismissedJobModal || (!onboard && !state.forceJobModal));
  $("jobModal").classList.toggle("is-onboarding", onboard);
  $("closeJobModal").classList.toggle("hidden", onboard);
  $("tutorialReward").classList.toggle("hidden", !onboard);
  $("jobTitle").textContent = onboard ? "첫 헌터를 선택하세요" : "변경할 헌터를 선택하세요";
  $("jobCopy").textContent = onboard
    ? "능력치는 동일해요. 마음에 드는 공격 모션을 고르세요."
    : isOneStoreTarget()
      ? "캐릭터 변경은 게임 내 보상으로 바로 적용돼요. 직업별 성장 차이는 없어요."
      : "캐릭터 변경은 광고 시청 후 적용돼요. 직업별 성장 차이는 없어요.";
  applyJob(state.selectedJob);
  applyEffectTier(player);

  const hunting = isActive(player.autoHuntEndsAt);
  document.querySelector(".game-shell").classList.toggle("is-hunting", hunting);
  $("huntMode").textContent = hunting ? "자동사냥" : "대기중";
  renderBattleTip(hunting);

  updateGoldViews();
  $("levelText").textContent = `Lv.${displayLevel(player)}`;
  $("skillPointsTop").textContent = player.skillPoints;
  renderAdventurePanel(player);
  renderRewardPanel(player);
  renderShopPanel(player);
  renderPets(player);
  renderRookieEvent(player);
  const autoHuntCooldownReady = timeRewardAdCooldownReady("AUTO_HUNT", player);
  $("autoHuntAd").disabled = !autoHuntCooldownReady;
  $("huntTime").textContent = !autoHuntCooldownReady
    ? timeRewardCooldownLabel(player.autoHuntEndsAt, nextTimeRewardAdAvailableAt("AUTO_HUNT", player))
    : timeRewardReadyLabel(player.autoHuntEndsAt, player.autoHuntAdSeconds, player.maxAdSeconds);
  $("combatPowerMetric").textContent = formatCombatPower(combatPower(player));
  renderMonster(player);
  renderExperience(player);
  renderDummyBanner();

  $("goldRateTop").textContent = `${player.goldPerHour.toLocaleString("ko-KR")}/시간`;
  renderSkills(player);
  renderNotification(player);
  syncBgmState();
  refreshWhenAutoHuntEnds(hunting);
  maybeStartFeatureTutorial();
  maybeResumeDeferredGameProfileCreation();
}

function combatPower(player = state.player) {
  if (!player) {
    return 0;
  }
  const totalSkillLevels = combatPowerSkillLevelTotal(player);
  const eventPetSkillBonus = rookieEventRewardActive(player)
    ? Number(player.rookieEvent?.eventPetSkillLevel || 15)
    : 0;
  const totalSp = Math.max(0, totalSkillLevels + eventPetSkillBonus);
  const levelRatio = Math.min(1, Math.max(0, displayLevel(player) / combatPowerLevelScale));
  const spRatio = Math.min(1, Math.max(0, totalSp / combatPowerSkillScale));
  const power = maxCombatPower * Math.pow(levelRatio, 0.5) * Math.pow(spRatio, 1.5);
  return Math.min(maxCombatPower, Math.max(0, Math.floor(power)));
}

function combatPowerSkillLevelTotal(player = state.player) {
  if (!Array.isArray(player?.skills)) {
    return 0;
  }
  const statSkillTypes = new Set(Object.values(statSkillByJob));
  const sharedStatLevel = player.skills
    .filter((skill) => statSkillTypes.has(skill?.type))
    .reduce((maxLevel, skill) => Math.max(maxLevel, Number(skill?.level || 0)), 0);
  const nonStatSkillLevels = player.skills
    .filter((skill) => !statSkillTypes.has(skill?.type))
    .reduce((sum, skill) => sum + Number(skill?.level || 0), 0);
  return Math.max(0, sharedStatLevel + nonStatSkillLevels);
}

function formatCombatPower(power) {
  const value = Math.max(0, Math.floor(Number(power) || 0));
  const man = Math.floor(value / 10_000);
  const rest = value % 10_000;
  if (man <= 0) {
    return String(value);
  }
  return rest > 0 ? `${man}만${rest}` : `${man}만`;
}

function renderDummyBanner() {
  renderRealBannerAd();
  const shouldShowDummyBanner = !isOneStoreTarget() && state.reviewToolsEnabled && state.showDummyBanner;
  $("dummyBannerAd").classList.toggle("hidden", !shouldShowDummyBanner);
  $("devToggleBanner").classList.toggle("is-on", state.showDummyBanner);
  $("devToggleBanner").textContent = state.showDummyBanner ? "배너 숨기기" : "배너 표시";
}

async function renderRealBannerAd() {
  const slot = $("realBannerAd");
  const groupId = adGroupId("banner");
  const shouldShow = shouldUseRealBannerAd() && Boolean(groupId);
  slot.classList.toggle("hidden", !shouldShow);
  if (!shouldShow) {
    destroyRealBannerAd();
    return;
  }
  if (bannerAdHandle || bannerAdLoading) {
    return;
  }
  bannerAdLoading = true;
  try {
    const sdk = await loadTossSdk();
    const tossAds = sdk.TossAds;
    if (!tossAds?.initialize?.isSupported?.() || !tossAds?.attachBanner?.isSupported?.()) {
      slot.classList.add("hidden");
      return;
    }
    await initializeTossBannerAds(tossAds);
    slot.replaceChildren();
    bannerAdHandle = tossAds.attachBanner(groupId, slot, {
      theme: "auto",
      tone: "blackAndWhite",
      variant: "expanded",
      callbacks: {
        onNoFill: () => slot.classList.add("hidden"),
        onAdFailedToRender: () => slot.classList.add("hidden"),
        onAdRendered: () => slot.classList.remove("hidden"),
      },
    });
  } catch {
    slot.classList.add("hidden");
  } finally {
    bannerAdLoading = false;
  }
}

async function initializeTossBannerAds(tossAds) {
  if (state.tossAdsInitialized) {
    return;
  }
  if (!state.tossAdsInitializationPromise) {
    state.tossAdsInitializationPromise = new Promise((resolve, reject) => {
      tossAds.initialize({
        callbacks: {
          onInitialized: () => {
            state.tossAdsInitialized = true;
            resolve();
          },
          onInitializationFailed: (error) => {
            state.tossAdsInitializationPromise = null;
            reject(error);
          },
        },
      });
    });
  }
  await state.tossAdsInitializationPromise;
}

function destroyRealBannerAd() {
  if (!bannerAdHandle) {
    return;
  }
  bannerAdHandle.destroy?.();
  bannerAdHandle = null;
  $("realBannerAd").replaceChildren();
}

function renderBattleTip(hunting = false) {
  const hasMessage = Boolean(state.noticeMessage && Date.now() < state.noticeExpiresAt);
  $("battleTip").textContent = hasMessage
    ? state.noticeMessage
    : hunting
    ? "자동사냥 진행 중"
    : isOneStoreTarget()
      ? "게임 내 보상으로 자동사냥을 시작하세요"
      : "광고를 보고 자동사냥을 시작하세요";
  $("battleTip").classList.toggle("has-message", hasMessage);
}

function refreshWhenAutoHuntEnds(hunting) {
  if (state.autoHuntWasActive && !hunting && !state.autoHuntEndRefreshInFlight) {
    state.autoHuntEndRefreshInFlight = true;
    refresh()
      .catch((error) => setMessage(error.message))
      .finally(() => {
        state.autoHuntEndRefreshInFlight = false;
      });
  }
  state.autoHuntWasActive = hunting;
}

function renderNotification(player) {
  const notification = player.latestNotification;
  if (!notification) {
    $("notificationModal").dataset.notificationId = "";
    $("notificationModal").classList.add("hidden");
    return;
  }
  if (state.shownNotificationId === notification.id) {
    return;
  }
  state.shownNotificationId = notification.id;
  $("notificationModal").dataset.notificationId = notification.id;
  $("notificationTitle").textContent = notification.title;
  $("notificationBody").textContent = notification.body;
  const settledGold = notificationSettledGold(notification);
  $("settledGoldAmount").textContent = settledGold > 0
    ? `${settledGold.toLocaleString("ko-KR")} 골드를 벌었어요`
    : "이번 정산에서 추가 골드는 없어요";
  renderNotificationGrowth(notification);
  $("notificationMeta").textContent = notification.sentAt
    ? `알림 도착 · ${formatShortTime(notification.sentAt)}`
    : "자동사냥 종료 안내";
  $("notificationModal").classList.remove("hidden");
}

function renderNotificationGrowth(notification) {
  const levelGained = Math.max(0, Math.floor(Number(notification?.levelGained) || 0));
  const skillPointsGained = Math.max(0, Math.floor(Number(notification?.skillPointsGained) || 0));
  const combatPowerGained = Math.max(0, Math.floor(Number(notification?.combatPowerGained) || 0));
  const summary = $("notificationGrowthSummary");
  const hasGrowth = levelGained > 0 || skillPointsGained > 0 || combatPowerGained > 0;
  summary.classList.toggle("hidden", !hasGrowth);
  if (!hasGrowth) {
    return;
  }
  const growthText = [];
  if (levelGained > 0) {
    growthText.push(`레벨 +${levelGained.toLocaleString("ko-KR")}`);
  }
  if (skillPointsGained > 0) {
    growthText.push(`SP +${skillPointsGained.toLocaleString("ko-KR")}`);
  }
  $("notificationGrowthText").textContent = growthText.length > 0
    ? growthText.join(" · ")
    : "레벨업 없이 전투력이 올랐어요";
  $("notificationCombatPowerGain").textContent = combatPowerGained > 0
    ? `전투력 +${formatCombatPower(combatPowerGained)}`
    : "전투력 변화 없음";
}

function notificationSettledGold(notification) {
  if (notification?.settledGold === null || notification?.settledGold === undefined) {
    return state.lastSettlementGold;
  }
  return Math.max(0, Math.floor(Number(notification.settledGold) || 0));
}

async function closeNotificationModal() {
  const notificationId = $("notificationModal").dataset.notificationId;
  $("notificationModal").classList.add("hidden");
  $("notificationModal").dataset.notificationId = "";
  if (!notificationId) {
    return;
  }
  try {
    setServerPlayer(await api(`/api/player/notifications/${notificationId}/read`, { method: "POST" }));
    render();
  } catch (error) {
    setMessage(error.message);
  }
}

function renderMonster(player) {
  const monster = displayMonster(player);
  const meta = monsterMeta[monster.key] || monsterMeta.BOSS_ROCK;
  applyBattleBackground(monster);
  $("monsterImage").src = meta.image;
  $("monsterName").textContent = `${meta.name} #${monster.defeatedMonsters + 1}`;
  $("monsterHp").style.width = `${Math.max(0, Math.min(100, monster.hp * 100 / monster.maxHp))}%`;
}

function refreshImageSource(elementId, src) {
  const image = $(elementId);
  if (!image || !src) {
    return;
  }
  const currentSrc = image.getAttribute("src") || "";
  if (currentSrc !== src) {
    image.src = src;
    return;
  }
  if (image.complete && image.naturalWidth > 0) {
    return;
  }
  image.removeAttribute("src");
  window.requestAnimationFrame(() => {
    image.src = src;
  });
}

function restoreBattleVisualAssets() {
  if (!state.player) {
    return;
  }
  const job = jobMeta[state.selectedJob || state.player.job || "WARRIOR"] || jobMeta.WARRIOR;
  const monster = displayMonster(state.player);
  const monsterAsset = monsterMeta[monster.key] || monsterMeta.BOSS_ROCK;
  applyBattleBackground(monster);
  const screen = document.querySelector(".battle-screen");
  const background = battleBackgrounds[state.battleBackgroundIndex];
  if (screen && background) {
    screen.style.backgroundImage = "none";
    window.requestAnimationFrame(() => {
      screen.style.backgroundImage = `url("${background}")`;
    });
  }
  refreshImageSource("heroImage", job.image);
  refreshImageSource("armImage", job.arm);
  refreshImageSource("monsterImage", monsterAsset.image);
  petMeta.forEach((pet, index) => {
    refreshImageSource(pet.imageId, petSkinForSlot(index + 1, state.player).image);
  });
  refreshImageSource("petRookieEvent", rookieEventAssets.pet);
}

function monsterSignature(monster) {
  return `${monster.key}:${monster.defeatedMonsters}`;
}

function nextBattleBackgroundIndex() {
  if (battleBackgrounds.length <= 1) {
    return 0;
  }
  let next = state.battleBackgroundIndex;
  while (next === state.battleBackgroundIndex) {
    next = Math.floor(Math.random() * battleBackgrounds.length);
  }
  return next;
}

function applyBattleBackground(monster) {
  const signature = monsterSignature(monster);
  if (!state.lastMonsterSignature) {
    state.lastMonsterSignature = signature;
    storeValue(monsterSignatureStorageKey, signature);
  } else if (state.lastMonsterSignature !== signature) {
    state.lastMonsterSignature = signature;
    state.battleBackgroundIndex = nextBattleBackgroundIndex();
    storeValue(monsterSignatureStorageKey, signature);
    storeValue(battleBackgroundStorageKey, String(state.battleBackgroundIndex));
  }

  const screen = document.querySelector(".battle-screen");
  screen.classList.add("has-art-bg");
  screen.style.backgroundImage = `url("${battleBackgrounds[state.battleBackgroundIndex]}")`;
}

function renderExperience(player) {
  const experience = displayExperience(player);
  const nextLevelExperience = displayNextLevelExperience(player);
  const percent = Math.max(0, Math.min(100, experience * 100 / nextLevelExperience));
  $("expBar").style.width = `${percent}%`;
  $("expText").textContent = `EXP ${experience.toLocaleString("ko-KR")} / ${nextLevelExperience.toLocaleString("ko-KR")}`;
}

function displayMonster(player = state.player) {
  return state.localMonster || player?.monster || { key: "BOSS_ROCK", hp: 120, maxHp: 120, defeatedMonsters: 0 };
}

function displayLevel(player = state.player) {
  return state.localLevel ?? player?.level ?? 1;
}

function displayExperience(player = state.player) {
  return state.localExperience ?? player?.experience ?? 0;
}

function displayNextLevelExperience(player = state.player) {
  return state.localNextLevelExperience ?? player?.nextLevelExperience ?? 1500;
}

function attackIntervalMillis(player = state.player) {
  if (!player) {
    return 667;
  }
	  if (player.attackIntervalMillis) {
    return player.attackIntervalMillis;
  }
  const rapidBonus = Math.min(167, skillLevel("RAPID_ATTACK") * 6);
  return Math.max(500, 667 - rapidBonus);
}

function bossRaidTier(player = state.player) {
  const power = combatPower(player);
  return bossRaidTiers.reduce((selected, tier) => power >= tier.minCombatPower ? tier : selected, bossRaidTiers[0]);
}

function renderAdventurePanel(player) {
  const dungeonCoupon = player.dungeonCoupon || {};
  const dungeonCouponEnabled = Boolean(dungeonCoupon.enabled);
  const bossTicketCount = dungeonCoupon.bossTicketCount ?? dungeonCoupon.count ?? 0;
	  const dungeonDailyLimit = dungeonCoupon.dungeonDailyLimit ?? 5;
	  const dungeonFreeLimit = dungeonCoupon.dungeonFreeDailyLimit ?? dungeonFreeDailyLimit;
	  const dungeonAdditionalLimit = dungeonCoupon.dungeonAdditionalDailyLimit ?? Math.max(0, dungeonDailyLimit - dungeonFreeLimit);
  const dungeonRunsToday = dungeonCoupon.dungeonRunsToday || 0;
  const dungeonRemainingRuns = Math.max(0, dungeonCoupon.dungeonRemainingRuns ?? (dungeonDailyLimit - dungeonRunsToday));
  const dungeonCooldownSeconds = dungeonCoupon.dungeonNextAvailableAt
    ? remainingSecondsFrom(dungeonCoupon.dungeonNextAvailableAt)
    : Math.max(0, dungeonCoupon.dungeonCooldownSeconds || 0);
  const dungeonHuntProgressSeconds = Math.max(0, dungeonCoupon.dungeonHuntProgressSeconds || 0);
  const dungeonHuntRequiredSeconds = Math.max(1, dungeonCoupon.dungeonHuntRequiredSeconds || 3600);
	  const dungeonHuntCompleted = dungeonCoupon.dungeonHuntRequirementCompleted ?? dungeonHuntProgressSeconds >= dungeonHuntRequiredSeconds;
	  const dungeonAvailable = dungeonCoupon.dungeonAvailable ?? (dungeonHuntCompleted && dungeonRemainingRuns > 0 && dungeonCooldownSeconds <= 0);
	  const adventureEntryPolicyCopy = document.getElementById("adventureEntryPolicyCopy");
	  if (adventureEntryPolicyCopy) {
	    adventureEntryPolicyCopy.textContent = `던전은 기본 ${dungeonFreeLimit}번과 광고 추가 ${dungeonAdditionalLimit}번, 보스는 입장권으로 도전해요`;
	  }
  const fallbackBossTier = bossRaidTier(player);
  const bossTier = {
    ...fallbackBossTier,
    bossName: dungeonCoupon.bossName || fallbackBossTier.bossName,
    difficultyName: dungeonCoupon.bossDifficultyName || fallbackBossTier.difficultyName,
  };
  const dungeonRequiresAd = dungeonRunsToday >= dungeonFreeLimit;
  $("dungeonCouponCard").hidden = !dungeonCouponEnabled;
  $("dungeonCouponCard").classList.toggle("has-coupon", dungeonAvailable);
  $("bossRaidCard").classList.toggle("locked", !dungeonCouponEnabled || bossTicketCount < 1);
  $("bossRaidImage").src = adventureAssets.bossRaid;
  $("bossRaidTitle").textContent = `${bossTier.bossName} · ${bossTier.difficultyName}`;
  $("bossRaidStatus").textContent = dungeonCouponEnabled
    ? bossTicketCount > 0
      ? `보스 입장권 ${bossTicketCount.toLocaleString("ko-KR")}장 보유 · 토벌 가능`
      : "던전에서 확률적으로 입장권 획득"
    : "모험 기능 준비중";
  $("challengeBossRaid").disabled = !dungeonCouponEnabled || bossTicketCount < 1;
  $("challengeBossRaid").innerHTML = bossTicketCount > 0
    ? `<span class="boss-ticket-button-label"><img src="${adventureAssets.bossTicket}" alt="" />입장권 ${bossTicketCount.toLocaleString("ko-KR")}장 · 토벌</span>`
    : `<span class="boss-ticket-button-label"><img src="${adventureAssets.bossTicket}" alt="" />입장권 필요</span>`;
  if (dungeonCouponEnabled) {
    $("dungeonCouponCopy").textContent = `${dungeonCoupon.tierName || "초급 던전"} · ${dungeonRunsToday}/${dungeonDailyLimit}`;
    $("dungeonHuntProgress").textContent = `사냥 진행도 ${timeRewardModalDurationLabel(dungeonHuntProgressSeconds)}/${timeRewardModalDurationLabel(dungeonHuntRequiredSeconds)}`;
    $("dungeonHuntProgress").classList.toggle("is-complete", dungeonHuntCompleted);
    $("dungeonCouponStatus").hidden = dungeonCooldownSeconds <= 0;
    if (dungeonRemainingRuns <= 0) {
      $("dungeonCouponStatus").hidden = true;
      $("dungeonCouponStatus").textContent = "";
    } else if (dungeonCooldownSeconds > 0) {
      $("dungeonCouponStatus").textContent = `${timeRewardModalDurationLabel(dungeonCooldownSeconds)} 후 재입장`;
    } else if (dungeonRequiresAd) {
      $("dungeonCouponStatus").textContent = "";
    } else {
      $("dungeonCouponStatus").textContent = "";
    }
    $("useDungeonCoupon").disabled = !dungeonAvailable;
    $("useDungeonCoupon").innerHTML = dungeonAvailable
      ? dungeonRequiresAd
        ? "<span>광고 보고 추가 입장</span>"
        : "<span>던전 입장</span>"
      : `<span>${dungeonCoupon.dungeonUnavailableReason || "입장 대기"}</span>`;
  }
}

function openAdventureInfoModal() {
  const power = combatPower();
  $("adventureInfoList").innerHTML = dungeonTiers.map((tier, index) => {
    const boss = bossRaidTiers[index] || bossRaidTiers[bossRaidTiers.length - 1];
    const unlocked = power >= tier.minCombatPower;
    const powerLabel = tier.powerLabel
      .replace("500만~2,000만", "500만<br>~2,000만")
      .replace("2,000만~6,000만", "2,000만<br>~6,000만");
    return `
      <div class="adventure-info-row ${unlocked ? "is-unlocked" : ""}">
        <strong>${powerLabel}</strong>
        <div>
          <b>${tier.name} · ${boss.bossName}</b>
          <span>던전: ${tier.rewardPreview}</span>
          <span>보스: ${boss.difficultyName} · ${boss.rewardPreview}</span>
        </div>
      </div>
    `;
  }).join("");
  $("adventureInfoModal").classList.remove("hidden");
}

function closeAdventureInfoModal() {
  $("adventureInfoModal").classList.add("hidden");
}

function openAdventureRewardModal(kind) {
  const adventure = state.player?.dungeonCoupon || {};
  const isBoss = kind === "boss";
  const rewards = isBoss ? adventure.bossRewards : adventure.dungeonRewards;
  $("adventureRewardKicker").textContent = isBoss ? "BOSS RAID" : "DUNGEON";
  $("adventureRewardTitle").textContent = isBoss ? "보스 토벌 보상 목록" : "던전 보상 목록";
  $("adventureRewardCopy").textContent = isBoss
    ? `${adventure.bossName || "보스"} 토벌에서 등장하는 보상과 확률이에요.`
    : `${adventure.tierName || "현재 던전"}에서 등장하는 보상과 확률이에요.`;
  $("adventureRewardList").innerHTML = Array.isArray(rewards) && rewards.length > 0
    ? rewards.map((reward) => `
      <div class="adventure-reward-row">
        <span>${reward.rewardLabel || "랜덤 보상"}</span>
        <strong>${reward.probabilityLabel || "-"}</strong>
      </div>
    `).join("")
    : '<div class="adventure-reward-empty">보상 정보를 불러오는 중이에요.</div>';
  $("adventureRewardModal").classList.remove("hidden");
}

function closeAdventureRewardModal() {
  $("adventureRewardModal").classList.add("hidden");
}

function adventureActionConfig(kind, result = {}) {
	if (kind === "boss") {
		return {
			icon: "B",
			kicker: "BOSS RAID",
			runningTitle: "보스 토벌 중",
			runningBody: "보스의 움직임을 살피는 중이에요.",
			successTitle: "토벌 완료",
			successBody: `${result.bossName || "보스"} 토벌에 성공했어요.`,
			rewardMessage: `${result.rewardLabel || "랜덤 보상"} 받았어요.`,
    };
  }
  return {
		icon: "D",
		kicker: "DUNGEON",
		runningTitle: "던전 탐험 중",
		runningBody: "입구를 열고 안쪽을 살피는 중이에요.",
		successTitle: "탐험 완료",
		successBody: `${result.tierName || "던전"} 탐험을 마쳤어요.`,
    rewardMessage: result.bossTicketGranted
      ? `${result.rewardLabel || "보스 입장권"}을 찾았어요.`
      : `${result.rewardLabel || "랜덤 보상"} 받았어요.`,
	};
}

function adventureActionSteps(kind) {
	return adventureSceneSpecs[kind]?.running || adventureSceneSpecs.dungeon.running;
}

function adventureActionScene(kind, phase = "success") {
	return adventureSceneSpecs[kind]?.[phase] || adventureSceneSpecs.dungeon[phase];
}

function adventureSceneImages(kind) {
	const spec = adventureSceneSpecs[kind] || adventureSceneSpecs.dungeon;
	return [
		spec.fallback,
		...spec.running.map((scene) => scene.image),
		spec.success.image,
		spec.failure.image,
	].filter(Boolean);
}

function preloadImage(src) {
	if (!src) {
		return Promise.resolve();
	}
	if (adventureImagePreloads.has(src)) {
		return adventureImagePreloads.get(src);
	}
	const promise = new Promise((resolve) => {
		const image = new Image();
		let resolved = false;
		const finish = () => {
			if (resolved) {
				return;
			}
			resolved = true;
			resolve(image);
		};
		image.onload = finish;
		image.onerror = finish;
		image.src = src;
		if (image.decode) {
			image.decode().then(finish).catch(finish);
		}
	});
	adventureImagePreloads.set(src, promise);
	return promise;
}

function preloadAdventureImages(kind) {
	return Promise.all([...new Set(adventureSceneImages(kind))].map(preloadImage));
}

function scheduleAdventureImagePreloads() {
	const preload = () => Object.keys(adventureSceneSpecs).forEach((kind) => preloadAdventureImages(kind));
	if ("requestIdleCallback" in window) {
		window.requestIdleCallback(preload, { timeout: 2500 });
		return;
	}
	setTimeout(preload, 900);
}

function setAdventureActionScene(kind, scene) {
	const fallback = adventureSceneSpecs[kind]?.fallback || adventureAssets.dungeon;
	const image = scene?.image || fallback;
	$("adventureActionIcon").className = `adventure-action-icon ${kind === "boss" ? "boss" : "dungeon"}`;
	$("adventureActionIcon").style.backgroundImage = `url("${image}"), url("${fallback}")`;
	$("adventureActionScene").textContent = scene?.label || "";
	if (scene?.body) {
		$("adventureActionBody").textContent = scene.body;
	}
}

function clearAdventureActionStepTimers() {
	(state.adventureActionStepTimers || []).forEach((timer) => clearTimeout(timer));
	state.adventureActionStepTimers = [];
}

function scheduleAdventureActionSteps(kind) {
	clearAdventureActionStepTimers();
	const steps = adventureActionSteps(kind);
	steps.forEach((scene, index) => {
		const timer = setTimeout(() => {
			if (!$("adventureActionModal").classList.contains("is-running")) {
				return;
			}
			setAdventureActionScene(kind, scene);
		}, index * adventureActionStepDurationMs);
		state.adventureActionStepTimers.push(timer);
	});
}

function openAdventureActionModal(kind) {
	const config = adventureActionConfig(kind);
  $("adventureActionIcon").textContent = config.icon;
  $("adventureActionKicker").textContent = config.kicker;
  $("adventureActionTitle").textContent = config.runningTitle;
  $("adventureActionBody").textContent = config.runningBody;
	setAdventureActionScene(kind, adventureActionSteps(kind)[0]);
  $("adventureActionReward").classList.add("hidden");
  $("adventureActionReward").textContent = "";
  $("closeAdventureActionModal").classList.add("hidden");
	$("adventureActionModal").classList.remove("is-complete");
	$("adventureActionModal").classList.add("is-running");
	$("adventureActionModal").classList.remove("hidden");
	scheduleAdventureActionSteps(kind);
}

function completeAdventureActionModal(kind, result) {
	clearAdventureActionStepTimers();
	const config = adventureActionConfig(kind, result);
	setAdventureActionScene(kind, adventureActionScene(kind, "success"));
  $("adventureActionTitle").textContent = config.successTitle;
  $("adventureActionBody").textContent = config.successBody;
  $("adventureActionReward").textContent = config.rewardMessage;
  $("adventureActionReward").classList.remove("hidden");
  $("closeAdventureActionModal").classList.remove("hidden");
  $("adventureActionModal").classList.remove("is-running");
  $("adventureActionModal").classList.add("is-complete");
}

function failAdventureActionModal(kind, message) {
	clearAdventureActionStepTimers();
	const config = adventureActionConfig(kind);
	setAdventureActionScene(kind, adventureActionScene(kind, "failure"));
  $("adventureActionTitle").textContent = kind === "boss" ? "토벌 실패" : "탐험 중단";
  $("adventureActionBody").textContent = message || "잠시 후 다시 시도해 주세요.";
  $("adventureActionReward").textContent = "";
  $("adventureActionReward").classList.add("hidden");
  $("closeAdventureActionModal").classList.remove("hidden");
  $("adventureActionModal").classList.remove("is-running");
  $("adventureActionModal").classList.add("is-complete");
  $("adventureActionKicker").textContent = config.kicker;
  $("adventureActionIcon").textContent = config.icon;
}

function closeAdventureActionModal() {
	if (state.adventureActionInFlight) {
		return;
	}
	clearAdventureActionStepTimers();
	$("adventureActionModal").classList.add("hidden");
  $("adventureActionModal").classList.remove("is-running", "is-complete");
}

function adventureActionDelay() {
	return new Promise((resolve) => setTimeout(resolve, adventureActionTotalDurationMs));
}

async function runAdventureAction(kind, request) {
  if (state.adventureActionInFlight) {
    return;
  }
  state.adventureActionInFlight = true;
  try {
    await preloadAdventureImages(kind);
    openAdventureActionModal(kind);
    await adventureActionDelay();
    const result = await request();
    if (result.state) {
      setServerPlayer(result.state, { resetDisplayGold: true });
      if (state.player.job) {
        state.selectedJob = state.player.job;
      }
      render();
      completeAdventureActionModal(kind, result);
      scheduleGameProfileSync(800);
      scheduleLeaderboardEntryScoreSubmit(1200);
    }
  } catch (error) {
    setMessage(error.message);
    failAdventureActionModal(kind, error.message);
  } finally {
    state.adventureActionInFlight = false;
  }
}

function renderRewardPanel(player) {
  const pointGoldRate = player.goldPerTossPoint || goldPerTossPoint;
  const minimumClaimPointAmount = player.rewardPointAmount || Math.floor(player.rewardGoldThreshold / pointGoldRate);
  const availablePointAmount = Math.floor(player.gold / pointGoldRate);
  const remainingPointAmount = Math.max(0, minimumClaimPointAmount - availablePointAmount);
  const inviteCount = player.friendInviteRewardCount ?? 0;
  const inviteLimit = player.friendInviteLimit ?? friendInviteLimit;
  const inviteReward = player.friendInviteRewardSkillPoints ?? friendInviteRewardSkillPoints;
  const friendInviteMaxed = inviteLimit > 0 && inviteCount >= inviteLimit;
  $("rewardPanel").classList.toggle("friend-invite-maxed", friendInviteMaxed);
  $("rewardBar").style.width = `${Math.min(100, availablePointAmount * 100 / minimumClaimPointAmount)}%`;
  $("rewardNeed").textContent = `${availablePointAmount.toLocaleString("ko-KR")} / ${minimumClaimPointAmount.toLocaleString("ko-KR")}P`;
  $("rewardPointEstimate").textContent = isOneStoreTarget()
    ? "게임 보상"
    : `현재 ${availablePointAmount.toLocaleString("ko-KR")}P`;
  $("rewardClaimAmount").textContent = player.rewardClaimable
    ? isOneStoreTarget()
      ? `수령 가능 SP ${inviteReward.toLocaleString("ko-KR")}`
      : `수령 가능 ${availablePointAmount.toLocaleString("ko-KR")}P`
    : `${remainingPointAmount.toLocaleString("ko-KR")}P 더 필요`;
  $("claimReward").disabled = !player.rewardClaimable;
  $("claimReward").innerHTML = player.rewardClaimable
    ? isOneStoreTarget()
      ? "<span>게임 보상 수령</span><small>심사용 즉시 지급</small>"
      : `<span>토스포인트 받기 · ${availablePointAmount.toLocaleString("ko-KR")}P 수령 가능</span>`
    : `<span>${remainingPointAmount.toLocaleString("ko-KR")}P 더 필요 · 조건 달성 후 수령 가능</span>`;
  $("friendInviteRewardCopy").textContent = `초대 성공 시 SP ${inviteReward.toLocaleString("ko-KR")}개 지급`;
  $("friendInviteRewardStatus").textContent = `${inviteCount.toLocaleString("ko-KR")} / ${inviteLimit.toLocaleString("ko-KR")}명 완료`;
  $("claimFriendInviteReward").disabled = inviteCount >= inviteLimit;
  $("claimFriendInviteReward").innerHTML = inviteCount >= inviteLimit
    ? "<span>초대 보상 MAX</span>"
    : `<span>친구 초대하기 · ${inviteLimit - inviteCount}명 남음</span>`;
  const nickname = gameProfileNickname(player);
  $("leaderboardProfileText").textContent = isOneStoreTarget()
    ? "심사용 게임 기록으로 랭킹을 확인해요"
    : `${nickname || "게임 프로필"} 닉네임으로 랭킹에 참여해요`;
  $("leaderboardStatus").textContent = `전투력 ${formatCombatPower(leaderboardCombatPowerScore(player))} 기준`;
}

function availableRewardPointAmount(player = state.player) {
  if (!player) {
    return 0;
  }
  return Math.floor(player.gold / (player.goldPerTossPoint || goldPerTossPoint));
}

function durationProgressLabel(milliseconds) {
  const totalMinutes = Math.max(0, Math.floor(Number(milliseconds || 0) / 60000));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours > 0 && minutes > 0) {
    return `${hours}시간 ${minutes}분`;
  }
  if (hours > 0) {
    return `${hours}시간`;
  }
  return `${minutes}분`;
}

function renderShopPanel(player) {
  const maxSlots = player.maxCharacterSlots || 3;
  const pets = unlockedPetCount(player);
  const maxPets = Math.max(0, maxSlots - 1);
  const price = player.companionPriceWon || companionPriceWon;
  const packPrice = player.skillPointPackPriceWon || skillPointPackPriceWon;
  const packAmount = player.skillPointPackAmount || skillPointPackAmount;
  $("companionCount").textContent = `${pets}/${maxPets}`;
  petMeta.forEach((pet, index) => {
    const unlocked = pets > index;
    const purchasable = !unlocked && pets === index && pets < maxPets;
    const skin = petSkinForSlot(index + 1, player);
    const shopCard = $(`pet${index + 1 === 1 ? "One" : "Two"}Shop`);
    const status = $(pet.statusId);
    $(pet.shopImageId).src = skin.image;
    $(pet.shopNameId).textContent = skin.name;
    $(pet.shopCopyId).textContent = skin.copy;
    shopCard.classList.toggle("locked", !unlocked);
    shopCard.classList.toggle("skin-change-available", unlocked);
    const skinButton = $(index === 0 ? "changePetOneSkin" : "changePetTwoSkin");
    skinButton.dataset.petShopAction = unlocked ? "skin" : purchasable ? "purchase" : "locked";
    skinButton.disabled = !unlocked && !purchasable;
    skinButton.innerHTML = unlocked
      ? "<span>스킨 변경</span>"
      : purchasable
        ? "<span>구매하기</span>"
        : "<span>잠김</span>";
    status.textContent = unlocked
      ? `동행 중 · ${skin.name} 공격 가능`
      : purchasable
        ? isOneStoreTarget()
          ? "잠금 해제 가능 · 심사용"
          : `구매 가능 · ${iapDisplayAmount(index === 0 ? "flarePet" : "aquaPet", price)}`
        : "잠김 · 이전 펫을 먼저 데려오세요";
  });
  $("skillPointPackCopy").textContent = `SP ${packAmount.toLocaleString("ko-KR")}개 즉시 지급`;
  const spAvailable = skillPointRewardsAvailable(player);
  $("skillPointPackStatus").textContent = spAvailable
    ? isOneStoreTarget()
      ? "심사용 게임 보상"
      : iapDisplayAmount("skillPointPack", packPrice)
    : "모든 스킬 MAX";
  $("buySkillPointPack").disabled = !spAvailable;
  $("buySkillPointPack").innerHTML = spAvailable
    ? "<span>구매하기</span>"
    : "<span>MAX</span><small>완료</small>";
  renderPetSkinShop(player);
}

function renderPetSkinShop(player) {
  const pets = unlockedPetCount(player);
  const modal = $("petSkinModal");
  const list = $("petSkinList");
  if (modal.classList.contains("hidden") || pets < 1) {
    list.replaceChildren();
    return;
  }

  state.petSkinModalSlot = Math.max(1, Math.min(Number(state.petSkinModalSlot || 1), pets));
  const slot = state.petSkinModalSlot;
  const owned = ownedPetSkinKeys(player);
  const priceGold = Number(player.petSkinPriceGold || 30000);
  const selectedSkinKey = petSkinKeyForSlot(slot, player);
  const easterEggUnlocked = hasUnlockedPetEasterEggSkins(player);
  $("petSkinHeading").textContent = `${slot}번 펫 스킨`;
  $("petSkinModalCopy").textContent = `${petSkinForSlot(slot, player).name}에게 적용할 스킨을 선택하세요. 기본 스킨도 언제든 다시 착용할 수 있어요.`;
  const easterEggButton = $("unlockPetEasterEgg");
  easterEggButton.classList.toggle("is-unlocked", easterEggUnlocked);
  easterEggButton.textContent = easterEggUnlocked
    ? state.petEasterEggSkinsHidden
      ? "히든 스킨 보기"
      : "히든 스킨 숨기기"
    : "";
  list.replaceChildren(...petSkinOrder
    .filter((skinKey) => {
      const skin = petSkinByKey(skinKey);
      return !skin.easterEgg || (owned.has(skinKey) && !state.petEasterEggSkinsHidden);
    })
    .map((skinKey) => {
      const skin = petSkinByKey(skinKey);
      const ownedSkin = owned.has(skinKey);
      const card = document.createElement("article");
      card.className = "pet-skin-card";
      card.classList.toggle("locked", !ownedSkin);

      const image = document.createElement("img");
      image.src = skin.image;
      image.alt = "";
      card.appendChild(image);

      const copy = document.createElement("div");
      copy.className = "pet-skin-copy";
      const title = document.createElement("strong");
      title.textContent = skin.name;
      const description = document.createElement("p");
      description.textContent = skin.copy;
      const status = document.createElement("small");
      status.textContent = ownedSkin
        ? selectedSkinKey === skinKey
          ? `${slot}번 펫 착용 중`
          : "보유 중"
        : `${priceGold.toLocaleString("ko-KR")}G로 해금`;
      copy.append(title, description, status);
      card.appendChild(copy);

      const actions = document.createElement("div");
      actions.className = "pet-skin-actions";
      if (!ownedSkin) {
        const button = document.createElement("button");
        button.type = "button";
        button.dataset.petSkinAction = "purchase";
        button.dataset.skinKey = skinKey;
        button.disabled = player.gold < priceGold;
        button.textContent = player.gold < priceGold ? "골드 부족" : `${priceGold.toLocaleString("ko-KR")}G 구매`;
        actions.appendChild(button);
      } else {
        const button = document.createElement("button");
        button.type = "button";
        button.dataset.petSkinAction = "equip";
        button.dataset.skinKey = skinKey;
        button.disabled = selectedSkinKey === skinKey;
        button.textContent = selectedSkinKey === skinKey ? "착용 중" : "이 스킨 착용";
        actions.appendChild(button);
      }
      card.appendChild(actions);
      return card;
    }));
}

function openPetSkinModal(slot) {
  if (unlockedPetCount() < slot) {
    setMessage("펫을 먼저 잠금 해제해주세요.");
    return;
  }
  state.petSkinModalSlot = slot;
  $("petSkinModal").classList.remove("hidden");
  renderPetSkinShop(state.player);
}

function closePetSkinModal() {
  $("petSkinModal").classList.add("hidden");
}

function hasUnlockedPetEasterEggSkins(player = state.player) {
  const owned = ownedPetSkinKeys(player);
  return petSkinOrder.some((skinKey) => petSkinByKey(skinKey).easterEgg && owned.has(skinKey));
}

function equippedPetSkinLabel(skinKey, selectedBySlot) {
  const equippedSlots = Object.entries(selectedBySlot)
    .filter(([, selectedSkinKey]) => selectedSkinKey === skinKey)
    .map(([slot]) => `${slot}번`);
  return equippedSlots.length > 0 ? `${equippedSlots.join(", ")} 펫 착용 중` : "보유 중";
}

function iapProduct(productKey) {
  return state.iapProducts[iapProductId(productKey)] || null;
}

function iapDisplayAmount(productKey, fallbackWon) {
  return iapProduct(productKey)?.displayAmount || `₩${Number(fallbackWon || 0).toLocaleString("ko-KR")}`;
}

function petSkinByKey(skinKey) {
  return petSkinMeta[skinKey] || petSkinMeta.FIRE_FOX;
}

function petSkinKeyForSlot(slot, player = state.player) {
  if (slot === 2) {
    return player?.petTwoSkinKey || "ICE";
  }
  return player?.petOneSkinKey || "FIRE_FOX";
}

function petSkinForSlot(slot, player = state.player) {
  return petSkinByKey(petSkinKeyForSlot(slot, player));
}

function ownedPetSkinKeys(player = state.player) {
  const keys = Array.isArray(player?.ownedPetSkinKeys) && player.ownedPetSkinKeys.length > 0
    ? player.ownedPetSkinKeys
    : ["FIRE_FOX", "ICE"];
  return new Set(keys);
}

function unlockedPetCount(player = state.player) {
  return Math.max(0, (player?.characterSlots || 1) - 1);
}

function renderPets(player) {
  const pets = unlockedPetCount(player);
  petMeta.forEach((pet, index) => {
    const visible = pets > index;
    const element = $(pet.imageId);
    element.classList.toggle("hidden", !visible);
    if (visible) {
      element.src = petSkinForSlot(index + 1, player).image;
    }
  });
  const eventPet = $("petRookieEvent");
  const showEventPet = rookieEventRewardActive(player);
  eventPet.classList.toggle("hidden", !showEventPet);
  if (showEventPet) {
    eventPet.src = rookieEventAssets.pet;
  }
}

function renderRookieEvent(player) {
  const event = player?.rookieEvent;
  const visible = Boolean(event?.visible);
  const button = $("rookieEventButton");
  const callout = button.querySelector(".rookie-event-callout");
  button.classList.toggle("hidden", !visible);
  document.querySelector(".battle-screen").classList.toggle("has-rookie-event", visible);
  if (!visible) {
    callout?.classList.add("hidden");
    closeRookieEventModal();
    return;
  }
  $("rookieEventButtonProgress").textContent = event.started ? `${event.completedDays || 0}/7` : "시작";
  callout?.classList.toggle("hidden", !shouldShowRookieEventCallout(event, player));
  if (!$("rookieEventModal").classList.contains("hidden")) {
    renderRookieEventModal(event);
  }
}

async function openRookieEventModal() {
  let event = state.player?.rookieEvent;
  if (!event?.visible) {
    return;
  }
  markRookieEventButtonSeenToday();
  renderRookieEvent(state.player);
  if (!event.started && event.startable) {
    const button = $("rookieEventButton");
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    try {
      setMessage("이벤트를 시작하는 중이에요.");
      const player = await requestWithLoginRetry(() => api("/api/player/rookie-event/start", { method: "POST" }));
      setServerPlayer(player, { resetDisplayGold: true });
      render();
      event = state.player?.rookieEvent;
      setMessage("7일 사냥 동행 이벤트가 시작됐어요.");
    } catch (error) {
      setMessage(error.message || "이벤트 시작에 실패했어요.");
      return;
    } finally {
      button.disabled = false;
      button.setAttribute("aria-busy", "false");
    }
  }
  if (!event?.started) {
    setMessage("이벤트를 시작할 수 없어요.");
    return;
  }
  state.rookieEventSelectedDay = event.currentDay || 1;
  $("rookieEventModal").classList.remove("hidden");
  renderRookieEventModal(event);
  window.setTimeout(() => requestRookieEventMissionNotificationAgreementIfNeeded(), 450);
}

function closeRookieEventModal() {
  const modal = $("rookieEventModal");
  if (modal) {
    modal.classList.add("hidden");
  }
}

function renderRookieEventModal(event) {
  const days = Array.isArray(event?.days) ? event.days : [];
  const selectedDay = days.find((day) => day.day === state.rookieEventSelectedDay)
    || days.find((day) => day.day === event.currentDay)
    || days[0];
  if (!selectedDay) {
    return;
  }
  state.rookieEventSelectedDay = selectedDay.day;
  $("rookieEventSummary").textContent = rookieEventSummaryText(event);
  $("rookieEventDaysLeft").textContent = event.rewardActive
    ? `이벤트 펫 ${rookieEventRewardRemainingText(event)}`
    : event.rewardClaimed
      ? "이벤트 펫 기간 종료"
    : event.expired
      ? "이벤트 종료"
      : `진행 기간 ${event.daysRemaining || 0}일 남음`;
  $("rookieEventProgress").textContent = `${event.completedDays || 0} / 7일 완료`;
  $("rookieEventRewardName").textContent = event.rewardName || "별빛토";
  renderRookieEventRewardCopy(event.rewardDescription || "일반 펫 15레벨 기준 성능 · 펫 보유 수 제한 미포함");
  $("rookieEventRewardStatus").textContent = rookieEventRewardStatusText(event);
  const finalClaimButton = $("rookieEventFinalClaimButton");
  const finalClaimable = rookieEventFinalRewardClaimable(event);
  finalClaimButton.classList.toggle("hidden", !finalClaimable);
  finalClaimButton.disabled = !finalClaimable;
  renderRookieEventDayTabs(days);
  renderRookieEventSelectedDay(selectedDay);
}

function shouldRequestRookieEventMissionNotificationAgreement(event) {
  const status = notificationAgreementStatus(rookieEventMissionNotificationAgreementStatusStorageKey);
  return shouldUseRealSmartMessage()
    && Boolean(state.rookieEventMissionNotificationAgreementTemplateCode)
    && !state.notificationAgreementInFlight
    && status !== "agreed"
    && status !== "rejected"
    && Boolean(event?.started)
    && Boolean(event?.active)
    && !event.expired
    && !event.completed
    && !event.rewardClaimed;
}

async function requestRookieEventMissionNotificationAgreementIfNeeded() {
  if (!shouldRequestRookieEventMissionNotificationAgreement(state.player?.rookieEvent)) {
    return;
  }
  try {
    const agreed = await requestNotificationAgreement(
      state.rookieEventMissionNotificationAgreementTemplateCode,
      rookieEventMissionNotificationAgreementStatusStorageKey,
    );
    if (agreed) {
      const player = await requestWithLoginRetry(() => api("/api/player/rookie-event/mission-notifications/agreement", { method: "POST" }));
      setServerPlayer(player, { resetDisplayGold: true });
    }
    setMessage(agreed ? "미션 알림 동의가 완료됐어요." : "미션 알림 동의를 완료하지 않았어요.");
  } catch (error) {
    removeStoredValue(rookieEventMissionNotificationAgreementStatusStorageKey);
    setMessage(error.message || "미션 알림 동의 기록에 실패했어요.");
  }
}

function rookieEventSummaryText(event) {
  if (event.rewardActive) {
    return `별빛토가 전투에 함께하고 있어요. ${rookieEventRewardRemainingText(event)}`;
  }
  if (event.rewardClaimed) {
    return "이벤트 펫 동행 기간이 종료됐어요.";
  }
  if (event.expired) {
    return "이벤트 기간이 종료됐어요.";
  }
  if (event.lockedUntilTomorrow) {
    return "오늘 미션을 완료했어요. 다음 일차는 내일 열려요.";
  }
  return "시작 후 10일 안에 7일 미션을 완료하고 이벤트 전용 펫을 받아요.";
}

function rookieEventRewardStatusText(event) {
  if (event.rewardActive) {
    return `전투 보너스 적용 중 · ${rookieEventRewardRemainingText(event)}`;
  }
  if (event.rewardClaimed) {
    return "이벤트 펫 기간 종료";
  }
  if (event.expired) {
    return "이벤트 종료";
  }
  if (rookieEventFinalRewardClaimable(event)) {
    return "보상 받기 버튼으로 획득 가능";
  }
  return "7일차 완료 후 수령 가능";
}

function rookieEventFinalRewardClaimable(event) {
  return Boolean(event?.completed && !event.rewardClaimed);
}

function renderRookieEventRewardCopy(description) {
  const target = $("rookieEventRewardCopy");
  const [main, sub] = String(description).split(" · ");
  target.replaceChildren(document.createTextNode(main || "일반 펫 15레벨 기준 성능"));
  if (sub) {
    target.appendChild(document.createElement("br"));
    target.appendChild(document.createTextNode(sub));
  }
}

function rookieEventRewardRemainingText(event = state.player?.rookieEvent) {
  const days = Number(event?.rewardDaysRemaining || 0);
  return days > 0 ? `${days}일 남음` : "오늘 종료";
}

function rookieEventRewardActive(player = state.player) {
  return Boolean(player?.rookieEvent?.rewardActive);
}

function rookieEventButtonSeenDateStorageKey(player = state.player) {
  const userKey = String(player?.userKey || "guest").trim() || "guest";
  return `${rookieEventButtonSeenDateStoragePrefix}.${userKey}`;
}

function rookieEventButtonSeenToday(player = state.player) {
  return storedValue(rookieEventButtonSeenDateStorageKey(player)) === localDateKey();
}

function markRookieEventButtonSeenToday(player = state.player) {
  storeValue(rookieEventButtonSeenDateStorageKey(player), localDateKey());
}

function shouldShowRookieEventCallout(event = state.player?.rookieEvent, player = state.player) {
  return Boolean(event?.visible)
    && !event.expired
    && !event.rewardClaimed
    && !event.rewardActive
    && !rookieEventButtonSeenToday(player);
}

function rookieHomeShortcutReturnRequested() {
  return new URLSearchParams(window.location.search).get(rookieHomeShortcutParam) === rookieHomeShortcutValue;
}

function clearRookieHomeShortcutMissionUrl() {
  const url = new URL(window.location.href);
  if (!url.searchParams.has(rookieHomeShortcutParam)) {
    return;
  }
  url.searchParams.delete(rookieHomeShortcutParam);
  window.history.replaceState(null, "", url);
}

function rookieEventMissionCompleted(player, missionKey) {
  const days = player?.rookieEvent?.days || [];
  return days.some((day) => (day.missions || []).some((mission) => mission.key === missionKey && mission.completed));
}

function rookieHomeShortcutMissionActive(player = state.player) {
  return (player?.rookieEvent?.days || []).some((day) => {
    const mission = (day?.missions || []).find((entry) => entry.key === "home_shortcut_return");
    return Boolean(day?.current && !day.locked && mission && !mission.completed);
  });
}

function markRookieHomeShortcutGuideViewed() {
  const marker = clientRequestId("home-shortcut");
  storeValue(rookieHomeShortcutGuideStorageKey, marker);
  try {
    window.sessionStorage.setItem(rookieHomeShortcutGuideSessionKey, marker);
  } catch {
    // Session storage can be unavailable in embedded test environments.
  }
}

function rookieHomeShortcutGuideViewedInPreviousSession() {
  const marker = storedValue(rookieHomeShortcutGuideStorageKey);
  if (!marker) {
    return false;
  }
  try {
    return window.sessionStorage.getItem(rookieHomeShortcutGuideSessionKey) !== marker;
  } catch {
    return true;
  }
}

function clearRookieHomeShortcutGuideViewed() {
  removeStoredValue(rookieHomeShortcutGuideStorageKey);
  try {
    window.sessionStorage.removeItem(rookieHomeShortcutGuideSessionKey);
  } catch {
    // Session storage can be unavailable in embedded test environments.
  }
}

function openHomeShortcutGuide() {
  markRookieHomeShortcutGuideViewed();
  state.homeShortcutGuideIndex = 0;
  $("homeShortcutGuideModal").classList.remove("hidden");
  renderHomeShortcutGuide();
}

function closeHomeShortcutGuide() {
  $("homeShortcutGuideModal").classList.add("hidden");
}

function renderHomeShortcutGuide() {
  const index = Math.max(0, Math.min(homeShortcutGuideSteps.length - 1, state.homeShortcutGuideIndex));
  const step = homeShortcutGuideSteps[index];
  state.homeShortcutGuideIndex = index;
  $("homeShortcutGuideCount").textContent = `${index + 1} / ${homeShortcutGuideSteps.length}`;
  $("homeShortcutGuideTitle").textContent = step.title;
  $("homeShortcutGuideBody").textContent = step.body;
  $("homeShortcutGuidePreview").dataset.step = step.preview;
  $("homeShortcutGuideBack").disabled = index === 0;
  $("homeShortcutGuideNext").textContent = index >= homeShortcutGuideSteps.length - 1 ? "확인" : "다음";
}

function nextHomeShortcutGuideStep() {
  if (state.homeShortcutGuideIndex >= homeShortcutGuideSteps.length - 1) {
    closeHomeShortcutGuide();
    return;
  }
  state.homeShortcutGuideIndex += 1;
  renderHomeShortcutGuide();
}

function previousHomeShortcutGuideStep() {
  if (state.homeShortcutGuideIndex <= 0) {
    return;
  }
  state.homeShortcutGuideIndex -= 1;
  renderHomeShortcutGuide();
}

async function completeRookieHomeShortcutMissionIfNeeded() {
  const shouldComplete = rookieHomeShortcutReturnRequested() || rookieHomeShortcutGuideViewedInPreviousSession();
  if (state.rookieHomeShortcutReturnHandled || !shouldComplete) {
    return;
  }
  state.rookieHomeShortcutReturnHandled = true;
  const wasActive = rookieHomeShortcutMissionActive(state.player);
  const wasCompleted = rookieEventMissionCompleted(state.player, "home_shortcut_return");
  try {
    const player = await api("/api/player/rookie-event/home-shortcut-return", { method: "POST" });
    applyServerPlayer(player);
    clearRookieHomeShortcutMissionUrl();
    const completed = rookieEventMissionCompleted(player, "home_shortcut_return");
    if (completed) {
      clearRookieHomeShortcutGuideViewed();
    }
    if (wasActive && !wasCompleted && completed) {
      setMessage("홈 화면 재접속 미션을 확인했어요.");
    }
  } catch (error) {
    state.rookieHomeShortcutReturnHandled = false;
    throw error;
  }
}

function renderRookieEventDayTabs(days) {
  const tabs = $("rookieEventDayTabs");
  tabs.replaceChildren(...days.map((day) => {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = `${day.day}일`;
    button.classList.toggle("is-current", day.current || day.day === state.rookieEventSelectedDay);
    button.classList.toggle("is-completed", day.completed);
    button.classList.toggle("is-locked", day.locked);
    button.addEventListener("click", () => {
      state.rookieEventSelectedDay = day.day;
      renderRookieEventModal(state.player?.rookieEvent);
    });
    return button;
  }));
}

function renderRookieEventSelectedDay(day) {
  $("rookieEventSelectedDayTitle").textContent = `${day.day}일차 · ${day.title}`;
  $("rookieEventSelectedDayState").textContent = rookieEventDayStateText(day);
  const missions = Array.isArray(day.missions) ? day.missions : [];
  $("rookieEventMissionList").replaceChildren(rookieEventDailyRewardElement(day), ...missions.map((mission) => {
    const isHomeShortcutMission = mission.key === "home_shortcut_return";
    const hasMissionAction = Boolean(mission.action && mission.actionEnabled && !mission.completed);
    const row = document.createElement("div");
    row.className = "rookie-event-mission";
    row.classList.toggle("is-completed", mission.completed);
    const label = document.createElement("strong");
    label.textContent = isHomeShortcutMission
      ? "머니헌터 홈 화면에 추가하고\n홈 화면에서 들어오기"
      : mission.label;
    label.classList.toggle("is-multiline", isHomeShortcutMission);
    const progress = document.createElement("span");
    progress.textContent = mission.completed ? "완료" : mission.progressText;
    const bar = document.createElement("i");
    bar.style.setProperty("--progress", `${Math.max(0, Math.min(100, mission.progressPercent || 0))}%`);
    row.append(label);
    if ((!isHomeShortcutMission && !hasMissionAction) || mission.completed) {
      row.append(progress);
    }
    if (isHomeShortcutMission && !mission.completed) {
      row.classList.add("has-action");
      const action = document.createElement("button");
      action.className = "rookie-event-mission-action";
      action.type = "button";
      action.textContent = "방법 보기";
      action.addEventListener("click", openHomeShortcutGuide);
      row.append(action);
    }
    if (hasMissionAction && mission.action === "CLAIM_SKILL_POINT_HELP") {
      row.classList.add("has-action");
      const action = document.createElement("button");
      action.className = "rookie-event-mission-action";
      action.type = "button";
      action.textContent = mission.actionLabel || "SP 받기";
      action.addEventListener("click", () => claimRookieEventSkillPointHelp(action));
      row.append(action);
    }
    row.append(bar);
    return row;
  }));
}

async function claimRookieEventSkillPointHelp(button) {
  if (button) {
    button.disabled = true;
    button.textContent = "지급 중";
  }
  try {
    const player = await requestWithLoginRetry(() => api("/api/player/rookie-event/skill-point-help", { method: "POST" }));
    setServerPlayer(player, { resetDisplayGold: true });
    render();
    setMessage("이벤트 SP 1개를 받았어요. 스킬을 강화해 미션을 완료해 보세요.");
  } catch (error) {
    setMessage(error.message || "이벤트 SP 지급에 실패했어요.");
    if (button) {
      button.disabled = false;
      button.textContent = "SP 받기";
    }
  }
}

function rookieEventDailyRewardElement(day) {
  const reward = document.createElement("div");
  reward.className = "rookie-event-day-reward";
  reward.classList.toggle("is-claimed", day.rewardClaimed);
  const title = document.createElement("strong");
  title.textContent = "일일 보상";
  const label = document.createElement("span");
  label.textContent = day.rewardLabel || "보상 확인";
  const actions = document.createElement("div");
  actions.className = "rookie-event-day-reward-actions";
  const stateText = document.createElement("small");
  stateText.textContent = rookieEventDailyRewardStateText(day);
  actions.append(stateText);
  if (day.rewardClaimable) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "rookie-event-claim";
    button.textContent = "보상 받기";
    button.addEventListener("click", () => claimRookieEventDailyReward(day.day, button));
    actions.append(button);
  }
  reward.append(title, label, actions);
  return reward;
}

function rookieEventDailyRewardStateText(day) {
  if (day.rewardClaimed) {
    return "지급 완료";
  }
  if (day.rewardClaimable) {
    return "수령 가능";
  }
  if (day.completed) {
    return "이전 보상 먼저 수령";
  }
  return "미션 완료 후 수령 가능";
}

async function claimRookieEventDailyReward(day, button) {
  await claimRookieEventReward(
    button,
    () => api(`/api/player/rookie-event/days/${day}/reward/claim`, { method: "POST" }),
    `${day}일차 일일 보상을 받았어요.`,
  );
}

async function claimRookieEventFinalReward(button) {
  await claimRookieEventReward(
    button,
    () => api("/api/player/rookie-event/final-reward/claim", { method: "POST" }),
    "이벤트 펫 별빛토를 받았어요.",
  );
}

async function claimRookieEventReward(button, request, message) {
  if (button) {
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
  }
  try {
    setMessage("보상을 지급하는 중이에요.");
    const player = await requestWithLoginRetry(request);
    setServerPlayer(player, { resetDisplayGold: true });
    setMessage(message);
    render();
    if (!$("rookieEventModal").classList.contains("hidden")) {
      renderRookieEventModal(state.player?.rookieEvent);
    }
  } catch (error) {
    setMessage(error.message || "보상 수령에 실패했어요.");
  } finally {
    if (button) {
      button.disabled = false;
      button.setAttribute("aria-busy", "false");
    }
  }
}

function rookieEventDayStateText(day) {
  if (day.completed) {
    return "완료";
  }
  if (day.locked) {
    return "대기";
  }
  return "진행 중";
}

function skillPointRewardsAvailable(player = state.player) {
  if (!player) {
    return true;
  }
  if (player.skillPointRewardsAvailable === false) {
    return false;
  }
  const spendableTypes = new Set(Object.keys(skillMeta));
  return !player.skills
    ?.filter((skill) => spendableTypes.has(skill.type))
    .every((skill) => skill.level >= (skillMeta[skill.type]?.max || skillMaxLevel));
}

function skillPointAdCooldownReady(player = state.player) {
  return !player?.nextSkillPointAdAvailableAt || !isActive(player.nextSkillPointAdAvailableAt);
}

function timeRewardAdCooldownReady(type, player = state.player) {
	  const nextAvailableAt = player?.nextAutoHuntAdAvailableAt;
	  return !nextAvailableAt || !isActive(nextAvailableAt);
	}

function nextTimeRewardAdAvailableAt(type, player = state.player) {
	  return player?.nextAutoHuntAdAvailableAt;
	}

function timeRewardCooldownLabel(currentEndAt, cooldownEndAt) {
  const remaining = remainingSecondsFrom(currentEndAt);
  const effectLabel = remaining > 0 ? remain(currentEndAt) : "꺼짐";
  return `${effectLabel} · 쿨 ${remain(cooldownEndAt)}`;
}

function timeRewardReadyLabel(currentEndAt, rewardSeconds, maxSeconds) {
  const remainingSeconds = remainingSecondsFrom(currentEndAt);
  if (remainingSeconds <= 0) {
    return `${rewardGateLabel()} · ${secondsLabel(rewardSeconds)}`;
  }
  const reward = Number(rewardSeconds || 3600);
  const max = Number(maxSeconds || 14400);
  return remainingSeconds + reward <= max
    ? `${remain(currentEndAt)} · 추가 가능`
    : remain(currentEndAt);
}

function timeRewardAvailability(currentEndAt, rewardSeconds, maxSeconds) {
  const reward = Number(rewardSeconds || 3600);
  const max = Number(maxSeconds || 14400);
  const remainingSeconds = remainingSecondsFrom(currentEndAt);
  const grantedSeconds = Math.max(0, Math.min(reward, max - remainingSeconds));
  return {
    available: true,
    capped: remainingSeconds + reward > max,
    grantedSeconds,
    maxSeconds: max,
    remainingSeconds,
    rewardSeconds: reward,
  };
}

function timeRewardGrantLabel(availability, fallbackSeconds) {
  return availability.grantedSeconds > 0
    ? timeRewardModalDurationLabel(availability.grantedSeconds || fallbackSeconds)
    : "최대 시간";
}

function timeRewardOverflowBody(rewardName, availability) {
  const remainingLabel = availability.remainingSeconds > 0
    ? timeRewardModalDurationLabel(availability.remainingSeconds)
    : "0분";
  const maxLabel = timeRewardModalDurationLabel(availability.maxSeconds);
  const particle = topicParticle(rewardName);
  if (availability.grantedSeconds <= 0) {
    return `${rewardName}${particle} 최대 ${maxLabel}까지 충전할 수 있어요.\n현재 ${remainingLabel}이 남아 있어, 광고를 봐도 더 추가되지 않아요.`;
  }
  const grantedLabel = timeRewardModalDurationLabel(availability.grantedSeconds);
  return `${rewardName}${particle} 최대 ${maxLabel}까지 충전할 수 있어요.\n현재 ${remainingLabel}이 남아 있어, 광고를 보면 ${grantedLabel}만 추가돼요.`;
}

function showTimeRewardOverflowModal(rewardName, availability, action) {
  state.timeRewardOverflowAction = action;
  $("timeRewardOverflowTitle").textContent = `${rewardName} 시간이 거의 가득 찼어요`;
  $("timeRewardOverflowBody").textContent = timeRewardOverflowBody(rewardName, availability);
  $("confirmTimeRewardOverflowAd").textContent = "광고 보고 충전하기";
  $("timeRewardOverflowModal").classList.remove("hidden");
}

function closeTimeRewardOverflowModal() {
  state.timeRewardOverflowAction = null;
  $("timeRewardOverflowModal").classList.add("hidden");
}

function confirmTimeRewardOverflowAd() {
  const action = state.timeRewardOverflowAction;
  closeTimeRewardOverflowModal();
  if (typeof action === "function") {
    action();
  }
}

function remainingSecondsFrom(value) {
  if (!value) {
    return 0;
  }
  const target = new Date(value).getTime();
  if (!target || Number.isNaN(target)) {
    return 0;
  }
  return Math.max(0, Math.ceil((target - Date.now()) / 1000));
}

function secondsLabel(seconds) {
  return timeRewardModalDurationLabel(seconds || 3600);
}

function timeRewardModalDurationLabel(seconds) {
  const totalMinutes = Math.max(0, Math.ceil(Number(seconds || 0) / 60));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours > 0 && minutes > 0) {
    return `${hours}시간 ${minutes}분`;
  }
  if (hours > 0) {
    return `${hours}시간`;
  }
  return `${minutes}분`;
}

function topicParticle(value) {
  const last = [...String(value || "")].pop();
  if (!last) {
    return "은";
  }
  const code = last.charCodeAt(0);
  if (code < 0xac00 || code > 0xd7a3) {
    return "는";
  }
  return (code - 0xac00) % 28 > 0 ? "은" : "는";
}

function showContentPanel(panel) {
  const showReward = panel === "reward";
  const showShop = panel === "shop";
  const showAdventure = panel === "adventure";
  document.querySelector(".game-shell").dataset.panel = showReward ? "reward" : showShop ? "shop" : showAdventure ? "adventure" : "skill";
  $("skillPanel").classList.toggle("hidden", showReward || showShop || showAdventure);
  $("adventurePanel").classList.toggle("hidden", !showAdventure);
  $("rewardPanel").classList.toggle("hidden", !showReward);
  $("shopPanel").classList.toggle("hidden", !showShop);
  $("showSkillPanel").classList.toggle("active", !showReward && !showShop && !showAdventure);
  $("showAdventurePanel").classList.toggle("active", showAdventure);
  $("showRewardPanel").classList.toggle("active", showReward);
  $("showShopPanel").classList.toggle("active", showShop);
  document.querySelector(".content-scroll")?.scrollTo({ top: 0 });
  if (showShop) {
    loadIapProductInfo();
  }
}

function closeJobModalIfReady() {
  if (state.player && !state.player.onboardingRequired) {
    state.forceJobModal = false;
    $("jobModal").classList.add("hidden");
  }
}

function applyJob(job) {
  const meta = jobMeta[job] || jobMeta.WARRIOR;
  const shell = document.querySelector(".game-shell");
  const hero = $("mainHero");
  const image = $("heroImage");
  const weapon = $("armImage");
  shell.dataset.job = job;
  hero.className = `hero-sprite ${meta.className}`;
  image.src = meta.image;
  weapon.className = `hero-weapon ${meta.weapon}`;
  weapon.src = meta.arm;
  $("partyName").textContent = `${meta.name} (Lv.${displayLevel()})`;
}

function applyEffectTier(player) {
  const shell = document.querySelector(".game-shell");
  const tier = Math.max(0, ...player.skills.map((skill) => skill.effectTier || 0));
  shell.dataset.effectTier = tier;
}

function renderSkills(player) {
  const activeStat = activeStatSkill();
  document.querySelectorAll(".stat-skill-row").forEach((row) => {
    row.classList.toggle("hidden", row.dataset.skillRow !== activeStat);
  });

  const skillsByType = new Map(player.skills.map((skill) => [skill.type, skill]));
  player.skills.forEach((skill) => {
    const meta = skillMeta[skill.type];
    if (!meta) {
      return;
    }
    const maxLevel = meta.max || skillMaxLevel;
    const locked = isPetSkillLocked(skill.type, player);
    const current = skill.level * (meta.step || 0);
    const next = Math.min(maxLevel, skill.level + 1) * (meta.step || 0);
    const levelElement = document.querySelector(`[data-skill-level="${skill.type}"]`);
    const effectElement = document.querySelector(`[data-skill-effect="${skill.type}"]`);
    const tierElement = document.querySelector(`[data-skill-tier="${skill.type}"]`);
    const iconElement = document.querySelector(`[data-effect-icon="${skill.type}"]`);
    const labelElement = document.querySelector(`[data-skill-label="${skill.type}"]`);
    if (labelElement && meta.petIndex) {
      labelElement.textContent = `${petSkinForSlot(meta.petIndex, player).name} 공격`;
      const portrait = document
        .querySelector(`[data-skill-row="${skill.type}"]`)
        ?.querySelector(".skill-portrait");
      if (portrait) {
        portrait.style.backgroundImage = `url("${petSkinForSlot(meta.petIndex, player).image}")`;
      }
    }
    if (levelElement) {
      levelElement.textContent = `Lv.${skill.level}`;
    }
    if (effectElement) {
      if (locked) {
        effectElement.textContent = "동료 펫 잠금 해제 필요";
      } else if (meta.petIndex) {
        effectElement.textContent = petSkillEffectText(skill, maxLevel, meta);
      } else {
        effectElement.textContent = skill.level >= maxLevel
        ? `${meta.short} +${formatPercentValue(current)}% · MAX`
        : `${meta.short} +${formatPercentValue(current)}% → +${formatPercentValue(next)}%`;
      }
    }
    if (tierElement) {
      tierElement.textContent = locked
        ? `${meta.petIndex}번째 동료 펫을 상점에서 구매해야 해요`
        : meta.effectPrefix
        ? skillEffectTierText(skill, maxLevel)
        : meta.petIndex
          ? `${petSkinForSlot(meta.petIndex, player).name} 공격 피해량이 올라가요`
          : meta.description;
    }
    if (iconElement) {
      const previewTier = Math.max(0, skill.effectTier || 0);
      iconElement.dataset.tier = previewTier;
    }
  });
  renderRookieEventSkillRow(player);

  document.querySelectorAll(".upgrade-skill").forEach((button) => {
    if (!button.dataset.skill) {
      return;
    }
    const skill = skillsByType.get(button.dataset.skill);
    const meta = skillMeta[button.dataset.skill];
    const maxLevel = meta?.max || skillMaxLevel;
    const locked = isPetSkillLocked(button.dataset.skill, player);
    const isMaxed = !skill || skill.level >= maxLevel;
    const upgradeCost = skillUpgradeCost(skill);
    button.disabled = locked || player.skillPoints < upgradeCost || isMaxed;
    button.innerHTML = locked
      ? "<span>잠금</span><small>펫 필요</small>"
      : isMaxed
        ? "<span>MAX</span><small>완료</small>"
        : `<span>강화</span><small>SP ${upgradeCost}</small>`;
  });
}

function skillUpgradeCost(skill) {
  return Math.max(1, Number(skill?.upgradeCost || 1));
}

function renderRookieEventSkillRow(player = state.player) {
  const row = $("rookieEventSkillRow");
  const event = player?.rookieEvent;
  const active = Boolean(event?.rewardActive);
  row.classList.toggle("hidden", !active);
  if (!active) {
    return;
  }
  const level = Number(event.eventPetSkillLevel || 15);
  const damage = 4 + scaledSkillValue(level, 40);
  $("rookieEventSkillRemaining").textContent = rookieEventRewardRemainingText(event);
  $("rookieEventSkillEffect").textContent = `공격 피해량 +${damage} · 보상 효율 보너스 적용`;
  $("rookieEventSkillTier").textContent = "일반 펫 15레벨 기준 성능 · 스킨 변경 불가";
}

function formatPercentValue(value) {
  const rounded = Math.round(value * 10) / 10;
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
}

function isPetSkillLocked(type, player = state.player) {
  const petIndex = skillMeta[type]?.petIndex;
  return Boolean(petIndex && unlockedPetCount(player) < petIndex);
}

function petSkillEffectText(skill, maxLevel, meta) {
  const maxDamage = meta.maxDamage || 40;
  const damage = Math.round((maxDamage * skill.level) / maxLevel);
  const nextDamage = Math.round((maxDamage * Math.min(maxLevel, skill.level + 1)) / maxLevel);
  const skinName = meta.petIndex ? petSkinForSlot(meta.petIndex).name : "펫";
  return skill.level >= maxLevel
    ? `${skinName} 피해량 +${damage} · MAX`
    : `${skinName} 피해량 +${damage} → +${nextDamage}`;
}

function skillLevel(type) {
  return state.player?.skills.find((skill) => skill.type === type)?.level || 0;
}

function skillTier(type) {
  const skill = state.player?.skills.find((item) => item.type === type);
  if (!skill) {
    return 0;
  }
  return Math.max(skill.effectTier || 0, skillEffectTierFromLevel(skill.level));
}

function skillEffectTierFromLevel(level) {
  const safeLevel = Math.max(0, Number(level || 0));
  if (safeLevel >= 21) {
    return 5;
  }
  if (safeLevel >= 16) {
    return 4;
  }
  if (safeLevel >= 11) {
    return 3;
  }
  if (safeLevel >= 6) {
    return 2;
  }
  return 1;
}

function skillEffectTierText(skill, maxLevel = skillMaxLevel) {
  const level = Math.max(0, Number(skill?.level || 0));
  const tier = Math.max(skill?.effectTier || 0, skillEffectTierFromLevel(level));
  if (tier >= skillEffectMaxTier || level >= maxLevel) {
    return `이펙트 ${tier}단계 · 최고 단계`;
  }
  const nextTierLevel = skillEffectTierStartLevels[tier] || maxLevel;
  const remainingLevels = Math.max(1, nextTierLevel - level);
  return `이펙트 ${tier}단계 · 다음 이펙트까지 ${remainingLevels}레벨`;
}

function effectImageFor(type, tier) {
  const prefix = skillMeta[type]?.effectPrefix;
  if (!prefix || tier < 1) {
    return "";
  }
  return `/assets/effects/${prefix}-tier-${tier}.png?v=${effectAssetVersion}`;
}

function updateGoldViews() {
  const value = Math.floor(state.displayGold).toLocaleString("ko-KR");
  $("gold").textContent = `${value} 골드`;
}

function isActive(value) {
  return value && new Date(value).getTime() > Date.now();
}

function isActiveAt(value, timeMs) {
  return value && new Date(value).getTime() > timeMs;
}

function localGoldPerHour(player) {
  if (player?.goldPerHour !== undefined) {
    return player.goldPerHour;
  }
  if (player?.baseGoldPerHour !== undefined) {
    return player.baseGoldPerHour;
  }
  return 0;
}

function applyLocalMonsterHit(player = state.player) {
  if (!player) {
    return { defeated: false, leveledUp: false };
  }
  let monster = displayMonster(player);
  let remainingDamage = localDamage(player);
  let defeated = false;
  let leveledUp = false;
  while (remainingDamage > 0 && monster) {
    if (remainingDamage < monster.hp) {
      monster.hp = Math.max(0, monster.hp - remainingDamage);
      remainingDamage = 0;
      break;
    }
    remainingDamage -= monster.hp;
    monster.hp = 0;
    defeated = true;
    leveledUp = recordLocalMonsterDefeat(monster) || leveledUp;
    monster = nextLocalMonster(monster);
    state.localMonster = monster;
  }
  return { defeated, leveledUp };
}

function localDamage(player) {
  const rapidLevel = skillLevel("RAPID_ATTACK");
  const statLevel = skillLevel(activeStatSkill());
  let damage = 16
    + displayLevel(player) * 2
    + scaledSkillValue(rapidLevel, 40)
    + scaledSkillValue(statLevel, 60)
    + localPetDamage(player);
  return Math.max(1, damage);
}

function localPetDamage(player) {
  const pets = unlockedPetCount(player);
  let damage = pets * 4;
  if (pets >= 1) {
    damage += scaledSkillValue(skillLevel("PET_FLARE_ATTACK"), 40);
  }
  if (pets >= 2) {
    damage += scaledSkillValue(skillLevel("PET_AQUA_ATTACK"), 40);
  }
  if (rookieEventRewardActive(player)) {
    damage += 4 + scaledSkillValue(player?.rookieEvent?.eventPetSkillLevel || 15, 40);
  }
  return damage;
}

function scaledSkillValue(level, maxValue) {
  return Math.round(maxValue * Number(level || 0) / skillMaxLevel);
}

function recordLocalMonsterDefeat(monster) {
  const gainedExp = 8 + Math.floor((monster.defeatedMonsters || 0) / 2);
  state.localExperience = displayExperience() + gainedExp;
  state.localLevel = displayLevel();
  state.localNextLevelExperience = displayNextLevelExperience();
  let leveledUp = false;
  while (state.localExperience >= state.localNextLevelExperience) {
    state.localExperience -= state.localNextLevelExperience;
    state.localLevel += 1;
    state.localNextLevelExperience = nextLevelExperience(state.localLevel);
    leveledUp = true;
  }
  return leveledUp;
}

function nextLocalMonster(monster) {
  const defeatedMonsters = (monster.defeatedMonsters || 0) + 1;
  const key = randomNextMonsterKey(monster.key);
  const maxHp = localMaxMonsterHp(defeatedMonsters, key);
  return {
    key,
    hp: maxHp,
    maxHp,
    defeatGold: Math.max(1, Math.floor((state.player?.goldPerHour || 0) * 0.25 / 60)),
    defeatedMonsters,
  };
}

function randomNextMonsterKey(currentKey) {
  if (monsterKeys.length <= 1) {
    return monsterKeys[0];
  }
  let next = currentKey;
  while (next === currentKey) {
    next = monsterKeys[Math.floor(Math.random() * monsterKeys.length)];
  }
  return next;
}

function localMaxMonsterHp(defeatedMonsters, key) {
  return (monsterBaseHp[key] || 120) + defeatedMonsters * 18;
}

function nextLevelExperience(level) {
  return 1000 + level * level * 500;
}

function accrueLocalCombatGold(now = Date.now()) {
  const player = state.player;
  if (!player || !isActiveAt(player.autoHuntEndsAt, now - 1)) {
    state.lastLocalGoldEstimateAt = now;
    return 0;
  }
  const fromMs = state.lastLocalGoldEstimateAt || state.lastCombatSyncAt || now;
  const autoHuntEndsAt = new Date(player.autoHuntEndsAt).getTime();
  const toMs = Math.min(now, autoHuntEndsAt);
  if (toMs <= fromMs) {
    state.lastLocalGoldEstimateAt = now;
    return 0;
  }

  const gainedGold = localGoldPerHour(player) * (toMs - fromMs) / 3_600_000;
  state.displayGold = Math.max(state.displayGold, player.gold) + Math.max(0, gainedGold);
  state.lastLocalGoldEstimateAt = toMs;
  return gainedGold;
}

function remain(value) {
  const seconds = Math.max(0, Math.floor((new Date(value).getTime() - Date.now()) / 1000));
  if (seconds <= 0) {
    return "꺼짐";
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return hours > 0 ? `${hours}시간 ${minutes}분` : `${minutes}분`;
}

function formatShortTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "방금";
  }
  return date.toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function showDummyAd(title, description, action) {
  if (!state.mockMonetizationEnabled) {
    setMessage("광고 설정이 아직 완료되지 않았어요.");
    return;
  }
  state.adAction = action;
  $("adTitle").textContent = title;
  $("adDescription").textContent = description;
  $("adProgress").style.width = "100%";
  $("adSkip").disabled = false;
  $("adSkip").classList.add("ready");
  $("adSkip").textContent = "광고 완료하고 보상 받기";
  $("adModal").classList.remove("hidden");
  window.clearInterval(state.adTimer);
}

async function runRealFullScreenAd(title, description, action) {
  if (state.realAdInFlight) {
    setMessage("이미 광고를 준비하고 있어요.");
    return;
  }
  const groupId = adGroupId(action.adGroupKey);
  if (!groupId) {
    setMessage("광고 그룹 ID가 아직 설정되지 않았어요.");
    return;
  }
  state.realAdInFlight = true;
  setMessage(isRealFullScreenAdLoaded(groupId) ? `${title} 여는 중...` : `${title} 준비 중...`);
  try {
    const adSession = action.requiresReward === false
      ? null
      : await api("/api/player/ads/sessions", {
          method: "POST",
          body: JSON.stringify({ type: action.adEventType }),
        });
    await loadRealFullScreenAd(groupId);
    markRealFullScreenAdConsumed(groupId);
    await showRealFullScreenAdWithRetry(groupId, action.requiresReward !== false);
    setMessage("광고 시청 완료, 보상을 지급하는 중이에요.");
    if (typeof action.afterAd === "function") {
      await action.afterAd(adSession?.sessionToken);
    } else {
      await run(() => action.request(adSession?.sessionToken), action.message);
    }
  } catch (error) {
    const message = error?.message || "광고를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
    setMessage(message);
  } finally {
    state.realAdInFlight = false;
    window.setTimeout(() => scheduleRealFullScreenAdPreloads(), 800);
  }
}

async function loadRealFullScreenAd(groupId) {
  const entry = realAdPreloadEntry(groupId);
  if (entry.status === "loaded" && Date.now() - entry.loadedAt < realAdLoadedTtlMs) {
    return;
  }
  if (entry.status === "loaded") {
    markRealFullScreenAdConsumed(groupId);
  }
  if (entry.status === "loading" && entry.promise) {
    return entry.promise;
  }
  entry.status = "loading";
  entry.promise = requestRealFullScreenAdLoad(groupId)
    .then(() => {
      entry.status = "loaded";
      entry.loadedAt = Date.now();
      entry.error = null;
    })
    .catch((error) => {
      entry.status = "failed";
      entry.failedAt = Date.now();
      entry.error = error;
      throw error;
    })
    .finally(() => {
      entry.promise = null;
    });
  return entry.promise;
}

async function loadFreshRealFullScreenAd(groupId) {
  markRealFullScreenAdConsumed(groupId);
  return loadRealFullScreenAd(groupId);
}

async function requestRealFullScreenAdLoad(groupId) {
  const sdk = await loadTossSdk();
  const loadFullScreenAd = sdk.loadFullScreenAd;
  if (typeof loadFullScreenAd !== "function" || loadFullScreenAd.isSupported?.() !== true) {
    throw new Error("현재 토스 앱에서 광고를 사용할 수 없어요.");
  }
  return new Promise((resolve, reject) => {
    let unregister = null;
    const timeout = window.setTimeout(() => {
      unregister?.();
      reject(new Error("광고 준비 시간이 초과됐어요. 잠시 후 다시 시도해 주세요."));
    }, 12000);
    unregister = loadFullScreenAd({
      options: { adGroupId: groupId },
      onEvent: (event) => {
        if (event.type === "loaded") {
          window.clearTimeout(timeout);
          unregister?.();
          resolve();
        }
      },
      onError: (error) => {
        window.clearTimeout(timeout);
        unregister?.();
        reject(error instanceof Error ? error : new Error("광고 로드에 실패했어요."));
      },
    });
  });
}

async function showRealFullScreenAd(groupId, requiresReward) {
  const sdk = await loadTossSdk();
  const showFullScreenAd = sdk.showFullScreenAd;
  if (typeof showFullScreenAd !== "function" || showFullScreenAd.isSupported?.() !== true) {
    throw new Error("현재 토스 앱에서 광고를 표시할 수 없어요.");
  }
  return new Promise((resolve, reject) => {
    let completed = false;
    let unregister = null;
    const finish = (result, error) => {
      if (completed) {
        return;
      }
      completed = true;
      unregister?.();
      error ? reject(error) : resolve(result);
    };
    unregister = showFullScreenAd({
      options: { adGroupId: groupId },
      onEvent: (event) => {
        if (requiresReward && event.type === "userEarnedReward") {
          finish("reward");
          return;
        }
        if (!requiresReward && event.type === "impression") {
          finish("impression");
          return;
        }
        if (event.type === "failedToShow") {
          const error = new Error("광고가 아직 준비되지 않았어요. 다시 준비하고 있어요.");
          error.retryableAdShowFailure = true;
          finish(null, error);
          return;
        }
        if (event.type === "dismissed" && requiresReward && !completed) {
          finish(null, new Error("광고 시청을 완료해야 보상이 지급돼요."));
        }
      },
      onError: (error) => {
        const nextError = error instanceof Error ? error : new Error("광고 표시가 실패했어요.");
        nextError.retryableAdShowFailure = true;
        finish(null, nextError);
      },
    });
  });
}

async function showRealFullScreenAdWithRetry(groupId, requiresReward) {
  try {
    return await showRealFullScreenAd(groupId, requiresReward);
  } catch (error) {
    if (!error?.retryableAdShowFailure) {
      throw error;
    }
    setMessage("광고를 다시 준비하고 있어요.");
    await loadFreshRealFullScreenAd(groupId);
    markRealFullScreenAdConsumed(groupId);
    return showRealFullScreenAd(groupId, requiresReward);
  }
}

async function runRealIapPurchase(action) {
  if (state.realPaymentInFlight) {
    setMessage("이미 결제를 진행하고 있어요.");
    return;
  }
  const productId = action.productId?.();
  if (!productId) {
    setMessage("인앱 상품 ID가 아직 설정되지 않았어요.");
    return;
  }
  state.realPaymentInFlight = true;
  setMessage(`${action.title || "상품"} 결제 준비 중...`);
  try {
    const result = await createIapOrder(productId, action);
    if (!result?.state) {
      throw new Error("상품 지급 결과를 확인하지 못했어요.");
    }
    setServerPlayer(result.state);
    setMessage(action.message || "상품이 지급됐어요.");
    render();
  } catch (error) {
    setMessage(iapErrorMessage(error));
  } finally {
    state.realPaymentInFlight = false;
  }
}

function applyGrantedIapState(stateAfterGrant, message = "상품이 지급됐어요.") {
  setServerPlayer(stateAfterGrant);
  setMessage(message);
  render();
}

function grantIapProduct(orderId, productId) {
  return requestWithLoginRetry(() => api("/api/player/shop/iap/grant", {
    method: "POST",
    body: JSON.stringify({ orderId, productId }),
  }));
}

function isKnownIapProductId(productId) {
  const normalized = String(productId || "").trim();
  return Boolean(normalized && Object.values(state.iapProductIds || {}).includes(normalized));
}

function pendingOrderProductId(order) {
  const candidates = [order?.sku, order?.productId, order?.productItemId];
  const directProductId = candidates.find(isKnownIapProductId);
  if (directProductId) {
    return directProductId;
  }
  const rememberedProductId = state.pendingIapProductIdsByOrderId[order?.orderId];
  return isKnownIapProductId(rememberedProductId) ? rememberedProductId : "";
}

function schedulePendingIapRestoreRetries() {
  if (!shouldUseRealPayments() || !hasPendingIapProduct()) {
    return;
  }
  [300, 1000, 3000, 7000, 15000].forEach((delayMs) => {
    window.setTimeout(() => {
      restorePendingIapOrders();
    }, delayMs);
  });
}

function hasPendingIapProduct() {
  return Object.keys(state.pendingIapProductIdsByOrderId || {}).length > 0;
}

function createIapOrder(productId, action) {
  return new Promise(async (resolve, reject) => {
    const sdk = await loadTossSdk().catch(reject);
    if (!sdk?.IAP?.createOneTimePurchaseOrder) {
      reject(new Error("현재 토스 앱에서 인앱 결제를 사용할 수 없어요."));
      return;
    }
    let cleanup = null;
    cleanup = sdk.IAP.createOneTimePurchaseOrder({
      options: {
        sku: productId,
        processProductGrant: ({ orderId }) => {
          rememberPendingIapProduct(orderId, productId);
          setMessage("구매 상품을 지급하는 중...");
          schedulePendingIapRestoreRetries();

          if (isReviewIapSandboxMode()) {
            grantIapProduct(orderId, productId)
              .then((stateAfterGrant) => {
                forgetPendingIapProduct(orderId);
                applyGrantedIapState(stateAfterGrant, action.message || "상품이 지급됐어요.");
                resolve({ state: stateAfterGrant, orderId });
              })
              .catch((error) => {
                reject(error);
                setMessage(error.message || "상품 지급에 실패했어요.");
              });
            return true;
          }

          return grantIapProduct(orderId, productId)
            .then((stateAfterGrant) => {
              forgetPendingIapProduct(orderId);
              resolve({ state: stateAfterGrant, orderId });
              return true;
            })
            .catch((error) => {
              reject(error);
              return false;
            });
        },
      },
      onEvent: (event) => {
        if (event.type === "success") {
          if (event.data?.sku) {
            state.iapProducts[event.data.sku] = event.data;
          }
          schedulePendingIapRestoreRetries();
          cleanup?.();
        }
      },
      onError: (error) => {
        schedulePendingIapRestoreRetries();
        cleanup?.();
        reject(error instanceof Error ? error : new Error(error?.message || "인앱 결제가 취소되었거나 실패했어요."));
      },
    });
  });
}

async function completeIapProductGrantIfSupported(sdk, orderId) {
  if (typeof sdk?.IAP?.completeProductGrant !== "function") {
    return;
  }
  try {
    await sdk.IAP.completeProductGrant({ params: { orderId } });
  } catch {
    // createOneTimePurchaseOrder can complete the grant automatically after
    // processProductGrant returns true, so duplicate completion failures are safe to ignore.
  }
}

async function restorePendingIapOrders() {
  if (isOneStoreTarget() || state.mockMonetizationEnabled || !state.realPaymentsEnabled) {
    return;
  }
  if (!hasPendingIapProduct()) {
    return;
  }
  if (state.pendingIapRestoreInFlight) {
    return;
  }
  state.pendingIapRestoreInFlight = true;
  const sdk = await loadTossSdk().catch(() => null);
  if (typeof sdk?.IAP?.getPendingOrders !== "function") {
    state.pendingIapRestoreInFlight = false;
    return;
  }
  try {
    const response = await sdk.IAP.getPendingOrders().catch(() => null);
    const orders = Array.isArray(response?.orders) ? response.orders : [];
    for (const order of orders) {
      const productId = pendingOrderProductId(order);
      if (!order?.orderId || !productId) {
        continue;
      }
      try {
        const stateAfterGrant = await grantIapProduct(order.orderId, productId);
        await completeIapProductGrantIfSupported(sdk, order.orderId);
        forgetPendingIapProduct(order.orderId);
        setServerPlayer(stateAfterGrant);
        setMessage("대기 중이던 구매 상품을 지급했어요.");
        render();
      } catch (error) {
        setMessage(error.message || "대기 중인 구매 상품 지급에 실패했어요.");
        break;
      }
    }
  } finally {
    state.pendingIapRestoreInFlight = false;
  }
}

function iapErrorMessage(error) {
  const code = error?.code || error?.errorCode;
  if (code === "USER_CANCELED") {
    return "결제가 취소됐어요.";
  }
  if (code === "ITEM_ALREADY_OWNED") {
    return "이미 보유한 상품이에요.";
  }
  return error?.message || "인앱 결제에 실패했어요.";
}

async function runFriendInviteRewardFlow() {
  if (isOneStoreTarget()) {
    return api("/api/player/onestore/friend-invite/claim", { method: "POST" });
  }
  if (state.mockMonetizationEnabled) {
    return api("/api/player/reward/friend-invite/claim", { method: "POST" });
  }
  if (!shouldUseRealShareReward()) {
    throw new Error("공유 리워드 설정이 아직 완료되지 않았어요.");
  }
  if (!state.shareRewardModuleId) {
    throw new Error("공유 리워드 모듈 ID가 아직 설정되지 않았어요.");
  }
  if (state.shareRewardInFlight) {
    throw new Error("이미 공유 리워드를 준비하고 있어요.");
  }
  state.shareRewardInFlight = true;
  try {
    const completedInvites = await requestTossShareReward();
    return claimFriendInviteReward(completedInvites);
  } finally {
    state.shareRewardInFlight = false;
  }
}

function claimFriendInviteReward(completedInvites = 1) {
  return api("/api/player/reward/friend-invite/claim", {
    method: "POST",
    body: JSON.stringify({ completedInvites }),
  });
}

function dungeonAdditionalEntryRequired(player = state.player) {
  const dungeon = player?.dungeonCoupon || {};
  const runsToday = dungeon.dungeonRunsToday || 0;
  const freeLimit = dungeon.dungeonFreeDailyLimit ?? dungeonFreeDailyLimit;
  return runsToday >= freeLimit;
}

function dungeonRunRequest(adSessionToken = null) {
  const additionalEntry = dungeonAdditionalEntryRequired();
  return api(additionalEntry ? "/api/player/ads/dungeon-additional/complete" : "/api/player/dungeon/run", {
    method: "POST",
    body: additionalEntry ? JSON.stringify({ adSessionToken }) : undefined,
  });
}

async function useDungeonCoupon() {
  if (dungeonAdditionalEntryRequired()) {
    setMessage("광고를 보면 던전에 추가 입장할 수 있어요.");
    return runRewardFlow(
      "던전 추가 입장 광고",
      "완료하면 오늘 던전에 한 번 더 입장해요.",
      {
        adGroupKey: "dungeonAdditional",
        adEventType: "DUNGEON_ADDITIONAL_ENTRY",
        requiresReward: true,
        afterAd: (adSessionToken) => runAdventureAction("dungeon", () => requestWithLoginRetry(() => dungeonRunRequest(adSessionToken))),
      }
    );
  }
  setMessage("던전 탐험을 시작했어요.");
  await runAdventureAction("dungeon", () => requestWithLoginRetry(() => dungeonRunRequest()));
}

async function challengeBossRaid() {
  setMessage("보스 토벌을 시작했어요.");
  await runAdventureAction("boss", () => requestWithLoginRetry(() => api("/api/player/boss/raid", { method: "POST" })));
}

function showRewardClaimConfirmModal() {
  const points = availableRewardPointAmount();
  $("rewardConfirmBody").textContent = isOneStoreTarget()
    ? "확인을 누르면 게임 내 보상을 바로 받아요."
    : `확인을 누르면 현재 받을 수 있는 ${points.toLocaleString("ko-KR")}P 수령을 신청해요.`;
  $("rewardConfirmModal").classList.remove("hidden");
}

function closeRewardClaimConfirmModal() {
  $("rewardConfirmModal").classList.add("hidden");
}

function claimRewardAfterConfirmation() {
  closeRewardClaimConfirmModal();
  return runRewardFlow(
    "리워드 수령 광고",
    isOneStoreTarget()
      ? "게임 내 보상으로 SP가 지급돼요."
      : "완료하면 토스포인트 수령을 신청해요.",
    {
      adGroupKey: "rewardClaim",
      adEventType: "REWARD_CLAIM",
      requiresReward: true,
      request: (adSessionToken) => api(isOneStoreTarget()
        ? "/api/player/onestore/reward/claim"
        : "/api/player/reward/claim-after-ad", {
        method: "POST",
        body: isOneStoreTarget()
          ? undefined
          : JSON.stringify({ idempotencyKey: clientRequestId("reward"), adSessionToken }),
      }),
      message: isOneStoreTarget() ? "게임 내 보상을 받았어요." : "리워드 수령을 완료했어요.",
    }
  );
}

function shouldUseGameCenter() {
  return !isOneStoreTarget() && Boolean(state.tossLoginEnabled);
}

function gameProfileNickname(player = state.player) {
  return String(player?.gameProfileNickname || "").trim();
}

function canSyncGameProfile() {
  return shouldUseGameCenter() && isTossMiniRuntime() && Boolean(state.player) && !state.player?.onboardingRequired;
}

function shouldDeferGameProfileEditor() {
  return (
    !canSyncGameProfile()
    || state.featureTutorialActive
    || state.player?.featureTutorialRequired === true
    || !state.player?.job
  );
}

function gameProfileSyncLogValue(value, maxLength = 160) {
  const normalized = String(value || "").trim();
  return normalized.length > maxLength ? normalized.slice(0, maxLength) : normalized;
}

function gameProfileSyncRuntime() {
  if (isTossMiniRuntime()) {
    return "toss-mini";
  }
  return "web";
}

function gameProfileSyncErrorMessage(error) {
  if (!error) {
    return "";
  }
  return gameProfileSyncLogValue(error.message || error.code || error.name || String(error));
}

function reportGameProfileSyncEvent(event = {}) {
  if (!shouldUseGameCenter() || !state.authToken) {
    return;
  }
  const eventKey = [
    event.statusCode || "",
    event.source || "auto",
    event.message || "",
  ].join(":");
  const now = Date.now();
  if (eventKey === state.lastGameProfileSyncEventKey && now - state.lastGameProfileSyncEventAt < 30 * 1000) {
    return;
  }
  state.lastGameProfileSyncEventKey = eventKey;
  state.lastGameProfileSyncEventAt = now;
  const body = {
    statusCode: gameProfileSyncLogValue(event.statusCode, 40),
    source: gameProfileSyncLogValue(event.source || "auto", 40),
    runtime: gameProfileSyncRuntime(),
    hostname: gameProfileSyncLogValue(window.location.hostname, 120),
    appName: gameProfileSyncLogValue(window.__appsInToss?.appName || appsInTossAppName(), 80),
    webViewType: gameProfileSyncLogValue(window.__appsInToss?.webViewType || "", 40),
    sdkAvailable: Boolean(window.MoneyHunterTossSdk || window.ReactNativeWebView || window.__appsInToss),
    message: gameProfileSyncLogValue(event.message),
  };
  fetch(apiUrl("/api/player/game-profile/sync-events"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authRequestHeaders(),
    },
    body: JSON.stringify(body),
  }).catch(() => {});
}

function cancelScheduledGameProfileSync() {
  window.clearTimeout(state.gameProfileSyncTimer);
  state.gameProfileSyncTimer = null;
}

function scheduleGameProfileSync(delayMs = 1200) {
  if (!canSyncGameProfile()) {
    return;
  }
  if (Date.now() - state.lastGameProfileSyncAt < 5 * 60 * 1000) {
    return;
  }
  if (Date.now() - state.lastGameProfileMissingAt < 60 * 1000) {
    return;
  }
  cancelScheduledGameProfileSync();
  state.gameProfileSyncTimer = window.setTimeout(() => {
    state.gameProfileSyncTimer = null;
    ensureGameProfile({ silent: true, openIfMissing: true, source: "auto" }).catch(() => {});
  }, delayMs);
}

async function gameCenterProfileBridge() {
  const sdk = await loadTossSdk();
  if (typeof sdk.getGameCenterGameProfile === "function") {
    return sdk;
  }
  return loadTossBridge();
}

function syncedProfileNickname(profile) {
  if (profile?.statusCode !== "SUCCESS") {
    return "";
  }
  return String(profile.nickname || profile.name || profile.profile?.nickname || "").trim();
}

function appsInTossAppName() {
  return String(window.__appsInToss?.appName || "gold-hunter").trim() || "gold-hunter";
}

function gameProfileEditorUrl() {
  const url = new URL(gameProfileWebviewUrl);
  url.searchParams.set("appName", appsInTossAppName());
  url.searchParams.set("referrer", `appsintoss.${appsInTossAppName()}`);
  return url.toString();
}

async function openTransparentServiceWeb(innerUrl, onClose) {
  const bridge = await loadTossBridge();
  const sdk = window.MoneyHunterTossSdk || {};
  const openURL = bridge.openURL || sdk.openURL || (await loadTossSdk()).openURL;
  if (typeof openURL !== "function") {
    throw new Error("토스 앱에서 게임 프로필 화면을 열 수 없어요.");
  }

  const url = new URL("supertoss://transparent-service-web");
  url.searchParams.set("url", innerUrl);
  if (typeof bridge.onVisibilityChangedByTransparentServiceWeb === "function") {
    const callbackId = `moneyHunterProfile_${Date.now()}_${Math.random().toString(16).slice(2)}`;
    url.searchParams.set("onVisibilityChangeCallback", callbackId);
    return new Promise((resolve, reject) => {
      let finished = false;
      let cleanup = null;
      const finish = () => {
        if (finished) {
          return;
        }
        finished = true;
        cleanup?.();
        Promise.resolve(onClose?.()).then(resolve, reject);
      };
      cleanup = bridge.onVisibilityChangedByTransparentServiceWeb({
        options: { callbackId },
        onEvent(value) {
          if (value === true) {
            finish();
          }
        },
        onError(error) {
          if (!finished) {
            cleanup?.();
            reject(error);
          }
        },
      });
      Promise.resolve(openURL(url.toString())).catch((error) => {
        cleanup?.();
        reject(error);
      });
    });
  }

  await openURL(url.toString());
  return new Promise((resolve, reject) => {
    window.setTimeout(() => Promise.resolve(onClose?.()).then(resolve, reject), 1400);
  });
}

async function syncGameProfileIfNeeded({ force = false, silent = true, source = "auto" } = {}) {
  if (!canSyncGameProfile() || state.gameProfileSyncInFlight) {
    reportGameProfileSyncEvent({
      statusCode: "SKIPPED",
      source,
      message: !canSyncGameProfile() ? "canSyncGameProfile=false" : "sync-in-flight",
    });
    return { statusCode: "SKIPPED" };
  }
  const now = Date.now();
  if (!force && now - state.lastGameProfileSyncAt < 5 * 60 * 1000) {
    return { statusCode: "COOLDOWN" };
  }
  state.gameProfileSyncInFlight = true;
  try {
    const bridge = await gameCenterProfileBridge();
    if (typeof bridge.getGameCenterGameProfile !== "function") {
      reportGameProfileSyncEvent({ statusCode: "UNSUPPORTED", source, message: "missing-getGameCenterGameProfile" });
      return { statusCode: "UNSUPPORTED" };
    }
    const profile = await bridge.getGameCenterGameProfile();
    if (!profile || profile.statusCode !== "SUCCESS") {
      if (profile?.statusCode === "PROFILE_NOT_FOUND") {
        state.lastGameProfileMissingAt = Date.now();
      }
      reportGameProfileSyncEvent({
        statusCode: profile?.statusCode || "UNSUPPORTED",
        source,
        message: profile ? "profile-status" : "empty-profile-response",
      });
      return { statusCode: profile?.statusCode || "UNSUPPORTED" };
    }
    const nickname = syncedProfileNickname(profile);
    if (!nickname) {
      reportGameProfileSyncEvent({ statusCode: "EMPTY_PROFILE", source, message: "missing-nickname" });
      return { statusCode: "EMPTY_PROFILE" };
    }
    state.lastGameProfileSyncAt = Date.now();
    if (!force && nickname === gameProfileNickname()) {
      reportGameProfileSyncEvent({ statusCode: "SUCCESS", source, message: "unchanged" });
      return { statusCode: "SUCCESS", nickname, unchanged: true };
    }
    const updatedPlayer = await api("/api/player/game-profile", {
      method: "POST",
      body: JSON.stringify({ nickname }),
    });
    setServerPlayer(updatedPlayer);
    render();
    reportGameProfileSyncEvent({ statusCode: "SUCCESS", source, message: "saved" });
    return { statusCode: "SUCCESS", nickname };
  } catch (error) {
    reportGameProfileSyncEvent({ statusCode: "ERROR", source, message: gameProfileSyncErrorMessage(error) });
    if (!silent) {
      throw error;
    }
    return { statusCode: "ERROR", error };
  } finally {
    state.gameProfileSyncInFlight = false;
  }
}

async function ensureGameProfile({ force = false, silent = true, openIfMissing = false, source = "auto" } = {}) {
  const result = await syncGameProfileIfNeeded({ force, silent, source });
  if (result?.statusCode === "PROFILE_NOT_FOUND" && openIfMissing) {
    return openGameProfileEditorAfterMissing({ source, silent });
  }
  return result;
}

function rememberPendingLeaderboardActions({ submitScore = false, openLeaderboard = false } = {}) {
  state.pendingLeaderboardScoreSubmit = state.pendingLeaderboardScoreSubmit || submitScore;
  state.pendingLeaderboardOpen = state.pendingLeaderboardOpen || openLeaderboard;
}

function queueGameProfileCreation({ submitScore = false, openLeaderboard = false } = {}) {
  state.gameProfileCreationDeferred = true;
  rememberPendingLeaderboardActions({ submitScore, openLeaderboard });
}

function maybeResumeDeferredGameProfileCreation() {
  if (!state.gameProfileCreationDeferred || shouldDeferGameProfileEditor() || state.gameProfileEditorInFlight) {
    return;
  }
  state.gameProfileCreationDeferred = false;
  openGameProfileEditorAfterMissing({ source: "deferred", silent: true }).catch(() => {});
}

async function openGameProfileEditorAfterMissing({ source = "auto", silent = true } = {}) {
  rememberPendingLeaderboardActions({
    submitScore: source === "score" || source === "leaderboard",
    openLeaderboard: source === "leaderboard",
  });
  if (shouldDeferGameProfileEditor()) {
    queueGameProfileCreation({
      submitScore: source === "score" || source === "leaderboard",
      openLeaderboard: source === "leaderboard",
    });
    reportGameProfileSyncEvent({ statusCode: "DEFERRED", source, message: "profile-editor-deferred" });
    return { statusCode: "DEFERRED" };
  }
  if (state.gameProfileEditorInFlight) {
    reportGameProfileSyncEvent({ statusCode: "IN_FLIGHT", source, message: "profile-editor-in-flight" });
    return { statusCode: "IN_FLIGHT" };
  }
  const now = Date.now();
  const forceOpen = source === "leaderboard";
  if (!forceOpen && now - state.lastGameProfileEditorOpenedAt < 60 * 1000) {
    reportGameProfileSyncEvent({ statusCode: "PROFILE_NOT_FOUND", source, message: "profile-editor-cooldown" });
    return { statusCode: "PROFILE_NOT_FOUND" };
  }
  state.lastGameProfileEditorOpenedAt = now;
  state.gameProfileEditorInFlight = true;
  setMessage("토스 게임 프로필을 먼저 만들게요.");
  try {
    reportGameProfileSyncEvent({ statusCode: "PROFILE_EDITOR_OPENING", source, message: "open-profile-editor" });
    await openTransparentServiceWeb(gameProfileEditorUrl(), async () => {
      const syncResult = await syncGameProfileIfNeeded({ force: true, silent: true, source });
      if (syncResult?.statusCode === "SUCCESS") {
        setMessage("게임 프로필을 확인했어요.");
        await flushPendingLeaderboardActions();
      } else if (syncResult?.statusCode === "PROFILE_NOT_FOUND") {
        setMessage("토스 게임센터 프로필을 만든 뒤 랭킹에 참여할 수 있어요.");
      }
    });
    return { statusCode: "PROFILE_EDITOR_OPENED" };
  } catch (error) {
    reportGameProfileSyncEvent({ statusCode: "PROFILE_EDITOR_ERROR", source, message: gameProfileSyncErrorMessage(error) });
    if (!silent) {
      throw error;
    }
    setMessage(error.message || "게임 프로필 화면을 열지 못했어요.");
    return { statusCode: "ERROR", error };
  } finally {
    state.gameProfileEditorInFlight = false;
  }
}

async function flushPendingLeaderboardActions() {
  const shouldSubmitScore = state.pendingLeaderboardScoreSubmit || state.pendingLeaderboardOpen;
  const shouldOpenLeaderboard = state.pendingLeaderboardOpen;
  state.pendingLeaderboardScoreSubmit = false;
  state.pendingLeaderboardOpen = false;
  if (shouldSubmitScore) {
    await submitLeaderboardScore({ silent: true, force: true, ensureProfile: false });
  }
  if (shouldOpenLeaderboard) {
    const sdk = await loadTossSdk();
    if (typeof sdk.openGameCenterLeaderboard === "function") {
      await sdk.openGameCenterLeaderboard();
    }
  }
}

function canSubmitLeaderboardScore() {
  return shouldUseGameCenter() && Boolean(state.player?.job) && !state.player?.onboardingRequired;
}

function canAutoSubmitLeaderboardScore() {
  return canSubmitLeaderboardScore() && isTossMiniRuntime();
}

function cancelScheduledLeaderboardScoreSubmit() {
  window.clearTimeout(state.leaderboardEntrySubmitTimer);
  state.leaderboardEntrySubmitTimer = null;
}

function scheduleLeaderboardEntryScoreSubmit(delayMs = 2000) {
  if (!canAutoSubmitLeaderboardScore()) {
    return;
  }
  const score = leaderboardCombatPowerScore(state.player);
  if (state.lastLeaderboardScoreSubmitted !== null && score <= state.lastLeaderboardScoreSubmitted) {
    return;
  }
  cancelScheduledLeaderboardScoreSubmit();
  state.leaderboardEntrySubmitTimer = window.setTimeout(() => {
    state.leaderboardEntrySubmitTimer = null;
    submitLeaderboardScore({ silent: true }).catch(() => {});
  }, delayMs);
}

async function submitLeaderboardScore({ silent = false, force = false, ensureProfile = true, source = "score" } = {}) {
  if (!canSubmitLeaderboardScore()) {
    return;
  }
  if (ensureProfile) {
    const profileResult = await ensureGameProfile({ force: true, silent: true, openIfMissing: true, source });
    if (["PROFILE_NOT_FOUND", "DEFERRED", "IN_FLIGHT", "ERROR"].includes(profileResult?.statusCode)) {
      return profileResult;
    }
  }
  const scoreValue = leaderboardCombatPowerScore(state.player);
  if (!force && state.lastLeaderboardScoreSubmitted !== null && scoreValue <= state.lastLeaderboardScoreSubmitted) {
    return;
  }
  if (state.leaderboardSubmitInFlight) {
    return;
  }
  state.leaderboardSubmitInFlight = true;
  try {
    const sdk = await loadTossSdk();
    const submit = sdk.submitGameCenterLeaderBoardScore || sdk.submitGameCenterLeaderboardScore;
    if (typeof submit !== "function") {
      return;
    }
    const result = await submit({ score: String(scoreValue) });
    if (result?.statusCode === "SUCCESS") {
      state.lastLeaderboardScoreSubmitted = scoreValue;
    } else if (result?.statusCode === "PROFILE_NOT_FOUND") {
      state.lastGameProfileMissingAt = Date.now();
      await openGameProfileEditorAfterMissing({ source, silent: true });
    }
    return result;
  } catch (error) {
    if (!silent) {
      throw error;
    }
  } finally {
    state.leaderboardSubmitInFlight = false;
  }
}

function leaderboardCombatPowerScore(player = state.player) {
  return combatPower(player);
}

async function openGameLeaderboard() {
  if (!shouldUseGameCenter()) {
    setMessage("토스 게임센터는 토스 앱에서 확인할 수 있어요.");
    return;
  }
  const sdk = await loadTossSdk();
  if (typeof sdk.openGameCenterLeaderboard !== "function") {
    setMessage("현재 토스 앱에서 랭킹을 열 수 없어요.");
    return;
  }
  cancelScheduledLeaderboardScoreSubmit();
  const submitResult = await submitLeaderboardScore({ force: true, source: "leaderboard" });
  if (submitResult?.statusCode === "PROFILE_NOT_FOUND") {
    setMessage("토스 게임센터 프로필을 만든 뒤 랭킹에 참여할 수 있어요.");
    return;
  }
  if (["DEFERRED", "IN_FLIGHT", "PROFILE_EDITOR_OPENED"].includes(submitResult?.statusCode)) {
    return;
  }
  await sdk.openGameCenterLeaderboard();
}

async function requestTossShareReward() {
  const sdk = await loadTossSdk();
  const bridge = typeof sdk.contactsViral === "function" ? sdk : await loadTossBridge();
  if (typeof bridge.contactsViral === "function") {
    return new Promise((resolve, reject) => {
      let cleanup = null;
      let settled = false;
      let timeoutId = null;
      let observedSendCount = 0;
      const settle = (callback, value) => {
        if (settled) {
          return;
        }
        settled = true;
        window.clearTimeout(timeoutId);
        cleanup?.();
        callback(value);
      };
      timeoutId = window.setTimeout(() => {
        settle(reject, new Error("공유 리워드 응답 시간이 초과됐어요. 다시 시도해 주세요."));
      }, 180_000);
      cleanup = bridge.contactsViral({
        options: { moduleId: state.shareRewardModuleId },
        onEvent: (event) => {
          if (event.type === "sendViral") {
            observedSendCount += 1;
            return;
          }
          if (event.type === "close") {
            const completedInvites = completedShareRewardCount(event, observedSendCount);
            if (completedInvites > 0) {
              settle(resolve, completedInvites);
              return;
            }
            const message = event.data?.closeReason === "noReward"
              ? "받을 수 있는 공유 리워드가 없어요."
              : "친구 초대 공유가 취소됐어요.";
            settle(reject, new Error(message));
          }
        },
        onError: (error) => {
          settle(reject, error instanceof Error ? error : new Error("공유 리워드가 완료되지 않았어요."));
        },
      });
    });
  }
  const shareLink = typeof sdk.getTossShareLink === "function"
    ? await sdk.getTossShareLink("intoss://gold-hunter")
    : "intoss://gold-hunter";
  if (typeof sdk.share !== "function") {
    throw new Error("현재 토스 앱에서 공유 기능을 사용할 수 없어요.");
  }
  await sdk.share({ message: `${state.shareRewardMessage}\n${shareLink}` });
  return 1;
}

function completedShareRewardCount(event, observedSendCount = 0) {
  const data = event?.data || {};
  const count = Number(data.sentRewardsCount ?? data.sentRewardCount ?? data.rewardCount);
  if (Number.isFinite(count) && count > 0) {
    return Math.floor(count);
  }
  return observedSendCount;
}

async function finishDummyAd() {
  if (!state.adAction || $("adSkip").disabled) {
    return;
  }
  const action = state.adAction;
  state.adAction = null;
  $("adModal").classList.add("hidden");
  if (typeof action.afterAd === "function") {
    await action.afterAd(null);
    return;
  }
  await run(action.request, action.message);
}

function showDummyPayment(action) {
  if (!state.mockMonetizationEnabled) {
    setMessage("토스 결제 연동 전에는 리뷰 환경에서만 사용할 수 있어요.");
    return;
  }
  state.paymentAction = action;
  $("paymentTitle").textContent = action.title || "결제";
  $("paymentDescription").textContent = action.description || "결제를 완료하면 선택한 상품이 지급돼요.";
  $("paymentAmount").textContent = action.amountText || "";
  $("paymentModal").classList.remove("hidden");
}

async function finishDummyPayment() {
  if (!state.paymentAction) {
    return;
  }
  const action = state.paymentAction;
  state.paymentAction = null;
  $("paymentModal").classList.add("hidden");
  await run(action.request, action.message);
}

async function run(request, message, options = {}) {
  try {
    setMessage("처리 중...");
    const result = await requestWithLoginRetry(request);
    if (result.state) {
      setServerPlayer(result.state, { resetDisplayGold: true });
      setMessage(rewardClaimResultMessage(result));
    } else {
      setServerPlayer(result, { resetDisplayGold: Boolean(options.resetDisplayGold) });
      setMessage(message);
    }
    if (state.player.job) {
      state.selectedJob = state.player.job;
    }
    state.dismissedJobModal = false;
    render();
    closeJobModalIfReady();
    scheduleGameProfileSync(800);
    scheduleLeaderboardEntryScoreSubmit(1200);
  } catch (error) {
    setMessage(error.message);
  }
}

function rewardClaimResultMessage(result) {
  const pointAmount = Number(result?.pointAmount || 0).toLocaleString("ko-KR");
  const status = String(result?.status || "").toUpperCase();
  if (status === "GRANTED") {
    return `${pointAmount} 토스포인트 지급 요청이 완료됐어요.`;
  }
  if (status === "FAILED") {
    return "토스포인트 지급에 실패했어요. 잠시 후 다시 시도해 주세요.";
  }
  return `${pointAmount} 토스포인트 지급 처리 중이에요.`;
}

async function requestWithLoginRetry(request) {
  if (shouldUseTossLogin() && !state.authToken) {
    setMessage("토스 로그인 후 계속 진행할게요.");
    const loggedIn = await startTossLogin({ refreshAfterLogin: false });
    if (!loggedIn) {
      throw new Error("토스 로그인이 필요해요.");
    }
  }

  try {
    return await request();
  } catch (error) {
    if (!error.requiresLogin || !shouldUseTossLogin()) {
      throw error;
    }
    setMessage("로그인 세션을 다시 확인할게요.");
    const loggedIn = await startTossLogin({ refreshAfterLogin: false });
    if (!loggedIn) {
      throw error;
    }
    return request();
  }
}

async function purchasePetSkin(skinKey) {
  const skin = petSkinByKey(skinKey);
  await run(
    () => api(`/api/player/shop/pet-skins/${encodeURIComponent(skinKey)}/purchase`, { method: "POST" }),
    `${skin.name} 스킨을 해금했어요.`,
    { resetDisplayGold: true }
  );
}

async function equipPetSkin(skinKey, slot) {
  const skin = petSkinByKey(skinKey);
  await run(
    () => api(`/api/player/shop/pet-skins/${encodeURIComponent(skinKey)}/equip`, {
      method: "POST",
      body: JSON.stringify({ slot }),
    }),
    `${slot}번 펫이 ${skin.name} 스킨을 착용했어요.`
  );
}

async function unlockPetEasterEggSkins() {
  await run(
    () => api("/api/player/shop/pet-skins/easter-eggs/unlock", { method: "POST" }),
    "숨겨진 펫 스킨이 열렸어요."
  );
}

function togglePetEasterEggSkinsVisibility() {
  state.petEasterEggSkinsHidden = !state.petEasterEggSkinsHidden;
  storeValue(hiddenPetSkinsStorageKey, String(state.petEasterEggSkinsHidden));
  renderPetSkinShop(state.player);
  setMessage(state.petEasterEggSkinsHidden ? "히든 스킨 목록을 숨겼어요." : "히든 스킨 목록을 다시 표시했어요.");
}

function openPetEasterEggPasswordModal() {
  const modal = $("petEasterEggPasswordModal");
  const input = $("petEasterPasswordInput");
  if (!modal || !input) {
    return;
  }
  state.petEasterEggPasswordOpen = true;
  input.value = "";
  modal.classList.remove("hidden");
  window.setTimeout(() => input.focus(), 40);
}

function closePetEasterEggPasswordModal() {
  const modal = $("petEasterEggPasswordModal");
  if (!modal) {
    return;
  }
  state.petEasterEggPasswordOpen = false;
  modal.classList.add("hidden");
}

function submitPetEasterEggPassword() {
  const input = $("petEasterPasswordInput");
  const password = String(input?.value || "").trim();
  if (password !== "1234") {
    closePetEasterEggPasswordModal();
    setMessage("히든 스킨 비밀번호가 맞지 않아요.");
    return;
  }
  closePetEasterEggPasswordModal();
  if (hasUnlockedPetEasterEggSkins()) {
    togglePetEasterEggSkinsVisibility();
    return;
  }
  unlockPetEasterEggSkins();
}

function setDevStatus(message) {
  $("devStatus").textContent = message;
}

function setDevBusy(isBusy) {
  $("devPanel").classList.toggle("is-busy", isBusy);
  document.querySelectorAll(".dev-panel button:not(.dev-toggle)").forEach((button) => {
    button.disabled = isBusy;
  });
}

function applyDevPlayer(player, message) {
  setServerPlayer(player, { resetDisplayGold: true });
  if (player.job) {
    state.selectedJob = player.job;
  }
  state.dismissedJobModal = false;
  render();
  closeJobModalIfReady();
  setMessage(message);
  setDevStatus(message);
  return player;
}

async function chooseJobForTest(job) {
  state.selectedJob = job;
  return api("/api/player/test/job", {
    method: "POST",
    body: JSON.stringify({ job }),
  });
}

async function ensureJobForTest(job = "WARRIOR") {
  if (state.player?.job) {
    return state.player;
  }
  const player = await chooseJobForTest(job);
  setServerPlayer(player);
  return player;
}

async function buyAllPetsForTest(player = state.player) {
  const next = await api("/api/player/test/companions/unlock-all", { method: "POST" });
  setServerPlayer(next);
  return next;
}

async function battlePresetForTest() {
  let next = state.player;
  if (!next?.job) {
    next = await chooseJobForTest("WARRIOR");
    setServerPlayer(next);
  }
  next = await buyAllPetsForTest(next);
  setServerPlayer(next);
  next = await api("/api/player/test/auto-hunt", { method: "POST" });
  return next;
}

async function runDevAction(request, message) {
  if ($("devPanel").classList.contains?.("is-busy")) {
    return;
  }
  try {
    setDevBusy(true);
    setDevStatus("테스트 상태 적용 중...");
    const result = await request();
    const player = result.state || result;
    applyDevPlayer(player, message);
  } catch (error) {
    setMessage(error.message);
    setDevStatus(error.message);
  } finally {
    setDevBusy(false);
  }
}

function promotionExecutionSummary(executions = []) {
  if (!executions.length) {
    return "아직 기록된 mock 프로모션이 없어요.";
  }
  return executions
    .map((execution, index) => {
      const code = execution.promotionCode || "-";
      const amount = Number(execution.amount || 0).toLocaleString("ko-KR");
      const result = execution.result || "UNKNOWN";
      return `${index + 1}. ${code} / ${amount}P / ${result}`;
    })
    .join("\n");
}

async function showPromotionExecutionsForTest() {
  if ($("devPanel").classList.contains?.("is-busy")) {
    return;
  }
  try {
    setDevBusy(true);
    setDevStatus("프로모션 기록 확인 중...");
    const executions = await api("/api/player/test/promotion-executions");
    const message = promotionExecutionSummary(executions);
    setMessage(message);
    setDevStatus(message);
  } catch (error) {
    setMessage(error.message);
    setDevStatus(error.message);
  } finally {
    setDevBusy(false);
  }
}

function shouldSyncCombat(now = Date.now()) {
  return !state.combatSyncInFlight && (!state.lastCombatSyncAt || now - state.lastCombatSyncAt >= combatSyncIntervalMs);
}

async function syncCombatState() {
  const player = state.player;
  if (!player || player.onboardingRequired || state.combatSyncInFlight) {
    return;
  }
  state.combatSyncInFlight = true;
  const beforeLevel = displayLevel(player);
  const beforeMonsterSignature = monsterSignature(displayMonster(player));
  try {
    const next = await api("/api/player/combat/hit", { method: "POST" });
    const impactJob = activeJob();
    setServerPlayer(next);
    if (beforeMonsterSignature !== monsterSignature(displayMonster(next))) {
      scheduleMonsterImpact(true, impactJob);
    }
    if (displayLevel(next) > beforeLevel) {
      showLevelUpModal(displayLevel(next), displayLevel(next) - beforeLevel);
      setMessage(`Lv.${displayLevel(next)} 달성! 스킬 포인트 1개를 얻었어요.`);
    }
    render();
  } catch (error) {
    setMessage(error.message);
  } finally {
    state.combatSyncInFlight = false;
  }
}

function simulateHit() {
  const player = state.player;
  if (!player || player.onboardingRequired || !isActive(player.autoHuntEndsAt)) {
    return;
  }
  const now = Date.now();
  if (shouldSyncCombat(now)) {
    syncCombatState();
  }
  const interval = attackIntervalMillis(player);
  if (now - state.lastHitAt < interval) {
    return;
  }
  state.lastHitAt = now;
  const beforeDisplayGold = Math.floor(state.displayGold);
  accrueLocalCombatGold(now);
  const gainedGold = Math.max(0, Math.floor(state.displayGold) - beforeDisplayGold);
  const localCombat = applyLocalMonsterHit(player);
  playAttackMotion();
  spawnProjectile();
  spawnSkillEffect();
  spawnPetAttacks();
  if (gainedGold > 0) {
    spawnGoldPop(gainedGold);
  }
  scheduleMonsterImpact(localCombat.defeated, activeJob());
  if (localCombat.leveledUp) {
    setMessage(`Lv.${displayLevel()} 달성! 서버 동기화 후 SP가 반영돼요.`);
  }
  render();
}

function attackMotionMs(job = activeJob()) {
  return attackMotionMsByJob[job] || attackMotionMsByJob.WARRIOR;
}

function playAttackMotion() {
  const shell = document.querySelector(".game-shell");
  if (!shell) {
    return;
  }
  window.clearTimeout(state.attackTimer);
  shell.classList.remove("is-attacking");
  void shell.offsetWidth;
  shell.classList.add("is-attacking");
  state.attackTimer = window.setTimeout(() => {
    shell.classList.remove("is-attacking");
  }, attackMotionMs() + 90);
}

function spawnProjectile() {
  const job = activeJob();
  const meta = jobMeta[job] || jobMeta.WARRIOR;
  const type = statSkillByJob[job];
  const tier = skillTier(type);
  const tieredImage = effectImageFor(type, tier);
  const prefix = skillMeta[type]?.effectPrefix;
  const projectile = document.createElement("img");
  projectile.className = tieredImage
    ? `projectile ${meta.weapon} skill-projectile ${prefix} tier-${tier}`
    : `projectile ${meta.weapon}`;
  projectile.src = tieredImage || meta.projectile;
  projectile.alt = "";
  $("projectileLayer").appendChild(projectile);
  window.setTimeout(() => projectile.remove(), attackMotionMs(job) + 120);
}

function spawnSkillEffect() {
  const type = activeStatSkill();
  const tier = skillTier(type);
  const image = effectImageFor(type, tier);
  if (!image) {
    return;
  }
  const effect = document.createElement("img");
  effect.className = `skill-effect ${skillMeta[type].effectPrefix} tier-${tier}`;
  effect.src = image;
  effect.alt = "";
  $("skillEffectLayer").appendChild(effect);
  window.setTimeout(() => effect.remove(), attackMotionMs() + 120);
}

function spawnPetAttacks() {
  const pets = unlockedPetCount();
  petMeta.slice(0, pets).forEach((pet, index) => {
    const skin = petSkinForSlot(index + 1);
    const delayMs = 340 + index * 140;
    window.setTimeout(() => {
      const attack = document.createElement("img");
      attack.className = `pet-attack pet-${pet.key}-attack`;
      attack.src = skin.attack;
      attack.alt = "";
      $("petAttackLayer").appendChild(attack);
      window.setTimeout(() => attack.remove(), 960);
    }, delayMs);
  });
  if (rookieEventRewardActive()) {
    window.setTimeout(() => {
      const attack = document.createElement("img");
      attack.className = "pet-attack pet-event-attack";
      attack.src = rookieEventAssets.trail;
      attack.alt = "";
      $("petAttackLayer").appendChild(attack);
      window.setTimeout(() => attack.remove(), 980);
    }, 520);
  }
}

function showLevelUpModal(level, gainedSkillPoints = 1) {
  $("levelUpTitle").textContent = `Lv.${level} 달성`;
  $("levelUpDescription").textContent = `축하합니다! SP ${gainedSkillPoints}개가 지급됐어요.`;
  $("levelUpModal").classList.remove("hidden");
}

function closeLevelUpModal() {
  $("levelUpModal").classList.add("hidden");
}

function spawnGoldPop(amount) {
  const stage = document.querySelector(".battle-screen");
  const pop = document.createElement("span");
  pop.className = "gold-pop";
  pop.textContent = `+${amount}G`;
  stage.appendChild(pop);
  window.setTimeout(() => pop.remove(), 820);
}

function pulseMonsterHp(defeated = false) {
  const monster = document.querySelector(".monster-wrap");
  monster.classList.remove("hit", "defeated");
  playMonsterHitSound(defeated);
  window.requestAnimationFrame(() => monster.classList.add(defeated ? "defeated" : "hit"));
}

function scheduleMonsterImpact(defeated = false, job = activeJob()) {
  const impactDelayMs = Math.round(attackMotionMs(job) * 0.72);
  window.setTimeout(() => pulseMonsterHp(defeated), impactDelayMs);
}

document.querySelectorAll(".job-select").forEach((button) => {
  button.addEventListener("click", async () => {
    startBgm();
    const job = button.dataset.job;
    const isChange = state.player && !state.player.onboardingRequired && state.player.job !== job;
    const firstPick = state.player?.onboardingRequired;
    const request = () => api("/api/player/job", { method: "POST", body: JSON.stringify({ job }) });
    const applySelectedJob = async () => {
      state.selectedJob = job;
      applyJob(job);
      await run(request, firstPick
        ? `${jobMeta[job].pick} 선택했어요. 자동사냥 30분이 지급됐어요.`
        : `${jobMeta[job].pick} 선택했어요.`);
    };

    if (isChange) {
      runRewardFlow(
        "캐릭터 변경 광고",
        isOneStoreTarget()
          ? "선택한 헌터로 바로 변경돼요. 성장 수치는 그대로 유지돼요."
          : "완료하면 선택한 헌터로 변경돼요. 성장 수치는 그대로 유지돼요.",
        {
          adGroupKey: "jobChange",
          requiresReward: false,
          request,
          message: `${jobMeta[job].change} 변경했어요.`,
        }
      );
      return;
    }

    await applySelectedJob();
  });
});

$("openJobModal").addEventListener("click", () => {
  state.forceJobModal = true;
  state.dismissedJobModal = false;
  $("jobModal").classList.remove("hidden");
});

$("closeJobModal").addEventListener("click", () => {
  if (state.player?.onboardingRequired) {
    setMessage("첫 헌터를 선택해야 게임을 시작할 수 있어요.");
    return;
  }
  state.forceJobModal = false;
  state.dismissedJobModal = true;
  $("jobModal").classList.add("hidden");
});

$("closeAdModal").addEventListener("click", () => {
  window.clearInterval(state.adTimer);
  state.adAction = null;
  $("adModal").classList.add("hidden");
  setMessage("광고를 닫았어요. 보상은 지급되지 않았어요.");
});

$("closeTimeRewardOverflowModal").addEventListener("click", closeTimeRewardOverflowModal);
$("confirmTimeRewardOverflowAd").addEventListener("click", confirmTimeRewardOverflowAd);

$("closePaymentModal").addEventListener("click", () => {
  state.paymentAction = null;
  $("paymentModal").classList.add("hidden");
  setMessage("결제를 닫았어요.");
});

$("closeLevelUpModal").addEventListener("click", () => closeLevelUpModal());
$("confirmLevelUp").addEventListener("click", () => closeLevelUpModal());
$("closeNotificationModal").addEventListener("click", () => closeNotificationModal());
$("confirmNotification").addEventListener("click", () => closeNotificationModal());
$("loginButton").addEventListener("click", () => startTossLogin());
$("muteToggle").addEventListener("click", () => toggleBgmMute());
$("tutorialNext").addEventListener("click", () => nextFeatureTutorialStep());
$("tutorialSkip").addEventListener("click", () => finishFeatureTutorial());
window.addEventListener("resize", () => positionFeatureTutorial());

$("showSkillPanel").addEventListener("click", () => showContentPanel("skill"));
$("showAdventurePanel").addEventListener("click", () => showContentPanel("adventure"));
$("showRewardPanel").addEventListener("click", () => showContentPanel("reward"));
$("showShopPanel").addEventListener("click", () => showContentPanel("shop"));

$("autoHuntAd").addEventListener("click", () => {
  if (!timeRewardAdCooldownReady("AUTO_HUNT")) {
    setMessage(`자동사냥 광고 보상은 ${remain(nextTimeRewardAdAvailableAt("AUTO_HUNT"))} 후 다시 받을 수 있어요.`);
    return;
  }
  const availability = timeRewardAvailability(
    state.player?.autoHuntEndsAt,
    state.player?.autoHuntAdSeconds,
    state.player?.maxAdSeconds,
  );
  const grantLabel = timeRewardGrantLabel(availability, state.player?.autoHuntAdSeconds);
  const startAd = () => runRewardFlow(
      "자동사냥 광고",
      isOneStoreTarget()
        ? `게임 내 보상으로 자동사냥 시간이 ${grantLabel}까지 충전돼요.`
        : `완료하면 자동사냥 시간이 ${grantLabel}까지 충전돼요.`,
      {
        adGroupKey: "autoHunt",
        adEventType: "AUTO_HUNT",
        requiresReward: true,
        request: (adSessionToken) => api(isOneStoreTarget()
          ? "/api/player/onestore/auto-hunt/claim"
          : "/api/player/ads/auto-hunt/complete", {
          method: "POST",
          body: isOneStoreTarget() ? undefined : JSON.stringify({ adSessionToken }),
        }),
        message: availability.grantedSeconds > 0
          ? `자동사냥 시간이 ${grantLabel} 충전됐어요.`
          : "자동사냥 시간이 최대치로 유지돼요.",
      }
    );
  if (availability.capped) {
    showTimeRewardOverflowModal("자동사냥", availability, startAd);
    return;
  }
  startAd();
});

document.querySelectorAll(".upgrade-skill").forEach((button) => {
  button.addEventListener("click", () => {
    const type = button.dataset.skill;
    run(
      () => api("/api/player/skills/upgrade", {
        method: "POST",
        body: JSON.stringify({ type }),
      }),
      `${skillMeta[type].label}을 강화했어요.`
    );
  });
});

$("claimReward").addEventListener("click", () => {
  if (!state.player?.rewardClaimable) {
    return;
  }
  showRewardClaimConfirmModal();
});

$("closeRewardConfirmModal").addEventListener("click", closeRewardClaimConfirmModal);
$("confirmRewardClaim").addEventListener("click", () => claimRewardAfterConfirmation());

$("claimFriendInviteReward").addEventListener("click", () => run(
  () => runFriendInviteRewardFlow(),
  `친구 초대 보상으로 SP ${(state.player?.friendInviteRewardSkillPoints || friendInviteRewardSkillPoints).toLocaleString("ko-KR")}개를 받았어요.`
));

$("useDungeonCoupon").addEventListener("click", useDungeonCoupon);
$("challengeBossRaid").addEventListener("click", challengeBossRaid);
$("openAdventureInfo").addEventListener("click", openAdventureInfoModal);
$("closeAdventureInfoModal").addEventListener("click", closeAdventureInfoModal);
$("adventureInfoModal").addEventListener("click", (event) => {
  if (event.target === $("adventureInfoModal")) {
    closeAdventureInfoModal();
  }
});
$("openDungeonRewardList").addEventListener("click", () => openAdventureRewardModal("dungeon"));
$("openBossRewardList").addEventListener("click", () => openAdventureRewardModal("boss"));
$("closeAdventureRewardModal").addEventListener("click", closeAdventureRewardModal);
$("adventureRewardModal").addEventListener("click", (event) => {
  if (event.target === $("adventureRewardModal")) {
    closeAdventureRewardModal();
  }
});
$("closeAdventureActionModal").addEventListener("click", closeAdventureActionModal);

$("openLeaderboard").addEventListener("click", async () => {
  try {
    setMessage("랭킹을 여는 중...");
    await openGameLeaderboard();
  } catch (error) {
    setMessage(error.message);
  }
});

$("petSkinList").addEventListener("click", (event) => {
  const button = event.target.closest("button[data-pet-skin-action]");
  if (!button) {
    return;
  }
  const skinKey = button.dataset.skinKey;
  if (button.dataset.petSkinAction === "purchase") {
    purchasePetSkin(skinKey);
    return;
  }
  equipPetSkin(skinKey, Number(state.petSkinModalSlot || 1));
});

function runCompanionPurchaseFlow() {
  return runPurchaseFlow({
    title: "동료 펫 입양",
    description: isOneStoreTarget()
      ? "심사용 게임 내 보상으로 동료 펫이 1마리 추가돼요."
      : "결제를 완료하면 동료 펫이 1마리 추가돼요.",
    amountText: isOneStoreTarget()
      ? "심사용 잠금 해제"
      : iapDisplayAmount(unlockedPetCount() === 0 ? "flarePet" : "aquaPet", state.player?.companionPriceWon || companionPriceWon),
    productId: () => {
      const pets = unlockedPetCount();
      return pets === 0 ? iapProductId("flarePet") : iapProductId("aquaPet");
    },
    request: () => api(isOneStoreTarget()
      ? "/api/player/onestore/shop/companions/unlock"
      : "/api/player/shop/companions/purchase", { method: "POST" }),
    message: "동료 펫을 데려왔어요.",
  });
}

function handlePetShopAction(slot) {
  const button = $(slot === 1 ? "changePetOneSkin" : "changePetTwoSkin");
  if (button.dataset.petShopAction === "purchase") {
    return runCompanionPurchaseFlow();
  }
  if (button.dataset.petShopAction === "skin") {
    openPetSkinModal(slot);
  }
}

$("changePetOneSkin").addEventListener("click", () => handlePetShopAction(1));
$("changePetTwoSkin").addEventListener("click", () => handlePetShopAction(2));
$("closePetSkinModal").addEventListener("click", closePetSkinModal);
$("petSkinModal").addEventListener("click", (event) => {
  if (event.target.id === "petSkinModal") {
    closePetSkinModal();
  }
});

$("rookieEventButton").addEventListener("click", openRookieEventModal);
$("closeRookieEventModal").addEventListener("click", closeRookieEventModal);
$("rookieEventFinalClaimButton").addEventListener("click", (event) => {
  claimRookieEventFinalReward(event.currentTarget);
});
$("rookieEventModal").addEventListener("click", (event) => {
  if (event.target.id === "rookieEventModal") {
    closeRookieEventModal();
  }
});
$("closeHomeShortcutGuide").addEventListener("click", closeHomeShortcutGuide);
$("homeShortcutGuideBack").addEventListener("click", previousHomeShortcutGuideStep);
$("homeShortcutGuideNext").addEventListener("click", nextHomeShortcutGuideStep);
$("homeShortcutGuideModal").addEventListener("click", (event) => {
  if (event.target.id === "homeShortcutGuideModal") {
    closeHomeShortcutGuide();
  }
});

$("unlockPetEasterEgg").addEventListener("click", () => {
  if (unlockedPetCount() < 1) {
    return;
  }
  if (hasUnlockedPetEasterEggSkins()) {
    togglePetEasterEggSkinsVisibility();
    return;
  }
  const now = Date.now();
  if (now - state.petEasterEggTapStartedAt > 3000) {
    state.petEasterEggTapStartedAt = now;
    state.petEasterEggTapCount = 0;
  }
  state.petEasterEggTapCount += 1;
  if (state.petEasterEggTapCount === 8) {
    setMessage("상점 어딘가에서 숨겨진 기운이 느껴져요.");
  }
  if (state.petEasterEggTapCount >= 10) {
    state.petEasterEggTapCount = 0;
    state.petEasterEggTapStartedAt = 0;
    openPetEasterEggPasswordModal();
  }
});

$("closePetEasterEggPasswordModal").addEventListener("click", closePetEasterEggPasswordModal);
$("cancelPetEasterEggPassword").addEventListener("click", closePetEasterEggPasswordModal);
$("confirmPetEasterEggPassword").addEventListener("click", submitPetEasterEggPassword);
$("petEasterPasswordInput").addEventListener("keydown", (event) => {
  if (event.key === "Enter") {
    submitPetEasterEggPassword();
  }
});

$("adSkip").addEventListener("click", () => finishDummyAd());
$("buySkillPointPack").addEventListener("click", () => {
  if (!skillPointRewardsAvailable()) {
    setMessage("모든 스킬 강화가 완료되어 SP 구매가 비활성화됐어요.");
    return;
  }
  runPurchaseFlow({
  title: "스킬 포인트 팩",
  description: isOneStoreTarget()
    ? `게임 내 보상으로 SP ${(state.player?.skillPointPackAmount || skillPointPackAmount).toLocaleString("ko-KR")}개가 지급돼요.`
    : `결제를 완료하면 SP ${(state.player?.skillPointPackAmount || skillPointPackAmount).toLocaleString("ko-KR")}개가 지급돼요.`,
  amountText: isOneStoreTarget()
    ? "심사용 게임 보상"
    : iapDisplayAmount("skillPointPack", state.player?.skillPointPackPriceWon || skillPointPackPriceWon),
  productId: () => iapProductId("skillPointPack"),
  request: () => api(isOneStoreTarget()
    ? "/api/player/onestore/shop/skill-points/claim"
    : "/api/player/shop/skill-points/purchase", { method: "POST" }),
  message: `SP ${(state.player?.skillPointPackAmount || skillPointPackAmount).toLocaleString("ko-KR")}개를 구매했어요.`,
  });
});
$("confirmPayment").addEventListener("click", () => finishDummyPayment());

function initializeDevPanel() {
  const panel = $("devPanel");
  renderReviewToolsVisibility();
  if (window.matchMedia("(min-width: 1040px)").matches) {
    panel.classList.add("open");
    $("devToggle").setAttribute("aria-expanded", "true");
  }

  $("devToggle").addEventListener("click", () => {
    const opened = panel.classList.toggle("open");
    $("devToggle").setAttribute("aria-expanded", String(opened));
  });

  document.querySelectorAll(".dev-job-button").forEach((button) => {
    button.addEventListener("click", () => {
      const job = button.dataset.devJob;
      runDevAction(
        () => chooseJobForTest(job),
        `${jobMeta[job].change} 바로 변경했어요.`
      );
    });
  });

  $("devBattlePreset").addEventListener("click", () => runDevAction(
    () => battlePresetForTest(),
    "전사, 동료 펫 2마리, 자동사냥을 세팅했어요."
  ));

  $("devAutoHunt").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/auto-hunt", { method: "POST" });
    },
    "자동사냥 시간이 30분 충전됐어요."
  ));

  $("devSkillPoint").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/skill-point", { method: "POST" });
    },
    "SP 1개를 지급했어요."
  ));

  $("devLevelUp").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/level/up", { method: "POST" });
    },
    "레벨을 1 올렸어요."
  ));

  $("devLevelDown").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/level/down", { method: "POST" });
    },
    "레벨을 1 낮췄어요."
  ));

  $("devDungeonCoupon").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/boss-ticket", { method: "POST" });
    },
    "보스 입장권 1장을 지급했어요."
  ));

	$("devDungeonReentryReset").addEventListener("click", () => runDevAction(
		async () => {
			await ensureJobForTest();
			return api("/api/player/test/dungeon-reentry-reset", { method: "POST" });
		},
		"던전 재입장 시간을 초기화했어요."
	));

	$("devDungeonDailyLimitReset").addEventListener("click", () => runDevAction(
		async () => {
			await ensureJobForTest();
			return api("/api/player/test/dungeon-daily-limit-reset", { method: "POST" });
		},
		"던전 하루 입장 제한을 초기화했어요."
	));

	$("devBuyPets").addEventListener("click", () => runDevAction(
    async () => {
      const player = await ensureJobForTest();
      return buyAllPetsForTest(player);
    },
    "동료 펫 2마리를 모두 세팅했어요."
  ));

  $("devEndAutoHunt").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/end-auto-hunt", { method: "POST" });
    },
    "자동사냥 종료 알림을 생성했어요."
  ));

  $("devFillReward").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/fill-reward-gauge", { method: "POST" });
    },
    "보상 게이지를 수령 가능 상태로 채웠어요."
  ));

  $("devClaimReward").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      if (!state.player.rewardClaimable) {
        setServerPlayer(await api("/api/player/test/fill-reward-gauge", { method: "POST" }));
      }
      return api("/api/player/test/claim-reward", { method: "POST" });
    },
    "테스트 리워드 수령 기록을 만들었어요."
  ));

  $("devBenefitTabEntry").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      await api("/api/player/test/promotion-executions/clear", { method: "POST" });
      return api("/api/player/test/benefit-tab-entry", { method: "POST" });
    },
    "혜택 탭 신규 유저로 표시했어요. 게이지를 채우고 토스포인트 수령 버튼을 눌러보세요."
  ));

  $("devPromotionLog").addEventListener("click", () => showPromotionExecutionsForTest());

  $("devToggleBanner").addEventListener("click", () => {
    state.showDummyBanner = !state.showDummyBanner;
    storeValue(dummyBannerStorageKey, String(state.showDummyBanner));
    renderDummyBanner();
    const message = state.showDummyBanner
      ? "배너 광고를 표시했어요."
      : "배너 광고를 숨겼어요.";
    setMessage(message);
    setDevStatus(message);
  });

  $("devReset").addEventListener("click", () => runDevAction(
    async () => {
      state.selectedJob = "WARRIOR";
      state.forceJobModal = false;
      state.dismissedJobModal = false;
      state.featureTutorialStarted = false;
      state.featureTutorialActive = false;
      $("featureTutorial").classList.add("hidden");
      await api("/api/player/test/promotion-executions/clear", { method: "POST" });
      return api("/api/player/test/reset", { method: "POST" });
    },
    "테스트 상태를 초기화했어요. 직업을 다시 선택하세요."
  ));
}

initializeDevPanel();
markBenefitTabEntryIfNeeded();
applyRuntimeSafeAreaFallback();
syncBgmState();
document.addEventListener("pointerdown", () => startBgm(), { once: true, passive: true });
document.addEventListener("visibilitychange", () => handlePageVisibility(!document.hidden));
window.addEventListener("focus", () => handlePageVisibility(true));
window.addEventListener("resize", () => applyRuntimeSafeAreaFallback(), { passive: true });
window.addEventListener("orientationchange", () => applyRuntimeSafeAreaFallback(), { passive: true });
window.addEventListener("pagehide", () => {
  syncPageSoundState(false);
});
window.addEventListener("pageshow", () => {
  syncPageSoundState(!document.hidden);
  restoreBattleVisualAssets();
  schedulePendingIapRestoreRetries();
});

setInterval(() => {
  if (state.player) {
    simulateHit();
    render();
  }
}, 250);

loadAppConfig()
  .then(async () => {
    await refresh();
    restorePendingIapOrders();
    scheduleRealFullScreenAdPreloads();
    scheduleAdventureImagePreloads();
  })
  .catch((error) => setMessage(error.message));
