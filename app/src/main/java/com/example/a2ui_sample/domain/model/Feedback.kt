package com.example.a2ui_sample.domain.model

import com.example.a2ui_sample.domain.valueobjects.FeedbackId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.Rating

/**
 * Feedback Entity
 * Represents customer feedback for an order.
 */
data class Feedback(
    val id: FeedbackId,
    val orderId: OrderId,
    val customerId: CustomerId,
    val rating: Rating,
    val comment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
