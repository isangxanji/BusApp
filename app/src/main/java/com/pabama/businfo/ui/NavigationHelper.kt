package com.pabama.businfo.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.widget.TextView
import com.pabama.businfo.R

object NavigationHelper {

    fun setupPassengerBottomNav(activity: Activity, currentScreen: String) {
        val btnHome = activity.findViewById<TextView>(R.id.btnHome) ?: return
        val btnBuses = activity.findViewById<TextView>(R.id.btnBuses) ?: return
        val btnUpdates = activity.findViewById<TextView>(R.id.btnUpdates) ?: return
        val btnAbout = activity.findViewById<TextView>(R.id.btnAbout) ?: return

        val inactiveColor = activity.getColor(R.color.text_secondary)
        val activeColor = activity.getColor(R.color.accent)

        // Reset all to inactive state first
        listOf(btnHome, btnBuses, btnUpdates, btnAbout).forEach { btn ->
            btn.setTextColor(inactiveColor)
            btn.compoundDrawableTintList = ColorStateList.valueOf(inactiveColor)
            btn.setTypeface(null, Typeface.NORMAL)
        }

        // Highlight ONLY active screen
        when (currentScreen) {
            "HOME" -> {
                btnHome.setTextColor(activeColor)
                btnHome.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnHome.setTypeface(null, Typeface.BOLD)
            }
            "BUSES" -> {
                btnBuses.setTextColor(activeColor)
                btnBuses.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnBuses.setTypeface(null, Typeface.BOLD)
            }
            "UPDATES" -> {
                btnUpdates.setTextColor(activeColor)
                btnUpdates.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnUpdates.setTypeface(null, Typeface.BOLD)
            }
            "ABOUT" -> {
                btnAbout.setTextColor(activeColor)
                btnAbout.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnAbout.setTypeface(null, Typeface.BOLD)
            }
        }

        btnHome.setOnClickListener {
            if (currentScreen != "HOME") {
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        btnBuses.setOnClickListener {
            if (currentScreen != "BUSES") {
                val intent = Intent(activity, BusInformationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        btnUpdates.setOnClickListener {
            if (currentScreen != "UPDATES") {
                val intent = Intent(activity, TripUpdatesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        btnAbout.setOnClickListener {
            if (currentScreen != "ABOUT") {
                val intent = Intent(activity, AboutActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    fun setupStaffBottomNav(activity: Activity, currentScreen: String) {
        val btnDashboard = activity.findViewById<TextView>(R.id.btnStaffDashboard) ?: return
        val btnRecord = activity.findViewById<TextView>(R.id.btnStaffRecord) ?: return
        val btnBuses = activity.findViewById<TextView>(R.id.btnStaffBuses) ?: return
        val btnTrips = activity.findViewById<TextView>(R.id.btnStaffTrips) ?: return

        val inactiveColor = activity.getColor(R.color.text_secondary)
        val activeColor = activity.getColor(R.color.accent)

        // Reset all buttons to inactive state first
        listOf(btnDashboard, btnRecord, btnBuses, btnTrips).forEach { btn ->
            btn.setTextColor(inactiveColor)
            btn.compoundDrawableTintList = ColorStateList.valueOf(inactiveColor)
            btn.setTypeface(null, Typeface.NORMAL)
        }

// Highlight ONLY active screen
        when (currentScreen) {
            "DASHBOARD" -> {
                btnDashboard.setTextColor(activeColor)
                btnDashboard.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnDashboard.setTypeface(null, Typeface.BOLD)
            }
            "RECORD" -> {
                btnRecord.setTextColor(activeColor)
                btnRecord.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnRecord.setTypeface(null, Typeface.BOLD)
            }
            "BUSES" -> {
                btnBuses.setTextColor(activeColor)
                btnBuses.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnBuses.setTypeface(null, Typeface.BOLD)
            }
            "TRIPS" -> {
                btnTrips.setTextColor(activeColor)
                btnTrips.compoundDrawableTintList = ColorStateList.valueOf(activeColor)
                btnTrips.setTypeface(null, Typeface.BOLD)
            }
        }

        btnDashboard.setOnClickListener {
            if (currentScreen != "DASHBOARD") {
                val intent = Intent(activity, StaffDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        btnRecord.setOnClickListener {
            if (currentScreen != "RECORD") {
                val intent = Intent(activity, RecordBusMovementActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        btnBuses.setOnClickListener {
            if (currentScreen != "BUSES") {
                val intent = Intent(activity, ManageBusActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        btnTrips.setOnClickListener {
            if (currentScreen != "TRIPS") {
                val intent = Intent(activity, ManageTripsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }
}
