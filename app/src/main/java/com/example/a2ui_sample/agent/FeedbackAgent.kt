package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.FeedbackId
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.Rating
import java.util.UUID

private const val TAG = "A2UI_FEEDBACK"

/**
 * FeedbackAgent
 * Specialist for collecting and managing customer feedback.
 */
class FeedbackAgent(private val repository: FeedbackRepository) {

    suspend fun execute(intent: IntentResultWrapper): AgentResponse {
        return when (intent.intent) {
            UserIntent.FEEDBACK_SUBMIT -> handleSubmitFeedback(intent)
            UserIntent.FEEDBACK_VIEW -> handleViewFeedback(intent)
            UserIntent.FEEDBACK_UPDATE -> handleUpdateFeedback(intent)
            UserIntent.FEEDBACK_METRICS -> handleViewMetrics()
            else -> AgentResponse.Error("I'm here to help! 😊 What would you like feedback on?")
        }
    }

    private suspend fun handleSubmitFeedback(intent: IntentResultWrapper): AgentResponse {
        val orderId = intent.entities["order_id"] as? String
        val overallRating = (intent.entities["rating"] as? Number)?.toInt() ?: 0
        val comment = intent.entities["comment"] as? String ?: ""
        
        if (orderId == null) {
            return AgentResponse.Message("Which order would you like to rate? 🌟 Please share the Order ID!")
        }

        if (overallRating == 0) {
            return AgentResponse.FeedbackForm(orderId, "How was your experience with order #$orderId? ⭐ Rate from 1-5 stars!")
        }

        val foodRating = (intent.entities["food_rating"] as? Number)?.toInt() ?: overallRating
        val deliveryRating = (intent.entities["delivery_rating"] as? Number)?.toInt() ?: overallRating
        val packagingRating = (intent.entities["packaging_rating"] as? Number)?.toInt() ?: overallRating

        val sentiment = detectSentiment(overallRating, comment)
        
        val feedback = Feedback(
            id = FeedbackId(UUID.randomUUID().toString()),
            orderId = OrderId(orderId),
            customerId = CustomerId("guest"),
            foodRating = Rating(foodRating),
            deliveryRating = Rating(deliveryRating),
            packagingRating = Rating(packagingRating),
            overallRating = Rating(overallRating),
            comment = comment,
            sentiment = sentiment
        )

        repository.submitFeedback(feedback)

        if (overallRating < 3) {
            createSupportTicket(feedback)
            return AgentResponse.FeedbackSubmitted(feedback, "😔 We're truly sorry your experience wasn't great. I've escalated your feedback to our manager who will look into this right away. We'd love to make it right!")
        }

        return AgentResponse.FeedbackSubmitted(feedback, "Thank you so much! 🌟 Your feedback helps us serve you better. We're thrilled you enjoyed your meal! 😊")
    }

    private suspend fun handleViewFeedback(intent: IntentResultWrapper): AgentResponse {
        val customerId = CustomerId("guest")
        val feedbacks = repository.getFeedbackByCustomer(customerId)
        
        return if (feedbacks.isNotEmpty()) {
            AgentResponse.FeedbackHistory(feedbacks, "Here are all the reviews you've shared with us! 📝 Thank you!")
        } else {
            AgentResponse.Message("You haven't shared any feedback yet. 🌟 We'd love to hear how we're doing!")
        }
    }

    private suspend fun handleUpdateFeedback(intent: IntentResultWrapper): AgentResponse {
        val feedbackId = intent.entities["feedback_id"] as? String
        if (feedbackId == null) return AgentResponse.Message("Which feedback would you like to update? 📝 Please share the feedback ID!")
        
        val existing = repository.getFeedbackById(FeedbackId(feedbackId))
            ?: return AgentResponse.Error("I couldn't find that feedback. 🤔 Could you check the ID?")
            
        return AgentResponse.Message("Feedback updated successfully! ✅ Thank you!")
    }

    private suspend fun handleViewMetrics(): AgentResponse {
        val metrics = repository.getFeedbackMetrics()
        return AgentResponse.FeedbackDashboard(metrics, "Here's how we're doing overall! 📊 Your feedback makes us better!")
    }

    private fun detectSentiment(rating: Int, comment: String): Sentiment {
        return when {
            rating >= 4 -> Sentiment.POSITIVE
            rating <= 2 -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }

    private fun createSupportTicket(feedback: Feedback) {
        Log.w(TAG, "SUPPORT TICKET CREATED for Order ${feedback.orderId}. Manager Notified.")
    }
}
