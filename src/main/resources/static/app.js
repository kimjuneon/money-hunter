const distributionTargetOverride = new URLSearchParams(window.location.search).get("target");

const state = {
  player: null,
  displayGold: 0,
  selectedJob: "WARRIOR",
  battleBackgroundIndex: 0,
  lastMonsterSignature: "",
  lastHitAt: 0,
  hitInFlight: false,
  attackTimer: null,
  adAction: null,
  adTimer: null,
  paymentAction: null,
  forceJobModal: false,
  dismissedJobModal: false,
  autoHuntWasActive: false,
  autoHuntEndRefreshInFlight: false,
  shownNotificationId: null,
  noticeMessage: "",
  noticeExpiresAt: 0,
  noticeTimer: null,
  showDummyBanner: false,
  bgmMuted: false,
  bgmStarted: false,
  soundContext: null,
  featureTutorialStarted: false,
  featureTutorialActive: false,
  featureTutorialIndex: 0,
  featureTutorialPositionTimer: null,
  authToken: "",
  reviewToolsEnabled: false,
  mockMonetizationEnabled: false,
  tossReleaseReady: false,
  tossLoginEnabled: false,
  realRewardAdsEnabled: false,
  realBannerAdsEnabled: false,
  adMode: "test",
  adGroupIds: {},
  realAdInFlight: false,
  loginInFlight: false,
  distributionTarget: String(distributionTargetOverride || "").trim().toUpperCase() === "ONESTORE" ? "ONESTORE" : "TOSS",
};

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

const attackMotionMsByJob = {
  WARRIOR: 820,
  ARCHER: 950,
  MAGE: 900,
  ROGUE: 740,
};

const petMeta = [
  {
    key: "flare",
    name: "해빛냥",
    skill: "PET_FLARE_ATTACK",
    image: "/assets/pet-flarefox.svg?v=20260525-01",
    attack: "/assets/pet-flarefox-attack.svg?v=20260525-01",
  },
  {
    key: "aqua",
    name: "물방울토",
    skill: "PET_AQUA_ATTACK",
    image: "/assets/pet-aquabun.svg?v=20260525-01",
    attack: "/assets/pet-aquabun-attack.svg?v=20260525-01",
  },
];

const effectAssetVersion = "20260525-02";
const goldPerTossPoint = 100;
const companionPriceWon = 4900;
const skillPointPackPriceWon = 999;
const skillPointPackAmount = 10;
const friendInviteRewardSkillPoints = 5;
const friendInviteLimit = 3;
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
const featureTutorialStorageKey = "moneyHunterFeatureTutorialV2";
const appsInTossSdkUrl = "https://cdn.jsdelivr.net/npm/@apps-in-toss/web-framework@latest/+esm";
const productionApiBaseUrl = "https://money-hunter-prod-4qddpaimyq-du.a.run.app";
let tossSdkPromise = null;
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

