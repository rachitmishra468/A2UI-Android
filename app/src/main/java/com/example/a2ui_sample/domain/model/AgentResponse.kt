package com.example.a2ui_sample.domain.model

sealed class AgentResponse {
    data class Message(val message: String) : AgentResponse()
    data class Error(val message: String) : AgentResponse()
    
    data class BookingRequest(val step: String, val message: String) : AgentResponse()
    data class BookingConfirmation(val booking: TableBooking) : AgentResponse()
    
    data class MenuResults(val items: List<MenuItem>, val message: String) : AgentResponse()
    data class Recommendations(val items: List<MenuItem>) : AgentResponse()
    
    data class CartUpdate(val item: MenuItem, val totalCount: Int) : AgentResponse()
    data class CartView(val items: List<CartItem>, val totalAmount: Int) : AgentResponse()
    
    data class OrderConfirmation(val order: Order) : AgentResponse()
}
