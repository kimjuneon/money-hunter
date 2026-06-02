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
  notificationAgreementInFlight: false,
  adMode: "test",
  adGroupIds: {},
  iapProductIds: {},
  iapProducts: {},
  shareRewardModuleId: "",
  shareRewardMessage: "친구에게 공유하고 SP 1개를 받아요",
  realAdInFlight: false,
  realPaymentInFlight: false,
  tossAdsInitialized: false,
  tossAdsInitializationPromise: null,
  iapProductInfoRequested: false,
  pendingIapRestoreInFlight: false,
  pendingIapProductIdsByOrderId: readPendingIapProductIds(),
  shareRewardInFlight: false,
  gameProfileSyncTimer: null,
  gameProfileSyncInFlight: false,
  lastGameProfileSyncAt: 0,
  lastGameProfileMissingAt: 0,
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

const effectAssetVersion = "20260525-02";
const goldPerTossPoint = 100;
const companionPriceWon = 4900;
const skillPointPackPriceWon = 999;
const skillPointPackAmount = 10;
const friendInviteRewardSkillPoints = 1;
const friendInviteLimit = 5;
const battleBackgrounds = [
  "/assets/background-options/option-1-sunny-ruins.png?v=20260525-01",
  "/assets/background-options/option-2-crystal-cave.png?v=20260525-01",
  "/assets/background-options/option-3-moon-forest.png?v=20260525-01",
  "/assets/background-options/option-4-lava-forge.png?v=20260525-01",
];
const battleBackgroundExpTextThemes = ["dark", "light", "light", "light"];
const battleBackgroundStorageKey = "moneyHunterBattleBackgroundIndex";
const monsterSignatureStorageKey = "moneyHunterMonsterSignature";
const dummyBannerStorageKey = "moneyHunterShowDummyBanner";
const bgmMutedStorageKey = "moneyHunterBgmMuted";
const guestUserKeyStorageKey = "moneyHunterGuestUserKey";
const authTokenStorageKey = "moneyHunterAuthToken";
const notificationAgreementStatusStorageKey = "moneyHunter.autoHuntNotificationAgreementStatus";
const gameProfileWebviewUrl = "servicetoss://game-center/profile";
const appsInTossSdkUrl = "https://cdn.jsdelivr.net/npm/@apps-in-toss/web-framework@2.5.0/+esm";
const appsInTossBridgeUrl = "https://cdn.jsdelivr.net/npm/@apps-in-toss/web-bridge@2.5.0/dist/bridge.js/+esm";
const productionApiBaseUrl = "https://money-hunter-prod-4qddpaimyq-du.a.run.app";
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
    body: "자동사냥, 공속버프, 스킬포인트는 여기서 충전해요. 광고 보상은 최대 누적 시간까지 추가로 쌓을 수 있어요.",
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
    body: "동료 펫을 데려오면 함께 공격하고 수익률이 올라가요. 필요한 경우 SP 팩도 이 화면에서 확인해요.",
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
    body: "스킬, 보상 수령, 상점, 직업 변경을 아래 메뉴에서 빠르게 오갈 수 있어요.",
  },
  {
    target: "#muteToggle",
    title: "사운드",
    body: "BGM이나 타격음이 부담스러우면 오른쪽 위 버튼으로 바로 끌 수 있어요.",
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
  return window.location.hostname.endsWith("tossmini.com") || Boolean(window.MoneyHunterTossSdk);
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

function iapProductId(key) {
  return String(state.iapProductIds?.[key] || "").trim();
}

function runRewardFlow(title, description, action) {
  if (isOneStoreTarget()) {
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
    setMessage("튜토리얼을 완료했어요. 이제 자유롭게 성장해보세요.");
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
    || previousPlayer.boostedGoldPerHour !== nextPlayer.boostedGoldPerHour
    || previousPlayer.normalAttackIntervalMillis !== nextPlayer.normalAttackIntervalMillis
    || previousPlayer.boostedAttackIntervalMillis !== nextPlayer.boostedAttackIntervalMillis
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

function notificationAgreementStatus() {
  return storedValue(notificationAgreementStatusStorageKey) || "";
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
  const status = notificationAgreementStatus();
  return status !== "agreed" && status !== "rejected";
}

async function requestAutoHuntNotificationAgreementIfNeeded() {
  if (!shouldRequestAutoHuntNotificationAgreement()) {
    return;
  }
  state.notificationAgreementInFlight = true;
  try {
    const sdk = await loadTossSdk();
    if (typeof sdk.requestNotificationAgreement !== "function") {
      return;
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
        options: { templateCode: state.notificationAgreementTemplateCode },
        onEvent: (event) => {
          if (event?.type === "newAgreement" || event?.type === "alreadyAgreed") {
            storeValue(notificationAgreementStatusStorageKey, "agreed");
          }
          if (event?.type === "agreementRejected") {
            storeValue(notificationAgreementStatusStorageKey, "rejected");
          }
          settle(resolve);
        },
        onError: (error) => {
          settle(reject, error instanceof Error ? error : new Error("알림 동의 요청에 실패했어요."));
        },
      });
    });
  } catch {
    // 알림 동의는 편의 기능이라 실패해도 자동사냥 흐름을 막지 않아요.
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
      body: JSON.stringify({ authorizationCode, referrer }),
    });
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
    state.adMode = config.adMode || "test";
    state.adGroupIds = config.adGroupIds || {};
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
    state.adMode = "test";
    state.adGroupIds = {};
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
  renderRewardPanel(player);
  renderShopPanel(player);
  renderPets(player);
  const autoHuntCooldownReady = timeRewardAdCooldownReady("AUTO_HUNT", player);
  const boostCooldownReady = timeRewardAdCooldownReady("BOOST", player);
  $("autoHuntAd").disabled = !autoHuntCooldownReady;
  $("boostAd").disabled = !boostCooldownReady;
  $("huntTime").textContent = !autoHuntCooldownReady
    ? timeRewardCooldownLabel(player.autoHuntEndsAt, nextTimeRewardAdAvailableAt("AUTO_HUNT", player))
    : timeRewardReadyLabel(player.autoHuntEndsAt, player.autoHuntAdSeconds);
  $("boostTime").textContent = !boostCooldownReady
    ? timeRewardCooldownLabel(player.boostEndsAt, nextTimeRewardAdAvailableAt("BOOST", player))
    : timeRewardReadyLabel(player.boostEndsAt, player.boostAdSeconds);
  const spAvailable = skillPointRewardsAvailable(player);
  const spCooldownReady = skillPointAdCooldownReady(player);
  $("skillAd").disabled = !spAvailable || !spCooldownReady;
  $("skillGateLabel").textContent = !spAvailable
    ? "스킬 MAX"
    : spCooldownReady
      ? rewardGateLabel()
      : `${remain(player.nextSkillPointAdAvailableAt)} 후`;
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
  $("notificationMeta").textContent = notification.sentAt
    ? `알림 도착 · ${formatShortTime(notification.sentAt)}`
    : "자동사냥 종료 안내";
  $("notificationModal").classList.remove("hidden");
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
  screen.classList.remove("exp-text-dark", "exp-text-light");
  screen.classList.add(`exp-text-${battleBackgroundExpTextThemes[state.battleBackgroundIndex] || "dark"}`);
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
    return 1500;
  }
  const boosted = isActive(player.boostEndsAt);
  if (boosted && player.boostedAttackIntervalMillis) {
    return player.boostedAttackIntervalMillis;
  }
  if (!boosted && player.normalAttackIntervalMillis) {
    return player.normalAttackIntervalMillis;
  }
  if (player.attackIntervalMillis) {
    return player.attackIntervalMillis;
  }
  const rapidBonus = Math.min(900, skillLevel("RAPID_ATTACK") * 45);
  return Math.max(750, (isActive(player?.boostEndsAt) ? 750 : 1500) - rapidBonus);
}

