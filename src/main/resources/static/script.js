const API_BASE_URL = `${window.location.origin}/api`;

const state = {
    rows: [],
    summary: null,
    selectedTicker: null
};

const investmentForm = document.getElementById("investmentForm");
const riskTableBody = document.querySelector("#riskTable tbody");
const emptyStateRow = document.getElementById("emptyState");
const loadingSpinner = document.getElementById("riskLoading");
const refreshButton = document.getElementById("refreshButton");
const feedback = document.getElementById("formFeedback");
const searchInput = document.getElementById("tableSearch");
const sortSelect = document.getElementById("sortBy");

const summarySection = document.getElementById("summaryCards");
const positionsCountEl = document.getElementById("positionsCount");
const totalExposureEl = document.getElementById("totalExposure");
const weightedReturnEl = document.getElementById("weightedReturn");
const weightedVolatilityEl = document.getElementById("weightedVolatility");
const totalVaREl = document.getElementById("totalVaR");
const portfolioRiskLevelEl = document.getElementById("portfolioRiskLevel");
const portfolioRiskHintEl = document.getElementById("portfolioRiskHint");
const topMoverEl = document.getElementById("topMover");
const benchmarkTickerEl = document.getElementById("benchmarkTicker");
const summaryUpdatedAtEl = document.getElementById("summaryUpdatedAt");

const detailsPanel = document.getElementById("positionDetails");
const detailTickerEl = document.getElementById("detailTicker");
const detailPriceEl = document.getElementById("detailPrice");
const detailChangeEl = document.getElementById("detailChange");
const detailRangeEl = document.getElementById("detailRange");
const detailVolumeEl = document.getElementById("detailVolume");
const detailMarketCapEl = document.getElementById("detailMarketCap");
const detailBetaEl = document.getElementById("detailBeta");
const detailEsEl = document.getElementById("detailEs");

const backtestForm = document.getElementById("backtestForm");
const backtestFeedback = document.getElementById("backtestFeedback");
const backtestResult = document.getElementById("backtestResult");
const backtestCurveLine = document.getElementById("backtestCurveLine");
const backtestCurveArea = document.getElementById("backtestCurveArea");
const backtestGrid = document.getElementById("backtestGrid");
const backtestBaseline = document.getElementById("backtestBaseline");
const btCurrentValueEl = document.getElementById("btCurrentValue");
const btTotalReturnEl = document.getElementById("btTotalReturn");
const btCagrEl = document.getElementById("btCagr");
const btMaxDrawdownEl = document.getElementById("btMaxDrawdown");
const btSharpeEl = document.getElementById("btSharpe");
const btDataSourceEl = document.getElementById("btDataSource");
const btRangeTextEl = document.getElementById("btRangeText");
const btMaxLabelEl = document.getElementById("btMaxLabel");
const btMidLabelEl = document.getElementById("btMidLabel");
const btMinLabelEl = document.getElementById("btMinLabel");
const btStartLabelEl = document.getElementById("btStartLabel");
const btEndLabelEl = document.getElementById("btEndLabel");

investmentForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const ticker = document.getElementById("ticker").value.trim().toUpperCase();
    const amount = Number.parseFloat(document.getElementById("amount").value);

    if (!ticker || Number.isNaN(amount) || amount <= 0) {
        setFeedback("Provide a valid ticker and amount.", true);
        return;
    }

    toggleFormState(true);
    clearFeedback();
    try {
        const response = await fetch(`${API_BASE_URL}/add-stock?ticker=${encodeURIComponent(ticker)}&amount=${encodeURIComponent(amount)}`, {
            method: "POST",
            headers: { "Accept": "application/json" }
        });
        if (!response.ok) {
            throw await buildError(response, "Unable to sync this position right now.");
        }
        investmentForm.reset();
        setFeedback(`${ticker} synced successfully.`, false);
        state.selectedTicker = ticker;
        await loadDashboard(true);
    } catch (error) {
        setFeedback(error.message, true);
    } finally {
        toggleFormState(false);
    }
});

refreshButton.addEventListener("click", async (event) => {
    event.preventDefault();
    await loadDashboard(true);
});

searchInput.addEventListener("input", renderTableAndDetails);
sortSelect.addEventListener("change", renderTableAndDetails);

backtestForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const ticker = document.getElementById("backtestTicker").value.trim().toUpperCase();
    const amount = Number.parseFloat(document.getElementById("backtestAmount").value);
    const startDate = document.getElementById("backtestStartDate").value;
    const endDate = document.getElementById("backtestEndDate").value;

    if (!ticker || Number.isNaN(amount) || amount <= 0 || !startDate) {
        setBacktestFeedback("Provide ticker, amount, and start date.", true);
        return;
    }

    toggleBacktestState(true);
    setBacktestFeedback("", false);

    try {
        const response = await fetch(`${API_BASE_URL}/portfolio/backtest`, {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                ticker,
                amount,
                startDate,
                endDate: endDate || ""
            })
        });

        if (!response.ok) {
            throw await buildError(response, "Unable to run replay for this setup.");
        }

        const result = await response.json();
        renderBacktest(result);

        const adjustedStart = result?.startDate && result.startDate !== startDate;
        const adjustedEnd = endDate && result?.endDate && result.endDate !== endDate;
        if (adjustedStart || adjustedEnd) {
            setBacktestFeedback(
                `${ticker} replay completed using available data from ${result.startDate} to ${result.endDate}.`,
                false
            );
        } else {
            setBacktestFeedback(`${ticker} replay completed.`, false);
        }
    } catch (error) {
        backtestResult.hidden = true;
        setBacktestFeedback(error.message, true);
    } finally {
        toggleBacktestState(false);
    }
});

riskTableBody.addEventListener("click", (event) => {
    const row = event.target.closest("tr[data-ticker]");
    if (!row) {
        return;
    }
    state.selectedTicker = row.dataset.ticker;
    renderTableAndDetails();
});

async function loadDashboard(liveMode) {
    showLoading(true);
    try {
        let riskResponse;
        let summaryResponse;

        if (liveMode) {
            riskResponse = await fetch(`${API_BASE_URL}/risk/live`, {
                cache: "no-store",
                headers: { "Accept": "application/json" }
            });
            summaryResponse = await fetch(`${API_BASE_URL}/portfolio/summary`, {
                cache: "no-store",
                headers: { "Accept": "application/json" }
            });
        } else {
            [riskResponse, summaryResponse] = await Promise.all([
                fetch(`${API_BASE_URL}/risk`, { cache: "no-store", headers: { "Accept": "application/json" } }),
                fetch(`${API_BASE_URL}/portfolio/summary`, { cache: "no-store", headers: { "Accept": "application/json" } })
            ]);
        }

        if (!riskResponse.ok) {
            throw await buildError(riskResponse, "Could not load risk analytics.");
        }
        if (!summaryResponse.ok) {
            throw await buildError(summaryResponse, "Could not load portfolio summary.");
        }

        state.rows = await riskResponse.json();
        state.summary = await summaryResponse.json();

        if (state.rows.length > 0 && !state.rows.some((item) => item.ticker === state.selectedTicker)) {
            state.selectedTicker = state.rows[0].ticker;
        }

        renderSummary(state.summary);
        renderTableAndDetails();
    } catch (error) {
        state.rows = [];
        state.summary = null;
        renderSummary(null);
        renderTableAndDetails();
        setFeedback(error.message, true);
    } finally {
        showLoading(false);
    }
}

function renderTableAndDetails() {
    const filtered = applyFilterAndSort(state.rows);
    renderRiskTable(filtered);
    const selected = filtered.find((item) => item.ticker === state.selectedTicker) || filtered[0];
    if (selected) {
        state.selectedTicker = selected.ticker;
    }
    renderDetails(selected);
}

function applyFilterAndSort(rows) {
    const query = (searchInput.value || "").trim().toLowerCase();
    const sortBy = sortSelect.value;

    const filtered = rows.filter((item) => {
        if (!query) {
            return true;
        }
        return (item.ticker || "").toLowerCase().includes(query);
    });

    filtered.sort((left, right) => compareBySort(sortBy, left, right));
    return filtered;
}

function compareBySort(sortBy, left, right) {
    if (sortBy === "ticker") {
        return (left.ticker || "").localeCompare(right.ticker || "");
    }
    if (sortBy === "volatility") {
        return numberOrZero(right.volatility) - numberOrZero(left.volatility);
    }
    if (sortBy === "return") {
        return numberOrZero(right.averageReturn) - numberOrZero(left.averageReturn);
    }
    if (sortBy === "var") {
        return numberOrZero(right.valueAtRisk) - numberOrZero(left.valueAtRisk);
    }
    if (sortBy === "move") {
        return Math.abs(numberOrZero(right.dayChangePercent)) - Math.abs(numberOrZero(left.dayChangePercent));
    }
    return riskScore(right.riskLevel) - riskScore(left.riskLevel);
}