const skillMeta = {
  STRENGTH: { label: "STR 전사", short: "전사 공격", step: 4, max: 20, effectPrefix: "str" },
  DEXTERITY: { label: "DEX 궁수", short: "궁수 집중", step: 4, max: 20, effectPrefix: "dex" },
  INTELLIGENCE: { label: "INT 마법사", short: "마법 증폭", step: 4, max: 20, effectPrefix: "int" },
  LUCK: { label: "LUK 도적", short: "행운 타격", step: 4, max: 20, effectPrefix: "luk" },
  MINING_MASTERY: { label: "채굴 숙련", short: "채굴량", step: 5, max: 20, description: "골드 획득량이 증가해요" },
  RAPID_ATTACK: { label: "연속 공격", short: "공격 효율", step: 3, max: 20, description: "공격 간격이 짧아져요" },
  REWARD_AMPLIFIER: { label: "보상 증폭", short: "게이지 효율", step: 2, max: 20, description: "보상 게이지 효율이 좋아져요" },
  PET_FLARE_ATTACK: { label: "해빛냥 공격", short: "해빛냥 공격력", max: 20, petIndex: 1, description: "해빛냥 공격 피해량이 올라가요" },
  PET_AQUA_ATTACK: { label: "물방울토 공격", short: "물방울토 공격력", max: 20, petIndex: 2, description: "물방울토 공격 피해량이 올라가요" },
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
    body: "SP로 능력치를 강화해요. 직업별 핵심 능력치는 공유되고, 현재 직업에 맞는 스탯만 먼저 보여줘요.",
  },
  {
    target: "#rewardPanel",
    panel: "reward",
    title: "보상 수령",
    body: "모은 골드는 포인트 기준으로 환산돼요. 조건을 채우면 광고 확인 후 토스 포인트 지급 대기 기록을 만들 수 있어요.",
  },
  {
    target: "#shopPanel",
    panel: "shop",
    title: "상점과 동료 펫",
    body: "동료 펫을 데려오면 함께 공격하고 수익률이 올라가요. 필요한 경우 SP 팩도 이 화면에서 확인해요.",
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
  const randomValue = window.crypto?.randomUUID?.()
    || `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
  return `guest-${randomValue}`;
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

function adGroupId(key) {
  return String(state.adGroupIds?.[key] || "").trim();
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
  return showDummyPayment(action);
}

function setMessage(message, durationMs = 2400) {
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

function featureTutorialCompleted() {
  return storedValue(featureTutorialStorageKey) === "done";
}

function maybeStartFeatureTutorial() {
  if (
    state.featureTutorialStarted
    || state.featureTutorialActive
    || featureTutorialCompleted()
    || !state.player?.job
    || state.player.onboardingRequired
  ) {
    return;
  }
  state.featureTutorialStarted = true;
  window.setTimeout(() => startFeatureTutorial(), 520);
}

function startFeatureTutorial(index = 0) {
  if (!state.player?.job || state.player.onboardingRequired || featureTutorialCompleted()) {
    return;
  }
  state.featureTutorialActive = true;
  state.featureTutorialIndex = index;
  $("featureTutorial").classList.remove("hidden");
  document.body.classList.add("feature-tutorial-open");
  renderFeatureTutorialStep();
}

function finishFeatureTutorial() {
  state.featureTutorialActive = false;
  state.featureTutorialStarted = true;
  storeValue(featureTutorialStorageKey, "done");
  window.clearTimeout(state.featureTutorialPositionTimer);
  $("featureTutorial").classList.add("hidden");
  document.body.classList.remove("feature-tutorial-open");
  showContentPanel("skill");
  setMessage("튜토리얼을 완료했어요. 이제 자유롭게 성장해보세요.");
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
  target.scrollIntoView({ block: "center", inline: "nearest", behavior: "smooth" });
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
  }, 220);
}

function syncBgmState() {
  const audio = $("bgmAudio");
  audio.volume = 0.35;
  audio.muted = state.bgmMuted;
  $("muteToggle").classList.toggle("is-muted", state.bgmMuted);
  $("muteToggle").textContent = state.bgmMuted ? "♪" : "♫";
  $("muteToggle").setAttribute("aria-label", state.bgmMuted ? "BGM 켜기" : "BGM 음소거");
}

function startBgm() {
  syncBgmState();
  if (state.bgmMuted || state.bgmStarted) {
    return;
  }
  const audio = $("bgmAudio");
  const playPromise = audio.play?.();
  if (playPromise?.then) {
    playPromise
      .then(() => {
        state.bgmStarted = true;
      })
      .catch(() => {});
  }
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
  if (state.bgmMuted) {
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
  state.player = await api("/api/player");
  hideLoginGate();
  state.displayGold = Math.max(state.displayGold, state.player.gold);
  if (state.player.job) {
    state.selectedJob = state.player.job;
  }
  render();
}

function setAuthToken(token) {
  state.authToken = token || "";
  if (state.authToken) {
    storeValue(authTokenStorageKey, state.authToken);
  }
}

function clearAuthToken() {
  state.authToken = "";
  try {
    window.localStorage.removeItem(authTokenStorageKey);
  } catch {
    // Ignore storage failures in embedded environments.
  }
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
  if (!tossSdkPromise) {
    tossSdkPromise = import(appsInTossSdkUrl);
  }
  return tossSdkPromise;
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
    state.tossReleaseReady = Boolean(config.tossReleaseReady);
    state.tossLoginEnabled = Boolean(config.tossLoginEnabled);
    state.realRewardAdsEnabled = Boolean(config.realRewardAdsEnabled);
    state.realBannerAdsEnabled = Boolean(config.realBannerAdsEnabled);
    state.adMode = config.adMode || "test";
    state.adGroupIds = config.adGroupIds || {};
  } catch {
    state.distributionTarget = normalizedDistributionTarget(distributionTargetOverride);
    state.reviewToolsEnabled = false;
    state.mockMonetizationEnabled = isOneStoreTarget();
    state.tossReleaseReady = false;
    state.tossLoginEnabled = false;
    state.realRewardAdsEnabled = false;
    state.realBannerAdsEnabled = false;
    state.adMode = "test";
    state.adGroupIds = {};
  }
  document.body.classList.toggle("target-onestore", isOneStoreTarget());
  renderReviewToolsVisibility();
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
  $("levelText").textContent = `Lv.${player.level}`;
  $("skillPointsTop").textContent = player.skillPoints;
  renderRewardPanel(player);
  renderShopPanel(player);
  renderPets(player);
  $("huntTime").textContent = hunting ? `${remain(player.autoHuntEndsAt)} · 추가 가능` : `${rewardGateLabel()} · 1시간`;
  $("boostTime").textContent = isActive(player.boostEndsAt) ? `${remain(player.boostEndsAt)} · 추가 가능` : `${rewardGateLabel()} · 1시간`;
  const spAvailable = skillPointRewardsAvailable(player);
  $("skillAd").disabled = !spAvailable;
  $("skillGateLabel").textContent = spAvailable ? rewardGateLabel() : "스킬 MAX";
  renderMonster(player);
  renderExperience(player);
  renderDummyBanner();

  $("goldRateTop").textContent = `${player.goldPerHour.toLocaleString("ko-KR")}/시간`;
  renderSkills(player);
  renderNotification(player);
  syncBgmState();
  refreshWhenAutoHuntEnds(hunting);
  maybeStartFeatureTutorial();
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
    await new Promise((resolve, reject) => {
      tossAds.initialize({
        callbacks: {
          onInitialized: resolve,
          onInitializationFailed: reject,
        },
      });
    });
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
  $("notificationModal").classList.remove("hidden");
}

async function closeNotificationModal() {
  const notificationId = $("notificationModal").dataset.notificationId;
  $("notificationModal").classList.add("hidden");
  $("notificationModal").dataset.notificationId = "";
  if (!notificationId) {
    return;
  }
  try {
    state.player = await api(`/api/player/notifications/${notificationId}/read`, { method: "POST" });
    state.displayGold = Math.max(state.displayGold, state.player.gold);
    render();
  } catch (error) {
    setMessage(error.message);
  }
}

function renderMonster(player) {
  const monster = player.monster || { key: "BOSS_ROCK", hp: 120, maxHp: 120, defeatedMonsters: 0 };
  const meta = monsterMeta[monster.key] || monsterMeta.BOSS_ROCK;
  applyBattleBackground(monster);
  $("monsterImage").src = meta.image;
  $("monsterName").textContent = `${meta.name} #${monster.defeatedMonsters + 1}`;
  $("monsterHp").style.width = `${Math.max(0, Math.min(100, monster.hp * 100 / monster.maxHp))}%`;
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
  const percent = Math.max(0, Math.min(100, player.experience * 100 / player.nextLevelExperience));
  $("expBar").style.width = `${percent}%`;
  $("expText").textContent = `EXP ${player.experience.toLocaleString("ko-KR")} / ${player.nextLevelExperience.toLocaleString("ko-KR")}`;
}