function renderRewardPanel(player) {
  const pointGoldRate = player.goldPerTossPoint || goldPerTossPoint;
  const minimumClaimPointAmount = player.rewardPointAmount || Math.floor(player.rewardGoldThreshold / pointGoldRate);
  const availablePointAmount = Math.floor(player.gold / pointGoldRate);
  const remainingPointAmount = Math.max(0, minimumClaimPointAmount - availablePointAmount);
  const inviteCount = player.friendInviteRewardCount ?? 0;
  const inviteLimit = player.friendInviteLimit ?? friendInviteLimit;
  const inviteReward = player.friendInviteRewardSkillPoints ?? friendInviteRewardSkillPoints;
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
  $("leaderboardStatus").textContent = `누적 골드 ${leaderboardGoldScore(player).toLocaleString("ko-KR")}G 기준`;
}

function availableRewardPointAmount(player = state.player) {
  if (!player) {
    return 0;
  }
  return Math.floor(player.gold / (player.goldPerTossPoint || goldPerTossPoint));
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
    const skin = petSkinForSlot(index + 1, player);
    const shopCard = $(`pet${index + 1 === 1 ? "One" : "Two"}Shop`);
    const status = $(pet.statusId);
    $(pet.shopImageId).src = skin.image;
    $(pet.shopNameId).textContent = skin.name;
    $(pet.shopCopyId).textContent = skin.copy;
    shopCard.classList.toggle("locked", !unlocked);
    shopCard.classList.toggle("skin-change-available", unlocked);
    const skinButton = $(index === 0 ? "changePetOneSkin" : "changePetTwoSkin");
    skinButton.classList.toggle("hidden", !unlocked);
    skinButton.disabled = !unlocked;
    skinButton.textContent = "스킨 변경";
    status.textContent = unlocked
      ? `동행 중 · ${skin.name} 공격 가능`
      : isOneStoreTarget()
        ? "잠김 · 심사용 잠금 해제"
        : `잠김 · ${iapDisplayAmount(index === 0 ? "flarePet" : "aquaPet", price)}`;
  });
  $("buyCompanion").disabled = pets >= maxPets;
  $("buyCompanion").innerHTML = pets >= maxPets
    ? "<span>동료 펫 MAX · 모든 펫 동행 중</span>"
    : isOneStoreTarget()
      ? `<span>${petSkinForSlot(pets + 1, player).name || "동료 펫"} 잠금 해제 · 심사용</span>`
      : `<span>${petSkinForSlot(pets + 1, player).name || "동료 펫"} 구매 · ${iapDisplayAmount(pets === 0 ? "flarePet" : "aquaPet", price)}</span>`;
  $("skillPointPackCopy").textContent = `SP ${packAmount.toLocaleString("ko-KR")}개 즉시 지급`;
  const spAvailable = skillPointRewardsAvailable(player);
  $("skillPointPackStatus").textContent = spAvailable
    ? isOneStoreTarget()
      ? "심사용 게임 보상"
      : iapDisplayAmount("skillPointPack", packPrice)
    : "모든 스킬 MAX";
  $("buySkillPointPack").disabled = !spAvailable;
  $("buySkillPointPack").innerHTML = spAvailable
    ? isOneStoreTarget()
      ? `<span>SP ${packAmount.toLocaleString("ko-KR")} 받기 · 심사용</span>`
      : `<span>SP ${packAmount.toLocaleString("ko-KR")} 구매 · ${iapDisplayAmount("skillPointPack", packPrice)}</span>`
    : "<span>SP 구매 비활성 · 모든 스킬 MAX</span>";
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
  const nextAvailableAt = type === "AUTO_HUNT"
    ? player?.nextAutoHuntAdAvailableAt
    : player?.nextBoostAdAvailableAt;
  return !nextAvailableAt || !isActive(nextAvailableAt);
}