function renderRiskTable(data) {
    riskTableBody.querySelectorAll("tr:not(#emptyState)").forEach((row) => row.remove());

    if (!data || data.length === 0) {
        emptyStateRow.hidden = false;
        return;
    }
    emptyStateRow.hidden = true;

    const rows = data.map((item) => {
        const activeClass = item.ticker === state.selectedTicker ? "table-active-row" : "";
        const moveClass = trendClass(item.dayChangePercent);
        return `
            <tr data-ticker="${escapeHtml(item.ticker)}" class="${activeClass}">
                <td class="fw-semibold">${escapeHtml(item.ticker)}</td>
                <td>${formatCurrency(item.amount)}</td>
                <td>${formatCurrency(item.currentPrice, item.quoteCurrency)}</td>
                <td class="${moveClass}">${formatSignedPercentage(item.dayChangePercent)}</td>
                <td>${formatPercentage(item.averageReturn)}</td>
                <td>${formatPercentage(item.volatility)}</td>
                <td>${formatNumber(item.sharpeRatio)}</td>
                <td>${formatCurrency(item.valueAtRisk)}</td>
                <td>${formatCurrency(item.expectedShortfall)}</td>
                <td>${formatNumber(item.betaVsBenchmark)}</td>
                <td>${formatPercentage(item.maxDrawdown)}</td>
                <td><span class="badge rounded-pill ${badgeClass(item.riskLevel)}">${escapeHtml(item.riskLevel || "Unknown")}</span></td>
                <td>${formatTimestamp(item.lastUpdated)}</td>
            </tr>
        `;
    });

    riskTableBody.insertAdjacentHTML("beforeend", rows.join(""));
}

function renderSummary(summary) {
    if (!summary || Number(summary.positions) === 0) {
        summarySection.hidden = true;
        portfolioRiskLevelEl.className = "badge rounded-pill";
        portfolioRiskLevelEl.textContent = "-";
        portfolioRiskHintEl.textContent = "Awaiting data";
        return;
    }

    summarySection.hidden = false;
    positionsCountEl.textContent = String(summary.positions ?? 0);
    totalExposureEl.textContent = formatCurrency(summary.totalExposure);
    weightedReturnEl.textContent = formatPercentage(summary.weightedReturn);
    weightedVolatilityEl.textContent = formatPercentage(summary.weightedVolatility);
    totalVaREl.textContent = formatCurrency(summary.totalValueAtRisk);

    const riskLevel = summary.portfolioRiskLevel || "Unrated";
    portfolioRiskLevelEl.textContent = riskLevel;
    portfolioRiskLevelEl.className = `badge rounded-pill ${badgeClass(riskLevel)}`;
    portfolioRiskHintEl.textContent = riskHint(riskLevel, summary.worstDrawdown);

    if (summary.topMoverTicker && summary.topMoverPercent !== null && summary.topMoverPercent !== undefined) {
        topMoverEl.innerHTML = `<span class="${trendClass(summary.topMoverPercent)}">${escapeHtml(summary.topMoverTicker)} ${formatSignedPercentage(summary.topMoverPercent)}</span>`;
    } else {
        topMoverEl.textContent = "-";
    }

    benchmarkTickerEl.textContent = summary.benchmarkTicker || "-";
    summaryUpdatedAtEl.textContent = `Last updated: ${formatTimestamp(summary.updatedAt)}`;
}

function renderDetails(item) {
    if (!item) {
        detailsPanel.hidden = true;
        return;
    }
    detailsPanel.hidden = false;
    detailTickerEl.textContent = item.ticker || "-";
    detailPriceEl.textContent = formatCurrency(item.currentPrice, item.quoteCurrency);
    detailChangeEl.innerHTML = `<span class="${trendClass(item.dayChangePercent)}">${formatSignedCurrency(item.dayChange, item.quoteCurrency)} (${formatSignedPercentage(item.dayChangePercent)})</span>`;
    detailRangeEl.textContent = `${formatCurrency(item.dayLow, item.quoteCurrency)} - ${formatCurrency(item.dayHigh, item.quoteCurrency)}`;
    detailVolumeEl.textContent = formatInteger(item.volume);
    detailMarketCapEl.textContent = abbreviateCurrency(item.marketCap, item.quoteCurrency || "USD");
    detailBetaEl.textContent = formatNumber(item.betaVsBenchmark);
    detailEsEl.textContent = formatCurrency(item.expectedShortfall);
}

