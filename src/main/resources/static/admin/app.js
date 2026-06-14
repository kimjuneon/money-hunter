const state = {
  accessToken: sessionStorage.getItem("moneyHunterAdminAccessToken") || "",
  username: sessionStorage.getItem("moneyHunterAdminUsername") || "",
  view: "dashboardView",
  auditPage: 0,
  auditSize: 30,
  paymentPage: 0,
  paymentSize: 30,
  paymentTotalPages: 1,
  revenueDays: 30,
  toastTimer: null,
  overview: null,
  players: [],
  playerPage: 0,
  playerSize: 30,
  playerTotalElements: 0,
  playerTotalPages: 1,
  selectedUserKey: "",
  playerDetailExpanded: false,
  policies: [],
  growthDays: 30,
  loadingTimer: null,
  rookieEventSettings: null,
  rookieEventTestState: null,
  promotionTestState: null,
};

const maxGoldPerHour = 6000;
const dungeonEntryHuntRequiredSeconds = 3600;
const daySeconds = 86400;
const fullPowerDungeonRewards = [
  { type: "GOLD", minAmount: 2500, maxAmount: 4000, weight: 35 },
  { type: "SKILL_POINT", minAmount: 4, maxAmount: 4, weight: 30 },
  { type: "AUTO_HUNT_SECONDS", minAmount: 9000, maxAmount: 9000, weight: 25 },
  { type: "BOSS_TICKET", minAmount: 1, maxAmount: 1, weight: 10 },
];
const fullPowerBossRewards = [
  { type: "GOLD", minAmount: 10000, maxAmount: 12000, weight: 35 },
  { type: "SKILL_POINT", minAmount: 3, maxAmount: 3, weight: 40 },
  { type: "AUTO_HUNT_SECONDS", minAmount: 28800, maxAmount: 28800, weight: 25 },
];
const hourPolicyKeys = new Set([
  "autoHuntAdSeconds",
  "maxAdSeconds",
  "dungeonReentryCooldownSeconds",
  "skillPointAdCooldownSeconds",
  "anomalyTimerGraceSeconds",
]);
const minutePolicyKeys = new Set([
  "autoHuntAdCooldownSeconds",
]);
const $ = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
  bindEvents();
  restoreSession();
});

function bindEvents() {
  $("loginForm").addEventListener("submit", login);
  $("playerSearchForm").addEventListener("submit", (event) => {
    event.preventDefault();
    state.playerPage = 0;
    loadPlayers();
  });
  $("playerFilterStatus").addEventListener("change", reloadPlayersFromFirstPage);
  $("playerFilterProgress").addEventListener("change", reloadPlayersFromFirstPage);
  $("playerSortPreset").addEventListener("change", reloadPlayersFromFirstPage);
  $("playerFavoriteMode").addEventListener("change", reloadPlayersFromFirstPage);
  $("playerPageSize").addEventListener("change", reloadPlayersFromFirstPage);
  $("playerHiddenSkinsOnly").addEventListener("change", reloadPlayersFromFirstPage);
  $("playerActiveAutoHuntOnly").addEventListener("change", reloadPlayersFromFirstPage);
  $("playerFilterClearButton").addEventListener("click", clearPlayerFilters);
  $("rookieEventTestLoadButton").addEventListener("click", () => loadRookieEventTestState({ notify: true }));
  $("rookieEventTestResetButton").addEventListener("click", (event) => runRookieEventTestAction("reset", event.currentTarget));
  $("rookieEventTestCompleteDayButton").addEventListener("click", (event) => runRookieEventTestAction("complete-next-day", event.currentTarget));
  $("rookieEventTestAdvanceDayButton").addEventListener("click", (event) => runRookieEventTestAction("advance-day", event.currentTarget));
  $("rookieEventTestApplyButton").addEventListener("click", (event) => runRookieEventTestAction("state", event.currentTarget));
  $("promotionTestLoadButton").addEventListener("click", () => loadPromotionTestState({ notify: true }));
  $("promotionTestPrepareButton").addEventListener("click", (event) => runPromotionTestAction("prepare", event.currentTarget));
  $("promotionTestBenefitEntryButton").addEventListener("click", (event) => runPromotionTestAction("benefit-tab-entry", event.currentTarget));
  $("promotionTestClaimRewardButton").addEventListener("click", (event) => runPromotionTestAction("claim-reward", event.currentTarget));
  $("promotionTestClearLogButton").addEventListener("click", (event) => runPromotionTestAction("executions/clear", event.currentTarget));
  $("logoutButton").addEventListener("click", logout);
  $("refreshButton").addEventListener("click", () => refreshCurrentView({ notify: true }));
  $("prevAuditButton").addEventListener("click", () => {
    if (state.auditPage > 0) {
      state.auditPage -= 1;
      loadAudits();
    }
  });
  $("nextAuditButton").addEventListener("click", () => {
    state.auditPage += 1;
    loadAudits();
  });
  $("prevPaymentButton").addEventListener("click", () => {
    if (state.paymentPage > 0) {
      state.paymentPage -= 1;
      loadPayments();
    }
  });
  $("nextPaymentButton").addEventListener("click", () => {
    if (state.paymentPage + 1 < state.paymentTotalPages) {
      state.paymentPage += 1;
      loadPayments();
    }
  });
  $("prevPlayerButton").addEventListener("click", () => {
    if (state.playerPage > 0) {
      state.playerPage -= 1;
      loadPlayers();
    }
  });
  $("nextPlayerButton").addEventListener("click", () => {
    if (state.playerPage + 1 < state.playerTotalPages) {
      state.playerPage += 1;
      loadPlayers();
    }
  });
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });
  document.addEventListener("pointerover", showChartTooltip);
  document.addEventListener("pointermove", moveChartTooltip);
  document.addEventListener("pointerout", hideChartTooltip);
  document.addEventListener("mouseover", showChartTooltip);
  document.addEventListener("mousemove", moveChartTooltip);
  document.addEventListener("mouseout", hideChartTooltip);
}

async function restoreSession() {
  if (!state.accessToken) {
    showLogin();
    return;
  }
  try {
    const me = await request("/api/admin/auth/me");
    state.username = me.username || state.username;
    sessionStorage.setItem("moneyHunterAdminUsername", state.username);
    showAdmin();
  } catch (error) {
    clearSession(error.message);
  }
}

async function login(event) {
  event.preventDefault();
  $("loginError").textContent = "";
  const loginId = $("adminLoginIdInput").value.trim();
  const password = $("adminPasswordInput").value;
  try {
    const session = await publicRequest("/api/admin/auth/login", {
      method: "POST",
      body: { loginId, password },
    });
    state.accessToken = session.accessToken;
    state.username = session.username;
    sessionStorage.setItem("moneyHunterAdminAccessToken", state.accessToken);
    sessionStorage.setItem("moneyHunterAdminUsername", state.username);
    $("adminPasswordInput").value = "";
    showAdmin();
  } catch (error) {
    clearSession(error.message);
  }
}

async function logout() {
  try {
    if (state.accessToken) {
      await request("/api/admin/auth/logout", { method: "POST" });
    }
  } finally {
    clearSession();
  }
}

function clearSession(message = "") {
  state.accessToken = "";
  state.username = "";
  sessionStorage.removeItem("moneyHunterAdminAccessToken");
  sessionStorage.removeItem("moneyHunterAdminUsername");
  showLogin();
  if (message) {
    $("loginError").textContent = message;
  }
}

function showLogin() {
  $("loginView").classList.remove("hidden");
  $("adminView").classList.add("hidden");
}

function showAdmin() {
  $("adminName").textContent = state.username || "Admin";
  $("loginView").classList.add("hidden");
  $("adminView").classList.remove("hidden");
  refreshCurrentView();
}

function activateView(viewId) {
  state.view = viewId;
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.classList.toggle("active", button.dataset.view === viewId);
  });
  document.querySelectorAll(".view").forEach((view) => {
    view.classList.toggle("hidden", view.id !== viewId);
  });
  $("pageTitle").textContent = {
    dashboardView: "대시보드",
    eventView: "이벤트 관리",
    anomalyView: "이상징후",
    playerView: "유저 관리",
    paymentView: "결제",
    revenueView: "수익",
    policyView: "정책값",
    auditView: "감사 로그",
    monitoringView: "모니터링",
    testToolsView: "테스트 도구",
  }[viewId] || "대시보드";
}

function switchView(viewId) {
  activateView(viewId);
  refreshCurrentView();
}

async function refreshCurrentView(options = {}) {
  const notify = Boolean(options.notify);
  const button = $("refreshButton");
  const originalText = button.textContent;
  if (notify) {
    setButtonBusy(button, true);
    button.textContent = "새로고침 중";
    showLoading("새로고침 중이에요.");
  }
  try {
    if (state.view === "policyView") {
      await loadPolicies();
    } else if (state.view === "auditView") {
      await loadAudits();
    } else if (state.view === "paymentView") {
      await loadPayments();
    } else if (state.view === "revenueView") {
      await loadRevenue();
    } else if (state.view === "anomalyView") {
      await loadAnomalies();
    } else if (state.view === "playerView") {
      await loadPlayers();
    } else if (state.view === "monitoringView") {
      await loadServerMetrics();
    } else if (state.view === "eventView") {
      await loadRookieEventSettings();
    } else if (state.view === "testToolsView") {
      await loadRookieEventTestState();
    } else {
      await loadOverview();
    }
    if (notify) {
      showToast("새로고침 완료");
    }
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    if (notify) {
      hideLoading();
      button.textContent = originalText;
      setButtonBusy(button, false);
    }
  }
}

async function loadRookieEventSettings() {
  const result = $("rookieEventSettingsResult");
  result.textContent = "";
  result.classList.remove("error-text");
  try {
    const data = await request("/api/admin/events/rookie-event");
    state.rookieEventSettings = data;
    renderRookieEventSettings(data);
  } catch (error) {
    state.rookieEventSettings = null;
    $("rookieEventSettingsUpdatedAt").textContent = "";
    $("rookieEventSettingsPanel").innerHTML = `<p class="empty">${escapeHtml(error.message)}</p>`;
    result.textContent = error.message;
    result.classList.add("error-text");
    throw error;
  }
}

function renderRookieEventSettings(settings) {
  const panel = $("rookieEventSettingsPanel");
  if (!settings) {
    $("rookieEventSettingsUpdatedAt").textContent = "";
    panel.innerHTML = `<p class="empty">이벤트 설정을 불러오지 못했어요.</p>`;
    return;
  }
  const enabled = Boolean(settings.enabled);
  const statusLabel = enabled ? "활성화" : "비활성화";
  $("rookieEventSettingsUpdatedAt").textContent = `최근 변경 ${formatDate(settings.updatedAt)}`;
  panel.innerHTML = `
    <div class="event-settings-layout">
      <article class="event-status-card ${enabled ? "active" : "inactive"}">
        <div>
          <span class="event-status-chip">${escapeHtml(statusLabel)}</span>
          <strong>7일 사냥 동행 이벤트</strong>
          <p>
            참여 시작 가능 기간은 무기한이에요. 이벤트를 끄면 아직 시작하지 않은 유저의 신규 참여만 막고,
            이미 시작한 유저는 남은 기간 동안 계속 진행할 수 있어요.
          </p>
        </div>
        <label class="event-toggle-card">
          <input id="rookieEventEnabledInput" type="checkbox" ${enabled ? "checked" : ""} />
          <span>
            <strong>이벤트 공개</strong>
            <small>${enabled ? "신규 참여 허용 중" : "신규 참여 차단 중"}</small>
          </span>
        </label>
      </article>

      <div class="event-stat-grid">
        ${eventStat("시작 유저", settings.startedPlayers, "명")}
        ${eventStat("최종 완료", settings.completedPlayers, "명")}
        ${eventStat("펫 수령", settings.rewardClaimedPlayers, "명")}
        ${eventStat("미시작 대상", settings.eligibleUnstartedPlayers, "명")}
      </div>

      <section class="event-rule-list">
        ${eventRule("참여 시작 기한", settings.startWindowUnlimited ? "무기한" : "기간 제한", "관리자 활성화 상태로 신규 참여를 제어해요.")}
        ${eventRule("미션 구성", `${formatNumber(settings.eventDays)}일 미션`, `유저별 진행 가능 기간 ${formatNumber(settings.playerProgressDays)}일`)}
        ${eventRule("완료 보상", `이벤트 펫 ${formatNumber(settings.eventPetDurationDays)}일`, `일반 펫 ${formatNumber(settings.eventPetSkillLevel)}레벨 기준 성능`)}
      </section>

      <div class="event-save-row">
        <label>
          <span>변경 사유</span>
          <input id="rookieEventSettingsReason" type="text" maxlength="500" placeholder="감사 로그에 남길 사유" />
        </label>
        <button id="rookieEventSettingsSaveButton" class="secondary" type="button">설정 저장</button>
      </div>
    </div>
  `;
  $("rookieEventSettingsSaveButton").addEventListener("click", (event) => saveRookieEventSettings(event.currentTarget));
}

function eventStat(label, value, unit) {
  return `
    <article class="event-stat-card">
      <span>${escapeHtml(label)}</span>
      <strong>${formatNumber(value)}${escapeHtml(unit)}</strong>
    </article>
  `;
}

function eventRule(label, value, detail) {
  return `
    <article class="event-rule-card">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value)}</strong>
      <small>${escapeHtml(detail)}</small>
    </article>
  `;
}