function nextTimeRewardAdAvailableAt(type, player = state.player) {
  return type === "AUTO_HUNT"
    ? player?.nextAutoHuntAdAvailableAt
    : player?.nextBoostAdAvailableAt;
}

function timeRewardCooldownLabel(currentEndAt, cooldownEndAt) {
  const remaining = remainingSecondsFrom(currentEndAt);
  const effectLabel = remaining > 0 ? remain(currentEndAt) : "꺼짐";
  return `${effectLabel} · 쿨 ${remain(cooldownEndAt)}`;
}

function timeRewardReadyLabel(currentEndAt, rewardSeconds) {
  return remainingSecondsFrom(currentEndAt) > 0
    ? remain(currentEndAt)
    : `${rewardGateLabel()} · ${secondsLabel(rewardSeconds)}`;
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
  const totalSeconds = Math.max(0, Number(seconds || 3600));
  const hours = totalSeconds / 3600;
  if (Number.isInteger(hours)) {
    return `${hours}시간`;
  }
  return `${hours.toFixed(1).replace(/\\.0$/, "")}시간`;
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
  document.querySelector(".game-shell").dataset.panel = showReward ? "reward" : showShop ? "shop" : "skill";
  $("skillPanel").classList.toggle("hidden", showReward || showShop);
  $("rewardPanel").classList.toggle("hidden", !showReward);
  $("shopPanel").classList.toggle("hidden", !showShop);
  $("showSkillPanel").classList.toggle("active", !showReward && !showShop);
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
        ? `이펙트 ${skill.effectTier}단계`
        : meta.petIndex
          ? `${petSkinForSlot(meta.petIndex, player).name} 공격 피해량이 올라가요`
          : meta.description;
    }
    if (iconElement) {
      const previewTier = Math.max(0, skill.effectTier || 0);
      iconElement.dataset.tier = previewTier;
    }
  });

  document.querySelectorAll(".upgrade-skill").forEach((button) => {
    const skill = skillsByType.get(button.dataset.skill);
    const meta = skillMeta[button.dataset.skill];
    const maxLevel = meta?.max || skillMaxLevel;
    const locked = isPetSkillLocked(button.dataset.skill, player);
    const isMaxed = !skill || skill.level >= maxLevel;
    button.disabled = locked || player.skillPoints < 1 || isMaxed;
    button.innerHTML = locked
      ? "<span>잠금</span><small>펫 필요</small>"
      : isMaxed
        ? "<span>MAX</span><small>완료</small>"
        : "<span>강화</span><small>SP 1</small>";
  });
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
  return state.player?.skills.find((skill) => skill.type === type)?.effectTier || 0;
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

