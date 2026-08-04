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
    val foodRating: Rating,
    val deliveryRating: Rating,
    val packagingRating: Rating,
    val overallRating: Rating,
    val comment: String? = null,
    val sentiment: Sentiment = Sentiment.NEUTRAL,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Sentiment {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}

data class FeedbackMetrics(
    val averageRating: Double,
    val totalReviews: Int,
    val ratingDistribution: Map<Int, Int>,
    val sentimentSummary: Map<Sentiment, Int>
)