async function saveRookieEventSettings(button) {
  const result = $("rookieEventSettingsResult");
  result.textContent = "";
  result.classList.remove("error-text");
  const enabled = Boolean($("rookieEventEnabledInput")?.checked);
  const reason = $("rookieEventSettingsReason")?.value?.trim() || "";
  setButtonBusy(button, true);
  try {
    const data = await request("/api/admin/events/rookie-event", {
      method: "PATCH",
      body: { enabled, reason },
    });
    state.rookieEventSettings = data;
    renderRookieEventSettings(data);
    const message = enabled ? "이벤트를 활성화했어요." : "이벤트를 비활성화했어요.";
    $("rookieEventSettingsResult").textContent = message;
    showToast(message);
  } catch (error) {
    result.textContent = error.message;
    result.classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function loadRookieEventTestState(options = {}) {
  const userKey = rookieEventTestUserKey();
  const result = $("rookieEventTestResult");
  result.textContent = "";
  result.classList.remove("error-text");
  if (!userKey) {
    state.rookieEventTestState = null;
    renderRookieEventTestState(null);
    return;
  }
  try {
    const data = await request(`/api/admin/test-tools/rookie-event/${encodeURIComponent(userKey)}`);
    state.rookieEventTestState = data;
    syncRookieEventTestInputs(data);
    renderRookieEventTestState(data);
    if (options.notify) {
      result.textContent = "이벤트 상태를 불러왔어요.";
      showToast("이벤트 상태 조회 완료");
    }
  } catch (error) {
    state.rookieEventTestState = null;
    renderRookieEventTestState(null);
    result.textContent = error.message;
    result.classList.add("error-text");
    showToast(error.message, "error");
  }
}

async function runRookieEventTestAction(action, button) {
  const userKey = rookieEventTestUserKey();
  const result = $("rookieEventTestResult");
  result.textContent = "";
  result.classList.remove("error-text");
  if (!userKey) {
    result.textContent = "대상 유저 키를 입력해 주세요.";
    result.classList.add("error-text");
    return;
  }
  const payload = rookieEventTestPayload();
  if (!payload) {
    return;
  }
  if (action === "state" && !confirm(`${userKey} 유저의 이벤트 상태를 직접 적용할까요?`)) {
    return;
  }
  const endpoint = action === "state"
    ? "state"
    : action === "reset"
      ? "reset"
      : action === "advance-day"
        ? "advance-day"
        : "complete-next-day";
  setButtonBusy(button, true);
  try {
    const data = await request(`/api/admin/test-tools/rookie-event/${encodeURIComponent(userKey)}/${endpoint}`, {
      method: "POST",
      body: payload,
    });
    state.rookieEventTestState = data;
    syncRookieEventTestInputs(data);
    renderRookieEventTestState(data);
    const message = {
      reset: "이벤트 테스트 상태를 초기화했어요.",
      "complete-next-day": "다음 일차를 완료 상태로 만들었어요.",
      "advance-day": "이벤트 테스트 시간을 하루 넘겼어요.",
      state: "이벤트 테스트 상태를 적용했어요.",
    }[action];
    result.textContent = message;
    showToast(message);
  } catch (error) {
    result.textContent = error.message;
    result.classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

function rookieEventTestUserKey() {
  return $("rookieEventTestUserKey")?.value?.trim() || "";
}

function rookieEventTestPayload() {
  const completedDays = Number($("rookieEventTestCompletedDays").value);
  const rewardedDays = Number($("rookieEventTestRewardedDays").value);
  const finalRewardClaimed = $("rookieEventTestFinalClaimed").checked;
  const reason = $("rookieEventTestReason").value.trim();
  const result = $("rookieEventTestResult");
  if (!Number.isInteger(completedDays) || completedDays < 0 || completedDays > 7) {
    result.textContent = "완료 일차는 0부터 7 사이로 입력해 주세요.";
    result.classList.add("error-text");
    return null;
  }
  if (!Number.isInteger(rewardedDays) || rewardedDays < 0 || rewardedDays > 7) {
    result.textContent = "수령한 일일 보상은 0부터 7 사이로 입력해 주세요.";
    result.classList.add("error-text");
    return null;
  }
  if (rewardedDays > completedDays) {
    result.textContent = "수령한 일일 보상 수는 완료 일차보다 클 수 없어요.";
    result.classList.add("error-text");
    return null;
  }
  if (finalRewardClaimed && completedDays < 7) {
    result.textContent = "완료 펫 보상 수령 처리는 7일차 완료 상태에서만 가능해요.";
    result.classList.add("error-text");
    return null;
  }
  return { completedDays, rewardedDays, finalRewardClaimed, reason };
}

function syncRookieEventTestInputs(player) {
  const event = player?.rookieEvent;
  if (!event?.visible) {
    return;
  }
  $("rookieEventTestCompletedDays").value = String(event.completedDays || 0);
  $("rookieEventTestRewardedDays").value = String((event.days || []).filter((day) => day.rewardClaimed).length);
  $("rookieEventTestFinalClaimed").checked = Boolean(event.rewardClaimed);
}

function renderRookieEventTestState(player) {
  const target = $("rookieEventTestSummary");
  if (!player) {
    target.innerHTML = `
      <div class="empty test-tool-empty">
        대상 유저 키를 입력하고 상태 조회를 누르면 이벤트 상태가 표시돼요.
      </div>
    `;
    return;
  }
  const event = player.rookieEvent || {};
  const days = Array.isArray(event.days) ? event.days : [];
  const finalClaimable = Boolean(event.completed && !event.rewardClaimed);
  target.innerHTML = `
    <div class="test-tool-metrics">
      ${testMetric("유저", player.userKey)}
      ${testMetric("직업", jobLabel(player.job || "미선택"))}
      ${testMetric("이벤트", event.visible ? (event.active ? "진행 중" : event.completed ? "완료" : event.expired ? "종료" : "대기") : "미시작")}
      ${testMetric("완료", `${formatNumber(event.completedDays || 0)} / 7일`)}
      ${testMetric("남은 기간", event.visible ? `${formatNumber(event.daysRemaining || 0)}일` : "-")}
      ${testMetric("최종 보상", event.rewardClaimed ? "수령 완료" : finalClaimable ? "수령 가능" : "대기")}
      ${testMetric("SP", formatNumber(player.skillPoints))}
      ${testMetric("자동사냥", formatDate(player.autoHuntEndsAt))}
    </div>
    <div class="test-tool-days">
      ${days.map((day) => `
        <article class="test-tool-day ${day.completed ? "is-completed" : ""}">
          <strong>${formatNumber(day.day)}일차 · ${escapeHtml(day.title)}</strong>
          <span>${escapeHtml(rookieEventTestDayState(day))}</span>
          <small>${escapeHtml(day.rewardLabel || "-")} · ${escapeHtml(rookieEventTestRewardState(day))}</small>
        </article>
      `).join("") || `<p class="empty">이벤트 일차 정보가 없어요.</p>`}
    </div>
  `;
}

function testMetric(label, value) {
  return `
    <article class="test-tool-metric">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value)}</strong>
    </article>
  `;
}

function rookieEventTestDayState(day) {
  if (day.completed) {
    return "미션 완료";
  }
  if (day.current) {
    return "진행 중";
  }
  if (day.locked) {
    return "대기";
  }
  return "미진행";
}

function rookieEventTestRewardState(day) {
  if (day.rewardClaimed) {
    return "일일 보상 수령 완료";
  }
  if (day.rewardClaimable) {
    return "일일 보상 수령 가능";
  }
  if (day.completed) {
    return "이전 보상 먼저 수령";
  }
  return "미션 완료 후 가능";
}

async function loadPromotionTestState(options = {}) {
  const userKey = promotionTestUserKey();
  const result = $("promotionTestResult");
  result.textContent = "";
  result.classList.remove("error-text");
  if (!userKey) {
    state.promotionTestState = null;
    renderPromotionTestState(null);
    return;
  }
  try {
    const data = await request(`/api/admin/test-tools/promotion/${encodeURIComponent(userKey)}`);
    state.promotionTestState = data;
    renderPromotionTestState(data);
    if (options.notify) {
      result.textContent = "프로모션 테스트 상태를 불러왔어요.";
      showToast("프로모션 상태 조회 완료");
    }
  } catch (error) {
    state.promotionTestState = null;
    renderPromotionTestState(null);
    result.textContent = error.message;
    result.classList.add("error-text");
    showToast(error.message, "error");
  }
}

async function runPromotionTestAction(action, button) {
  const userKey = promotionTestUserKey();
  const result = $("promotionTestResult");
  result.textContent = "";
  result.classList.remove("error-text");
  if (!userKey) {
    result.textContent = "대상 유저 키를 입력해 주세요.";
    result.classList.add("error-text");
    return;
  }
  if (action === "prepare" && !confirm(`${userKey} 유저를 초기화하고 전사로 설정할까요?`)) {
    return;
  }
  const payload = { reason: $("promotionTestReason").value.trim() };
  setButtonBusy(button, true);
  try {
    const data = await request(`/api/admin/test-tools/promotion/${encodeURIComponent(userKey)}/${action}`, {
      method: "POST",
      body: payload,
    });
    state.promotionTestState = data;
    renderPromotionTestState(data);
    const message = {
      prepare: "프로모션 테스트 준비를 완료했어요.",
      "benefit-tab-entry": "혜택 탭 프로모션 조건을 충족시켰어요.",
      "claim-reward": "보상 수령을 실행했어요.",
      "executions/clear": "프로모션 로그를 초기화했어요.",
    }[action] || "프로모션 테스트 작업을 완료했어요.";
    result.textContent = message;
    showToast(message);
  } catch (error) {
    result.textContent = error.message;
    result.classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

function promotionTestUserKey() {
  return $("promotionTestUserKey")?.value?.trim() || "";
}

function renderPromotionTestState(data) {
  const target = $("promotionTestSummary");
  if (!data?.player) {
    target.innerHTML = `
      <div class="empty test-tool-empty">
        대상 유저 키를 입력하고 상태 조회를 누르면 프로모션 기록이 표시돼요.
      </div>
    `;
    return;
  }
  const player = data.player;
  const rewardClaim = data.rewardClaim;
  const executions = Array.isArray(data.executions) ? data.executions : [];
  const mockExecutionLogAvailable = Boolean(data.mockExecutionLogAvailable);
  target.innerHTML = `
    <div class="test-tool-metrics">
      ${testMetric("유저", player.userKey)}
      ${testMetric("직업", jobLabel(player.job || "미선택"))}
      ${testMetric("골드", `${formatNumber(player.gold)}G`)}
      ${testMetric("SP", formatNumber(player.skillPoints))}
      ${testMetric("최근 수령", rewardClaim ? `${formatNumber(rewardClaim.pointAmount)}P · ${rewardClaim.status}` : "-")}
      ${testMetric("지급 기록", mockExecutionLogAvailable ? `${formatNumber(executions.length)}건` : "실제 호출")}
    </div>
    <div class="promotion-execution-list">
      ${executions.map((execution, index) => `
        <article class="promotion-execution-row">
          <strong>${formatNumber(index + 1)}. ${escapeHtml(execution.promotionCode || "-")}</strong>
          <span>${formatNumber(execution.amount || 0)}P · ${escapeHtml(execution.result || "-")} · ${formatDate(execution.executedAt)}</span>
          <small>${escapeHtml(execution.executionKey || "-")}</small>
        </article>
      `).join("") || `<p class="empty test-tool-empty">${mockExecutionLogAvailable ? "아직 기록된 mock 프로모션이 없어요." : "prod에서는 실제 토스 프로모션 API를 호출하고, mock 로그는 남기지 않아요."}</p>`}
    </div>
  `;
}

async function loadOverview() {
  const data = await request("/api/admin/overview");
  state.overview = data;
  renderStatus(data);
  renderMetrics(data);
  renderEconomyPanel(data);
  renderMonitoringPanel(data);
  await loadPlayerGrowth(state.growthDays);
}

function renderStatus(data) {
  const ready = data.tossReleaseReady;
  const statusItems = data.runtimeStatusItems || [];
  $("statusBand").innerHTML = `
    <div class="release-card ${ready ? "ready" : "blocked"}">
      <div class="release-head">
        <span>${ready ? "READY" : "CHECK"}</span>
        <strong>${escapeHtml(data.integrationMode)} · ${escapeHtml(data.distributionTarget)}</strong>
      </div>
      <p>${ready ? "운영 차단 항목이 없어요." : escapeHtml((data.releaseBlockers || []).join(", "))}</p>
      <div class="runtime-status-grid">
        ${renderRuntimeStatusItems(statusItems)}
      </div>
    </div>
  `;
}

function renderMetrics(data) {
  const metrics = [
    ["전체 유저", data.totalPlayers, "명"],
    ["온보딩 완료", data.onboardedPlayers, "명"],
    ["정지 유저", data.suspendedPlayers, "명"],
    ["오늘 신규", data.newPlayersToday, "명"],
    ["오늘 앱 진입", data.appEnteredUsersToday, "명"],
    ["3일 충성 활성", data.activeUsersToday, "명"],
    ["방문/저관여", data.visitorOnlyUsersToday, "명"],
    ["자동사냥 활성", data.activeAutoHuntPlayers, "명"],
    ["총 보유 골드", data.totalGoldInCirculation, "G"],
    ["오늘 광고 이벤트", data.rewardAdEventsToday, "회"],
    ["오늘 보상 수령", data.rewardClaimsToday, "건"],
    ["오늘 지급 포인트", data.rewardPointsToday, "P"],
    ["대기 중 포인트", data.pendingRewardPoints, "P"],
    ["오늘 광고 매출 추정", data.estimatedAdRevenueWonToday, "원"],
    ["오늘 순수익 추정", data.estimatedNetWonToday, "원"],
  ];
  $("metricGrid").innerHTML = metrics.map(([label, value, unit]) => `
    <article class="metric-card">
      <span>${escapeHtml(label)}</span>
      <strong>${formatNumber(value)}${escapeHtml(unit)}</strong>
    </article>
  `).join("");
}

function renderEconomyPanel(data) {
  const economy = data.economy || {};
  const pendingPoints = Number(data.pendingRewardPoints || 0);
  const pendingWon = pendingPoints;
  const adRevenue = Number(economy.adRevenuePerRewardAdWon || 0);
  const goldPerPoint = Number(economy.goldPerTossPoint || 0);
  const expectedGoldPerAd = expectedFullPowerGoldPerRewardAd(economy);
  const dungeonOpportunities = dungeonOpportunitiesUnlockedByRewardAd(economy);
  const derivedGoldPerPoint = deriveGoldPerPoint(adRevenue, economy);
  const currentMaxPointValue = goldPerPoint > 0 ? Math.floor(maxGoldPerHour / goldPerPoint) : 0;
  $("economyPanel").innerHTML = `
    <section class="panel economy-panel">
      <div class="panel-head">
        <div>
          <h3>수익/보상 정책</h3>
          <p>광고 단가 기준 환산과 포인트 지급 기준을 운영 중 바로 확인해요.</p>
        </div>
        <span class="muted">${formatDate(data.generatedAt)}</span>
      </div>
      <div class="economy-grid">
        ${economyItem("리워드 광고 단가", economy.adRevenuePerRewardAdWon, "원/회")}
        ${economyItem("최대 수급 기준", maxGoldPerHour, "G/h")}
        ${economyItem("광고 1회 기대 골드", Math.ceil(expectedGoldPerAd), "G")}
        ${economyItem("무료 던전 기대 횟수", dungeonOpportunities.toFixed(2), "회")}
        ${economyItem("현재 최대 환산", currentMaxPointValue, "P/h")}
        ${economyItem("포인트 환산", economy.goldPerTossPoint, "골드/P")}
        ${economyItem("보상 신청 기준", economy.rewardPointAmount, "P")}
        ${economyItem("신청 필요 골드", economy.rewardGoldThreshold, "G")}
        ${economyItem("대기 중 포인트", pendingPoints, "P")}
      </div>
      <div class="quick-policy-card calibration-card">
        <div>
          <strong>광고 수익 기준 자동 환산</strong>
          <p>풀강 유저의 광고 1회 기대 골드가 광고 단가와 같아지도록 환산값을 계산해요.</p>
        </div>
        <label>
          <span>1회 예상 매출(원)</span>
          <input id="revenueAdInput" type="number" min="1" max="10000" value="${escapeHtml(adRevenue || 1)}" />
        </label>
        <div class="derived-policy">
          <span>자동 환산값</span>
          <strong id="derivedGoldPerPoint">${formatNumber(derivedGoldPerPoint)}G/P</strong>
          <small id="expectedGoldPerAd">기대 ${formatNumber(Math.ceil(expectedGoldPerAd))}G</small>
        </div>
        <button id="revenueCalibrationSaveButton" class="secondary" type="button">환산 저장</button>
      </div>
      <div class="quick-policy-card direct-policy-card">
        <div>
          <strong>토스포인트 1P당 골드 직접 수정</strong>
          <p>광고 단가와 무관하게 지급 기준만 직접 조정할 때 사용해요.</p>
        </div>
        <label>
          <span>골드/P</span>
          <input id="directGoldPerPointInput" type="number" min="1" max="1000000" value="${escapeHtml(goldPerPoint || 1)}" />
        </label>
        <button id="directGoldPerPointSaveButton" class="secondary" type="button">직접 저장</button>
      </div>
      <p class="note">
        대기 중 포인트 ${formatNumber(pendingPoints)}P(예상 지급액 ${formatNumber(pendingWon)}원)는 실제 생성된 보상 수령 대기 기록의 합계예요.
        토스포인트 환산 기준을 바꾸면 보유 골드는 같은 포인트 가치가 되도록 자동 보정되고, 이미 대기 중인 보상 기록은 재계산하지 않아요.
      </p>
    </section>
  `;
  $("revenueAdInput").addEventListener("input", () => {
    $("derivedGoldPerPoint").textContent = `${formatNumber(deriveGoldPerPoint(Number($("revenueAdInput").value), economy))}G/P`;
    $("expectedGoldPerAd").textContent = `기대 ${formatNumber(Math.ceil(expectedFullPowerGoldPerRewardAd(economy)))}G`;
  });
  $("revenueCalibrationSaveButton").addEventListener("click", () => {
    saveRevenueCalibration($("revenueCalibrationSaveButton"));
  });
  $("directGoldPerPointSaveButton").addEventListener("click", () => {
    saveQuickPolicy(
      "goldPerTossPoint",
      Number($("directGoldPerPointInput").value),
      "토스포인트 환산 기준을 저장했어요.",
      $("directGoldPerPointSaveButton"),
    );
  });
}

function economyItem(label, value, unit) {
  return `
    <article class="economy-item">
      <span>${escapeHtml(label)}</span>
      <strong>${formatNumber(value)}${escapeHtml(unit)}</strong>
    </article>
  `;
}

function deriveGoldPerPoint(adRevenuePerAd, economy = {}) {
  const adRevenue = Number(adRevenuePerAd || 0);
  if (!Number.isFinite(adRevenue) || adRevenue <= 0) {
    return 1;
  }
  return Math.max(1, Math.min(1000000, Math.ceil(expectedFullPowerGoldPerRewardAd(economy) / adRevenue)));
}

function expectedFullPowerGoldPerRewardAd(economy = {}) {
  const autoHuntGold = secondsToFullPowerGold(Number(economy.autoHuntAdSeconds || 0));
  const bossExpectedGold = expectedRewardGold(fullPowerBossRewards, 0);
  const dungeonExpectedGold = expectedRewardGold(fullPowerDungeonRewards, bossExpectedGold);
  return autoHuntGold + dungeonExpectedGold * dungeonOpportunitiesUnlockedByRewardAd(economy);
}

function dungeonOpportunitiesUnlockedByRewardAd(economy = {}) {
  const freeDailyLimit = Math.max(0, Number(economy.dungeonFreeDailyLimit || 0));
  const autoHuntSeconds = Math.max(0, Number(economy.autoHuntAdSeconds || 0));
  if (freeDailyLimit <= 0 || autoHuntSeconds <= 0) {
    return 0;
  }
  const gateContribution = Math.min(1, autoHuntSeconds / dungeonEntryHuntRequiredSeconds);
  const cooldownSeconds = Math.max(0, Number(economy.dungeonReentryCooldownSeconds || 0));
  if (cooldownSeconds <= 0) {
    return freeDailyLimit * gateContribution;
  }
  const availableSecondsAfterGate = Math.max(0, daySeconds - dungeonEntryHuntRequiredSeconds);
  const dailyRunsPossible = 1 + Math.floor(availableSecondsAfterGate / cooldownSeconds);
  return Math.min(freeDailyLimit, Math.max(1, dailyRunsPossible)) * gateContribution;
}

function expectedRewardGold(rewards, bossExpectedGold) {
  const totalWeight = rewards.reduce((sum, reward) => sum + Number(reward.weight || 0), 0);
  if (totalWeight <= 0) {
    return 0;
  }
  return rewards.reduce((sum, reward) => {
    const probability = Number(reward.weight || 0) / totalWeight;
    return sum + probability * rewardGoldValue(reward, bossExpectedGold);
  }, 0);
}

function rewardGoldValue(reward, bossExpectedGold) {
  const averageAmount = (Number(reward.minAmount || 0) + Number(reward.maxAmount || 0)) / 2;
  if (reward.type === "GOLD") {
    return averageAmount;
  }
  if (reward.type === "AUTO_HUNT_SECONDS") {
    return secondsToFullPowerGold(averageAmount);
  }
  if (reward.type === "BOSS_TICKET") {
    return bossExpectedGold * averageAmount;
  }
  return 0;
}

function secondsToFullPowerGold(seconds) {
  return maxGoldPerHour * Math.max(0, Number(seconds || 0)) / 3600;
}

async function saveRevenueCalibration(button) {
  const value = Number($("revenueAdInput").value);
  if (!Number.isFinite(value) || value < 1 || value > 10000) {
    showToast("광고 1회 예상 매출은 1원 이상 10,000원 이하로 입력해주세요.", "error");
    return;
  }
  setButtonBusy(button, true);
  try {
    await request("/api/admin/economy/revenue-calibration", {
      method: "PATCH",
      body: { adRevenuePerRewardAdWon: value },
    });
    showToast("광고 수익 기준 환산값을 저장했어요.");
    await loadOverview();
    if (state.view === "policyView") {
      await loadPolicies();
    }
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function saveQuickPolicy(key, value, successMessage, button = null) {
  if (!Number.isFinite(value)) {
    showToast("정책값을 숫자로 입력해주세요.", "error");
    return;
  }
  if (button) {
    setButtonBusy(button, true);
  }
  try {
    await request("/api/admin/policies", {
      method: "PATCH",
      body: { key, value, resetToDefault: false, reason: "" },
    });
    showToast(successMessage);
    await loadOverview();
    if (state.view === "policyView") {
      await loadPolicies();
    }
  } catch (error) {
    showToast(error.message, "error");
  } finally {
    if (button) {
      setButtonBusy(button, false);
    }
  }
}

function renderMonitoringPanel(data) {
  const totalPlayers = Number(data.totalPlayers || 0);
  const appEnteredUsers = Number(data.appEnteredUsersToday || 0);
  const activeUsers = Number(data.activeUsersToday || 0);
  const adEvents = Number(data.rewardAdEventsToday || 0);
  const items = [
    ["온보딩 전환", formatPercent(data.onboardedPlayers, totalPlayers), "직업 선택 완료 비율"],
    ["자동사냥 활성률", formatPercent(data.activeAutoHuntPlayers, totalPlayers), "현재 사냥 중인 유저"],
    ["3일 충성 활성", `${formatNumber(activeUsers)}명`, "오늘 제외 직전 3일 기준"],
    ["방문/저관여 비율", formatPercent(data.visitorOnlyUsersToday, appEnteredUsers), "오늘 진입 중 충성 조건 미충족"],
    ["충성 활성당 광고", activeUsers > 0 ? (adEvents / activeUsers).toFixed(2) : "0.00", "서버 광고/보상 이벤트 기준"],
    ["대기 보상", `${formatNumber(data.pendingRewardClaims)}건`, `${formatNumber(data.pendingRewardPoints)}P 지급 대기`],
    ["오늘 순수익", `${formatNumber(data.estimatedNetWonToday)}원`, "광고 추정 - 포인트 지급"],
  ];
  $("monitoringPanel").innerHTML = `
    <section class="panel monitoring-panel">
      <div class="panel-head">
        <div>
          <h3>운영 모니터링</h3>
          <p>현재 플레이, 광고, 보상 흐름이 정상 범위인지 빠르게 확인해요.</p>
        </div>
      </div>
      <div class="monitoring-grid">
        ${items.map(([label, value, detail]) => `
          <article class="monitoring-card">
            <span>${escapeHtml(label)}</span>
            <strong>${escapeHtml(value)}</strong>
            <small>${escapeHtml(detail)}</small>
          </article>
        `).join("")}
      </div>
    </section>
  `;
}

async function loadPlayerGrowth(days = state.growthDays) {
  const data = await request(`/api/admin/player-growth?days=${encodeURIComponent(days)}`);
  state.growthDays = data.days || days;
  renderPlayerGrowth(data);
}

function renderPlayerGrowth(data) {
  const points = data.points || [];
  const totalNewPlayers = points.reduce((sum, point) => sum + Number(point.newPlayers || 0), 0);
  const latestTotal = points.length ? Number(points[points.length - 1].totalPlayers || 0) : 0;
  $("playerGrowthPanel").innerHTML = `
    <section class="panel player-growth-panel">
      <div class="panel-head">
        <div>
          <h3>전체 유저 추이</h3>
          <p>기간별 누적 유저 수를 선형 그래프로 확인해요.</p>
        </div>
        <label class="compact-select">
          <span>기간</span>
          <select id="growthPeriodSelect">
            ${[7, 14, 30, 90].map((days) => `
              <option value="${days}" ${days === state.growthDays ? "selected" : ""}>${days}일</option>
            `).join("")}
          </select>
        </label>
      </div>
      <div class="growth-summary">
        ${economyItem("현재 전체 유저", latestTotal, "명")}
        ${economyItem("기간 신규 유저", totalNewPlayers, "명")}
        ${economyItem("집계 기간", data.days || state.growthDays, "일")}
      </div>
      ${renderLineChart(points)}
      ${renderActiveUserChart(points)}
    </section>
  `;
  $("growthPeriodSelect").addEventListener("change", (event) => {
    state.growthDays = Number(event.target.value);
    loadPlayerGrowth(state.growthDays).catch((error) => showToast(error.message, "error"));
  });
}

function renderLineChart(points) {
  if (!points.length) {
    return `<p class="empty">유저 추이 데이터가 아직 없어요.</p>`;
  }
  const width = 640;
  const height = 176;
  const totals = points.map((point) => Number(point.totalPlayers || 0));
  const rawMinTotal = Math.min(...totals);
  const rawMaxTotal = Math.max(1, ...totals);
  const rawRange = Math.max(1, rawMaxTotal - rawMinTotal);
  const padding = Math.max(1, Math.ceil(rawRange * 0.12));
  const minTotal = Math.max(0, rawMinTotal === rawMaxTotal
    ? rawMinTotal - Math.max(1, Math.ceil(rawMaxTotal * 0.05))
    : rawMinTotal - padding);
  const maxTotal = Math.max(minTotal + 1, rawMaxTotal === rawMinTotal
    ? rawMaxTotal + Math.max(1, Math.ceil(rawMaxTotal * 0.05))
    : rawMaxTotal + padding);
  const midTotal = Math.round((minTotal + maxTotal) / 2);
  const x = (index) => points.length === 1 ? width / 2 : width * index / (points.length - 1);
  const y = (value) => height - height * (Number(value || 0) - minTotal) / (maxTotal - minTotal);
  const linePoints = points.map((point, index) => `${x(index).toFixed(1)},${y(point.totalPlayers).toFixed(1)}`).join(" ");
  const areaPoints = `0,${height} ${linePoints} ${width},${height}`;
  const lastPoint = points[points.length - 1];
  const lastX = x(points.length - 1).toFixed(1);
  const lastY = y(lastPoint.totalPlayers).toFixed(1);
  const labelStep = Math.max(1, Math.ceil(points.length / 5));
  const labels = points
    .map((point, index) => ({ point, index }))
    .filter(({ index }) => index === 0 || index === points.length - 1 || index % labelStep === 0)
    .map(({ point, index }) => {
      const edgeClass = index === 0 ? " first" : index === points.length - 1 ? " last" : "";
      return `
        <span class="growth-x-label${edgeClass}">
          ${escapeHtml(String(point.date || "").slice(5))}
          <small>+${formatNumber(point.newPlayers || 0)}</small>
        </span>
      `;
    }).join("");
  return `
    <div class="growth-chart" role="img" aria-label="전체 유저 선형 그래프">
      <div class="growth-chart-top">
        <div>
          <span>표시 범위</span>
          <strong>${formatNumber(minTotal)}명 - ${formatNumber(maxTotal)}명</strong>
        </div>
        <div class="growth-latest">
          <span>최신 누적</span>
          <strong>${formatNumber(lastPoint.totalPlayers)}명</strong>
          <small>${escapeHtml(lastPoint.date || "-")} 기준</small>
        </div>
      </div>
      <div class="growth-chart-body">
        <div class="chart-y-axis" aria-hidden="true">
          <span>${formatNumber(maxTotal)}명</span>
          <span>${formatNumber(midTotal)}명</span>
          <span>${formatNumber(minTotal)}명</span>
        </div>
        <div class="growth-plot">
          <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
            <line class="chart-grid" x1="0" y1="0" x2="${width}" y2="0"></line>
            <line class="chart-grid" x1="0" y1="${height / 2}" x2="${width}" y2="${height / 2}"></line>
            <line class="chart-grid" x1="0" y1="${height}" x2="${width}" y2="${height}"></line>
            <polygon class="growth-area" points="${areaPoints}"></polygon>
            <polyline class="growth-line" points="${linePoints}"></polyline>
            <circle class="growth-dot" cx="${lastX}" cy="${lastY}" r="5"></circle>
          </svg>
          ${renderChartHoverLayer(points, (point) => [
            chartTooltipRow("누적 유저", `${formatNumber(point.totalPlayers)}명`, "growth-total"),
            chartTooltipRow("신규 유저", `+${formatNumber(point.newPlayers)}명`, "new"),
            chartTooltipRow("앱 진입", `${formatNumber(point.appEnteredUsers)}명`, "entered"),
            chartTooltipRow("충성 활성", `${formatNumber(point.activeUsers)}명`, "active"),
          ])}
        </div>
      </div>
      <div class="growth-x-labels">${labels}</div>
    </div>
  `;
}

function chartTooltipRow(label, value, className = "") {
  return { label, value, className };
}

function renderChartHoverLayer(points, rowsForPoint) {
  if (!points.length) {
    return "";
  }
  const lastIndex = points.length - 1;
  const position = (index) => points.length === 1 ? 50 : index * 100 / lastIndex;
  return `
    <div class="chart-hover-layer" aria-hidden="true">
      ${points.map((point, index) => {
        const current = position(index);
        const previous = index === 0 ? 0 : position(index - 1);
        const next = index === lastIndex ? 100 : position(index + 1);
        const left = index === 0 ? 0 : (previous + current) / 2;
        const right = index === lastIndex ? 100 : (current + next) / 2;
        const width = Math.max(1, right - left);
        const pointOffset = Math.min(100, Math.max(0, (current - left) / width * 100));
        const tooltip = encodeURIComponent(renderChartTooltip(point.date || "-", rowsForPoint(point, index)));
        return `
          <span
            class="chart-hover-zone"
            style="left: ${left.toFixed(3)}%; width: ${width.toFixed(3)}%; --point-offset: ${pointOffset.toFixed(3)}%;"
            data-chart-tooltip="${tooltip}"
          ></span>
        `;
      }).join("")}
    </div>
  `;
}

function renderChartTooltip(date, rows) {
  return `
    <div class="chart-tooltip-date">${escapeHtml(date)}</div>
    <div class="chart-tooltip-rows">
      ${(rows || []).filter(Boolean).map((row) => `
        <div class="chart-tooltip-row">
          <span>
            <i class="chart-tooltip-swatch ${escapeHtml(row.className || "")}" aria-hidden="true"></i>
            ${escapeHtml(row.label)}
          </span>
          <strong>${escapeHtml(row.value)}</strong>
        </div>
      `).join("")}
    </div>
  `;
}

function ensureChartTooltipElement() {
  let tooltip = document.querySelector(".chart-hover-tooltip");
  if (!tooltip) {
    tooltip = document.createElement("div");
    tooltip.className = "chart-hover-tooltip";
    tooltip.hidden = true;
    document.body.appendChild(tooltip);
  }
  return tooltip;
}

function showChartTooltip(event) {
  const zone = event.target.closest?.("[data-chart-tooltip]");
  if (!zone) {
    return;
  }
  const tooltip = ensureChartTooltipElement();
  try {
    tooltip.innerHTML = decodeURIComponent(zone.dataset.chartTooltip || "");
  } catch (error) {
    tooltip.innerHTML = "";
  }
  tooltip.hidden = false;
  tooltip.classList.add("visible");
  positionChartTooltip(event, zone, tooltip);
}

function moveChartTooltip(event) {
  const zone = event.target.closest?.("[data-chart-tooltip]");
  if (!zone) {
    return;
  }
  const tooltip = document.querySelector(".chart-hover-tooltip");
  if (tooltip && !tooltip.hidden) {
    positionChartTooltip(event, zone, tooltip);
  }
}

function hideChartTooltip(event) {
  const zone = event.target.closest?.("[data-chart-tooltip]");
  if (!zone) {
    return;
  }
  if (event.relatedTarget?.closest?.("[data-chart-tooltip]") === zone) {
    return;
  }
  const tooltip = document.querySelector(".chart-hover-tooltip");
  if (tooltip) {
    tooltip.classList.remove("visible");
    tooltip.hidden = true;
  }
}

function positionChartTooltip(event, zone, tooltip) {
  const zoneRect = zone.getBoundingClientRect();
  const pointerX = Number.isFinite(event.clientX) ? event.clientX : zoneRect.left + zoneRect.width / 2;
  const pointerY = Number.isFinite(event.clientY) ? event.clientY : zoneRect.top + zoneRect.height / 2;
  const gap = 14;
  let left = pointerX + gap;
  let top = pointerY + gap;
  const width = tooltip.offsetWidth || 240;
  const height = tooltip.offsetHeight || 140;
  if (left + width + 10 > window.innerWidth) {
    left = pointerX - width - gap;
  }
  if (top + height + 10 > window.innerHeight) {
    top = pointerY - height - gap;
  }
  tooltip.style.left = `${Math.max(10, left)}px`;
  tooltip.style.top = `${Math.max(10, top)}px`;
}

function renderActiveUserChart(points) {
  if (!points.length) {
    return `<p class="empty">충성 활성 사용자 추이 데이터가 아직 없어요.</p>`;
  }
  const series = [
    { key: "appEnteredUsers", label: "앱 진입", className: "entered" },
    { key: "onboardedEnteredUsers", label: "게임 시작 후 진입", className: "onboarded" },
    { key: "activeUsers", label: "충성 활성", className: "active" },
    { key: "visitorOnlyUsers", label: "방문/저관여", className: "visitor-only" },
  ];
  const width = 640;
  const height = 190;
  const maxValue = Math.max(1, ...points.flatMap((point) => series.map((item) => Number(point[item.key] || 0))));
  const paddedMax = Math.max(1, Math.ceil(maxValue * 1.15));
  const midValue = Math.round(paddedMax / 2);
  const x = (index) => points.length === 1 ? width / 2 : width * index / (points.length - 1);
  const y = (value) => height - height * Number(value || 0) / paddedMax;
  const labelStep = Math.max(1, Math.ceil(points.length / 5));
  const labels = points
    .map((point, index) => ({ point, index }))
    .filter(({ index }) => index === 0 || index === points.length - 1 || index % labelStep === 0)
    .map(({ point, index }) => {
      const edgeClass = index === 0 ? " first" : index === points.length - 1 ? " last" : "";
      return `
        <span class="growth-x-label${edgeClass}">
          ${escapeHtml(String(point.date || "").slice(5))}
          <small>${formatNumber(point.activeUsers || 0)}명</small>
        </span>
      `;
    }).join("");
  const latest = points[points.length - 1] || {};
  return `
    <div class="active-user-section">
      <div class="active-user-head">
        <div>
          <h4>충성 활성 사용자 추이</h4>
          <p>충성 활성은 기준일을 제외한 직전 3일 매일 접속, 레벨 3+, 토스포인트 보상 이력, 리워드 광고 10회+, 펀치킹과 던전 탐험 이력을 모두 만족한 유저예요.</p>
        </div>
        <div class="active-user-legend">
          ${series.map((item) => `
            <span class="${escapeHtml(item.className)}">${escapeHtml(item.label)}</span>
          `).join("")}
        </div>
      </div>
      <div class="active-user-latest">
        ${economyItem("최신 앱 진입", latest.appEnteredUsers, "명")}
        ${economyItem("최신 충성 활성", latest.activeUsers, "명")}
        ${economyItem("최신 방문/저관여", latest.visitorOnlyUsers, "명")}
      </div>
      <div class="growth-chart active-user-chart" role="img" aria-label="충성 활성 사용자 추이 그래프">
        <div class="growth-chart-body">
          <div class="chart-y-axis" aria-hidden="true">
            <span>${formatNumber(paddedMax)}명</span>
            <span>${formatNumber(midValue)}명</span>
            <span>0명</span>
          </div>
          <div class="growth-plot">
            <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
              <line class="chart-grid" x1="0" y1="0" x2="${width}" y2="0"></line>
              <line class="chart-grid" x1="0" y1="${height / 2}" x2="${width}" y2="${height / 2}"></line>
              <line class="chart-grid" x1="0" y1="${height}" x2="${width}" y2="${height}"></line>
              ${series.map((item) => `
                <polyline
                  class="active-user-line ${escapeHtml(item.className)}"
                  points="${points.map((point, index) => `${x(index).toFixed(1)},${y(point[item.key]).toFixed(1)}`).join(" ")}"
                ></polyline>
              `).join("")}
            </svg>
            ${renderChartHoverLayer(points, (point) => series.map((item) => (
              chartTooltipRow(item.label, `${formatNumber(point[item.key])}명`, item.className)
            )))}
          </div>
        </div>
        <div class="growth-x-labels">${labels}</div>
      </div>
    </div>
  `;
}

async function loadAnomalies() {
  const data = await request("/api/admin/anomalies");
  $("anomalyUpdatedAt").textContent = new Date(data.generatedAt).toLocaleString("ko-KR");
  renderAnomalySummary(data);
  renderAnomalyRules(data.rules || []);
  renderAnomalyList(data.anomalies || []);
}

function renderAnomalySummary(data) {
  const summary = [
    ["CRITICAL", data.criticalCount, "즉시 확인"],
    ["WARNING", data.warningCount, "주의"],
    ["INFO", data.infoCount, "관찰"],
  ];
  $("anomalySummary").innerHTML = summary.map(([severity, count, label]) => `
    <article class="anomaly-count ${severity.toLowerCase()}">
      <span>${escapeHtml(label)}</span>
      <strong>${formatNumber(count)}</strong>
    </article>
  `).join("");
}

function renderAnomalyList(anomalies) {
  $("anomalyList").innerHTML = anomalies.map((anomaly) => `
    <article
      class="anomaly-row ${escapeHtml(anomaly.severity.toLowerCase())}"
      data-anomaly-row
      data-anomaly-key="${escapeHtml(anomaly.anomalyKey)}"
      data-category="${escapeHtml(anomaly.category)}"
      data-user-key="${escapeHtml(anomaly.userKey)}"
    >
      <div>
        <div class="anomaly-title">
          <span class="severity-chip">${escapeHtml(severityLabel(anomaly.severity))}</span>
          <strong>${escapeHtml(anomaly.title)}</strong>
          <span class="anomaly-status ${escapeHtml(String(anomaly.status || "OPEN").toLowerCase())}">
            ${escapeHtml(anomalyStatusLabel(anomaly.status))}
          </span>
        </div>
        <p>${escapeHtml(anomaly.detail)}</p>
        <small>${escapeHtml(anomaly.category)} · 기준 ${formatNumber(anomaly.thresholdValue)} / 감지 ${formatNumber(anomaly.observedValue)}</small>
        ${renderAnomalyActions(anomaly.actions || [])}
      </div>
      <div class="anomaly-meta" data-anomaly-control>
        <button class="ghost" type="button" data-open-user>유저 보기</button>
        <strong>${escapeHtml(anomaly.userKey)}</strong>
        <span>${formatDate(anomaly.detectedAt)}</span>
        <select data-anomaly-status aria-label="이상징후 처리 상태">
          ${["OPEN", "IN_PROGRESS", "RESOLVED"].map((status) => `
            <option value="${status}" ${status === (anomaly.status || "OPEN") ? "selected" : ""}>
              ${escapeHtml(anomalyStatusLabel(status))}
            </option>
          `).join("")}
        </select>
        <textarea data-anomaly-note maxlength="500" placeholder="조치 내역 입력">${escapeHtml(anomaly.note || "")}</textarea>
        <button class="secondary" type="button" data-anomaly-save>조치 저장</button>
      </div>
    </article>
  `).join("") || `<p class="empty">현재 감지된 이상징후가 없어요.</p>`;

  document.querySelectorAll("[data-anomaly-row]").forEach((row) => {
    row.addEventListener("click", (event) => {
      if (event.target.closest("[data-anomaly-control]")) {
        return;
      }
      openPlayerFromAnomaly(row.dataset.userKey);
    });
  });
  document.querySelectorAll("[data-open-user]").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      openPlayerFromAnomaly(button.closest("[data-anomaly-row]").dataset.userKey);
    });
  });
  document.querySelectorAll("[data-anomaly-save]").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      saveAnomalyAction(button.closest("[data-anomaly-row]"), button);
    });
  });
}

