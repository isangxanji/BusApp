/**
 * Pabama Bus Info Sys - Main Mobile Application Controller
 * Realizes HCI Principles (Visibility, Error Prevention, User Control, Recognition over Recall)
 */

document.addEventListener("DOMContentLoaded", () => {
  // Application State
  const state = {
    currentTab: "buses", // 'buses' | 'updates' | 'staff'
    selectedBusType: "ALL", // 'ALL' | 'REGULAR' | '24_HOUR'
    selectedOrigin: "",
    selectedDestination: "",
    staffSession: ApiClient.getSavedStaffSession(),
    pendingMovementData: null,
    routes: [],
    busesList: []
  };

  // DOM Elements
  const elStatusChip = document.getElementById("status-chip");
  const elLastSync = document.getElementById("last-sync-time");
  const elStaleAlert = document.getElementById("stale-alert");

  // Tab Containers
  const tabBuses = document.getElementById("tab-buses");
  const tabUpdates = document.getElementById("tab-updates");
  const tabStaff = document.getElementById("tab-staff");

  // Nav Buttons
  const navBtns = document.querySelectorAll(".nav-item");

  // Filter Elements
  const selOrigin = document.getElementById("filter-origin");
  const selDest = document.getElementById("filter-destination");
  const btnResetFilter = document.getElementById("btn-reset-filter");
  const busTypeBtns = document.querySelectorAll(".bus-type-btn");
  const tripsContainer = document.getElementById("trips-container");

  // Updates & Metrics Elements
  const metricTotal = document.getElementById("metric-total-available");
  const metricRegular = document.getElementById("metric-regular");
  const metric24h = document.getElementById("metric-24h");
  const announcementsList = document.getElementById("announcements-list");
  const updatesFeed = document.getElementById("updates-feed");

  // Staff Elements
  const staffAuthContainer = document.getElementById("staff-auth-container");
  const staffDashboardContainer = document.getElementById("staff-dashboard-container");
  const staffNameSelect = document.getElementById("staff-name-select");
  const staffPinInput = document.getElementById("staff-pin-input");
  const btnStaffLogin = document.getElementById("btn-staff-login");
  const btnShowRegister = document.getElementById("btn-show-register");
  const staffProfileBadge = document.getElementById("staff-profile-badge");
  const btnStaffLogout = document.getElementById("btn-staff-logout");

  // Staff Movement Form Elements
  const formStaffMovement = document.getElementById("form-staff-movement");
  const staffBusSelect = document.getElementById("staff-bus-select");
  const staffMovementType = document.getElementById("staff-movement-type");
  const staffRouteSelect = document.getElementById("staff-route-select");
  const staffDriverName = document.getElementById("staff-driver-name");
  const staffDriverLicense = document.getElementById("staff-driver-license");
  const staffSeatStatus = document.getElementById("staff-seat-status");
  const staffNotes = document.getElementById("staff-notes");

  // Review & Confirm Modal (HCI Error Prevention)
  const modalReview = document.getElementById("modal-review");
  const reviewBus = document.getElementById("rev-bus");
  const reviewType = document.getElementById("rev-type");
  const reviewRoute = document.getElementById("rev-route");
  const reviewDriver = document.getElementById("rev-driver");
  const reviewLicense = document.getElementById("rev-license");
  const reviewSeats = document.getElementById("rev-seats");
  const btnEditAgain = document.getElementById("btn-review-edit");
  const btnConfirmSave = document.getElementById("btn-review-confirm");

  // Register Modal
  const modalRegister = document.getElementById("modal-register");
  const btnCloseRegister = document.getElementById("btn-close-register");
  const formRegister = document.getElementById("form-register");

  // 1. Navigation Controller
  function switchTab(targetTab) {
    state.currentTab = targetTab;
    navBtns.forEach(btn => {
      btn.classList.toggle("active", btn.dataset.tab === targetTab);
    });

    tabBuses.style.display = targetTab === "buses" ? "flex" : "none";
    tabUpdates.style.display = targetTab === "updates" ? "flex" : "none";
    tabStaff.style.display = targetTab === "staff" ? "flex" : "none";

    if (targetTab === "buses") loadTrips();
    if (targetTab === "updates") loadDashboardAndUpdates();
    if (targetTab === "staff") renderStaffSection();
  }

  navBtns.forEach(btn => {
    btn.addEventListener("click", () => switchTab(btn.dataset.tab));
  });

  // 2. Network Connectivity Monitoring
  function updateNetworkStatus() {
    if (navigator.onLine) {
      elStatusChip.className = "online-chip";
      elStatusChip.innerHTML = '<span class="pulse-dot"></span> Online';
    } else {
      elStatusChip.className = "online-chip offline";
      elStatusChip.innerHTML = '<span class="pulse-dot offline"></span> Offline Mode';
    }
  }
  window.addEventListener("online", updateNetworkStatus);
  window.addEventListener("offline", updateNetworkStatus);
  updateNetworkStatus();

  // 3. Populate Routes & Filter Options
  async function initRoutes() {
    try {
      const data = await ApiClient.getRoutes();
      state.routes = data.routes || [];

      // Populate Passenger Dropdowns
      selOrigin.innerHTML = '<option value="">All Starting Points</option>';
      data.origins.forEach(orig => {
        selOrigin.innerHTML += `<option value="${orig}">${orig}</option>`;
      });

      selDest.innerHTML = '<option value="">All Destinations</option>';
      data.destinations.forEach(dest => {
        selDest.innerHTML += `<option value="${dest}">${dest}</option>`;
      });

      // Populate Staff Route Selector
      staffRouteSelect.innerHTML = '<option value="">-- Select Assigned Route --</option>';
      data.routes.forEach(r => {
        staffRouteSelect.innerHTML += `<option value="${r.id}">${r.origin} ➔ ${r.destination}</option>`;
      });
    } catch (err) {
      console.error("Could not load routes:", err);
    }
  }

  // 4. Passenger Trip Query & Feed (Wireframes 1 & 2)
  async function loadTrips() {
    tripsContainer.innerHTML = '<div style="text-align:center; padding: 20px; color:#64748b; font-size: 0.8rem;">Loading active fleet schedules...</div>';
    try {
      const res = await ApiClient.getTrips({
        origin: state.selectedOrigin,
        destination: state.selectedDestination,
        busType: state.selectedBusType
      });

      if (!res.trips || res.trips.length === 0) {
        tripsContainer.innerHTML = `
          <div class="empty-state">
            <div class="empty-icon">🚌</div>
            <div class="empty-title">No Buses Found</div>
            <div class="empty-desc">
              ${state.selectedBusType === "24_HOUR" 
                ? "No 24-Hour buses currently logged at this terminal. Late-night service operates based on incoming handoffs."
                : "No active trips match your route filter right now. Inquire with terminal dispatch for incoming departures."}
            </div>
            <button class="btn-secondary" id="btn-empty-reset" style="margin-top: 8px; font-size: 0.75rem; padding: 6px 12px;">Show All Buses</button>
          </div>
        `;
        document.getElementById("btn-empty-reset")?.addEventListener("click", resetFilters);
        return;
      }

      tripsContainer.innerHTML = res.trips.map(trip => {
        const is24h = trip.bus_type === "24_HOUR";
        const badgeClass = is24h ? "badge-24hour" : "badge-regular";
        const badgeText = is24h ? "🌙 24-Hour Bus (Night Service)" : "☀️ Regular Bus (Until 8PM)";

        let statusClass = "status-scheduled";
        if (trip.status === "BOARDING") statusClass = "status-boarding";
        if (trip.status === "ARRIVED") statusClass = "status-arrived";
        if (trip.status === "DEPARTED") statusClass = "status-departed";

        const updateAgoText = trip.minutes_since_update === 0 
          ? "Just now" 
          : `${trip.minutes_since_update}m ago`;

        return `
          <div class="trip-card">
            <div class="trip-header">
              <div class="bus-title">
                <span>🚌</span>
                <span>${trip.bus_number}</span>
                <span style="font-size: 0.72rem; color: #64748b; font-weight: normal;">(${trip.plate_number})</span>
              </div>
              <span class="badge ${badgeClass}">${badgeText}</span>
            </div>

            <div class="trip-route">
              <span>${trip.origin}</span>
              <span class="route-arrow">➔</span>
              <span>${trip.destination}</span>
            </div>

            <div class="trip-meta">
              <div>Status: <span class="trip-status-tag ${statusClass}">${trip.status}</span></div>
              <div>Seats: <strong>${formatSeatEstimate(trip.seat_status)}</strong></div>
              <div>Driver: <span>${trip.driver_name}</span></div>
              <div>Updated: <span>${updateAgoText}</span></div>
            </div>
          </div>
        `;
      }).join("");

    } catch (err) {
      tripsContainer.innerHTML = `
        <div class="empty-state">
          <div class="empty-icon">⚠️</div>
          <div class="empty-title">Connection Error</div>
          <div class="empty-desc">${err.message}</div>
          <button class="btn-primary" onclick="window.location.reload()" style="margin-top: 8px; font-size: 0.75rem;">Retry Sync</button>
        </div>
      `;
    }
  }

  function formatSeatEstimate(status) {
    if (status === "AVAILABLE") return "Available";
    if (status === "FEW_SEATS") return "Few Seats Left";
    if (status === "FULL_LOAD") return "Full Load";
    if (status === "STANDING_ONLY") return "Standing Only";
    return status;
  }

  function resetFilters() {
    state.selectedOrigin = "";
    state.selectedDestination = "";
    state.selectedBusType = "ALL";
    selOrigin.value = "";
    selDest.value = "";
    busTypeBtns.forEach(btn => btn.classList.toggle("active", btn.dataset.type === "ALL"));
    loadTrips();
  }

  // Filter Event Listeners
  selOrigin.addEventListener("change", (e) => {
    state.selectedOrigin = e.target.value;
    loadTrips();
  });

  selDest.addEventListener("change", (e) => {
    state.selectedDestination = e.target.value;
    loadTrips();
  });

  btnResetFilter.addEventListener("click", resetFilters);

  busTypeBtns.forEach(btn => {
    btn.addEventListener("click", () => {
      busTypeBtns.forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      state.selectedBusType = btn.dataset.type;
      loadTrips();
    });
  });

  // 5. Dashboard Summary & Recent Movement Updates (Wireframe 3)
  async function loadDashboardAndUpdates() {
    try {
      const summary = await ApiClient.getDashboard();

      // Top Counters
      metricTotal.textContent = summary.available_buses_count;
      metricRegular.textContent = summary.regular_buses_count;
      metric24h.textContent = summary.twenty_four_hour_count;

      // Freshness Status
      const syncAgo = summary.last_sync_minutes_ago === 0 ? "Just now" : `${summary.last_sync_minutes_ago}m ago`;
      elLastSync.textContent = `Sync: ${syncAgo}`;

      if (summary.is_stale_warning) {
        elStaleAlert.style.display = "block";
      } else {
        elStaleAlert.style.display = "none";
      }

      // Announcements (Digital Megaphone)
      if (summary.announcements && summary.announcements.length > 0) {
        announcementsList.innerHTML = summary.announcements.map(a => `
          <div class="announcement-box" style="margin-bottom: 8px;">
            <div class="announcement-icon">📢</div>
            <div class="announcement-text">
              <h4>${a.title}</h4>
              <p>${a.content}</p>
              <span style="font-size: 0.65rem; color: #92400e; margin-top: 4px; display: block;">— ${a.created_by}</span>
            </div>
          </div>
        `).join("");
      } else {
        announcementsList.innerHTML = `<div style="font-size: 0.75rem; color:#64748b;">No active terminal bulletins.</div>`;
      }

      // Chronological Updates Feed
      const updatesData = await ApiClient.getUpdates();
      if (updatesData.updates && updatesData.updates.length > 0) {
        updatesFeed.innerHTML = updatesData.updates.map(u => {
          const isArr = u.movement_type === "ARRIVAL";
          const icon = isArr ? "📥" : "📤";
          const tagColor = isArr ? "#059669" : "#2563eb";

          return `
            <div class="update-item">
              <div class="update-left">
                <span class="update-time-badge">${u.recorded_time_str}</span>
                <div>
                  <strong style="color: ${tagColor}">${icon} ${u.bus_number} ${u.movement_type}</strong>
                  <div style="font-size: 0.68rem; color: #64748b;">${u.notes || u.recorded_by}</div>
                </div>
              </div>
              <div style="font-size: 0.68rem; color: #94a3b8; font-weight: 600;">
                ${u.minutes_ago === 0 ? "Now" : `${u.minutes_ago}m ago`}
              </div>
            </div>
          `;
        }).join("");
      } else {
        updatesFeed.innerHTML = `<div style="font-size: 0.75rem; color:#64748b;">No recent movement logs today.</div>`;
      }

    } catch (err) {
      console.error("Dashboard update failed:", err);
    }
  }

  // 6. Staff Interface Management
  async function renderStaffSection() {
    state.staffSession = ApiClient.getSavedStaffSession();
    if (!state.staffSession) {
      staffAuthContainer.style.display = "flex";
      staffDashboardContainer.style.display = "none";
      loadStaffRoster();
    } else {
      staffAuthContainer.style.display = "none";
      staffDashboardContainer.style.display = "flex";
      staffProfileBadge.textContent = `${state.staffSession.staff.role}: ${state.staffSession.staff.full_name}`;
      loadStaffBuses();
    }
  }

  async function loadStaffRoster() {
    try {
      const res = await ApiClient.getStaffRoster();
      staffNameSelect.innerHTML = '<option value="">-- Choose Name from Roster --</option>';
      res.staff.forEach(s => {
        staffNameSelect.innerHTML += `<option value="${s.full_name}">${s.role}: ${s.full_name}</option>`;
      });
    } catch (e) {
      console.warn("Could not load staff roster", e);
    }
  }

  async function loadStaffBuses() {
    try {
      const res = await ApiClient.getBusesForStaff();
      state.busesList = res.buses || [];
      staffBusSelect.innerHTML = '<option value="">-- Select Bus --</option>';
      res.buses.forEach(b => {
        const typeEmoji = b.bus_type === "24_HOUR" ? "🌙" : "☀️";
        staffBusSelect.innerHTML += `
          <option value="${b.id}" data-type="${b.bus_type}">
            ${typeEmoji} ${b.bus_number} (${b.plate_number}) - Currently: ${b.current_status}
          </option>
        `;
      });
    } catch (e) {
      console.warn("Could not load buses for staff", e);
    }
  }

  // Staff Login
  btnStaffLogin.addEventListener("click", async () => {
    const name = staffNameSelect.value;
    const pin = staffPinInput.value.trim();

    if (!name || !pin) {
      alert("Please select your name and enter your 4-digit PIN.");
      return;
    }

    try {
      await ApiClient.loginStaff(name, pin);
      staffPinInput.value = "";
      renderStaffSection();
    } catch (err) {
      alert(err.message);
    }
  });

  // Staff Logout
  btnStaffLogout.addEventListener("click", () => {
    ApiClient.logoutStaff();
    renderStaffSection();
  });

  // Register Modal Handlers
  btnShowRegister.addEventListener("click", () => {
    modalRegister.style.display = "flex";
  });
  btnCloseRegister.addEventListener("click", () => {
    modalRegister.style.display = "none";
  });

  formRegister.addEventListener("submit", async (e) => {
    e.preventDefault();
    const name = document.getElementById("reg-name").value.trim();
    const role = document.getElementById("reg-role").value;
    const contact = document.getElementById("reg-contact").value.trim();
    const pin = document.getElementById("reg-pin").value.trim();

    if (pin.length < 4 || !/^\d+$/.test(pin)) {
      alert("PIN must be 4 to 6 numbers.");
      return;
    }

    try {
      await ApiClient.registerStaff(name, role, contact, pin);
      modalRegister.style.display = "none";
      alert(`Welcome, ${name}! You are registered.`);
      renderStaffSection();
    } catch (err) {
      alert(err.message);
    }
  });

  // 7. HCI Heuristic #4: Error Prevention with Review Interstitial Modal
  formStaffMovement.addEventListener("submit", (e) => {
    e.preventDefault();
    const busId = parseInt(staffBusSelect.value);
    const routeId = parseInt(staffRouteSelect.value);
    const movType = staffMovementType.value;
    const driver = staffDriverName.value.trim();
    const license = staffDriverLicense.value.trim();
    const seats = staffSeatStatus.value;
    const notes = staffNotes.value.trim();

    if (!busId || !routeId || !driver || !license) {
      alert("Please fill in all required operational fields.");
      return;
    }

    const selectedBus = state.busesList.find(b => b.id === busId);
    const selectedRoute = state.routes.find(r => r.id === routeId);

    state.pendingMovementData = {
      staff_id: state.staffSession.staff.id,
      bus_id: busId,
      route_id: routeId,
      movement_type: movType,
      driver_name: driver,
      driver_license: license,
      seat_status: seats,
      notes: notes
    };

    // Populate Review Screen
    reviewBus.textContent = `${selectedBus ? selectedBus.bus_number : busId} (${selectedBus ? selectedBus.plate_number : ''})`;
    reviewType.textContent = movType;
    reviewType.style.color = movType === "ARRIVAL" ? "#059669" : "#2563eb";
    reviewRoute.textContent = selectedRoute ? `${selectedRoute.origin} ➔ ${selectedRoute.destination}` : routeId;
    reviewDriver.textContent = driver;
    reviewLicense.textContent = license;
    reviewSeats.textContent = formatSeatEstimate(seats);

    // Show Interstitial Modal
    modalReview.style.display = "flex";
  });

  btnEditAgain.addEventListener("click", () => {
    modalReview.style.display = "none";
  });

  btnConfirmSave.addEventListener("click", async () => {
    if (!state.pendingMovementData) return;
    btnConfirmSave.textContent = "Broadcasting...";
    btnConfirmSave.disabled = true;

    try {
      await ApiClient.recordMovement(state.pendingMovementData);
      modalReview.style.display = "none";
      alert("Movement successfully logged & broadcast to commuters!");
      formStaffMovement.reset();
      loadStaffBuses();
      loadDashboardAndUpdates();
    } catch (err) {
      alert("Error saving record: " + err.message);
    } finally {
      btnConfirmSave.textContent = "Confirm & Broadcast";
      btnConfirmSave.disabled = false;
    }
  });

  // Initial Startup
  initRoutes().then(() => {
    loadTrips();
    loadDashboardAndUpdates();
  });
});
