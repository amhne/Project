package com.example.project.utils

import org.json.JSONObject

fun parseApiErrorMessage(errorBody: String?): String {
    if (errorBody.isNullOrEmpty()) return "Unknown error occurred"
    return try {
        val json = JSONObject(errorBody)
        when {
            json.has("errors") -> {
                val errorsArray = json.getJSONArray("errors")
                val messages = mutableListOf<String>()
                for (i in 0 until errorsArray.length()) {
                    val errorObj = errorsArray.getJSONObject(i)
                    val detail = errorObj.optString("detail")
                    if (detail.isNotEmpty()) messages.add(detail)
                }
                if (messages.isNotEmpty()) messages.joinToString("\n") else "Unknown error occurred"
            }
            json.has("detail") -> json.optString("detail", "Unknown error occurred")
            else -> "Unknown error occurred"
        }
    } catch (e: Exception) {
        "Unknown error occurred"
    }
}