function renderAnomalyRules(rules) {
  $("anomalyRules").innerHTML = rules.map((rule) => {
    const displayValue = policyDisplayValue(rule.policyKey, rule.thresholdValue);
    const displayMin = policyDisplayValue(rule.policyKey, rule.min);
    const displayMax = policyDisplayValue(rule.policyKey, rule.max);
    return `
      <article class="anomaly-rule ${escapeHtml(rule.severity.toLowerCase())}">
        <div>
          <div class="anomaly-rule-title">
            <span class="severity-chip">${escapeHtml(severityLabel(rule.severity))}</span>
            <strong>${escapeHtml(rule.title)}</strong>
          </div>
          <p>${escapeHtml(rule.description)}</p>
          <small>${escapeHtml(rule.policyKey)} · ${formatNumber(displayMin)}~${formatNumber(displayMax)} ${escapeHtml(policyDisplayUnit(rule.policyKey, rule.unit))}</small>
        </div>
        ${rule.editable ? `
          <div class="anomaly-rule-control" data-anomaly-rule data-key="${escapeHtml(rule.policyKey)}">
            <input
              type="number"
              min="${escapeHtml(displayMin)}"
              max="${escapeHtml(displayMax)}"
              step="${escapeHtml(policyInputStep(rule.policyKey))}"
              value="${escapeHtml(displayValue)}"
              data-anomaly-rule-value
              aria-label="${escapeHtml(rule.title)} 기준값"
            />
            <button class="secondary" type="button" data-anomaly-rule-save>기준 저장</button>
          </div>
        ` : ""}
      </article>
    `;
  }).join("");

  document.querySelectorAll("[data-anomaly-rule-save]").forEach((button) => {
    button.addEventListener("click", () => saveAnomalyRule(button.closest("[data-anomaly-rule]"), button));
  });
}

