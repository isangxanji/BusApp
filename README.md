# 🚌 Pabama Bus Information System (PabamaApp)
**HCI Term Project: Native Android Mobile Application**

---

## 🌟 Overview
Pabama Bus Info Sys is a native Android application designed to bridge the operational communication gap in regional public utility vehicle (PUV) transit across Northern Mindanao (Cagayan de Oro, Malaybalay, Valencia, and Kibawe corridors).

### Design Realization (From Double Diamond Design Phase)
1. **Regular Bus Trips Only**: Strictly accommodates regular scheduled and rolling daytime public bus operations, avoiding company narrowing or centralization conflicts.
2. **Offline-First Architecture (Room SQLite)**: Operates standalone directly on the phone using Android Room Database. No internet connection, external web server, or background terminal process is required during an exhibit or presentation.
3. **Walk-in Rights Protection**: Displays informational seat availability estimates (`Available Seats`, `Few Seats Left`, `Full Load`, `Standing Room Only`) without locking seats or requiring transactional booking, safeguarding the rights of walk-in commuters.

---

## 📱 Deliverable & How to Run for the Tech Exhibit

### Option 1: Direct APK Installation (Zero-Installation for Evaluators)
The compiled standalone APK is located at:
📁 **`app/build/outputs/apk/debug/app-debug.apk`** (6.2 MB)

- **On Physical Android Phones**: Transfer the `.apk` via USB, Bluetooth, or Google Drive link to any Android smartphone (Android 7.0+). Tap the file to install and open immediately.
- **On Android Emulator**: Drag and drop `app-debug.apk` directly into the Android Studio Emulator window to install it.

### Option 2: Opening in Android Studio
1. Launch **Android Studio**.
2. Click **Open**, and select:
   `C:\Users\James Patrick Rotia\PabamaApp`
3. Click the green **Run (Play)** button in the top toolbar to launch it on your connected phone or virtual device.

### Option 3: Building from Terminal / Antigravity IDE
In the terminal, run:
```powershell
# Run automated JUnit unit tests
.\gradlew.bat testDebugUnitTest

# Rebuild the standalone APK
.\gradlew.bat assembleDebug
```

---

## 🔑 Exhibit Test Credentials (Pre-Seeded)
- **Bus Inspector**: `Juan Dela Cruz` | PIN: **`1234`**
- **Dispatcher**: `Maria Santos` | PIN: **`4321`**

---

## 🎯 HCI Principles & Requirements Realized

| HCI Heuristic (Nielsen) | Native Android Implementation |
| :--- | :--- |
| **1. Visibility of System Status** | Real-time `Sync: Xm ago` indicator in toolbar; automatic `⚠️ Update Pending` chip when records exceed 45 minutes; operational counters (`Available Buses`, `En Route Trips`); descriptive empty-state view in `RecyclerView`. |
| **2. Recognition Rather Than Recall** | Route `From` and `To` dropdown spinners populated with terminal stops (Cagayan de Oro, Malaybalay, Valencia, Kibawe); clear visual status tags (`BOARDING`, `ARRIVED`, `DEPARTED`, `SCHEDULED`). |
| **3. Consistency & Standards** | Material 3 Design; standard `BottomNavigationView` with 3 sections (`Departures`, `Board & Logs`, `Staff Portal`); uniform `MaterialCardView` trip rows. |
| **4. Error Prevention** | **Review & Confirm Interstitial Dialog (`ReviewConfirmDialogFragment`)**: Terminal staff must review Bus #, Plate, Route, Driver Name, License, and Seats before saving any record to the live database. |
| **5. User Control & Freedom** | One-tap `Clear Filters` button; `✏️ Edit Details` option on the review screen allowing staff to correct mistakes before saving. |
