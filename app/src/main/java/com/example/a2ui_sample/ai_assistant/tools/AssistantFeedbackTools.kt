package com.example.a2ui_sample.ai_assistant.tools

import com.example.a2ui_sample.domain.model.Feedback
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.valueobjects.*
import com.google.adk.kt.annotations.Tool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantFeedbackTools @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) {
    @Tool(
        name = "submit_feedback",
        description = "Submit a rating and comment for the restaurant or an order."
    )
    suspend fun submitFeedback(rating: Int, comment: String, orderId: String?): String {
        return try {
            val r = Rating(rating.coerceIn(1, 5))
            val feedback = Feedback(
                id = FeedbackId(),
                orderId = OrderId(orderId ?: "guest_order"),
                customerId = CustomerId("guest"),
                foodRating = r,
                deliveryRating = r,
                packagingRating = r,
                overallRating = r,
                comment = comment,
                createdAt = System.currentTimeMillis()
            )
            feedbackRepository.submitFeedback(feedback)
            "✅ Thank you for your $rating-star feedback!"
        } catch (e: Exception) {
            "❌ Failed to submit feedback: ${e.message}"
        }
    }
}