function renderAnomalyActions(actions) {
  if (!actions.length) {
    return "";
  }
  return `
    <div class="anomaly-history">
      ${actions.slice(0, 3).map((action) => `
        <small>
          ${escapeHtml(anomalyStatusLabel(action.status))} · ${escapeHtml(action.note || "메모 없음")} · ${formatDate(action.createdAt)}
        </small>
      `).join("")}
    </div>
  `;
}

async function saveAnomalyRule(row, button) {
  const key = row.dataset.key;
  const rawValue = Number(row.querySelector("[data-anomaly-rule-value]").value);
  const value = policyRequestValue(key, rawValue);
  $("anomalyResult").textContent = "";
  $("anomalyResult").classList.remove("error-text");
  setButtonBusy(button, true);
  try {
    await request("/api/admin/policies", {
      method: "PATCH",
      body: { key, value, resetToDefault: false, reason: "" },
    });
    $("anomalyResult").textContent = "이상징후 기준을 저장했어요.";
    showToast("이상징후 기준을 저장했어요.");
    await loadAnomalies();
    if (state.view === "policyView") {
      await loadPolicies();
    }
  } catch (error) {
    $("anomalyResult").textContent = error.message;
    $("anomalyResult").classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function saveAnomalyAction(row, button) {
  const body = {
    anomalyKey: row.dataset.anomalyKey,
    category: row.dataset.category,
    userKey: row.dataset.userKey,
    status: row.querySelector("[data-anomaly-status]").value,
    note: row.querySelector("[data-anomaly-note]").value.trim(),
  };
  $("anomalyResult").textContent = "";
  $("anomalyResult").classList.remove("error-text");
  setButtonBusy(button, true);
  try {
    await request("/api/admin/anomalies/actions", { method: "POST", body });
    $("anomalyResult").textContent = `${body.userKey} 이상징후 조치를 저장했어요.`;
    showToast("이상징후 조치를 저장했어요.");
    await loadAnomalies();
  } catch (error) {
    $("anomalyResult").textContent = error.message;
    $("anomalyResult").classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function openPlayerFromAnomaly(userKey) {
  if (!userKey) {
    return;
  }
  activateView("playerView");
  $("playerSearchInput").value = userKey;
  $("playerFilterStatus").value = "ALL";
  $("playerFilterProgress").value = "ALL";
  $("playerFavoriteMode").value = "ALL";
  $("playerHiddenSkinsOnly").checked = false;
  $("playerActiveAutoHuntOnly").checked = false;
  state.playerPage = 0;
  state.selectedUserKey = userKey;
  await loadPlayers();
  showToast(`${userKey} 유저 정보를 열었어요.`);
}

function reloadPlayersFromFirstPage() {
  state.playerPage = 0;
  state.selectedUserKey = "";
  loadPlayers();
}

async function loadPlayers() {
  const query = $("playerSearchInput").value.trim();
  const hiddenSkinsOnly = $("playerHiddenSkinsOnly").checked;
  const activeAutoHuntOnly = $("playerActiveAutoHuntOnly").checked;
  state.playerSize = Number($("playerPageSize").value || 30);
  const params = new URLSearchParams({
    query,
    page: String(state.playerPage),
    size: String(state.playerSize),
    favoriteMode: $("playerFavoriteMode").value,
    status: $("playerFilterStatus").value,
    progress: $("playerFilterProgress").value,
    hiddenSkinsOnly: String(hiddenSkinsOnly),
    activeAutoHuntOnly: String(activeAutoHuntOnly),
    sort: $("playerSortPreset").value,
  });
  const data = await request(`/api/admin/players?${params}`);
  const players = Array.isArray(data) ? data : data.content || [];
  state.players = players;
  state.playerPage = Number(data.page ?? state.playerPage);
  state.playerSize = Number(data.size ?? state.playerSize);
  state.playerTotalElements = Number(data.totalElements ?? players.length);
  state.playerTotalPages = Math.max(1, Number(data.totalPages ?? 1));
  if (state.playerPage >= state.playerTotalPages && state.playerPage > 0) {
    state.playerPage = state.playerTotalPages - 1;
    return loadPlayers();
  }
  if (!players.some((player) => player.userKey === state.selectedUserKey)) {
    state.selectedUserKey = players[0]?.userKey || "";
    state.playerDetailExpanded = false;
  }
  renderPlayers();
}

function clearPlayerFilters() {
  $("playerSearchInput").value = "";
  $("playerFilterStatus").value = "ALL";
  $("playerFilterProgress").value = "ALL";
  $("playerSortPreset").value = "lastAccessedAt:desc";
  $("playerFavoriteMode").value = "ALL";
  $("playerPageSize").value = "30";
  $("playerHiddenSkinsOnly").checked = false;
  $("playerActiveAutoHuntOnly").checked = false;
  state.playerPage = 0;
  state.selectedUserKey = "";
  loadPlayers();
}

function renderPlayers() {
  const players = state.players;
  $("playerCount").textContent = `${formatNumber(state.playerTotalElements)}명`;
  $("playerPageInfo").textContent = `${state.playerPage + 1} / ${state.playerTotalPages}`;
  $("prevPlayerButton").disabled = state.playerPage <= 0;
  $("nextPlayerButton").disabled = state.playerPage + 1 >= state.playerTotalPages;
  $("playerList").innerHTML = players.map((player) => playerRow(player)).join("")
    || `<p class="empty">검색 결과가 없어요.</p>`;
  if (!players.some((player) => player.userKey === state.selectedUserKey)) {
    state.selectedUserKey = players[0]?.userKey || "";
    state.playerDetailExpanded = false;
  }
  renderSelectedPlayerDetail();
  document.querySelectorAll("[data-player-select]").forEach((row) => {
    row.addEventListener("click", () => {
      if (state.selectedUserKey !== row.dataset.userKey) {
        state.playerDetailExpanded = false;
      }
      state.selectedUserKey = row.dataset.userKey;
      renderPlayers();
    });
  });
  document.querySelectorAll("[data-player-action]").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      runPlayerAction(button);
    });
  });
  document.querySelectorAll("[data-player-favorite]").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      togglePlayerFavorite(button);
    });
  });
}