function attackIntervalMillis(player = state.player) {
  if (player?.attackIntervalMillis) {
    return player.attackIntervalMillis;
  }
  const rapidBonus = Math.min(900, skillLevel("RAPID_ATTACK") * 45);
  return Math.max(750, (isActive(player?.boostEndsAt) ? 750 : 1500) - rapidBonus);
}

function renderRewardPanel(player) {
  const pointGoldRate = player.goldPerTossPoint || goldPerTossPoint;
  const claimPointAmount = player.rewardPointAmount || Math.floor(player.rewardGoldThreshold / pointGoldRate);
  const estimatedPointAmount = Math.floor(player.gold / pointGoldRate);
  const remainingPointAmount = Math.max(0, claimPointAmount - estimatedPointAmount);
  const inviteCount = player.friendInviteRewardCount ?? 0;
  const inviteLimit = player.friendInviteLimit ?? friendInviteLimit;
  const inviteReward = player.friendInviteRewardSkillPoints ?? friendInviteRewardSkillPoints;
  $("rewardBar").style.width = `${Math.min(100, estimatedPointAmount * 100 / claimPointAmount)}%`;
  $("rewardNeed").textContent = `${estimatedPointAmount.toLocaleString("ko-KR")} / ${claimPointAmount.toLocaleString("ko-KR")}P`;
  $("rewardPointEstimate").textContent = isOneStoreTarget()
    ? "게임 보상"
    : `현재 ${estimatedPointAmount.toLocaleString("ko-KR")}P`;
  $("rewardClaimAmount").textContent = player.rewardClaimable
    ? isOneStoreTarget()
      ? `수령 가능 SP ${inviteReward.toLocaleString("ko-KR")}`
      : `수령 가능 ${claimPointAmount.toLocaleString("ko-KR")}P`
    : `${remainingPointAmount.toLocaleString("ko-KR")}P 더 필요`;
  $("claimReward").disabled = !player.rewardClaimable;
  $("claimReward").textContent = player.rewardClaimable
    ? isOneStoreTarget()
      ? "게임 내 보상 받기"
      : "광고보고 토스 포인트 받기"
    : "포인트를 더 모아야 해요";
  $("friendInviteRewardCopy").textContent = `초대 성공 시 SP ${inviteReward.toLocaleString("ko-KR")}개 지급`;
  $("friendInviteRewardStatus").textContent = `${inviteCount.toLocaleString("ko-KR")} / ${inviteLimit.toLocaleString("ko-KR")}명 완료`;
  $("claimFriendInviteReward").disabled = inviteCount >= inviteLimit;
  $("claimFriendInviteReward").textContent = inviteCount >= inviteLimit
    ? "초대 보상 MAX"
    : `친구 초대하기 (${inviteLimit - inviteCount}명 남음)`;
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
    const shopCard = $(`pet${index + 1 === 1 ? "One" : "Two"}Shop`);
    const status = $(`pet${index + 1 === 1 ? "One" : "Two"}Status`);
    shopCard.classList.toggle("locked", !unlocked);
    status.textContent = unlocked
      ? `동행 중 · ${pet.name} 공격 가능`
      : isOneStoreTarget()
        ? "잠김 · 심사용 잠금 해제"
        : `잠김 · ₩${price.toLocaleString("ko-KR")}`;
  });
  $("buyCompanion").disabled = pets >= maxPets;
  $("buyCompanion").textContent = pets >= maxPets
    ? "동료 펫 MAX"
    : isOneStoreTarget()
      ? `${petMeta[pets]?.name || "동료 펫"} 잠금 해제`
      : `${petMeta[pets]?.name || "동료 펫"} 구매`;
  $("skillPointPackCopy").textContent = `SP ${packAmount.toLocaleString("ko-KR")}개 즉시 지급`;
  const spAvailable = skillPointRewardsAvailable(player);
  $("skillPointPackStatus").textContent = spAvailable
    ? isOneStoreTarget()
      ? "심사용 게임 보상"
      : `₩${packPrice.toLocaleString("ko-KR")}`
    : "모든 스킬 MAX";
  $("buySkillPointPack").disabled = !spAvailable;
  $("buySkillPointPack").textContent = spAvailable
    ? isOneStoreTarget()
      ? `SP ${packAmount.toLocaleString("ko-KR")} 받기`
      : `SP ${packAmount.toLocaleString("ko-KR")} 구매`
    : "SP 구매 비활성";
}

