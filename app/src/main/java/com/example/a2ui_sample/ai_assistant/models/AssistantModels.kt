package com.example.a2ui_sample.ai_assistant.models

import com.google.gson.annotations.SerializedName

/**
 * AssistantIntent
 * Structured output from Intent Agent.
 */
data class AssistantIntent(
    @SerializedName("intent") val intent: String,
    @SerializedName("category") val category: String? = null,
    @SerializedName("itemName") val itemName: String? = null,
    @SerializedName("diet") val diet: String? = null,
    @SerializedName("quantity") val quantity: Int? = 1
)

/**
 * AssistantResponse
 * Final response to be rendered in UI.
 */
data class AssistantResponse(
    val text: String,
    val a2uiPayloads: List<String> = emptyList()
)
