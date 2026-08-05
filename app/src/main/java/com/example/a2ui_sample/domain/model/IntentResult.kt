package com.example.a2ui_sample.domain.model

import com.google.gson.annotations.SerializedName

/**
 * IntentResult
 * Updated to follow the Restaurant Master Agent decision logic.
 */
data class IntentResult(
    @SerializedName("mode") val mode: String? = null, // "INTENT" or "TOOL_WORKFLOW"
    @SerializedName("intent") val intent: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("diet") val diet: String? = null,
    @SerializedName("priceLimit") val priceLimit: Int? = null,
    @SerializedName("peopleCount") val peopleCount: Int? = null,
    @SerializedName("target") val target: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("tasks") val tasks: List<MasterAgentTask>? = null,
    @SerializedName("language") val language: String? = "en",
    @SerializedName("rawQuery") val rawQuery: String? = ""
)

data class MasterAgentTask(
    @SerializedName("tool") val tool: String? = null,
    @SerializedName("itemName") val itemName: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("diet") val diet: String? = null,
    @SerializedName("priceLimit") val priceLimit: Int? = null,
    @SerializedName("target") val target: String? = null,
    @SerializedName("quantity") val quantity: Int? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("peopleCount") val peopleCount: Int? = null,
    @SerializedName("rating") val rating: Int? = null,
    @SerializedName("comment") val comment: String? = null
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
    ORDER_CANCEL,
    ORDER_PLACED,
    OFFER_LIST,
    OFFER_APPLY,
    CHECKOUT,
    FEEDBACK_SUBMIT,
    FEEDBACK_VIEW,
    FEEDBACK_UPDATE,
    FEEDBACK_METRICS,
    AI_LIMIT_REACHED,
    UNKNOWN
}

/**
 * Legacy wrapper for compatibility
 */
data class IntentResultWrapper(
    val intent: UserIntent,
    val entities: Map<String, Any>
)