function unlockedPetCount(player = state.player) {
  return Math.max(0, (player?.characterSlots || 1) - 1);
}

function renderPets(player) {
  const pets = unlockedPetCount(player);
  $("petFlare").classList.toggle("hidden", pets < 1);
  $("petAqua").classList.toggle("hidden", pets < 2);
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
    .every((skill) => skill.level >= 20);
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
  $("partyName").textContent = `${meta.name} (Lv.${state.player?.level || 1})`;
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
    const maxLevel = meta.max || 20;
    const locked = isPetSkillLocked(skill.type, player);
    const current = skill.level * (meta.step || 0);
    const next = Math.min(maxLevel, skill.level + 1) * (meta.step || 0);
    const levelElement = document.querySelector(`[data-skill-level="${skill.type}"]`);
    const effectElement = document.querySelector(`[data-skill-effect="${skill.type}"]`);
    const tierElement = document.querySelector(`[data-skill-tier="${skill.type}"]`);
    const iconElement = document.querySelector(`[data-effect-icon="${skill.type}"]`);
    if (levelElement) {
      levelElement.textContent = `Lv.${skill.level}`;
    }
    if (effectElement) {
      if (locked) {
        effectElement.textContent = "동료 펫 잠금 해제 필요";
      } else if (meta.petIndex) {
        effectElement.textContent = petSkillEffectText(skill, maxLevel);
      } else {
        effectElement.textContent = skill.level >= maxLevel
        ? `${meta.short} +${current}% · MAX`
        : `${meta.short} +${current}% → +${next}%`;
      }
    }
    if (tierElement) {
      tierElement.textContent = locked
        ? `${meta.petIndex}번째 동료 펫을 상점에서 구매해야 해요`
        : meta.effectPrefix
        ? `이펙트 ${skill.effectTier}단계`
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
    const maxLevel = meta?.max || 20;
    const locked = isPetSkillLocked(button.dataset.skill, player);
    const isMaxed = !skill || skill.level >= maxLevel;
    button.disabled = locked || player.skillPoints < 1 || isMaxed;
    button.innerHTML = locked ? "잠금" : isMaxed ? "MAX" : "강화<br />SP 1";
  });
}