function playerRow(player) {
  const status = player.suspended ? "정지" : "정상";
  const statusClass = player.suspended ? "suspended" : "active";
  const job = player.job || "미선택";
  const autoHunt = player.autoHuntEndsAt ? new Date(player.autoHuntEndsAt).toLocaleString("ko-KR") : "-";
  const lastAccessed = formatDate(player.lastAccessedAt);
  const selected = player.userKey === state.selectedUserKey ? "selected" : "";
  const name = playerDisplayName(player);
  return `
    <article class="player-row ${statusClass} ${selected}" data-player-select data-user-key="${escapeHtml(player.userKey)}">
      <div class="player-main">
        <div class="player-title">
          <button
            class="favorite-toggle ${player.adminFavorite ? "active" : ""}"
            type="button"
            title="즐겨찾기"
            data-player-favorite
            data-user-key="${escapeHtml(player.userKey)}"
            data-favorite="${player.adminFavorite ? "false" : "true"}"
          >${player.adminFavorite ? "★" : "☆"}</button>
          <strong>${escapeHtml(name)}</strong>
          <span class="player-status ${statusClass}">${status}</span>
        </div>
        <p>${escapeHtml(jobLabel(job))} · Lv.${formatNumber(player.level)} · 전투력 ${formatCombatPower(player.combatPower)} · 보유 ${formatNumber(player.gold)}G · SP ${formatNumber(player.skillPoints)}</p>
        <small>최근 접속 ${escapeHtml(lastAccessed)} · 자동사냥 ${escapeHtml(autoHunt)}</small>
        ${player.suspended ? `<small>정지 사유: ${escapeHtml(player.suspensionReason || "-")}</small>` : ""}
      </div>
      <div class="player-actions">
        ${player.suspended
          ? `<button class="secondary" data-player-action="resume" data-user-key="${escapeHtml(player.userKey)}">정지 해제</button>`
          : `<button class="danger" data-player-action="suspend" data-user-key="${escapeHtml(player.userKey)}">정지</button>`}
        <button class="ghost danger-ghost" data-player-action="reset" data-user-key="${escapeHtml(player.userKey)}">완전 초기화</button>
      </div>
    </article>
  `;
}

function renderSelectedPlayerDetail() {
  const player = state.players.find((item) => item.userKey === state.selectedUserKey);
  if (!player) {
    $("playerDetail").innerHTML = `
      <div class="empty-detail">
        <strong>유저를 선택하세요</strong>
        <p>검색 결과에서 유저를 선택하면 상세 정보와 운영 액션이 표시돼요.</p>
      </div>
    `;
    return;
  }
  const status = player.suspended ? "정지" : "정상";
  const statusClass = player.suspended ? "suspended" : "active";
  const progress = player.onboardingRequired ? "직업 미선택" : "게임 시작";
  const displayName = playerDisplayName(player);
  const benefitTabStatus = player.benefitTabNewUserEnteredAt
    ? player.benefitTabNewUserPromotionGrantedAt
      ? "지급 완료"
      : player.benefitTabNewUserPromotionRequested
        ? "지급 요청/확인 중"
        : player.benefitTabNewUserPromotionEligible
          ? "수령 조건 충족"
          : "진입 기록 있음"
    : "해당 없음";
  const basicDetails = [
    ["유저 식별 ID", player.userKey],
    ["관리자 별명", player.adminNickname || "미설정"],
    ["가입", formatDate(player.createdAt)],
    ["마지막 접속", formatDate(player.lastAccessedAt)],
    ["혜택 탭 진입", player.benefitTabNewUserEnteredAt ? "예" : "아니오"],
    ["혜택 탭 프로모션", benefitTabStatus],
    ["전투력", formatCombatPower(player.combatPower)],
    ["자동사냥 종료", formatDate(player.autoHuntEndsAt)],
    ["최근 정산 시간", formatDate(player.lastSettledAt)],
    ["총 정산 금액", `${formatNumber(player.totalSettledWon || 0)}원`],
  ];
  const extraDetails = [
    ["게임 프로필", player.gameProfileNickname || "미동기화"],
    ["히든 스킨", player.hiddenPetSkinsUnlocked ? "해금" : "미해금"],
    ["보유 골드", `${formatNumber(player.gold)}G`],
    ["누적 골드", `${formatNumber(player.cumulativeGoldEarned || player.gold)}G`],
    ["총 정산 골드", `${formatNumber(player.totalSettledGold)}G`],
    ["SP", formatNumber(player.skillPoints)],
    ["레벨", `Lv.${formatNumber(player.level)}`],
    ["경험치", `${formatNumber(player.experience)} / ${formatNumber(player.nextLevelExperience)}`],
    ["펫 슬롯", `${formatNumber(player.characterSlots)} / 3`],
    ["초대 보상", `${formatNumber(player.friendInviteRewardCount)}회`],
    ["처치 몬스터", formatNumber(player.defeatedMonsters)],
    ["현재 몬스터", `${player.currentMonsterKey || "-"} · HP ${formatNumber(player.currentMonsterHp)}`],
    ["튜토리얼 보상", player.tutorialRewardClaimed ? "완료" : "미수령"],
    ["기능 튜토리얼", player.featureTutorialCompleted ? "완료" : "미완료"],
    ["혜택 탭 진입 시간", formatDate(player.benefitTabNewUserEnteredAt)],
    ["혜택 탭 결과 확인", formatDate(player.benefitTabNewUserPromotionResultCheckedAt)],
    ["혜택 탭 지급 완료", formatDate(player.benefitTabNewUserPromotionGrantedAt)],
    ["최근 SP 광고", formatDate(player.lastSkillPointAdClaimedAt)],
    ["게임 프로필 갱신", formatDate(player.gameProfileUpdatedAt)],
    ["즐겨찾기", player.adminFavorite ? "예" : "아니오"],
    ["수정", formatDate(player.updatedAt)],
  ];
  $("playerDetail").innerHTML = `
    <div class="detail-head">
      <div>
        <span class="player-status ${statusClass}">${status}</span>
        <h3>${escapeHtml(displayName)}</h3>
        <p>${escapeHtml(jobLabel(player.job || "미선택"))} · ${escapeHtml(progress)}</p>
      </div>
      <div class="detail-actions">
        <button
          class="favorite-toggle detail-favorite ${player.adminFavorite ? "active" : ""}"
          type="button"
          data-player-favorite
          data-user-key="${escapeHtml(player.userKey)}"
          data-favorite="${player.adminFavorite ? "false" : "true"}"
        >${player.adminFavorite ? "★ 즐겨찾기" : "☆ 즐겨찾기"}</button>
        <button class="ghost" type="button" data-copy-user-key="${escapeHtml(player.userKey)}">유저 키 복사</button>
      </div>
    </div>
    <div class="detail-grid compact-detail-grid">
      ${basicDetails.map(([label, value]) => kv(label, value)).join("")}
    </div>
    <button class="ghost detail-toggle" type="button" data-detail-toggle>
      ${state.playerDetailExpanded ? "간략히 보기" : "상세보기"}
    </button>
    <div class="detail-grid extra-detail-grid ${state.playerDetailExpanded ? "" : "hidden"}">
      ${extraDetails.map(([label, value]) => kv(label, value)).join("")}
    </div>
    ${player.suspended ? `<p class="note danger-note">정지 사유: ${escapeHtml(player.suspensionReason || "-")}</p>` : ""}
    <div class="admin-nickname-panel">
      <label>
        <span>관리자 별명</span>
        <input id="adminNicknameInput" type="text" maxlength="80" value="${escapeHtml(player.adminNickname || "")}" placeholder="관리자 화면에서만 보이는 별명" />
      </label>
      <button class="secondary" type="button" data-player-admin-nickname data-user-key="${escapeHtml(player.userKey)}">별명 저장</button>
    </div>
    <div class="action-panel">
      <label>
        <span>운영 메모(선택)</span>
        <input id="playerActionReason" type="text" maxlength="500" placeholder="감사 로그에 남길 사유" />
      </label>
      <div class="action-row">
        ${player.suspended
          ? `<button class="secondary" data-player-action="resume" data-user-key="${escapeHtml(player.userKey)}">정지 해제</button>`
          : `<button class="danger" data-player-action="suspend" data-user-key="${escapeHtml(player.userKey)}">유저 정지</button>`}
        <button class="ghost danger-ghost" data-player-action="reset" data-user-key="${escapeHtml(player.userKey)}">로그인부터 초기화</button>
      </div>
    </div>
    <div class="cs-panel">
      <div class="cs-head">
        <strong>CS 보정</strong>
        <span>문의 대응용으로 즉시 반영되고 감사 로그에 남아요.</span>
      </div>
      <div class="cs-grid">
        <label>
          <span>골드 증감수량</span>
          <input id="csGoldAmount" type="number" step="1" value="0" placeholder="예: 100 또는 -100" />
        </label>
        <button class="secondary" type="button" data-player-cs="gold" data-user-key="${escapeHtml(player.userKey)}">골드 반영</button>
        <label>
          <span>SP 증감수량</span>
          <input id="csSkillPointAmount" type="number" step="1" value="0" placeholder="예: 5 또는 -2" />
        </label>
        <button class="secondary" type="button" data-player-cs="skill-points" data-user-key="${escapeHtml(player.userKey)}">SP 반영</button>
      </div>
      <div class="pet-cs-actions">
        <button class="secondary wide-action" type="button" data-player-cs="pet-grant" data-user-key="${escapeHtml(player.userKey)}" ${player.characterSlots >= 3 ? "disabled" : ""}>동료 펫 지급</button>
        <button class="ghost danger-ghost wide-action" type="button" data-player-cs="pet-remove" data-user-key="${escapeHtml(player.userKey)}" ${player.characterSlots <= 1 ? "disabled" : ""}>동료 펫 제거</button>
      </div>
    </div>
  `;
  const copyButton = document.querySelector("[data-copy-user-key]");
  copyButton?.addEventListener("click", async () => {
    await navigator.clipboard?.writeText(player.userKey);
    showToast("유저 키를 복사했어요.");
  });
  document.querySelector("[data-detail-toggle]")?.addEventListener("click", () => {
    state.playerDetailExpanded = !state.playerDetailExpanded;
    renderSelectedPlayerDetail();
  });
  document.querySelectorAll("#playerDetail [data-player-action]").forEach((button) => {
    button.addEventListener("click", () => runPlayerAction(button));
  });
  document.querySelectorAll("#playerDetail [data-player-favorite]").forEach((button) => {
    button.addEventListener("click", () => togglePlayerFavorite(button));
  });
  document.querySelectorAll("#playerDetail [data-player-admin-nickname]").forEach((button) => {
    button.addEventListener("click", () => savePlayerAdminNickname(button));
  });
  document.querySelectorAll("#playerDetail [data-player-cs]").forEach((button) => {
    button.addEventListener("click", () => runPlayerCsAction(button));
  });
}