function boostedMillisBetween(player, fromMs, toMs) {
  const boostEndsAt = player?.boostEndsAt ? new Date(player.boostEndsAt).getTime() : 0;
  if (!boostEndsAt || boostEndsAt <= fromMs) {
    return 0;
  }
  return Math.max(0, Math.min(toMs, boostEndsAt) - fromMs);
}

function localGoldPerHour(player, boosted) {
  if (boosted) {
    return player?.boostedGoldPerHour ?? player?.goldPerHour ?? 0;
  }
  if (player?.baseGoldPerHour !== undefined) {
    return player.baseGoldPerHour;
  }
  return isActive(player?.boostEndsAt)
    ? Math.round((player?.goldPerHour || 0) / 1.5)
    : player?.goldPerHour || 0;
}

function applyLocalMonsterHit(player = state.player) {
  if (!player) {
    return { defeated: false, leveledUp: false };
  }
  let monster = displayMonster(player);
  let remainingDamage = localDamage(player, isActive(player.boostEndsAt));
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

function localDamage(player, boosted) {
  const rapidLevel = skillLevel("RAPID_ATTACK");
  const statLevel = skillLevel(activeStatSkill());
  let damage = 16
    + displayLevel(player) * 2
    + scaledSkillValue(rapidLevel, 40)
    + scaledSkillValue(statLevel, 60)
    + localPetDamage(player);
  if (boosted) {
    damage = Math.round(damage * 1.35);
  }
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

  const boostedMillis = boostedMillisBetween(player, fromMs, toMs);
  const normalMillis = toMs - fromMs - boostedMillis;
  const gainedGold = (
    localGoldPerHour(player, false) * normalMillis
    + localGoldPerHour(player, true) * boostedMillis
  ) / 3_600_000;
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
  setMessage(`${title} 준비 중...`);
  try {
    const adSession = action.requiresReward === false
      ? null
      : await api("/api/player/ads/sessions", {
          method: "POST",
          body: JSON.stringify({ type: action.adEventType }),
        });
    await loadRealFullScreenAd(groupId);
    await showRealFullScreenAd(groupId, action.requiresReward !== false);
    setMessage("광고 시청 완료, 보상을 지급하는 중이에요.");
    await run(() => action.request(adSession?.sessionToken), action.message);
  } catch (error) {
    const message = error?.message || "광고를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
    setMessage(message);
  } finally {
    state.realAdInFlight = false;
  }
}

async function loadRealFullScreenAd(groupId) {
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
          finish(null, new Error("광고 표시가 실패했어요."));
          return;
        }
        if (event.type === "dismissed" && requiresReward && !completed) {
          finish(null, new Error("광고 시청을 완료해야 보상이 지급돼요."));
        }
      },
      onError: (error) => {
        finish(null, error instanceof Error ? error : new Error("광고 표시가 실패했어요."));
      },
    });
  });
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

