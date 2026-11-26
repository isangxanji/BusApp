# Transferable Implementation Plan & System Specification

> **For the AI Agent Reading This File:**
> You are acting as the implementation agent. Your objective is to inspect the target Android project, compare it against this specification, identify missing components, and implement or refactor code to achieve parity with the architecture detailed below. Execute each section methodically, respecting all constraints.

---

## 1. System Architecture & Constraints

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  Activities (Day/Night Theme, Vector Icons, Dynamic Badges) │
└──────────────┬──────────────────────────────▲───────────────┘
               │ ViewModels                   │ LiveData/Flow
┌──────────────▼──────────────────────────────┴───────────────┐
│                    Repository Layer                         │
│           BusRepository (Single Source of Truth)            │
└──────────────┬──────────────────────────────▲───────────────┘
               │ Write-Through                │ Real-Time Sync
┌──────────────▼──────────────┐┌──────────────┴───────────────┐
│     Local SQLite Cache      ││     Cloud Remote Backend     │
│   Room Database (Offline)   ││ Firebase Firestore (Spark)   │
└─────────────────────────────┘└──────────────────────────────┘
```

### Strict Compliance Constraints
1. **Zero Test Directories**: Do NOT create `app/src/test` or `app/src/androidTest`. If present, remove them.
2. **File Types**: Deliverables must strictly be standard Android `.apk` (no `.bin` or renamed files).
3. **No Legacy Brand Strings**: Do not introduce legacy bus brand names (e.g., "Pabama", "RTMI") into user-facing layout strings or dialog titles.
4. **Offline First**: The app must remain 100% operational offline using Room SQLite, syncing automatically with Firebase Firestore when network connectivity is available.

---

## 2. Dependencies & Build Configuration

### 2.1 Project Root `build.gradle.kts`
Ensure the Google Services Gradle plugin is registered:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
```

### 2.2 App Module `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.pabama.businfo" // Adapt to target namespace
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)

    // Room SQLite Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Lifecycle & Coroutines
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Firebase BoM (Spark Free Tier)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
}
```

---

## 3. Data Layer: Room Schema & Database Normalization

### 3.1 Entities (`data/entity/Entities.kt`)
Data models must contain `lastUpdatedAt: Long` for conflict resolution and two-way sync deduplication:

```kotlin
package com.pabama.businfo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buses")
data class BusEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plateNumber: String,
    val destination: String = "",
    val busType: String = "Regular Bus",
    val routeDescription: String = "",
    val status: String = "AVAILABLE", // AVAILABLE, EN ROUTE, COMPLETED, UNAVAILABLE
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseId: String = "",
    val busPlate: String,
    val route: String,
    val status: String = "AVAILABLE", // AVAILABLE, EN ROUTE, COMPLETED, UNAVAILABLE
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "movement_logs")
data class MovementLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val movementType: String, // "Arrival" or "Departure"
    val busPlate: String,
    val terminal: String = "",
    val originOrDestination: String,
    val movementTime: String, // e.g. "10:30 AM"
    val notes: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val staffId: String,
    val password: String,
    val name: String,
    val isActive: Boolean = true,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

// String extension to sanitize title case without breaking arrow tokens
fun String.capitalizeWords(): String {
    if (isBlank()) return this
    return this.split(" ").joinToString(" ") { word ->
        if (word.isEmpty()) {
            word
        } else if (word.contains("→")) {
            word.split("→").joinToString(" to ") { it.trim().capitalizeWords() }
        } else if (word.contains("->")) {
            word.split("->").joinToString(" to ") { it.trim().capitalizeWords() }
        } else {
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
        }
    }
}
```

### 3.2 DAOs
Define `BusDao`, `TripDao`, `MovementLogDao`, and `StaffDao` with:
- `OnConflictStrategy.REPLACE` on `@Insert` and `@Update`.
- Synchronous (`Sync`) methods for background threads and `LiveData<List<T>>` for UI observation.
- Queries matching canonical identifiers: `getBusByPlate(plate: String)`, `getStaffById(staffId: String)`.

---

## 4. Remote Layer: Firebase Spark Firestore Synchronization

### 4.1 Architecture (`data/remote/FirebaseSyncManager.kt`)
* **Collections**: `buses`, `trips`, `movement_logs`, `staff`.
* **Canonical Document IDs**:
  * Bus: Document ID = `cleanPlate` (e.g., `KBA-1010`).
  * Trip: Document ID = `cleanPlate_timestamp` or deterministic hash.
  * Movement Log: Document ID = `cleanPlate_timestamp`.
  * Staff: Document ID = `cleanStaffId` (e.g., `STAFF001`, `ADMIN001`).
* **Loop Prevention**: Compare `remote.lastUpdatedAt` against `local.lastUpdatedAt`. Only overwrite local cache if remote timestamp is strictly newer.
* **Write-Through**: When user modifies local database via Repository, write to Room SQLite immediately, then push asynchronous `set(entity, SetOptions.merge())` to Firestore.

---

## 5. UI Theme & Dynamic Day/Night Mode Alignment

### 5.1 Color Tokens Specification

#### Light Mode (`app/src/main/res/values/colors.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Background & Surface for Light Mode -->
    <color name="background">#F8FAFC</color>
    <color name="surface">#FFFFFF</color>
    <color name="card_inner">#F1F5F9</color>
    
    <!-- Primary Blue & Variations -->
    <color name="primary">#1E3A8A</color>
    <color name="primary_dark">#172554</color>
    <color name="primary_light">#3B82F6</color>
    
    <!-- Secondary & Accent Blues -->
    <color name="secondary">#2563EB</color>
    <color name="secondary_dark">#1D4ED8</color>
    <color name="accent">#1E3A8A</color>
    <color name="accent_light">#DBEAFE</color>
    
    <!-- Typography in Deep Navy & Slate -->
    <color name="text_primary">#0F172A</color>
    <color name="text_secondary">#475569</color>
    
    <!-- Borders -->
    <color name="border">#E2E8F0</color>
    <color name="border_color">#93C5FD</color>
    <color name="white">#FFFFFF</color>

    <!-- Operational Status Colors -->
    <color name="status_available">#15803D</color>
    <color name="status_en_route">#2563EB</color>
    <color name="status_completed">#0284C7</color>
    <color name="status_departed">#64748B</color>
    <color name="status_unavailable">#DC2626</color>

    <!-- Dialog Colors: Light mode dialog with crisp dark text -->
    <color name="dialog_background">#FFFFFF</color>
    <color name="dialog_text">#0F172A</color>
    <color name="dialog_hint">#64748B</color>
    <color name="dialog_action">#1E3A8A</color>
    <color name="dialog_delete">#DC2626</color>
