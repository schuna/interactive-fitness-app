package com.openai.interactivefitness.data

import android.content.Context
import com.openai.interactivefitness.domain.AppError
import com.openai.interactivefitness.domain.ErrorCategory
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject

class ErrorLogStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "diagnostic_error_log",
        Context.MODE_PRIVATE,
    )

    fun load(): List<AppError> = runCatching {
        val array = JSONArray(preferences.getString(KEY, "[]"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            AppError(
                code = item.getString("code"),
                category = ErrorCategory.valueOf(item.getString("category")),
                userMessage = item.getString("userMessage"),
                operation = item.getString("operation"),
                occurredAt = LocalDateTime.parse(item.getString("occurredAt")),
                isRecoverable = item.getBoolean("isRecoverable"),
            )
        }
    }.getOrDefault(emptyList())

    fun append(error: AppError) {
        val updated = (listOf(error) + load()).take(MAX_ENTRIES)
        val array = JSONArray()
        updated.forEach {
            array.put(
                JSONObject()
                    .put("code", it.code)
                    .put("category", it.category.name)
                    .put("userMessage", it.userMessage)
                    .put("operation", it.operation)
                    .put("occurredAt", it.occurredAt.toString())
                    .put("isRecoverable", it.isRecoverable),
            )
        }
        preferences.edit().putString(KEY, array.toString()).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "errors"
        const val MAX_ENTRIES = 50
    }
}
