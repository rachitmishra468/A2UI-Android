package com.example.a2ui_sample.infrastructure.repository

import com.example.a2ui_sample.domain.model.Feedback
import com.example.a2ui_sample.domain.model.FeedbackMetrics
import com.example.a2ui_sample.domain.model.Sentiment
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.FeedbackId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor() : FeedbackRepository {
    private val feedbacks = mutableListOf<Feedback>()
    private val _feedbackFlow = MutableStateFlow<List<Feedback>>(emptyList())

    override suspend fun submitFeedback(feedback: Feedback): Boolean {
        feedbacks.add(0, feedback)
        _feedbackFlow.value = feedbacks.toList()
        return true
    }

    override suspend fun getFeedbackByOrderId(orderId: OrderId): Feedback? {
        return feedbacks.find { it.orderId == orderId }
    }

    override suspend fun getFeedbackByCustomer(customerId: CustomerId): List<Feedback> {
        return feedbacks.filter { it.customerId == customerId }
    }

    override suspend fun getFeedbackById(id: FeedbackId): Feedback? {
        return feedbacks.find { it.id == id }
    }

    override suspend fun updateFeedback(feedback: Feedback): Boolean {
        val index = feedbacks.indexOfFirst { it.id == feedback.id }
        if (index != -1) {
            feedbacks[index] = feedback
            _feedbackFlow.value = feedbacks.toList()
            return true
        }
        return false
    }

    override suspend fun getFeedbackMetrics(): FeedbackMetrics {
        if (feedbacks.isEmpty()) {
            return FeedbackMetrics(0.0, 0, emptyMap(), emptyMap())
        }

        val avg = feedbacks.map { it.overallRating.value }.average()
        val dist = feedbacks.groupingBy { it.overallRating.value }.eachCount()
        val sentimentSummary = feedbacks.groupingBy { it.sentiment }.eachCount()

        return FeedbackMetrics(avg, feedbacks.size, dist, sentimentSummary)
    }

    override fun getFeedbackFlow(): Flow<List<Feedback>> = _feedbackFlow.asStateFlow()
}