function kv(label, value) {
  return `
    <div class="kv">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value)}</strong>
    </div>
  `;
}

function playerDisplayName(player) {
  const adminNickname = player?.adminNickname?.trim();
  const nickname = player?.gameProfileNickname?.trim();
  const userKey = player?.userKey || "-";
  return `${adminNickname || nickname || "프로필 미설정"}(${userKey})`;
}

function updatePlayerInState(updatedPlayer) {
  state.players = state.players.map((player) => (
    player.userKey === updatedPlayer.userKey ? updatedPlayer : player
  ));
  state.selectedUserKey = updatedPlayer.userKey;
  renderPlayers();
}

async function togglePlayerFavorite(button) {
  const userKey = button.dataset.userKey;
  const favorite = button.dataset.favorite === "true";
  const reason = $("playerActionReason")?.value?.trim() || "";
  setButtonBusy(button, true);
  $("playerResult").textContent = "";
  $("playerResult").classList.remove("error-text");
  try {
    const updatedPlayer = await request(`/api/admin/players/${encodeURIComponent(userKey)}/favorite`, {
      method: "POST",
      body: { favorite, reason },
    });
    updatePlayerInState(updatedPlayer);
    const message = favorite ? "즐겨찾기에 추가했어요." : "즐겨찾기에서 해제했어요.";
    $("playerResult").textContent = message;
    showToast(message);
    if ($("playerFavoriteMode").value === "FAVORITE" && !favorite) {
      await loadPlayers();
    } else if ($("playerFavoriteMode").value === "NOT_FAVORITE" && favorite) {
      await loadPlayers();
    }
  } catch (error) {
    $("playerResult").textContent = error.message;
    $("playerResult").classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function savePlayerAdminNickname(button) {
  const userKey = button.dataset.userKey;
  const nickname = $("adminNicknameInput")?.value?.trim() || "";
  const reason = $("playerActionReason")?.value?.trim() || "";
  setButtonBusy(button, true);
  $("playerResult").textContent = "";
  $("playerResult").classList.remove("error-text");
  try {
    const updatedPlayer = await request(`/api/admin/players/${encodeURIComponent(userKey)}/admin-nickname`, {
      method: "POST",
      body: { nickname, reason },
    });
    updatePlayerInState(updatedPlayer);
    const message = nickname ? "관리자 별명을 저장했어요." : "관리자 별명을 비웠어요.";
    $("playerResult").textContent = message;
    showToast(message);
  } catch (error) {
    $("playerResult").textContent = error.message;
    $("playerResult").classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function runPlayerCsAction(button) {
  const action = button.dataset.playerCs;
  const userKey = button.dataset.userKey;
  const reason = $("playerActionReason")?.value?.trim() || "";
  const payload = { reason };
  let endpoint = "";
  let label = "";

  if (action === "gold") {
    endpoint = "cs/gold";
    label = "골드";
    payload.mode = "ADD";
    payload.amount = Number($("csGoldAmount").value);
  } else if (action === "skill-points") {
    endpoint = "cs/skill-points";
    label = "SP";
    payload.mode = "ADD";
    payload.amount = Number($("csSkillPointAmount").value);
  } else if (action === "pet-grant" || action === "pet-remove") {
    endpoint = "cs/pet";
    payload.action = action === "pet-remove" ? "REMOVE" : "GRANT";
    label = action === "pet-remove" ? "동료 펫 제거" : "동료 펫 지급";
  }

  if (!endpoint) {
    return;
  }
  if (payload.amount !== undefined && !Number.isFinite(payload.amount)) {
    showToast("조정 수량을 숫자로 입력해 주세요.", "error");
    return;
  }

  $("playerResult").textContent = "";
  $("playerResult").classList.remove("error-text");
  setButtonBusy(button, true);
  try {
    const updatedPlayer = await request(`/api/admin/players/${encodeURIComponent(userKey)}/${endpoint}`, {
      method: "POST",
      body: payload,
    });
    updatePlayerInState(updatedPlayer);
    const message = `${label} CS 처리를 완료했어요.`;
    $("playerResult").textContent = message;
    showToast(message);
  } catch (error) {
    $("playerResult").textContent = error.message;
    $("playerResult").classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function runPlayerAction(button) {
  const action = button.dataset.playerAction;
  const userKey = button.dataset.userKey;
  const labels = {
    suspend: "정지",
    resume: "정지 해제",
    reset: "완전 초기화",
  };
  if (action === "reset" && !confirm(`${userKey} 유저의 세션과 게임 데이터를 모두 삭제할까요? 이 작업은 되돌릴 수 없어요.`)) {
    return;
  }
  const reason = $("playerActionReason")?.value?.trim() || "";
  $("playerResult").textContent = "";
  $("playerResult").classList.remove("error-text");
  setButtonBusy(button, true);
  try {
    await request(`/api/admin/players/${encodeURIComponent(userKey)}/${action}`, {
      method: "POST",
      body: { reason: reason.trim() },
    });
    const successMessage = `${userKey} 유저 ${labels[action]} 처리를 완료했어요.`;
    $("playerResult").textContent = successMessage;
    showToast(successMessage);
    await loadPlayers();
  } catch (error) {
    $("playerResult").textContent = error.message;
    $("playerResult").classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setButtonBusy(button, false);
  }
}

async function loadPolicies() {
  const policies = await request("/api/admin/policies");
  state.policies = policies;
  $("policyCount").textContent = `${policies.length}개`;
  $("policyList").innerHTML = policyGroups().map((group) => {
    const rows = policies.filter((policy) => group.keys.includes(policy.key));
    if (!rows.length) {
      return "";
    }
    return `
      <section class="policy-group">
        <div class="policy-group-head">
          <strong>${escapeHtml(group.title)}</strong>
          <span>${escapeHtml(group.description)}</span>
        </div>
        ${rows.map(policyRow).join("")}
      </section>
    `;
  }).join("");
  document.querySelectorAll("[data-save]").forEach((button) => {
    button.addEventListener("click", () => savePolicy(button.closest(".policy-row"), false));
  });
  document.querySelectorAll("[data-reset]").forEach((button) => {
    button.addEventListener("click", () => savePolicy(button.closest(".policy-row"), true));
  });
}

function policyRow(policy) {
  const value = policyDisplayValue(policy.key, policy.value);
  const min = policyDisplayValue(policy.key, policy.min);
  const max = policyDisplayValue(policy.key, policy.max);
  const unit = policyDisplayUnit(policy.key, policy.unit);
  return `
    <article class="policy-row" data-key="${escapeHtml(policy.key)}">
      <div class="policy-copy">
        <strong>${escapeHtml(policy.label)}</strong>
        <small>${escapeHtml(policy.key)} · ${formatNumber(min)}~${formatNumber(max)} ${escapeHtml(unit)}</small>
      </div>
      <label>
        <span>값</span>
        <input
          type="number"
          min="${escapeHtml(min)}"
          max="${escapeHtml(max)}"
          step="${escapeHtml(policyInputStep(policy.key))}"
          value="${escapeHtml(value)}"
          data-value
        />
      </label>
      <label>
        <span>변경 사유</span>
        <input type="text" placeholder="선택 입력" data-reason />
      </label>
      <div class="actions">
        <button class="secondary" data-save>저장</button>
        <button class="ghost" data-reset>기본값</button>
      </div>
    </article>
  `;
}

function policyGroups() {
  return [
    {
      title: "보상 기준",
      description: "토스포인트 수령 가능 기준",
      keys: ["goldPerTossPoint", "rewardPointAmount"],
    },
    {
      title: "광고 보상",
      description: "광고 1회 보상 시간과 누적 상한",
      keys: [
        "autoHuntAdSeconds",
        "autoHuntAdCooldownSeconds",
        "maxAdSeconds",
        "skillPointAdCooldownSeconds",
      ],
    },
    {
      title: "모험",
      description: "던전 입장 횟수와 재입장 대기 시간",
      keys: ["dungeonFreeDailyLimit", "dungeonAdditionalDailyLimit", "dungeonReentryCooldownSeconds"],
    },
    {
      title: "상점/초대",
      description: "펫, SP 패키지, 친구 초대 보상",
      keys: ["companionPriceWon", "skillPointPackPriceWon", "skillPointPackAmount", "friendInviteRewardSkillPoints", "friendInviteLimit", "maxCharacterSlots"],
    },
    {
      title: "이상징후 기준",
      description: "감지 기준, 유예 시간, 룰별 표시 수",
      keys: [
        "anomalyLimitPerRule",
        "anomalyAdEventsPerHourWarning",
        "anomalyRewardClaimsPerDayWarning",
        "anomalyGoldThresholdMultiplier",
        "anomalySkillPointsWarning",
        "anomalyTimerGraceSeconds",
      ],
    },
  ];
}

async function savePolicy(row, resetToDefault) {
  const key = row.dataset.key;
  const displayValue = Number(row.querySelector("[data-value]").value);
  const body = {
    key,
    value: policyRequestValue(key, displayValue),
    resetToDefault,
    reason: row.querySelector("[data-reason]").value.trim(),
  };
  $("policyResult").textContent = "";
  $("policyResult").classList.remove("error-text");
  setPolicyRowBusy(row, true);
  try {
    await request("/api/admin/policies", { method: "PATCH", body });
    const successMessage = resetToDefault
      ? `${body.key} 정책을 기본값으로 복원했어요.`
      : `${body.key} 정책을 저장했어요.`;
    $("policyResult").textContent = successMessage;
    showToast(successMessage);
    await loadPolicies();
  } catch (error) {
    $("policyResult").textContent = error.message;
    $("policyResult").classList.add("error-text");
    showToast(error.message, "error");
  } finally {
    setPolicyRowBusy(row, false);
  }
}

async function loadServerMetrics() {
  const [overview, metrics] = await Promise.all([
    request("/api/admin/overview"),
    request("/api/admin/server-metrics"),
  ]);
  state.overview = overview;
  $("serverMetricsUpdatedAt").textContent = formatDate(metrics.generatedAt);
  renderServerMetrics(metrics, overview);
}

function renderServerMetrics(metrics, overview) {
  const heapRatio = metrics.heapMaxBytes > 0 ? metrics.heapUsedBytes / metrics.heapMaxBytes : 0;
  const runtimeRatio = metrics.maxMemoryBytes > 0 ? (metrics.totalMemoryBytes - metrics.freeMemoryBytes) / metrics.maxMemoryBytes : 0;
  const cards = [
    ["업타임", formatDuration(metrics.uptimeMillis), "서버 프로세스 실행 시간"],
    ["Heap 사용", `${formatBytes(metrics.heapUsedBytes)} / ${formatBytes(metrics.heapMaxBytes)}`, formatPercent(metrics.heapUsedBytes, metrics.heapMaxBytes)],
    ["Runtime 메모리", `${formatBytes(metrics.totalMemoryBytes - metrics.freeMemoryBytes)} / ${formatBytes(metrics.maxMemoryBytes)}`, formatPercent(metrics.totalMemoryBytes - metrics.freeMemoryBytes, metrics.maxMemoryBytes)],
    ["Non-Heap", formatBytes(metrics.nonHeapUsedBytes), "클래스/메타 영역"],
    ["스레드", `${formatNumber(metrics.threadCount)}개`, `daemon ${formatNumber(metrics.daemonThreadCount)}개`],
    ["CPU", `${formatNumber(metrics.availableProcessors)} core`, metrics.systemLoadAverage >= 0 ? `load ${metrics.systemLoadAverage.toFixed(2)}` : "load 미지원"],
    ["Heap 상태", heapRatio >= 0.85 ? "주의" : "정상", `${Math.round(heapRatio * 100)}% 사용`],
    ["Runtime 상태", runtimeRatio >= 0.85 ? "주의" : "정상", `${Math.round(runtimeRatio * 100)}% 사용`],
  ];
  $("serverMetricsGrid").innerHTML = cards.map(([label, value, detail]) => `
    <article class="metric-card">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value)}</strong>
      <small>${escapeHtml(detail)}</small>
    </article>
  `).join("");
  const statusItems = overview.runtimeStatusItems || [];
  $("serverRuntimePanel").innerHTML = `
    <div class="panel-head compact-head">
      <div>
        <h3>운영 기능 상태</h3>
        <p>각 기능의 운영, 테스트, 꺼짐 상태를 배지로 표시해요.</p>
      </div>
    </div>
    <div class="runtime-status-grid">
      ${renderRuntimeStatusItems(statusItems)}
    </div>
  `;
}

async function loadRevenue() {
  const select = $("revenuePeriodSelect");
  if (select) {
    select.value = String(state.revenueDays);
  }
  const data = await request(`/api/admin/revenue?days=${encodeURIComponent(state.revenueDays)}`);
  state.revenueDays = Number(data.days || state.revenueDays);
  if (select) {
    select.value = String(state.revenueDays);
    select.onchange = (event) => {
      state.revenueDays = Number(event.target.value || 30);
      loadRevenue().catch((error) => showToast(error.message, "error"));
    };
  }
  renderRevenue(data);
}

function renderRevenue(data) {
  const today = data.today || {};
  const period = data.period || {};
  const points = data.points || [];
  $("revenueSummaryGrid").innerHTML = [
    ["오늘 추정 총매출", today.estimatedGrossRevenueWon, "원", "광고 추정 + IAP 추정"],
    ["오늘 광고 추정", today.estimatedAdRevenueWon, "원", "서버 광고/보상 이벤트 기준"],
    ["오늘 IAP 추정", today.estimatedIapRevenueWon, "원", "주문 상품가 매핑 기준"],
    ["오늘 보상 비용", today.rewardCostWon, "원", "토스포인트 지급/대기 기준"],
    ["오늘 추정 순수익", today.estimatedNetRevenueWon, "원", "추정 총매출 - 보상 비용"],
    ["3일 충성 활성", today.activeUsers, "명", "오늘 제외 직전 3일 충성 조건"],
    [`${formatNumber(data.days || state.revenueDays)}일 총매출`, period.estimatedGrossRevenueWon, "원", "기간 합산 추정"],
    [`${formatNumber(data.days || state.revenueDays)}일 순수익`, period.estimatedNetRevenueWon, "원", "기간 합산 추정"],
  ].map(([label, value, unit, detail]) => `
    <article class="metric-card">
      <span>${escapeHtml(label)}</span>
      <strong>${formatNumber(value)}${escapeHtml(unit)}</strong>
      <small>${escapeHtml(detail)}</small>
    </article>
  `).join("");

  renderRevenuePanelSafely("revenueDiagnosisPanel", () => renderRevenueDiagnosis(points), "eCPM 원인 분석을 표시하지 못했어요.");
  renderRevenuePanelSafely("appInTossMetricPanel", () => renderAppInTossMetricPanel(points), "앱인토스 콘솔 핵심값 입력 폼을 표시하지 못했어요.");
  $("appInTossMetricForm")?.addEventListener("submit", saveAppInTossMetric);
  $("revenueReferencePanel").innerHTML = `
    ${(data.referenceMetrics || []).map((item) => `
      <article class="revenue-reference-card">
        <span>${escapeHtml(item.label || "-")}</span>
        <p>${escapeHtml(item.description || "-")}</p>
      </article>
    `).join("")}
  `;
  renderRevenuePanelSafely("revenueTrendPanel", () => renderRevenueTrendChart(points), "수익 추이 그래프를 표시하지 못했어요.");
  renderRevenuePanelSafely("revenueIapPanel", () => renderIapRevenuePanel(data.iapProducts || []), "인앱결제 매출 표를 표시하지 못했어요.");
  renderRevenuePanelSafely("revenueAdPanel", () => renderAdRevenuePanel(data.adEvents || []), "광고/보상 이벤트 표를 표시하지 못했어요.");
}

function renderRevenuePanelSafely(targetId, renderer, fallbackMessage) {
  try {
    $(targetId).innerHTML = renderer();
  } catch (error) {
    console.error("Revenue panel render failed", targetId, error);
    $(targetId).innerHTML = `<p class="empty">${escapeHtml(fallbackMessage)}</p>`;
  }
}

function renderRevenueDiagnosis(points) {
  if (!points.length) {
    return `<p class="empty">수익 원인 분석 데이터가 아직 없어요.</p>`;
  }
  const latest = points[points.length - 1] || {};
  const previous = points.length > 1 ? points[points.length - 2] : null;
  const previousConsole = [...points].slice(0, -1).reverse().find(hasAppInTossMetric) || previous;
  const consoleMetricAvailable = hasAppInTossMetric(latest);
  const internalCompletionRate = ratioNumber(latest.adSessionCompletedCount, latest.adSessionStartedCount);
  const consoleWatchRate = consoleMetricAvailable ? Number(latest.appInTossAdWatchRatePercent || 0) / 100 : null;
  const adsPerActive = ratioNumber(latest.adEventCount, latest.activeUsers);
  const visitorRatio = ratioNumber(latest.visitorOnlyUsers, latest.appEnteredUsers);
  const rewardConversion = ratioNumber(latest.rewardClaimCount, latest.adSessionCompletedCount);
  const adRevenueBase = Number(latest.appInTossEstimatedRevenueWon ?? latest.estimatedAdRevenueWon ?? 0);
  const rewardCostRatio = ratioNumber(latest.rewardCostWon, adRevenueBase);
  const cards = [
    revenueCauseCard(
      "앱인토스 eCPM",
      consoleMetricAvailable ? `${formatNumber(latest.appInTossEcpmWon)}원` : "미입력",
      consoleMetricAvailable ? trendDetail(Number(latest.appInTossEcpmWon || 0), Number(previousConsole?.appInTossEcpmWon || 0), (value) => `${formatNumber(Math.abs(value))}원`) : "콘솔 값 입력 시 실제 변동 추적",
      ecpmStatus(latest, previousConsole)
    ),
    revenueCauseCard(
      "콘솔 광고 시청률",
      consoleMetricAvailable ? formatRatio(consoleWatchRate) : "미입력",
      consoleMetricAvailable ? trendDetail(consoleWatchRate, Number(previousConsole?.appInTossAdWatchRatePercent || 0) / 100, (value) => `${(Math.abs(value) * 100).toFixed(1)}%p`) : "앱인토스 콘솔 기준",
      consoleWatchRate == null ? "neutral" : consoleWatchRate < 0.4 ? "bad" : "good"
    ),
    revenueCauseCard(
      "내부 세션 완료율",
      formatRatio(internalCompletionRate),
      `${formatNumber(latest.adSessionCompletedCount)} / ${formatNumber(latest.adSessionStartedCount)} 세션`,
      internalCompletionRate < 0.4 && Number(latest.adSessionStartedCount || 0) > 0 ? "bad" : "good"
    ),
    revenueCauseCard(
      "충성 활성당 광고",
      adsPerActive > 0 ? adsPerActive.toFixed(2) : "0.00",
      previous ? trendDetail(adsPerActive, ratioNumber(previous.adEventCount, previous.activeUsers), (value) => Math.abs(value).toFixed(2)) : "충성 유저의 광고 소비 강도",
      previous && adsPerActive < ratioNumber(previous.adEventCount, previous.activeUsers) * 0.8 ? "bad" : "neutral"
    ),
    revenueCauseCard(
      "보상 전환율",
      formatRatio(rewardConversion),
      `${formatNumber(latest.rewardClaimCount)} / ${formatNumber(latest.adSessionCompletedCount)}건`,
      rewardConversion < 0.8 && Number(latest.adSessionCompletedCount || 0) > 0 ? "bad" : "neutral"
    ),
    revenueCauseCard(
      "방문/저관여 비율",
      formatRatio(visitorRatio),
      `${formatNumber(latest.visitorOnlyUsers)} / ${formatNumber(latest.appEnteredUsers)}명`,
      visitorRatio > 0.55 ? "bad" : "neutral"
    ),
    revenueCauseCard(
      "보상 비용률",
      formatRatio(rewardCostRatio),
      "광고 수익 대비 토스포인트 비용",
      rewardCostRatio > 0.8 ? "bad" : "neutral"
    ),
  ];
  const diagnoses = revenueDiagnoses(latest, previous, previousConsole, rewardConversion);
  return `
    <section class="revenue-diagnosis">
      <div class="active-user-head">
        <div>
          <h4>eCPM 원인 분석</h4>
          <p>관리자 입력은 앱인토스 일별 노출, 시청률, eCPM만 사용하고 나머지는 서버가 자동 집계해요.</p>
        </div>
      </div>
      <div class="revenue-cause-grid">${cards.join("")}</div>
      <div class="revenue-diagnosis-list">
        ${diagnoses.map((item) => `<p class="${escapeHtml(item.level)}">${escapeHtml(item.text)}</p>`).join("")}
      </div>
    </section>
  `;
}

function renderAppInTossMetricPanel(points) {
  const latest = points[points.length - 1] || {};
  const selected = latest;
  return `
    <section class="app-in-toss-panel">
      <div class="panel-head compact-head">
        <div>
          <h3>앱인토스 콘솔 핵심값</h3>
          <p>일별 노출, 광고 시청률, eCPM만 입력하면 실제 콘솔 기준 수익과 내부 퍼널을 함께 비교해요.</p>
        </div>
      </div>
      <form id="appInTossMetricForm" class="app-in-toss-form">
        <label>
          <span>날짜</span>
          <input id="appInTossMetricDate" type="date" value="${escapeHtml(latest.date || selected.date || "")}" required />
        </label>
        <label>
          <span>광고 노출</span>
          <input id="appInTossAdImpressions" type="number" min="0" step="1" value="${escapeHtml(selected.appInTossAdImpressions ?? "")}" placeholder="예: 82" required />
        </label>
        <label>
          <span>광고 시청률(%)</span>
          <input id="appInTossAdWatchRate" type="number" min="0" max="100" step="0.01" value="${escapeHtml(selected.appInTossAdWatchRatePercent ?? "")}" placeholder="예: 15.95" required />
        </label>
        <label>
          <span>eCPM(원)</span>
          <input id="appInTossEcpmWon" type="number" min="0" step="0.01" value="${escapeHtml(selected.appInTossEcpmWon ?? "")}" placeholder="예: 14595" required />
        </label>
        <button class="secondary" type="submit">저장</button>
      </form>
    </section>
  `;
}

async function saveAppInTossMetric(event) {
  event.preventDefault();
  const body = {
    date: $("appInTossMetricDate").value,
    adImpressions: Number($("appInTossAdImpressions").value),
    adWatchRatePercent: Number($("appInTossAdWatchRate").value),
    ecpmWon: Number($("appInTossEcpmWon").value),
  };
  if (!body.date || !Number.isFinite(body.adImpressions) || !Number.isFinite(body.adWatchRatePercent) || !Number.isFinite(body.ecpmWon)) {
    showToast("앱인토스 핵심값을 확인해 주세요.", "error");
    return;
  }
  await request("/api/admin/revenue/app-in-toss-metrics", {
    method: "POST",
    body,
  });
  showToast("앱인토스 광고 지표를 저장했어요.");
  await loadRevenue();
}

function revenueCauseCard(label, value, detail, status = "neutral") {
  return `
    <article class="revenue-cause-card ${escapeHtml(status)}">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value)}</strong>
      <small>${escapeHtml(detail)}</small>
    </article>
  `;
}

function revenueDiagnoses(latest, previous, previousConsole, rewardConversion) {
  const items = [];
  const hasConsole = hasAppInTossMetric(latest);
  const ecpm = Number(latest.appInTossEcpmWon || 0);
  const previousEcpm = Number(previousConsole?.appInTossEcpmWon || 0);
  const ecpmDropped = hasConsole && previousEcpm > 0 && ecpm < previousEcpm * 0.9;
  const watchRate = hasConsole ? Number(latest.appInTossAdWatchRatePercent || 0) / 100 : null;
  const previousWatchRate = previousConsole ? Number(previousConsole.appInTossAdWatchRatePercent || 0) / 100 : null;
  const watchDropped = watchRate != null && previousWatchRate != null && watchRate < previousWatchRate - 0.08;
  const completionRate = ratioNumber(latest.adSessionCompletedCount, latest.adSessionStartedCount);
  const visitorRatio = ratioNumber(latest.visitorOnlyUsers, latest.appEnteredUsers);
  const adsPerActive = ratioNumber(latest.adEventCount, latest.activeUsers);
  const previousAdsPerActive = previous ? ratioNumber(previous.adEventCount, previous.activeUsers) : 0;

  if (!hasConsole) {
    items.push({ level: "warning", text: "앱인토스 콘솔 핵심값이 없어서 실제 eCPM 변동은 아직 확정할 수 없어요. 노출, 시청률, eCPM 3개만 입력하면 진단 정확도가 올라가요." });
  }
  if (ecpmDropped && watchDropped) {
    items.push({ level: "bad", text: "eCPM 하락과 광고 시청률 하락이 같이 발생했어요. 보상 매력, 광고 노출 위치, 선로딩 후 이탈 구간을 먼저 확인하는 게 좋아요." });
  } else if (ecpmDropped && completionRate >= 0.4 && !watchDropped) {
    items.push({ level: "warning", text: "내부 완료율은 크게 무너지지 않았는데 eCPM이 내려갔어요. 광고 수요, 지면 믹스, 앱인토스 광고 그룹 단가 변동 가능성이 더 커요." });
  }
  if (completionRate < 0.4 && Number(latest.adSessionStartedCount || 0) > 0) {
    items.push({ level: "bad", text: "내부 광고 세션 완료율이 40% 미만이에요. 광고를 열었지만 보상 완료까지 못 가는 사용자가 많을 수 있어요." });
  }
  if (visitorRatio > 0.55 && Number(latest.appEnteredUsers || 0) > 0) {
    items.push({ level: "warning", text: "오늘 앱 진입 중 방문/저관여 비율이 높아요. 충성 유저가 아닌 트래픽이 늘면 광고 시청률과 eCPM이 같이 약해질 수 있어요." });
  }
  if (rewardConversion < 0.8 && Number(latest.adSessionCompletedCount || 0) > 0) {
    items.push({ level: "warning", text: "광고 완료 대비 보상 신청 전환이 낮아요. 보상 지급 버튼, 토스포인트 환급 안내, 완료 후 이동 흐름을 확인해 보세요." });
  }
  if (previous && previousAdsPerActive > 0 && adsPerActive < previousAdsPerActive * 0.8) {
    items.push({ level: "warning", text: "충성 활성 사용자당 광고 이벤트가 전일보다 줄었어요. 광고 보상 위치나 쿨타임, 보상 효율 변경 영향을 확인해 보세요." });
  }
  if (items.length === 0) {
    items.push({ level: "good", text: "현재 자동 집계 지표에서는 큰 내부 퍼널 급락이 보이지 않아요. 실제 eCPM이 하락했다면 앱인토스 콘솔 지면/광고그룹 단가 변동을 우선 확인하세요." });
  }
  return items;
}

function hasAppInTossMetric(point) {
  return point && point.appInTossEcpmWon !== null && point.appInTossEcpmWon !== undefined;
}

function ratioNumber(value, total) {
  const denominator = Number(total || 0);
  if (denominator <= 0) {
    return 0;
  }
  return Number(value || 0) / denominator;
}

function formatRatio(value) {
  return `${(Number(value || 0) * 100).toFixed(1)}%`;
}

function trendDetail(current, previous, formatter) {
  if (!Number.isFinite(previous) || previous <= 0) {
    return "비교 기준 없음";
  }
  const delta = Number(current || 0) - previous;
  const direction = delta >= 0 ? "상승" : "하락";
  return `${formatter(delta)} ${direction}`;
}

function ecpmStatus(latest, previous) {
  if (!hasAppInTossMetric(latest)) {
    return "neutral";
  }
  const ecpm = Number(latest.appInTossEcpmWon || 0);
  const previousEcpm = Number(previous?.appInTossEcpmWon || 0);
  if (previousEcpm > 0 && ecpm < previousEcpm * 0.9) {
    return "bad";
  }
  return "good";
}

function renderRevenueTrendChart(points) {
  if (!points.length) {
    return `<p class="empty">수익 추이 데이터가 아직 없어요.</p>`;
  }
  const width = 640;
  const height = 190;
  const series = [
    { key: "estimatedGrossRevenueWon", label: "추정 총매출", className: "gross" },
    { key: "estimatedAdRevenueWon", label: "광고 추정", className: "ad" },
    { key: "estimatedIapRevenueWon", label: "IAP 추정", className: "iap" },
    { key: "rewardCostWon", label: "보상 비용", className: "cost" },
  ];
  const maxValue = Math.max(1, ...points.flatMap((point) => series.map((item) => Number(point[item.key] || 0))));
  const paddedMax = Math.max(1, Math.ceil(maxValue * 1.12));
  const midValue = Math.round(paddedMax / 2);
  const x = (index) => points.length === 1 ? width / 2 : width * index / (points.length - 1);
  const y = (value) => height - height * Number(value || 0) / paddedMax;
  const labelStep = Math.max(1, Math.ceil(points.length / 5));
  const labels = points
    .map((point, index) => ({ point, index }))
    .filter(({ index }) => index === 0 || index === points.length - 1 || index % labelStep === 0)
    .map(({ point, index }) => {
      const edgeClass = index === 0 ? " first" : index === points.length - 1 ? " last" : "";
      return `
        <span class="growth-x-label${edgeClass}">
          ${escapeHtml(String(point.date || "").slice(5))}
          <small>${formatNumber(point.estimatedNetRevenueWon || 0)}원</small>
        </span>
      `;
    }).join("");
  return `
    <section class="revenue-trend-section">
      <div class="active-user-head">
        <div>
          <h4>일별 수익 추이</h4>
          <p>실제 eCPM이 아니라 서버 이벤트와 IAP 주문을 기반으로 한 내부 추정값이에요.</p>
        </div>
        <div class="active-user-legend revenue-legend">
          ${series.map((item) => `<span class="${escapeHtml(item.className)}">${escapeHtml(item.label)}</span>`).join("")}
        </div>
      </div>
      <div class="growth-chart revenue-chart" role="img" aria-label="일별 추정 수익 그래프">
        <div class="growth-chart-body">
          <div class="chart-y-axis" aria-hidden="true">
            <span>${formatNumber(paddedMax)}원</span>
            <span>${formatNumber(midValue)}원</span>
            <span>0원</span>
          </div>
          <div class="growth-plot">
            <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
              <line class="chart-grid" x1="0" y1="0" x2="${width}" y2="0"></line>
              <line class="chart-grid" x1="0" y1="${height / 2}" x2="${width}" y2="${height / 2}"></line>
              <line class="chart-grid" x1="0" y1="${height}" x2="${width}" y2="${height}"></line>
              ${series.map((item) => `
                <polyline
                  class="revenue-line ${escapeHtml(item.className)}"
                  points="${points.map((point, index) => `${x(index).toFixed(1)},${y(point[item.key]).toFixed(1)}`).join(" ")}"
                ></polyline>
              `).join("")}
            </svg>
            ${renderChartHoverLayer(points, (point) => {
              const rows = series.map((item) => (
                chartTooltipRow(item.label, `${formatNumber(point[item.key])}원`, item.className)
              ));
              rows.push(chartTooltipRow("순수익", `${formatNumber(point.estimatedNetRevenueWon)}원`, "net"));
              if (hasAppInTossMetric(point)) {
                rows.push(chartTooltipRow("앱인토스 eCPM", `${formatNumber(point.appInTossEcpmWon)}원`, "ecpm"));
                rows.push(chartTooltipRow("콘솔 시청률", `${Number(point.appInTossAdWatchRatePercent || 0).toFixed(1)}%`, "watch-rate"));
                rows.push(chartTooltipRow("콘솔 노출", `${formatNumber(point.appInTossAdImpressions)}회`, "impression"));
              }
              return rows;
            })}
          </div>
        </div>
        <div class="growth-x-labels">${labels}</div>
      </div>
    </section>
  `;
}

function renderIapRevenuePanel(products) {
  return `
    <section class="revenue-subpanel">
      <div class="panel-head compact-head">
        <div>
          <h3>인앱결제 상품별 추정 매출</h3>
          <p>주문 테이블의 상품 타입과 운영 가격표를 매핑해 계산해요.</p>
        </div>
      </div>
      <div class="revenue-table">
        ${products.map((item) => `
          <article class="revenue-table-row">
            <div>
              <strong>${escapeHtml(item.productLabel || item.productType || "-")}</strong>
              <small>${escapeHtml(item.productType || "-")} · 단가 ${formatNumber(item.unitPriceWon)}원</small>
            </div>
            <div>
              <span>${formatNumber(item.orderCount)}건</span>
              <small>지급 ${formatNumber(item.grantedCount)}건</small>
            </div>
            <strong>${formatNumber(item.estimatedRevenueWon)}원</strong>
          </article>
        `).join("") || `<p class="empty">기간 내 인앱결제 주문이 없어요.</p>`}
      </div>
    </section>
  `;
}

function renderAdRevenuePanel(events) {
  return `
    <section class="revenue-subpanel">
      <div class="panel-head compact-head">
        <div>
          <h3>광고/보상 이벤트별 추정 수익</h3>
          <p>SDK의 실제 eCPM이 아니라 서버 이벤트 수에 광고 단가 정책값을 곱한 참고값이에요.</p>
        </div>
      </div>
      <div class="revenue-table">
        ${events.map((item) => `
          <article class="revenue-table-row">
            <div>
              <strong>${escapeHtml(item.label || item.type || "-")}</strong>
              <small>${escapeHtml(item.type || "-")}</small>
            </div>
            <div>
              <span>${formatNumber(item.eventCount)}회</span>
              <small>서버 이벤트</small>
            </div>
            <strong>${formatNumber(item.estimatedRevenueWon)}원</strong>
          </article>
        `).join("") || `<p class="empty">기간 내 광고/보상 이벤트가 없어요.</p>`}
      </div>
    </section>
  `;
}

async function loadAudits() {
  const data = await request(`/api/admin/audits?page=${state.auditPage}&size=${state.auditSize}`);
  $("auditPageInfo").textContent = `${data.page + 1} / ${Math.max(1, data.totalPages)}`;
  $("auditList").innerHTML = (data.logs || []).map((log) => `
    <article class="audit-row">
      <div>
        <strong>${escapeHtml(log.action)} · ${escapeHtml(log.target)}</strong>
        <p>${escapeHtml(log.beforeValue)} → ${escapeHtml(log.afterValue)}</p>
        <small>${escapeHtml(log.reason || "-")}</small>
      </div>
      <div class="audit-meta">
        <span>${new Date(log.createdAt).toLocaleString("ko-KR")}</span>
        <span>${escapeHtml(log.actorFingerprint)}</span>
      </div>
    </article>
  `).join("") || `<p class="empty">감사 로그가 아직 없어요.</p>`;
}

async function loadPayments() {
  const data = await request(`/api/admin/payments?page=${state.paymentPage}&size=${state.paymentSize}`);
  const payments = data.content || [];
  state.paymentPage = Number(data.page ?? state.paymentPage);
  state.paymentTotalPages = Math.max(1, Number(data.totalPages ?? 1));
  $("paymentPageInfo").textContent = `${state.paymentPage + 1} / ${state.paymentTotalPages}`;
  $("prevPaymentButton").disabled = state.paymentPage <= 0;
  $("nextPaymentButton").disabled = state.paymentPage + 1 >= state.paymentTotalPages;
  $("paymentList").innerHTML = payments.map((payment) => `
    <article class="payment-row" data-payment-row data-user-key="${escapeHtml(payment.userKey)}">
      <div>
        <div class="payment-title">
          <span class="payment-status ${payment.granted ? "granted" : "pending"}">${payment.granted ? "지급 완료" : "지급 대기"}</span>
          <strong>${escapeHtml(payment.productLabel || payment.productType || "상품")}</strong>
        </div>
        <p>${escapeHtml(payment.productType || "-")} · ${escapeHtml(payment.productId || "-")}</p>
        <small>주문 ${escapeHtml(payment.orderId || "-")} · 구매 ${formatDate(payment.createdAt)} · 지급 ${formatDate(payment.grantedAt)}</small>
      </div>
      <div class="payment-meta" data-payment-control>
        <button class="ghost" type="button" data-payment-open-user>유저 보기</button>
        <strong>${escapeHtml(payment.userKey || "-")}</strong>
      </div>
    </article>
  `).join("") || `<p class="empty">결제 내역이 아직 없어요.</p>`;

  document.querySelectorAll("[data-payment-row]").forEach((row) => {
    row.addEventListener("click", (event) => {
      if (event.target.closest("[data-payment-control]")) {
        return;
      }
      openPlayerFromAnomaly(row.dataset.userKey);
    });
  });
  document.querySelectorAll("[data-payment-open-user]").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      openPlayerFromAnomaly(button.closest("[data-payment-row]").dataset.userKey);
    });
  });
}

async function request(url, options = {}) {
  return send(url, {
    ...options,
    headers: {
      ...(options.headers || {}),
      Authorization: `Bearer ${state.accessToken}`,
    },
  });
}

async function publicRequest(url, options = {}) {
  return send(url, options);
}

async function send(url, options = {}) {
  const headers = {
    ...(options.headers || {}),
  };
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const response = await fetch(url, {
    method: options.method || "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(data?.message || "요청 처리에 실패했어요.");
  }
  return data;
}

function setButtonBusy(button, busy) {
  if (!button) {
    return;
  }
  button.disabled = busy;
  button.setAttribute("aria-busy", String(busy));
}

function setPolicyRowBusy(row, busy) {
  row.querySelectorAll("input, button").forEach((element) => {
    element.disabled = busy;
    element.setAttribute("aria-busy", String(busy));
  });
}

function showToast(message, type = "success") {
  let toast = document.querySelector(".admin-toast");
  if (!toast) {
    toast = document.createElement("div");
    toast.className = "admin-toast";
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    document.body.appendChild(toast);
  }
  toast.textContent = message;
  toast.classList.remove("success", "error", "visible");
  toast.classList.add(type === "error" ? "error" : "success");
  window.requestAnimationFrame(() => toast.classList.add("visible"));
  window.clearTimeout(state.toastTimer);
  state.toastTimer = window.setTimeout(() => {
    toast.classList.remove("visible");
  }, 2600);
}

function showLoading(message) {
  let overlay = document.querySelector(".loading-overlay");
  if (!overlay) {
    overlay = document.createElement("div");
    overlay.className = "loading-overlay";
    overlay.setAttribute("role", "status");
    overlay.setAttribute("aria-live", "polite");
    document.body.appendChild(overlay);
  }
  overlay.innerHTML = `
    <div class="loading-box">
      <span class="loading-spinner" aria-hidden="true"></span>
      <strong>${escapeHtml(message)}</strong>
    </div>
  `;
  window.clearTimeout(state.loadingTimer);
  state.loadingTimer = window.setTimeout(() => overlay.classList.add("visible"), 80);
}

function hideLoading() {
  window.clearTimeout(state.loadingTimer);
  const overlay = document.querySelector(".loading-overlay");
  overlay?.classList.remove("visible");
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString("ko-KR");
}

function formatCombatPower(value) {
  const safeValue = Math.max(0, Math.floor(Number(value || 0)));
  if (safeValue >= 10000) {
    const man = Math.floor(safeValue / 10000);
    const rest = safeValue % 10000;
    return rest > 0 ? `${formatNumber(man)}만${String(rest).padStart(4, "0")}` : `${formatNumber(man)}만`;
  }
  return formatNumber(safeValue);
}

function formatPercent(value, total) {
  const denominator = Number(total || 0);
  if (denominator <= 0) {
    return "0.0%";
  }
  return `${(Number(value || 0) / denominator * 100).toFixed(1)}%`;
}

function formatDate(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleString("ko-KR");
}

function jobLabel(job) {
  return {
    WARRIOR: "전사",
    ARCHER: "궁수",
    MAGE: "마법사",
    ROGUE: "도적",
    미선택: "미선택",
  }[job] || job || "미선택";
}

function severityLabel(severity) {
  return {
    CRITICAL: "위험",
    WARNING: "주의",
    INFO: "관찰",
  }[severity] || severity;
}

function anomalyStatusLabel(status) {
  return {
    OPEN: "처리전",
    IN_PROGRESS: "처리중",
    RESOLVED: "처리완료",
  }[status] || "처리전";
}

function renderRuntimeStatusItems(statusItems) {
  return statusItems.map((item) => `
    <article class="runtime-status-card ${item.healthy ? "ok" : "blocked"}">
      <div class="status-lamp" aria-hidden="true"></div>
      <div>
        <div class="runtime-status-title">
          <strong>${escapeHtml(item.label)}</strong>
          <span class="runtime-mode ${runtimeModeClass(item.mode)}">${escapeHtml(runtimeModeLabel(item.mode))}</span>
        </div>
        <small>${escapeHtml(item.detail || item.status)}</small>
      </div>
    </article>
  `).join("");
}

function runtimeModeLabel(mode) {
  return {
    LIVE: "운영",
    TEST: "테스트",
    OFF: "꺼짐",
    CHECK: "확인",
  }[mode] || "확인";
}

function runtimeModeClass(mode) {
  return {
    LIVE: "live",
    TEST: "test",
    OFF: "off",
    CHECK: "check",
  }[mode] || "check";
}

function policyDisplayValue(key, rawValue) {
  const value = Number(rawValue || 0);
  if (minutePolicyKeys.has(key)) {
    return Number((value / 60).toFixed(1));
  }
  return hourPolicyKeys.has(key) ? Number((value / 3600).toFixed(2)) : value;
}

function policyRequestValue(key, displayValue) {
  const value = Number(displayValue);
  if (!Number.isFinite(value)) {
    return 0;
  }
  if (minutePolicyKeys.has(key)) {
    return Math.round(value * 60);
  }
  return hourPolicyKeys.has(key) ? Math.round(value * 3600) : value;
}

function policyDisplayUnit(key, unit) {
  if (minutePolicyKeys.has(key)) {
    return "분";
  }
  return hourPolicyKeys.has(key) ? "시간" : unit;
}

function policyInputStep(key) {
  if (minutePolicyKeys.has(key)) {
    return "1";
  }
  return hourPolicyKeys.has(key) ? "0.1" : "1";
}

function formatBytes(value) {
  const bytes = Number(value || 0);
  if (bytes < 1024) {
    return `${bytes}B`;
  }
  const units = ["KB", "MB", "GB"];
  let amount = bytes / 1024;
  let unitIndex = 0;
  while (amount >= 1024 && unitIndex < units.length - 1) {
    amount /= 1024;
    unitIndex += 1;
  }
  return `${amount.toFixed(amount >= 100 ? 0 : 1)}${units[unitIndex]}`;
}

function formatDuration(millis) {
  const totalSeconds = Math.floor(Number(millis || 0) / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (days > 0) {
    return `${days}일 ${hours}시간`;
  }
  if (hours > 0) {
    return `${hours}시간 ${minutes}분`;
  }
  return `${minutes}분`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