</resources>
```

#### Night Mode (`app/src/main/res/values-night/colors.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Background & Surface for Night Mode -->
    <color name="background">#0B1120</color>
    <color name="surface">#1E293B</color>
    <color name="card_inner">#0F172A</color>

    <!-- Primary Blues for Night -->
    <color name="primary">#3B82F6</color>
    <color name="primary_dark">#60A5FA</color>
    <color name="primary_light">#93C5FD</color>

    <!-- Secondary & Accent Blues -->
    <color name="secondary">#60A5FA</color>
    <color name="secondary_dark">#3B82F6</color>
    <color name="accent">#60A5FA</color>
    <color name="accent_light">#1E293B</color>

    <!-- Typography for Night Mode -->
    <color name="text_primary">#F8FAFC</color>
    <color name="text_secondary">#94A3B8</color>

    <!-- Borders -->
    <color name="border">#334155</color>
    <color name="border_color">#1E3A8A</color>
    <color name="white">#FFFFFF</color>

    <!-- Operational Status Colors for Night Mode -->
    <color name="status_available">#22C55E</color>
    <color name="status_en_route">#60A5FA</color>
    <color name="status_completed">#38BDF8</color>
    <color name="status_departed">#94A3B8</color>
    <color name="status_unavailable">#EF4444</color>

    <!-- Dialog Colors for Night Mode: Dark slate dialog with crisp white text -->
    <color name="dialog_background">#1E293B</color>
    <color name="dialog_text">#F8FAFC</color>
    <color name="dialog_hint">#94A3B8</color>
    <color name="dialog_action">#60A5FA</color>
    <color name="dialog_delete">#F87171</color>
