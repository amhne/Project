package com.example.project.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

fun scaleViewsToScreen(
    root: View,
    context: Context,
    designWidthDp: Float = 360f,
    designHeightDp: Float = 780f
) {
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density

    val screenWidthDp = displayMetrics.widthPixels / density
    val screenHeightDp = displayMetrics.heightPixels / density

    val scaleX = screenWidthDp / designWidthDp
    val scaleY = screenHeightDp / designHeightDp
    val scaleAvg = (scaleX + scaleY) / 2f

    fun scaleView(view: View) {
        val params = view.layoutParams ?: return

        if (params.width > 0) {
            params.width = (params.width * scaleX).toInt()
        }
        if (params.height > 0) {
            params.height = (params.height * scaleY).toInt()
        }

        view.setPadding(
            (view.paddingLeft * scaleX).toInt(),
            (view.paddingTop * scaleY).toInt(),
            (view.paddingRight * scaleX).toInt(),
            (view.paddingBottom * scaleY).toInt()
        )

        if (params is ViewGroup.MarginLayoutParams) {
            params.setMargins(
                (params.leftMargin * scaleX).toInt(),
                (params.topMargin * scaleY).toInt(),
                (params.rightMargin * scaleX).toInt(),
                (params.bottomMargin * scaleY).toInt()
            )
        }

        if (view is TextView) {
            val originalSp = view.textSize / density
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, originalSp * scaleAvg)

            val originalLineSpacingExtra = view.lineSpacingExtra / density
            view.setLineSpacing(originalLineSpacingExtra * scaleAvg * density, view.lineSpacingMultiplier)
        }

        view.translationY *= scaleY
        view.layoutParams = params
    }

    fun traverse(view: View) {
        scaleView(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverse(view.getChildAt(i))
            }
        }
    }

    traverse(root)
}