package com.example.a2ui_sample.common.logger

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

enum class LogEvent {
    REQUEST_RECEIVED,
    INTENT_DETECTED,
    ENTITY_EXTRACTED,
    AGENT_SELECTED,
    USE_CASE_EXECUTED,
    DATABASE_OPERATION,
    A2UI_RENDERED,
    RESPONSE_SENT
}

/**
 * Enterprise Tracing Logger
 */
@Singleton
class EnterpriseLogger @Inject constructor() {
    
    fun log(event: LogEvent, message: String, correlationId: String? = null) {
        val trace = if (correlationId != null) "[$correlationId] " else ""
        Log.i("ENTERPRISE_TRACE", "$trace${event.name}: $message")
    }

    fun error(message: String, throwable: Throwable? = null) {
        Log.e("ENTERPRISE_TRACE", "ERROR: $message", throwable)
    }
}
