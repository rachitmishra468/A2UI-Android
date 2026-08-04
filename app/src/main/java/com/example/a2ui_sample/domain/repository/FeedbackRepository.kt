package com.example.a2ui_sample.domain.repository

import com.example.a2ui_sample.domain.model.Feedback
import com.example.a2ui_sample.domain.model.FeedbackMetrics
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.FeedbackId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import kotlinx.coroutines.flow.Flow

/**
 * FeedbackRepository Contract
 * Manages customer feedback and reviews.
 */
interface FeedbackRepository {
    suspend fun submitFeedback(feedback: Feedback): Boolean
    suspend fun getFeedbackByOrderId(orderId: OrderId): Feedback?
    suspend fun getFeedbackByCustomer(customerId: CustomerId): List<Feedback>
    suspend fun getFeedbackById(id: FeedbackId): Feedback?
    suspend fun updateFeedback(feedback: Feedback): Boolean
    suspend fun getFeedbackMetrics(): FeedbackMetrics
    fun getFeedbackFlow(): Flow<List<Feedback>>
}