function renderBacktest(result) {
    if (!result) {
        backtestResult.hidden = true;
        return;
    }

    backtestResult.hidden = false;
    btCurrentValueEl.textContent = formatCurrency(result.currentValue);
    btTotalReturnEl.textContent = formatSignedPercentage(result.totalReturn);
    btCagrEl.textContent = formatSignedPercentage(result.cagr);
    btMaxDrawdownEl.textContent = formatSignedPercentage(-Math.abs(result.maxDrawdown));
    btSharpeEl.textContent = formatNumber(result.sharpeRatio);
    btDataSourceEl.textContent = prettifyBacktestSource(result.dataSource);

    const startDate = result.startDate || "-";
    const endDate = result.endDate || "-";
    btRangeTextEl.textContent =
        `${startDate} to ${endDate} | Start ${formatCurrency(result.startPrice)} | End ${formatCurrency(result.endPrice)} | Shares ${formatNumber(result.shares)}`;

    drawBacktestCurve(result.history || []);
}

function drawBacktestCurve(history) {
    if (!Array.isArray(history) || history.length < 2) {
        clearBacktestCurve();
        return;
    }

    const values = history.map((point) => Number(point.portfolioValue)).filter((value) => Number.isFinite(value));
    if (values.length < 2) {
        clearBacktestCurve();
        return;
    }

    const rawMin = Math.min(...values);
    const rawMax = Math.max(...values);
    const firstValue = values[0];
    const minVisualRange = Math.max(Math.abs(firstValue) * 0.08, 50);
    let min = rawMin;
    let max = rawMax;
    if ((max - min) < minVisualRange) {
        const center = (max + min) / 2;
        min = center - (minVisualRange / 2);
        max = center + (minVisualRange / 2);
    }

    const pad = (max - min) * 0.08;
    const plotMin = min - pad;
    const plotMax = max + pad;
    const plotRange = Math.max(plotMax - plotMin, 1e-9);
    const chartTop = 2;
    const chartBottom = 34;

    const mapped = values.map((value, index) => {
        const x = (index / (values.length - 1)) * 100;
        const y = chartBottom - ((value - plotMin) / plotRange) * (chartBottom - chartTop);
        return { x, y };
    });

    backtestCurveLine.setAttribute(
        "points",
        mapped.map((point) => `${point.x.toFixed(2)},${point.y.toFixed(2)}`).join(" ")
    );

    const firstPoint = mapped[0];
    const lastPoint = mapped[mapped.length - 1];
    const linePath = mapped.map((point, index) => `${index === 0 ? "M" : "L"}${point.x.toFixed(2)},${point.y.toFixed(2)}`).join(" ");
    const areaPath = `${linePath} L${lastPoint.x.toFixed(2)},${chartBottom} L${firstPoint.x.toFixed(2)},${chartBottom} Z`;
    backtestCurveArea.setAttribute("d", areaPath);

    const baselineY = chartBottom - ((firstValue - plotMin) / plotRange) * (chartBottom - chartTop);
    backtestBaseline.setAttribute("y1", baselineY.toFixed(2));
    backtestBaseline.setAttribute("y2", baselineY.toFixed(2));

    const mid = (plotMax + plotMin) / 2;
    btMaxLabelEl.textContent = formatCurrency(plotMax);
    btMidLabelEl.textContent = formatCurrency(mid);
    btMinLabelEl.textContent = formatCurrency(plotMin);

    btStartLabelEl.textContent = formatShortDate(history[0].date);
    btEndLabelEl.textContent = formatShortDate(history[history.length - 1].date);

    const lines = [chartTop, (chartTop + chartBottom) / 2, chartBottom];
    backtestGrid.innerHTML = lines
        .map((y) => `<line x1="0" y1="${y.toFixed(2)}" x2="100" y2="${y.toFixed(2)}"></line>`)
        .join("");
}

function clearBacktestCurve() {
    backtestCurveLine.setAttribute("points", "");
    backtestCurveArea.setAttribute("d", "");
    backtestBaseline.setAttribute("y1", "18");
    backtestBaseline.setAttribute("y2", "18");
    backtestGrid.innerHTML = "";
    btMaxLabelEl.textContent = "-";
    btMidLabelEl.textContent = "-";
    btMinLabelEl.textContent = "-";
    btStartLabelEl.textContent = "-";
    btEndLabelEl.textContent = "-";
}

function toggleBacktestState(disabled) {
    Array.from(backtestForm.elements).forEach((element) => {
        element.disabled = disabled;
    });
}

function setBacktestFeedback(message, isError) {
    backtestFeedback.textContent = message || "";
    backtestFeedback.classList.toggle("feedback-error", !!message && isError);
    backtestFeedback.classList.toggle("feedback-success", !!message && !isError);
}

