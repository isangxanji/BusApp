package com.pabama.businfo.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "staff_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_STAFF_ID = "staff_id"
        private const val KEY_STAFF_NAME = "staff_name"
        private const val KEY_STAFF_ROLE = "staff_role"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context).also { INSTANCE = it }
            }
        }
    }

    fun saveSession(staffId: String, name: String, role: String = "STAFF") {
        val calculatedRole = if (staffId.startsWith("ADMIN", ignoreCase = true) || role.equals("ADMIN", ignoreCase = true)) {
            "ADMIN"
        } else {
            "STAFF"
        }
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_STAFF_ID, staffId)
            putString(KEY_STAFF_NAME, name)
            putString(KEY_STAFF_ROLE, calculatedRole)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getStaffId(): String? = prefs.getString(KEY_STAFF_ID, null)

    fun getStaffName(): String? = prefs.getString(KEY_STAFF_NAME, null)

    fun getRole(): String = prefs.getString(KEY_STAFF_ROLE, "STAFF") ?: "STAFF"

    fun isAdmin(): Boolean {
        val id = getStaffId() ?: ""
        return getRole().equals("ADMIN", ignoreCase = true) || id.startsWith("ADMIN", ignoreCase = true)
    }
}
