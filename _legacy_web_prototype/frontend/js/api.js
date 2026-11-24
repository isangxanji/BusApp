/**
 * Pabama Bus Info Sys - Client API Client
 * Incorporates Offline Caching and HCI Visibility of System Status
 */

const API_BASE = window.location.origin;

const ApiClient = {
  isOnline: navigator.onLine,

  // Cache helper
  setCache(key, data) {
    try {
      localStorage.setItem(`pabama_cache_${key}`, JSON.stringify({
        data,
        cachedAt: new Date().toISOString()
      }));
    } catch (e) {
      console.warn("Storage quota exceeded or disabled", e);
    }
  },

  getCache(key) {
    try {
      const item = localStorage.getItem(`pabama_cache_${key}`);
      return item ? JSON.parse(item) : null;
    } catch (e) {
      return null;
    }
  },

  async fetchWithCache(endpoint, cacheKey) {
    if (!navigator.onLine) {
      const cached = this.getCache(cacheKey);
      if (cached) {
        return { ...cached.data, _is_cached: true, _cached_at: cached.cachedAt };
      }
      throw new Error("You are offline and no cached data is available.");
    }

    try {
      const res = await fetch(`${API_BASE}${endpoint}`);
      if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
      const data = await res.json();
      this.setCache(cacheKey, data);
      return data;
    } catch (err) {
      const cached = this.getCache(cacheKey);
      if (cached) {
        return { ...cached.data, _is_cached: true, _cached_at: cached.cachedAt };
      }
      throw err;
    }
  },

  // 1. Passenger Public API
  async getDashboard() {
    return this.fetchWithCache("/api/v1/dashboard", "dashboard");
  },

  async getRoutes() {
    return this.fetchWithCache("/api/v1/routes", "routes");
  },

  async getTrips({ origin, destination, busType, status } = {}) {
    const params = new URLSearchParams();
    if (origin) params.append("origin", origin);
    if (destination) params.append("destination", destination);
    if (busType && busType !== "ALL") params.append("bus_type", busType);
    if (status) params.append("status", status);

    const query = params.toString() ? `?${params.toString()}` : "";
    return this.fetchWithCache(`/api/v1/trips${query}`, `trips_${query}`);
  },

  async getUpdates() {
    return this.fetchWithCache("/api/v1/trips/updates", "updates");
  },

  // 2. Staff Authentication & Logging
  async getStaffRoster() {
    return this.fetchWithCache("/api/v1/staff/roster", "staff_roster");
  },

  async getBusesForStaff() {
    return this.fetchWithCache("/api/v1/staff/buses", "staff_buses");
  },

  async loginStaff(fullName, pin) {
    const res = await fetch(`${API_BASE}/api/v1/staff/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ full_name: fullName, pin: pin })
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || "Staff login failed. Check your PIN.");
    }
    const data = await res.json();
    sessionStorage.setItem("pabama_staff_session", JSON.stringify(data));
    return data;
  },

  async registerStaff(fullName, role, contactInfo, pin) {
    const res = await fetch(`${API_BASE}/api/v1/staff/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        full_name: fullName,
        role: role,
        contact_info: contactInfo,
        pin: pin
      })
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || "Staff registration failed.");
    }
    const data = await res.json();
    sessionStorage.setItem("pabama_staff_session", JSON.stringify(data));
    return data;
  },

  getSavedStaffSession() {
    try {
      const item = sessionStorage.getItem("pabama_staff_session");
      return item ? JSON.parse(item) : null;
    } catch (e) {
      return null;
    }
  },

  logoutStaff() {
    sessionStorage.removeItem("pabama_staff_session");
  },

  async recordMovement(payload) {
    const res = await fetch(`${API_BASE}/api/v1/staff/movements`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || "Failed to save movement record.");
    }
    return res.json();
  },

  async postAnnouncement(payload) {
    const res = await fetch(`${API_BASE}/api/v1/staff/announcements`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || "Failed to broadcast announcement.");
    }
    return res.json();
  }
};
