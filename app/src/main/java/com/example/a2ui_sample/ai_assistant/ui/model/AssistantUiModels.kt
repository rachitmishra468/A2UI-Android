package com.example.a2ui_sample.ai_assistant.ui.model

import com.example.a2ui_sample.domain.model.MenuItem

sealed class AssistantUiState {
    data class TextResponse(val text: String) : AssistantUiState()
    data class MenuSearch(val items: List<MenuItem>, val query: String? = null) : AssistantUiState()
    data class Recommendations(val items: List<MenuItem>) : AssistantUiState()
    data class MenuDetails(val item: MenuItem) : AssistantUiState()
    data class CartUpdate(val item: MenuItem, val quantity: Int, val message: String) : AssistantUiState()
    data class CartView(val items: List<com.example.a2ui_sample.domain.model.CartItem>, val total: Int, val message: String) : AssistantUiState()
    data class BookingResult(val message: String, val date: String? = null, val time: String? = null, val guests: Int? = null) : AssistantUiState()
    data class FeedbackResult(val message: String, val rating: Int? = null) : AssistantUiState()
    data class OrderStatus(val message: String, val status: String? = null, val eta: String? = null, val progress: Float = 0f) : AssistantUiState()
    data class RatingRequest(val orderId: String, val message: String = "How was your food? Rate us!") : AssistantUiState()
    data class CheckoutSummary(
        val items: List<com.example.a2ui_sample.domain.model.CartItem>,
        val total: Int,
        val message: String
    ) : AssistantUiState()
    data class CouponList(val coupons: List<com.example.a2ui_sample.domain.model.Coupon>, val message: String) : AssistantUiState()
    data class InfoCard(val title: String, val content: String, val icon: String? = null) : AssistantUiState()
    data class Error(val message: String) : AssistantUiState()
    object Loading : AssistantUiState()
}

data class AssistantChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: AssistantUiState,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