async function syncGameProfileIfNeeded({ force = false, silent = true } = {}) {
  if (!canSyncGameProfile() || state.gameProfileSyncInFlight) {
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
      return { statusCode: "UNSUPPORTED" };
    }
    const profile = await bridge.getGameCenterGameProfile();
    if (!profile || profile.statusCode !== "SUCCESS") {
      if (profile?.statusCode === "PROFILE_NOT_FOUND") {
        state.lastGameProfileMissingAt = Date.now();
      }
      return { statusCode: profile?.statusCode || "UNSUPPORTED" };
    }
    const nickname = syncedProfileNickname(profile);
    if (!nickname) {
      return { statusCode: "EMPTY_PROFILE" };
    }
    state.lastGameProfileSyncAt = Date.now();
    if (!force && nickname === gameProfileNickname()) {
      return { statusCode: "SUCCESS", nickname, unchanged: true };
    }
    const updatedPlayer = await api("/api/player/game-profile", {
      method: "POST",
      body: JSON.stringify({ nickname }),
    });
    setServerPlayer(updatedPlayer);
    render();
    return { statusCode: "SUCCESS", nickname };
  } catch (error) {
    if (!silent) {
      throw error;
    }
    return { statusCode: "ERROR", error };
  } finally {
    state.gameProfileSyncInFlight = false;
  }
}

