package com.example.project.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat

fun windowInsetsListener(originalTopMargin: Int, originalBottomMargin: Int): (View, WindowInsetsCompat) ->
WindowInsetsCompat = { v, insets ->
    val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    val params = v.layoutParams as ViewGroup.MarginLayoutParams
    params.topMargin = originalTopMargin + systemBarsInsets.top
    params.bottomMargin = originalBottomMargin + systemBarsInsets.bottom
    v.layoutParams = params
    insets
}