function isPetSkillLocked(type, player = state.player) {
  const petIndex = skillMeta[type]?.petIndex;
  return Boolean(petIndex && unlockedPetCount(player) < petIndex);
}

function petSkillEffectText(skill, maxLevel) {
  const damage = skill.level * 2;
  const nextDamage = Math.min(maxLevel, skill.level + 1) * 2;
  return skill.level >= maxLevel
    ? `피해량 +${damage} · MAX`
    : `피해량 +${damage} → +${nextDamage}`;
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

function remain(value) {
  const seconds = Math.max(0, Math.floor((new Date(value).getTime() - Date.now()) / 1000));
  if (seconds <= 0) {
    return "꺼짐";
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return hours > 0 ? `${hours}시간 ${minutes}분` : `${minutes}분`;
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
      state.player = result.state;
      state.displayGold = state.player.gold;
      setMessage(`${result.pointAmount} 토스 포인트 지급 대기 상태가 생성됐어요.`);
    } else {
      state.player = result;
      state.displayGold = Math.max(state.displayGold, state.player.gold);
      setMessage(message);
    }
    if (state.player.job) {
      state.selectedJob = state.player.job;
    }
    state.dismissedJobModal = false;
    render();
    closeJobModalIfReady();
  } catch (error) {
    setMessage(error.message);
  }
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
  state.player = player;
  state.displayGold = player.gold;
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
  return api("/api/player/job", {
    method: "POST",
    body: JSON.stringify({ job }),
  });
}

async function ensureJobForTest(job = "WARRIOR") {
  if (state.player?.job) {
    return state.player;
  }
  const player = await chooseJobForTest(job);
  state.player = player;
  return player;
}

async function buyAllPetsForTest(player = state.player) {
  let next = player;
  const maxSlots = next?.maxCharacterSlots || 3;
  while ((next?.characterSlots || 1) < maxSlots) {
    next = await api("/api/player/shop/companions/purchase", { method: "POST" });
    state.player = next;
  }
  return next;
}

