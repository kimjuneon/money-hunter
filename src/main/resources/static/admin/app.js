const state = {
  accessToken: sessionStorage.getItem("moneyHunterAdminAccessToken") || "",
  username: sessionStorage.getItem("moneyHunterAdminUsername") || "",
  view: "dashboardView",
  auditPage: 0,
  auditSize: 30,
  toastTimer: null,
};

const $ = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
  bindEvents();
  restoreSession();
});

function bindEvents() {
  $("loginForm").addEventListener("submit", login);
  $("playerSearchForm").addEventListener("submit", (event) => {
    event.preventDefault();
    loadPlayers();
  });
  $("logoutButton").addEventListener("click", logout);
  $("refreshButton").addEventListener("click", refreshCurrentView);
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
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });
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

function switchView(viewId) {
  state.view = viewId;
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.classList.toggle("active", button.dataset.view === viewId);
  });
  document.querySelectorAll(".view").forEach((view) => {
    view.classList.toggle("hidden", view.id !== viewId);
  });
  $("pageTitle").textContent = {
    dashboardView: "대시보드",
    anomalyView: "이상징후",
    playerView: "유저 관리",
    policyView: "정책값",
    auditView: "감사 로그",
  }[viewId] || "대시보드";
  refreshCurrentView();
}

function refreshCurrentView() {
  if (state.view === "policyView") {
    loadPolicies();
  } else if (state.view === "auditView") {
    loadAudits();
  } else if (state.view === "anomalyView") {
    loadAnomalies();
  } else if (state.view === "playerView") {
    loadPlayers();
  } else {
    loadOverview();
  }
}

async function loadOverview() {
  const data = await request("/api/admin/overview");
  renderStatus(data);
  renderMetrics(data);
}

function renderStatus(data) {
  const ready = data.tossReleaseReady;
  $("statusBand").innerHTML = `
    <div class="release-card ${ready ? "ready" : "blocked"}">
      <span>${ready ? "READY" : "BLOCKED"}</span>
      <strong>${escapeHtml(data.integrationMode)} · ${escapeHtml(data.distributionTarget)}</strong>
      <p>${ready ? "출시 차단 항목이 없어요." : escapeHtml((data.releaseBlockers || []).join(", "))}</p>
    </div>
  `;
}

function renderMetrics(data) {
  const metrics = [
    ["전체 유저", data.totalPlayers, "명"],
    ["온보딩 완료", data.onboardedPlayers, "명"],
    ["정지 유저", data.suspendedPlayers, "명"],
    ["오늘 신규", data.newPlayersToday, "명"],
    ["자동사냥 활성", data.activeAutoHuntPlayers, "명"],
    ["공속버프 활성", data.activeBoostPlayers, "명"],
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

async function loadAnomalies() {
  const data = await request("/api/admin/anomalies");
  $("anomalyUpdatedAt").textContent = new Date(data.generatedAt).toLocaleString("ko-KR");
  renderAnomalySummary(data);
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
    <article class="anomaly-row ${escapeHtml(anomaly.severity.toLowerCase())}">
      <div>
        <div class="anomaly-title">
          <span class="severity-chip">${escapeHtml(severityLabel(anomaly.severity))}</span>
          <strong>${escapeHtml(anomaly.title)}</strong>
        </div>
        <p>${escapeHtml(anomaly.detail)}</p>
        <small>${escapeHtml(anomaly.category)} · 기준 ${formatNumber(anomaly.thresholdValue)} / 감지 ${formatNumber(anomaly.observedValue)}</small>
      </div>
      <div class="anomaly-meta">
        <strong>${escapeHtml(anomaly.userKey)}</strong>
        <span>${new Date(anomaly.detectedAt).toLocaleString("ko-KR")}</span>
      </div>
    </article>
  `).join("") || `<p class="empty">현재 감지된 이상징후가 없어요.</p>`;
}

async function loadPlayers() {
  const query = $("playerSearchInput").value.trim();
  const players = await request(`/api/admin/players?query=${encodeURIComponent(query)}&limit=50`);
  $("playerCount").textContent = `${players.length}명`;
  $("playerList").innerHTML = players.map((player) => playerRow(player)).join("")
    || `<p class="empty">검색 결과가 없어요.</p>`;
  document.querySelectorAll("[data-player-action]").forEach((button) => {
    button.addEventListener("click", () => runPlayerAction(button));
  });
}

function playerRow(player) {
  const status = player.suspended ? "정지" : "정상";
  const statusClass = player.suspended ? "suspended" : "active";
  const job = player.job || "미선택";
  const autoHunt = player.autoHuntEndsAt ? new Date(player.autoHuntEndsAt).toLocaleString("ko-KR") : "-";
  const boost = player.boostEndsAt ? new Date(player.boostEndsAt).toLocaleString("ko-KR") : "-";
  return `
    <article class="player-row ${statusClass}">
      <div class="player-main">
        <div class="player-title">
          <strong>${escapeHtml(player.userKey)}</strong>
          <span class="player-status ${statusClass}">${status}</span>
        </div>
        <p>${escapeHtml(job)} · Lv.${formatNumber(player.level)} · ${formatNumber(player.gold)}G · SP ${formatNumber(player.skillPoints)}</p>
        <small>자동사냥 ${escapeHtml(autoHunt)} · 공속버프 ${escapeHtml(boost)}</small>
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
  const reason = prompt(`${labels[action]} 사유를 입력해 주세요. (선택)`, "");
  if (reason === null) {
    return;
  }
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
  $("policyCount").textContent = `${policies.length}개`;
  $("policyList").innerHTML = policies.map((policy) => `
    <article class="policy-row" data-key="${escapeHtml(policy.key)}">
      <div class="policy-copy">
        <strong>${escapeHtml(policy.label)}</strong>
        <small>${escapeHtml(policy.key)} · ${formatNumber(policy.min)}~${formatNumber(policy.max)} ${escapeHtml(policy.unit)}</small>
      </div>
      <input type="number" min="${policy.min}" max="${policy.max}" value="${escapeHtml(policy.value)}" data-value />
      <input type="text" placeholder="변경 사유(선택)" data-reason />
      <div class="actions">
        <button class="secondary" data-save>저장</button>
        <button class="ghost" data-reset>기본값</button>
      </div>
    </article>
  `).join("");
  document.querySelectorAll("[data-save]").forEach((button) => {
    button.addEventListener("click", () => savePolicy(button.closest(".policy-row"), false));
  });
  document.querySelectorAll("[data-reset]").forEach((button) => {
    button.addEventListener("click", () => savePolicy(button.closest(".policy-row"), true));
  });
}

async function savePolicy(row, resetToDefault) {
  const body = {
    key: row.dataset.key,
    value: Number(row.querySelector("[data-value]").value),
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

function formatNumber(value) {
  return Number(value || 0).toLocaleString("ko-KR");
}

function severityLabel(severity) {
  return {
    CRITICAL: "위험",
    WARNING: "주의",
    INFO: "관찰",
  }[severity] || severity;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
