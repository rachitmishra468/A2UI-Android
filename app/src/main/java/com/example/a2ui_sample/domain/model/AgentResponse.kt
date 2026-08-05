package com.example.a2ui_sample.domain.model

sealed class AgentResponse {
    data class Message(val message: String) : AgentResponse()
    data class Error(val message: String) : AgentResponse()
    
    data class BookingRequest(val step: String, val message: String) : AgentResponse()
    data class BookingConfirmation(val booking: TableBooking) : AgentResponse()
    data class BookingHistory(val bookings: List<Reservation>, val message: String) : AgentResponse()
    
    data class MenuResults(val items: List<MenuItem>, val message: String) : AgentResponse()
    data class Recommendations(
        val items: List<MenuItem>,
        val message: String = "Here are my recommendations:"
    ) : AgentResponse()
    
    data class CartUpdate(val item: MenuItem, val totalCount: Int, val message: String = "Item added to cart!") : AgentResponse()
    data class CartView(val items: List<CartItem>, val totalAmount: Int) : AgentResponse()
    
    data class OrderConfirmation(val order: Order) : AgentResponse()
    
    // Checkout Flow
    data class OrderSummary(val items: List<CartItem>, val subtotal: Int, val tax: Int, val total: Int, val message: String = "Here's your order summary:") : AgentResponse()
    data class PaymentChoice(val total: Int) : AgentResponse()
    data class NavigateToPayment(val orderId: String, val amount: Int) : AgentResponse()
    data class OrderPlaced(val order: Order) : AgentResponse()

    data class DeliveryUpdate(val delivery: Delivery, val order: Order) : AgentResponse()

    data class FeedbackForm(val orderId: String, val message: String) : AgentResponse()
    data class FeedbackSubmitted(val feedback: Feedback, val message: String) : AgentResponse()
    data class FeedbackHistory(val feedbacks: List<Feedback>, val message: String) : AgentResponse()
    data class FeedbackDashboard(val metrics: FeedbackMetrics, val message: String) : AgentResponse()
}