async function battlePresetForTest() {
  let next = state.player;
  if (!next?.job) {
    next = await chooseJobForTest("WARRIOR");
    state.player = next;
  }
  next = await buyAllPetsForTest(next);
  state.player = next;
  next = await api("/api/player/ads/auto-hunt/complete", { method: "POST" });
  state.player = next;
  next = await api("/api/player/ads/boost/complete", { method: "POST" });
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

async function simulateHit() {
  const player = state.player;
  if (!player || player.onboardingRequired || !isActive(player.autoHuntEndsAt) || state.hitInFlight) {
    return;
  }
  const now = Date.now();
  const interval = attackIntervalMillis(player);
  if (now - state.lastHitAt < interval) {
    return;
  }
  state.lastHitAt = now;
  state.hitInFlight = true;
  const beforeGold = state.player.gold;
  const beforeMonsterKey = state.player.monster?.key;
  const beforeLevel = state.player.level;
  try {
    const next = await api("/api/player/combat/hit", { method: "POST" });
    state.player = next;
    state.displayGold = Math.max(state.displayGold, next.gold);
    const gainedGold = Math.max(0, next.gold - beforeGold);
    playAttackMotion();
    spawnProjectile();
    spawnSkillEffect();
    spawnPetAttacks();
    if (gainedGold > 0) {
      spawnGoldPop(gainedGold);
    }
    pulseMonsterHp(beforeMonsterKey && beforeMonsterKey !== next.monster.key);
    if (next.level > beforeLevel) {
      showLevelUpModal(next.level, next.level - beforeLevel);
      setMessage(`Lv.${next.level} 달성! 스킬 포인트 1개를 얻었어요.`);
    }
    render();
  } catch (error) {
    setMessage(error.message);
  } finally {
    state.hitInFlight = false;
  }
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
    const delayMs = 340 + index * 140;
    window.setTimeout(() => {
      const attack = document.createElement("img");
      attack.className = `pet-attack pet-${pet.key}-attack`;
      attack.src = pet.attack;
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

$("autoHuntAd").addEventListener("click", () => runRewardFlow(
  "자동사냥 광고",
  isOneStoreTarget()
    ? "게임 내 보상으로 자동사냥 시간이 1시간 충전돼요."
    : "완료하면 자동사냥 시간이 1시간 충전돼요.",
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
    message: "자동사냥 시간이 1시간 충전됐어요.",
  }
));

$("boostAd").addEventListener("click", () => runRewardFlow(
  "공속버프 광고",
  isOneStoreTarget()
    ? "게임 내 보상으로 공격 모션과 골드 획득 속도가 빨라져요."
    : "완료하면 공격 모션과 골드 획득 속도가 빨라져요.",
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
    message: "공격 속도 부스터가 1시간 충전됐어요.",
  }
));

$("skillAd").addEventListener("click", () => {
  if (!skillPointRewardsAvailable()) {
    setMessage("모든 스킬 강화가 완료되어 SP를 더 받을 수 없어요.");
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

$("claimReward").addEventListener("click", () => runRewardFlow(
  "리워드 수령 광고",
  isOneStoreTarget()
    ? "게임 내 보상으로 SP가 지급돼요."
    : "완료하면 토스 포인트 지급 대기 기록이 생성돼요.",
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
        : JSON.stringify({ idempotencyKey: crypto.randomUUID(), adSessionToken }),
    }),
    message: isOneStoreTarget() ? "게임 내 보상을 받았어요." : "리워드 수령을 완료했어요.",
  }
));

$("claimFriendInviteReward").addEventListener("click", () => run(
  () => {
    if (!state.mockMonetizationEnabled && !isOneStoreTarget()) {
      throw new Error("토스 공유 리워드 연동 전에는 리뷰 환경에서만 사용할 수 있어요.");
    }
    return api(isOneStoreTarget()
      ? "/api/player/onestore/friend-invite/claim"
      : "/api/player/reward/friend-invite/claim", { method: "POST" });
  },
  `친구 초대 보상으로 SP ${(state.player?.friendInviteRewardSkillPoints || friendInviteRewardSkillPoints).toLocaleString("ko-KR")}개를 받았어요.`
));

$("adSkip").addEventListener("click", () => finishDummyAd());
$("buyCompanion").addEventListener("click", () => runPurchaseFlow({
  title: "동료 펫 입양",
  description: isOneStoreTarget()
    ? "심사용 게임 내 보상으로 동료 펫이 1마리 추가돼요."
    : "결제를 완료하면 동료 펫이 1마리 추가돼요.",
  amountText: isOneStoreTarget()
    ? "심사용 잠금 해제"
    : `₩${(state.player?.companionPriceWon || companionPriceWon).toLocaleString("ko-KR")}`,
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
    : `₩${(state.player?.skillPointPackPriceWon || skillPointPackPriceWon).toLocaleString("ko-KR")}`,
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
      return api("/api/player/ads/auto-hunt/complete", { method: "POST" });
    },
    "자동사냥 시간이 1시간 충전됐어요."
  ));

  $("devBoost").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/ads/boost/complete", { method: "POST" });
    },
    "공격 속도 부스터가 1시간 충전됐어요."
  ));

  $("devSkillPoint").addEventListener("click", () => runDevAction(
    async () => {
      await ensureJobForTest();
      return api("/api/player/ads/skill-point/complete", { method: "POST" });
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
        state.player = await api("/api/player/test/fill-reward-gauge", { method: "POST" });
      }
      return api("/api/player/reward/claim-after-ad", {
        method: "POST",
        body: JSON.stringify({ idempotencyKey: crypto.randomUUID() }),
      });
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
      removeStoredValue(featureTutorialStorageKey);
      $("featureTutorial").classList.add("hidden");
      return api("/api/player/test/reset", { method: "POST" });
    },
    "테스트 상태를 초기화했어요. 직업을 다시 선택하세요."
  ));
}

initializeDevPanel();
syncBgmState();
document.addEventListener("pointerdown", () => startBgm(), { once: true, passive: true });

setInterval(() => {
  if (state.player) {
    simulateHit();
    render();
  }
}, 250);

loadAppConfig()
  .then(() => refresh())
  .catch((error) => setMessage(error.message));