</resources>
```

### 5.2 Theme Configurations (`app/src/main/res/values/themes.xml`)
Connect both Material3 and AppCompat dialog themes to prevent unreadable dark-on-dark text:

```xml
<resources xmlns:tools="http://schemas.android.com/tools">

    <style name="Theme.PabamaApp" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryVariant">@color/primary_dark</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/secondary</item>
        <item name="colorOnSecondary">@color/white</item>
        <item name="colorAccent">@color/primary</item>
        <item name="colorControlActivated">@color/primary</item>
        <item name="android:statusBarColor">@color/primary_dark</item>
        <item name="android:windowBackground">@color/background</item>

        <!-- Instant Navigation Feel -->
        <item name="android:windowAnimationStyle">@style/NoActivityAnimation</item>

        <!-- Dialog Theme Bridges -->
        <item name="alertDialogTheme">@style/Theme.PabamaApp.Dialog</item>
        <item name="android:alertDialogTheme">@style/Theme.PabamaApp.Dialog</item>
        <item name="materialAlertDialogTheme">@style/Theme.PabamaApp.Dialog</item>
    </style>

    <style name="Theme.PabamaApp.Dialog" parent="ThemeOverlay.AppCompat.Dialog.Alert">
        <item name="android:windowAnimationStyle">@style/NoActivityAnimation</item>
        <item name="colorSurface">@color/dialog_background</item>
        <item name="colorAccent">@color/dialog_action</item>
        <item name="android:colorAccent">@color/dialog_action</item>
        <item name="colorControlActivated">@color/dialog_action</item>
        <item name="colorControlNormal">@color/dialog_hint</item>

        <item name="android:colorBackground">@color/dialog_background</item>
        <item name="android:textColorPrimary">@color/dialog_text</item>
        <item name="android:textColorSecondary">@color/dialog_hint</item>
        <item name="android:textColorHint">@color/dialog_hint</item>
        <item name="editTextColor">@color/dialog_text</item>
        <item name="android:editTextColor">@color/dialog_text</item>
        <item name="textColorAlertDialogListItem">@color/dialog_text</item>

        <item name="buttonBarPositiveButtonStyle">@style/DialogActionButton</item>
        <item name="buttonBarNegativeButtonStyle">@style/DialogCancelButton</item>
        <item name="buttonBarNeutralButtonStyle">@style/DialogDeleteButton</item>
    </style>

    <style name="DialogActionButton" parent="Widget.AppCompat.Button.ButtonBar.AlertDialog">
        <item name="android:textColor">@color/dialog_action</item>
        <item name="android:textStyle">bold</item>
    </style>

    <style name="DialogCancelButton" parent="Widget.AppCompat.Button.ButtonBar.AlertDialog">
        <item name="android:textColor">@color/dialog_text</item>
    </style>

    <style name="DialogDeleteButton" parent="Widget.AppCompat.Button.ButtonBar.AlertDialog">
        <item name="android:textColor">@color/dialog_delete</item>
        <item name="android:textStyle">bold</item>
    </style>

    <style name="NoActivityAnimation">
        <item name="android:activityOpenEnterAnimation">@null</item>
        <item name="android:activityOpenExitAnimation">@null</item>
        <item name="android:activityCloseEnterAnimation">@null</item>
        <item name="android:activityCloseExitAnimation">@null</item>
    </style>
</resources>
```

---

## 6. OEM Font Glyphs Hardening & Icon Assets

### 6.1 Vector Chevron Right (`app/src/main/res/drawable/ic_chevron_right.xml`)
Eliminates `'n` / `’n` glyph rendering artifacts on customized OEM fonts:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/text_secondary"
        android:pathData="M8.59,16.59L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.59Z" />
</vector>
```

### 6.2 Action Row Background (`app/src/main/res/drawable/bg_action_row.xml`)
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/card_inner" />
    <corners android:radius="12dp" />
    <stroke
        android:width="1dp"
        android:color="@color/border" />
</shape>
```

### 6.3 Glyph Replacement Rules Across Code & Layouts
1. **Action Rows**: Replace `<TextView android:text="→" .../>` with `<ImageView android:layout_width="20dp" android:layout_height="20dp" android:src="@drawable/ic_chevron_right" android:contentDescription="Open" />`.
2. **Back Buttons**: Replace `← Back` with `< Back` or `&lt; Back`.
3. **Route Waypoints**: Replace `${stops.first()} → ${stops.last()}` with `${stops.first()} to ${stops.last()}`.
4. **Parsing Robustness**: In string splits, always parse all variations: `.split("→", "->", "—", "to", "To")`.

---

## 7. UI Components & Programmatic Dialog Standards

When creating programmatic dialogs in Kotlin (`AdminDashboardActivity`, `ManageBusActivity`, `ManageTripsActivity`), always bind dynamic colors to programmatically added `EditText` views:

```kotlin
val inputField = EditText(this).apply {
    hint = "Placeholder"
    setTextColor(getColor(R.color.dialog_text))
    setHintTextColor(getColor(R.color.dialog_hint))
    setSingleLine(true)
}
```

### Primary Action Buttons Contrast
On primary filled buttons (e.g., `btnAddBus`, `btnAddTrip`, `btnSaveMovement`):
* Always specify `android:textColor="@color/white"` instead of `@color/surface`. (In Dark Mode, `@color/surface` is dark slate `#1E293B`, creating poor contrast against `@color/primary`).

---

## 8. Migration Verification Checklist

When executing this plan on a target project, the AI agent must verify:
- [ ] No `app/src/test` or `app/src/androidTest` directories exist.
- [ ] `build.gradle.kts` has Google Services and Firebase BoM configured.
- [ ] `values/colors.xml` and `values-night/colors.xml` have matching color keys with proper day/night contrast.
- [ ] `themes.xml` contains the dialog theme bridges (`alertDialogTheme`, `android:alertDialogTheme`, `materialAlertDialogTheme`).
- [ ] `ic_chevron_right.xml` and `bg_action_row.xml` exist in `res/drawable/`.
- [ ] No raw Unicode `→` or `←` characters remain in user-facing text attributes in layouts.
- [ ] Programmatic dialog inputs set explicit `dialog_text` and `dialog_hint` colors.
- [ ] App compiles cleanly with `gradlew.bat assembleDebug`.
- [ ] Deliverable outputs to `<AppName>.apk` in root without creating `.bin` files.
