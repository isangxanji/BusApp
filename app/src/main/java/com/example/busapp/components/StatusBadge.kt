
package com.example.busapp.components

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView

object StatusBadge {

    fun setStatus(textView: TextView, status: String) {

        val backgroundColor: Int
        val textColor: Int
        val borderColor: Int

        when (status) {

            "Available" -> {
                backgroundColor = Color.parseColor("#E5F3EA")
                textColor = Color.parseColor("#218653")
                borderColor = Color.parseColor("#A9D8BA")
            }

            "En Route" -> {
                backgroundColor = Color.parseColor("#FFF1D6")
                textColor = Color.parseColor("#A66D0B")
                borderColor = Color.parseColor("#E8C77F")
            }

            "Departed" -> {
                backgroundColor = Color.parseColor("#ECEBE8")
                textColor = Color.parseColor("#77736B")
                borderColor = Color.parseColor("#D5D2CB")
            }

            "Unavailable" -> {
                backgroundColor = Color.parseColor("#FCE8E8")
                textColor = Color.parseColor("#C74343")
                borderColor = Color.parseColor("#E8B5B5")
            }

            else -> {
                backgroundColor = Color.parseColor("#ECEBE8")
                textColor = Color.parseColor("#55575C")
                borderColor = Color.parseColor("#D5D2CB")
            }
        }

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 50f
        shape.setColor(backgroundColor)
        shape.setStroke(1, borderColor)

        textView.text = status
        textView.setTextColor(textColor)
        textView.background = shape
    }
}
