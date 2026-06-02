const state = {
  accessToken: sessionStorage.getItem("moneyHunterAdminAccessToken") || "",
  username: sessionStorage.getItem("moneyHunterAdminUsername") || "",
  view: "dashboardView",
  auditPage: 0,
  auditSize: 30,
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
};

const maxGoldPerHour = 6000;
const fullRateRequiredAdsPerHour = 2;
const hourPolicyKeys = new Set([
  "autoHuntAdSeconds",
  "boostAdSeconds",
  "maxAdSeconds",
  "skillPointAdCooldownSeconds",
  "anomalyTimerGraceSeconds",
]);
const minutePolicyKeys = new Set([
  "autoHuntAdCooldownSeconds",
  "boostAdCooldownSeconds",
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
    anomalyView: "이상징후",
    playerView: "유저 관리",
    policyView: "정책값",
    auditView: "감사 로그",
    monitoringView: "모니터링",
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
    } else if (state.view === "anomalyView") {
      await loadAnomalies();
    } else if (state.view === "playerView") {
      await loadPlayers();
    } else if (state.view === "monitoringView") {
      await loadServerMetrics();
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

function renderEconomyPanel(data) {
  const economy = data.economy || {};
  const pendingPoints = Number(data.pendingRewardPoints || 0);
  const pendingWon = pendingPoints;
  const adRevenue = Number(economy.adRevenuePerRewardAdWon || 0);
  const goldPerPoint = Number(economy.goldPerTossPoint || 0);
  const revenueBasis = adRevenue * fullRateRequiredAdsPerHour;
  const derivedGoldPerPoint = deriveGoldPerPoint(adRevenue);
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
        ${economyItem("광고 기준 수익", revenueBasis, "원/h")}
        ${economyItem("현재 최대 환산", currentMaxPointValue, "P/h")}
        ${economyItem("포인트 환산", economy.goldPerTossPoint, "골드/P")}
        ${economyItem("보상 신청 기준", economy.rewardPointAmount, "P")}
        ${economyItem("신청 필요 골드", economy.rewardGoldThreshold, "G")}
        ${economyItem("대기 중 포인트", pendingPoints, "P")}
      </div>
      <div class="quick-policy-card calibration-card">
        <div>
          <strong>광고 수익 기준 자동 환산</strong>
          <p>자동사냥 광고 1회와 공속버프 광고 1회를 기준으로 6,000G/h의 포인트 환산값을 계산해요.</p>
        </div>
        <label>
          <span>1회 예상 매출(원)</span>
          <input id="revenueAdInput" type="number" min="1" max="10000" value="${escapeHtml(adRevenue || 1)}" />
        </label>
        <div class="derived-policy">
          <span>자동 환산값</span>
          <strong id="derivedGoldPerPoint">${formatNumber(derivedGoldPerPoint)}G/P</strong>
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
        정책값을 바꿔도 이미 대기 중인 기록은 재계산하지 않고, 새 보상 신청부터 변경된 기준이 적용돼요.
      </p>
    </section>
  `;
  $("revenueAdInput").addEventListener("input", () => {
    $("derivedGoldPerPoint").textContent = `${formatNumber(deriveGoldPerPoint(Number($("revenueAdInput").value)))}G/P`;
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

function deriveGoldPerPoint(adRevenuePerAd) {
  const adRevenue = Number(adRevenuePerAd || 0);
  if (!Number.isFinite(adRevenue) || adRevenue <= 0) {
    return 1;
  }
  return Math.max(1, Math.ceil(maxGoldPerHour / (adRevenue * fullRateRequiredAdsPerHour)));
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
  const adEvents = Number(data.rewardAdEventsToday || 0);
  const items = [
    ["온보딩 전환", formatPercent(data.onboardedPlayers, totalPlayers), "직업 선택 완료 비율"],
    ["자동사냥 활성률", formatPercent(data.activeAutoHuntPlayers, totalPlayers), "현재 사냥 중인 유저"],
    ["공속버프 활성률", formatPercent(data.activeBoostPlayers, totalPlayers), "현재 공속버프 중인 유저"],
    ["오늘 광고/유저", totalPlayers > 0 ? (adEvents / totalPlayers).toFixed(2) : "0.00", "전체 유저 기준 평균"],
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
  const maxTotal = Math.max(1, ...points.map((point) => Number(point.totalPlayers || 0)));
  const x = (index) => points.length === 1 ? width / 2 : width * index / (points.length - 1);
  const y = (value) => height - height * Number(value || 0) / maxTotal;
  const linePoints = points.map((point, index) => `${x(index).toFixed(1)},${y(point.totalPlayers).toFixed(1)}`).join(" ");
  const areaPoints = `0,${height} ${linePoints} ${width},${height}`;
  const lastPoint = points[points.length - 1];
  const lastX = x(points.length - 1).toFixed(1);
  const lastY = y(lastPoint.totalPlayers).toFixed(1);
  const lastLabelLeft = Math.min(98, Math.max(14, Number(lastX) / width * 100));
  const lastLabelTop = Math.min(90, Math.max(12, Number(lastY) / height * 100));
  const labelStep = Math.max(1, Math.ceil(points.length / 6));
  const labels = points
    .map((point, index) => ({ point, index }))
    .filter(({ index }) => index === 0 || index === points.length - 1 || index % labelStep === 0)
    .map(({ point, index }) => {
      const position = points.length === 1 ? 50 : index / (points.length - 1) * 100;
      const edgeClass = index === 0 ? " first" : index === points.length - 1 ? " last" : "";
      return `<span class="growth-x-label${edgeClass}" style="left: ${position.toFixed(2)}%">${escapeHtml(point.date.slice(5))}</span>`;
    }).join("");
  return `
    <div class="growth-chart" role="img" aria-label="전체 유저 선형 그래프">
      <div class="growth-chart-body">
        <span class="chart-y-label chart-y-max">${formatNumber(maxTotal)}명</span>
        <span class="chart-y-label chart-y-zero">0명</span>
        <div class="growth-plot">
          <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" aria-hidden="true">
            <line class="chart-grid" x1="0" y1="0" x2="${width}" y2="0"></line>
            <line class="chart-grid" x1="0" y1="${height}" x2="${width}" y2="${height}"></line>
            <polygon class="growth-area" points="${areaPoints}"></polygon>
            <polyline class="growth-line" points="${linePoints}"></polyline>
            <circle class="growth-dot" cx="${lastX}" cy="${lastY}" r="5"></circle>
          </svg>
          <span class="growth-value-label" style="left: ${lastLabelLeft.toFixed(2)}%; top: ${lastLabelTop.toFixed(2)}%">${formatNumber(lastPoint.totalPlayers)}명</span>
        </div>
      </div>
      <div class="growth-x-labels">${labels}</div>
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
  const boost = player.boostEndsAt ? new Date(player.boostEndsAt).toLocaleString("ko-KR") : "-";
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
        <p>${escapeHtml(jobLabel(job))} · Lv.${formatNumber(player.level)} · 보유 ${formatNumber(player.gold)}G · 누적 ${formatNumber(player.cumulativeGoldEarned || player.gold)}G · SP ${formatNumber(player.skillPoints)}</p>
        <small>최근 접속 ${escapeHtml(lastAccessed)} · 자동사냥 ${escapeHtml(autoHunt)} · 공속버프 ${escapeHtml(boost)}</small>
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
  const basicDetails = [
    ["유저 식별 ID", player.userKey],
    ["관리자 별명", player.adminNickname || "미설정"],
    ["게임 프로필", player.gameProfileNickname || "미동기화"],
    ["마지막 접속", formatDate(player.lastAccessedAt)],
    ["히든 스킨", player.hiddenPetSkinsUnlocked ? "해금" : "미해금"],
    ["골드", `${formatNumber(player.gold)}G`],
    ["누적 골드", `${formatNumber(player.cumulativeGoldEarned || player.gold)}G`],
    ["SP", formatNumber(player.skillPoints)],
    ["레벨", `Lv.${formatNumber(player.level)}`],
    ["경험치", `${formatNumber(player.experience)} / ${formatNumber(player.nextLevelExperience)}`],
    ["펫 슬롯", `${formatNumber(player.characterSlots)} / 3`],
    ["초대 보상", `${formatNumber(player.friendInviteRewardCount)}회`],
  ];
  const extraDetails = [
    ["처치 몬스터", formatNumber(player.defeatedMonsters)],
    ["현재 몬스터", `${player.currentMonsterKey || "-"} · HP ${formatNumber(player.currentMonsterHp)}`],
    ["튜토리얼 보상", player.tutorialRewardClaimed ? "완료" : "미수령"],
    ["기능 튜토리얼", player.featureTutorialCompleted ? "완료" : "미완료"],
    ["마지막 정산", formatDate(player.lastSettledAt)],
    ["최근 SP 광고", formatDate(player.lastSkillPointAdClaimedAt)],
    ["자동사냥 종료", formatDate(player.autoHuntEndsAt)],
    ["공속버프 종료", formatDate(player.boostEndsAt)],
    ["게임 프로필 갱신", formatDate(player.gameProfileUpdatedAt)],
    ["즐겨찾기", player.adminFavorite ? "예" : "아니오"],
    ["가입", formatDate(player.createdAt)],
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
      keys: ["rewardPointAmount"],
    },
    {
      title: "광고 보상",
      description: "광고 1회 보상 시간과 누적 상한",
      keys: [
        "autoHuntAdSeconds",
        "boostAdSeconds",
        "autoHuntAdCooldownSeconds",
        "boostAdCooldownSeconds",
        "maxAdSeconds",
        "skillPointAdCooldownSeconds",
      ],
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
