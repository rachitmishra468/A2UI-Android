package com.example.a2ui_sample.presentation.viewmodel

import java.util.UUID

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isA2UI: Boolean = false,
    val a2uiPayload: String? = null
)
