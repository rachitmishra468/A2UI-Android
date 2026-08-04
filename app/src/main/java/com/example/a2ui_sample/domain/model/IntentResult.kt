package com.example.a2ui_sample.domain.model

import com.google.gson.annotations.SerializedName

/**
 * IntentResult
 * Structured output from Gemini reasoning engine.
 */
data class IntentResult(
    @SerializedName("intent") val intent: UserIntent,
    @SerializedName("language") val language: String,
    @SerializedName("entities") val entities: Map<String, Any> = emptyMap(),
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("rawQuery") val rawQuery: String
)

enum class UserIntent {
    MENU_SEARCH,
    MENU_RECOMMEND,
    CART_VIEW,
    CART_ADD,
    CART_REMOVE,
    CART_UPDATE,
    CART_CLEAR,
    BOOKING_CREATE,
    BOOKING_CHECK,
    BOOKING_MODIFY,
    BOOKING_CANCEL,
    BOOKING_LIST,
    ORDER_HISTORY,
    ORDER_TRACKING,
    ORDER_REPEAT,
    OFFER_LIST,
    OFFER_APPLY,
    CHECKOUT,
    FEEDBACK_SUBMIT,
    FEEDBACK_VIEW,
    FEEDBACK_UPDATE,
    FEEDBACK_METRICS,
    UNKNOWN
}
