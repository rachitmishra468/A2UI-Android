package com.example.a2ui_sample.infrastructure.repository

import com.example.a2ui_sample.domain.model.Feedback
import com.example.a2ui_sample.domain.model.FeedbackMetrics
import com.example.a2ui_sample.domain.model.Sentiment
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.valueobjects.*
import com.example.a2ui_sample.infrastructure.persistence.dao.FeedbackDao
import com.example.a2ui_sample.infrastructure.persistence.entity.CustomerFeedbackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val feedbackDao: FeedbackDao
) : FeedbackRepository {

    override suspend fun submitFeedback(feedback: Feedback): Boolean {
        val entity = CustomerFeedbackEntity(
            feedbackId = feedback.id.value,
            orderId = feedback.orderId.value,
            rating = feedback.overallRating.value,
            comment = feedback.comment ?: "",
            feedbackDate = feedback.createdAt
        )
        feedbackDao.insertFeedback(entity)
        return true
    }

    override suspend fun getFeedbackByOrderId(orderId: OrderId): Feedback? {
        // Implementation using non-flow query if needed
        return null
    }

    override suspend fun getFeedbackByCustomer(customerId: CustomerId): List<Feedback> {
        return emptyList()
    }

    override suspend fun getFeedbackById(id: FeedbackId): Feedback? {
        return null
    }

    override suspend fun updateFeedback(feedback: Feedback): Boolean {
        return false
    }

    override suspend fun getFeedbackMetrics(): FeedbackMetrics {
        return FeedbackMetrics(0.0, 0, emptyMap(), emptyMap())
    }

    override fun getFeedbackFlow(): Flow<List<Feedback>> {
        return feedbackDao.getAllFeedback().map { entities ->
            entities.map { mapToDomain(it) }
        }
    }

    private fun mapToDomain(entity: CustomerFeedbackEntity): Feedback {
        return Feedback(
            id = FeedbackId(entity.feedbackId),
            orderId = OrderId(entity.orderId),
            customerId = CustomerId("guest"),
            foodRating = Rating(entity.rating),
            deliveryRating = Rating(entity.rating),
            packagingRating = Rating(entity.rating),
            overallRating = Rating(entity.rating),
            comment = entity.comment,
            sentiment = if (entity.rating >= 4) Sentiment.POSITIVE else if (entity.rating <= 2) Sentiment.NEGATIVE else Sentiment.NEUTRAL,
            createdAt = entity.feedbackDate
        )
    }
}
