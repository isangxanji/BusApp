package com.pabama.businfo.ui.components

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView

object StatusBadge {

    fun setStatus(textView: TextView, status: String) {
        val backgroundColor: Int
        val textColor: Int
        val borderColor: Int

        when (status.trim().uppercase()) {
            "AVAILABLE" -> {
                backgroundColor = Color.parseColor("#DCFCE7")
                textColor = Color.parseColor("#166534")
                borderColor = Color.parseColor("#86EFAC")
            }
            "EN ROUTE" -> {
                backgroundColor = Color.parseColor("#DBEAFE")
                textColor = Color.parseColor("#1E3A8A")
                borderColor = Color.parseColor("#93C5FD")
            }
            "COMPLETED" -> {
                backgroundColor = Color.parseColor("#E0F2FE")
                textColor = Color.parseColor("#075985")
                borderColor = Color.parseColor("#7DD3FC")
            }
            "DEPARTED" -> {
                backgroundColor = Color.parseColor("#E2E8F0")
                textColor = Color.parseColor("#334155")
                borderColor = Color.parseColor("#CBD5E1")
            }
            "UNAVAILABLE" -> {
                backgroundColor = Color.parseColor("#FEE2E2")
                textColor = Color.parseColor("#991B1B")
                borderColor = Color.parseColor("#FCA5A5")
            }
            else -> {
                backgroundColor = Color.parseColor("#E2E8F0")
                textColor = Color.parseColor("#334155")
                borderColor = Color.parseColor("#CBD5E1")
            }
        }

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 50f
        shape.setColor(backgroundColor)
        shape.setStroke(2, borderColor)

        textView.text = status.trim().uppercase()
        textView.setTextColor(textColor)
        textView.background = shape
    }
}