function toggleFormState(disabled) {
    Array.from(investmentForm.elements).forEach((element) => {
        element.disabled = disabled;
    });
}

function showLoading(isLoading) {
    loadingSpinner.hidden = !isLoading;
    refreshButton.disabled = isLoading;
}

function setFeedback(message, isError) {
    if (!message) {
        clearFeedback();
        return;
    }
    feedback.textContent = message;
    feedback.classList.toggle("feedback-error", isError);
    feedback.classList.toggle("feedback-success", !isError);
}

function clearFeedback() {
    feedback.textContent = "";
    feedback.classList.remove("feedback-error", "feedback-success");
}

async function buildError(response, fallbackMessage) {
    let message = fallbackMessage;
    try {
        const json = await response.json();
        if (json?.message) {
            message = json.message;
        }
    } catch (_) {
        try {
            const text = await response.text();
            if (text) {
                message = text;
            }
        } catch (_) {
            // no-op
        }
    }
    return new Error(message);
}

function formatCurrency(value, currency = "USD") {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "-";
    }
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: currency || "USD",
        maximumFractionDigits: value >= 1000 ? 0 : 2
    }).format(value);
}

function formatPercentage(value) {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "-";
    }
    return `${(value * 100).toFixed(2)}%`;
}

function formatSignedPercentage(value) {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "-";
    }
    const prefix = value > 0 ? "+" : "";
    return `${prefix}${(value * 100).toFixed(2)}%`;
}

function formatSignedCurrency(value, currency = "USD") {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "-";
    }
    const absolute = formatCurrency(Math.abs(value), currency);
    if (value > 0) {
        return `+${absolute}`;
    }
    if (value < 0) {
        return `-${absolute}`;
    }
    return absolute;
}

function formatNumber(value) {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "-";
    }
    return Number(value).toFixed(2);
}

function formatInteger(value) {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "-";
    }
    return new Intl.NumberFormat("en-US").format(value);
}

function formatTimestamp(value) {
    if (!value) {
        return "-";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "-";
    }
    return date.toLocaleString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function formatShortDate(value) {
    if (!value) {
        return "-";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return String(value);
    }
    return date.toLocaleDateString(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric"
    });
}

function abbreviateCurrency(value, currency) {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "-";
    }
    const abs = Math.abs(value);
    const thresholds = [
        { min: 1e12, suffix: "T" },
        { min: 1e9, suffix: "B" },
        { min: 1e6, suffix: "M" },
        { min: 1e3, suffix: "K" }
    ];
    for (const threshold of thresholds) {
        if (abs >= threshold.min) {
            const shortValue = (value / threshold.min).toFixed(2);
            return `${formatCurrency(Number(shortValue), currency)}${threshold.suffix}`;
        }
    }
    return formatCurrency(value, currency);
}

function trendClass(value) {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return "text-soft";
    }
    if (value > 0) {
        return "trend-up";
    }
    if (value < 0) {
        return "trend-down";
    }
    return "text-soft";
}

function badgeClass(level) {
    switch ((level || "").toLowerCase()) {
        case "low":
            return "badge-low";
        case "moderate":
            return "badge-moderate";
        case "high":
            return "badge-high";
        default:
            return "badge-unknown";
    }
}

function riskHint(level, drawdown) {
    switch ((level || "").toLowerCase()) {
        case "low":
            return "Defensive profile with contained downside.";
        case "moderate":
            return `Balanced risk profile. Worst drawdown: ${formatPercentage(drawdown)}.`;
        case "high":
            return `Aggressive risk profile. Worst drawdown: ${formatPercentage(drawdown)}.`;
        default:
            return "No portfolio level signal yet.";
    }
}

function riskScore(level) {
    switch ((level || "").toLowerCase()) {
        case "low":
            return 0;
        case "moderate":
            return 1;
        case "high":
            return 2;
        default:
            return 3;
    }
}

function numberOrZero(value) {
    return Number.isFinite(value) ? value : 0;
}

function prettifyBacktestSource(value) {
    const source = String(value || "").toUpperCase();
    if (source === "FINNHUB_CANDLE") {
        return "Finnhub Candle";
    }
    if (source === "LOCAL_SNAPSHOTS") {
        return "Local Snapshots";
    }
    return source || "-";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

const defaultBacktestStart = new Date();
defaultBacktestStart.setFullYear(defaultBacktestStart.getFullYear() - 1);
document.getElementById("backtestStartDate").value = defaultBacktestStart.toISOString().slice(0, 10);
document.getElementById("backtestTicker").value = "AAPL";
document.getElementById("backtestAmount").value = "10000";

loadDashboard(true);