async function ensureGameProfile({ force = false, silent = true, openIfMissing = false, source = "auto" } = {}) {
  const result = await syncGameProfileIfNeeded({ force, silent });
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
    return { statusCode: "DEFERRED" };
  }
  if (state.gameProfileEditorInFlight) {
    return { statusCode: "IN_FLIGHT" };
  }
  const now = Date.now();
  const forceOpen = source === "leaderboard";
  if (!forceOpen && now - state.lastGameProfileEditorOpenedAt < 60 * 1000) {
    return { statusCode: "PROFILE_NOT_FOUND" };
  }
  state.lastGameProfileEditorOpenedAt = now;
  state.gameProfileEditorInFlight = true;
  setMessage("토스 게임 프로필을 먼저 만들게요.");
  try {
    await openTransparentServiceWeb(gameProfileEditorUrl(), async () => {
      const syncResult = await syncGameProfileIfNeeded({ force: true, silent: true });
      if (syncResult?.statusCode === "SUCCESS") {
        setMessage("게임 프로필을 확인했어요.");
        await flushPendingLeaderboardActions();
      } else if (syncResult?.statusCode === "PROFILE_NOT_FOUND") {
        setMessage("토스 게임센터 프로필을 만든 뒤 랭킹에 참여할 수 있어요.");
      }
    });
    return { statusCode: "PROFILE_EDITOR_OPENED" };
  } catch (error) {
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
  const score = leaderboardGoldScore(state.player);
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
  const scoreValue = leaderboardGoldScore(state.player);
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

function leaderboardGoldScore(player = state.player) {
  return Math.max(0, Math.floor(Number(player?.cumulativeGoldEarned ?? player?.gold ?? 0)));
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

async function run(request, message) {
  try {
    setMessage("처리 중...");
    const result = await requestWithLoginRetry(request);
    if (result.state) {
      setServerPlayer(result.state, { resetDisplayGold: true });
      setMessage(rewardClaimResultMessage(result));
    } else {
      setServerPlayer(result);
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
    `${skin.name} 스킨을 해금했어요.`
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
  setServerPlayer(next);
  next = await api("/api/player/test/boost", { method: "POST" });
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
        ? `${jobMeta[job].pick} 선택했어요. 자동사냥 1시간과 공속버프 1시간이 지급됐어요.`
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

$("boostAd").addEventListener("click", () => {
  if (!timeRewardAdCooldownReady("BOOST")) {
    setMessage(`공속버프 광고 보상은 ${remain(nextTimeRewardAdAvailableAt("BOOST"))} 후 다시 받을 수 있어요.`);
    return;
  }
  const availability = timeRewardAvailability(
    state.player?.boostEndsAt,
    state.player?.boostAdSeconds,
    state.player?.maxAdSeconds,
  );
  const grantLabel = timeRewardGrantLabel(availability, state.player?.boostAdSeconds);
  const startAd = () => runRewardFlow(
      "공속버프 광고",
      isOneStoreTarget()
        ? `게임 내 보상으로 공격 모션과 골드 획득 속도가 ${grantLabel}까지 유지돼요.`
        : `완료하면 공격 모션과 골드 획득 속도가 ${grantLabel}까지 유지돼요.`,
      {
        adGroupKey: "boost",
        adEventType: "BOOST",
        requiresReward: true,
        request: (adSessionToken) => api(isOneStoreTarget()
          ? "/api/player/onestore/boost/claim"
          : "/api/player/ads/boost/complete", {
          method: "POST",
          body: isOneStoreTarget() ? undefined : JSON.stringify({ adSessionToken }),
        }),
        message: availability.grantedSeconds > 0
          ? `공격 속도 부스터가 ${grantLabel} 충전됐어요.`
          : "공격 속도 부스터 시간이 최대치로 유지돼요.",
      }
    );
  if (availability.capped) {
    showTimeRewardOverflowModal("공속버프", availability, startAd);
    return;
  }
  startAd();
});

$("skillAd").addEventListener("click", () => {
  if (!skillPointRewardsAvailable()) {
    setMessage("모든 스킬 강화가 완료되어 SP를 더 받을 수 없어요.");
    return;
  }
  if (!skillPointAdCooldownReady()) {
    setMessage(`SP 광고 보상은 ${remain(state.player?.nextSkillPointAdAvailableAt)} 후 다시 받을 수 있어요.`);
    return;
  }
  runRewardFlow(
    "스킬포인트 광고",
    isOneStoreTarget()
      ? "게임 내 보상으로 헌터와 동료 펫 강화에 쓰는 SP를 받아요."
      : "완료하면 헌터와 동료 펫 강화에 쓰는 SP를 받아요.",
    {
      adGroupKey: "skillPoint",
      adEventType: "SKILL_POINT",
      requiresReward: true,
      request: (adSessionToken) => api(isOneStoreTarget()
        ? "/api/player/onestore/skill-point/claim"
        : "/api/player/ads/skill-point/complete", {
        method: "POST",
        body: isOneStoreTarget() ? undefined : JSON.stringify({ adSessionToken }),
      }),
      message: "스킬 포인트를 받았어요.",
    }
  );
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

$("changePetOneSkin").addEventListener("click", () => openPetSkinModal(1));
$("changePetTwoSkin").addEventListener("click", () => openPetSkinModal(2));
$("closePetSkinModal").addEventListener("click", closePetSkinModal);
$("petSkinModal").addEventListener("click", (event) => {
  if (event.target.id === "petSkinModal") {
    closePetSkinModal();
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
$("buyCompanion").addEventListener("click", () => runPurchaseFlow({
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
}));
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
    "전사, 동료 펫 2마리, 자동사냥, 공속버프를 세팅했어요."
  ));

  $("devAutoHunt").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/auto-hunt", { method: "POST" });
    },
    "자동사냥 시간이 1시간 충전됐어요."
  ));

  $("devBoost").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/boost", { method: "POST" });
    },
    "공격 속도 부스터가 1시간 충전됐어요."
  ));

  $("devSkillPoint").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/test/skill-point", { method: "POST" });
    },
    "SP 1개를 지급했어요."
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
      return api("/api/player/test/reset", { method: "POST" });
    },
    "테스트 상태를 초기화했어요. 직업을 다시 선택하세요."
  ));
}

initializeDevPanel();
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
  })
  .catch((error) => setMessage(error.message));
