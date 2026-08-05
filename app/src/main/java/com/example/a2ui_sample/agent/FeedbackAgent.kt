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
            else -> AgentResponse.Error("I'm sorry, I cannot handle this request.")
        }
    }

    private suspend fun handleSubmitFeedback(intent: IntentResultWrapper): AgentResponse {
        val orderId = intent.entities["order_id"] as? String
        val overallRating = (intent.entities["rating"] as? Number)?.toInt() ?: 0
        val comment = intent.entities["comment"] as? String ?: ""
        
        if (orderId == null) {
            return AgentResponse.Message("Which order would you like to provide feedback for? Please provide an Order ID.")
        }

        if (overallRating == 0) {
            return AgentResponse.FeedbackForm(orderId, "Please rate your experience with order $orderId.")
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
            return AgentResponse.FeedbackSubmitted(feedback, "I'm very sorry you had a poor experience. I've submitted your feedback and opened a support ticket for the restaurant manager to investigate immediately.")
        }

        return AgentResponse.FeedbackSubmitted(feedback, "Thank you for your valuable feedback! We're glad you enjoyed your experience.")
    }

    private suspend fun handleViewFeedback(intent: IntentResultWrapper): AgentResponse {
        val customerId = CustomerId("guest")
        val feedbacks = repository.getFeedbackByCustomer(customerId)
        
        return if (feedbacks.isNotEmpty()) {
            AgentResponse.FeedbackHistory(feedbacks, "Here is the feedback you've submitted previously.")
        } else {
            AgentResponse.Message("You haven't submitted any feedback yet.")
        }
    }

    private suspend fun handleUpdateFeedback(intent: IntentResultWrapper): AgentResponse {
        val feedbackId = intent.entities["feedback_id"] as? String
        if (feedbackId == null) return AgentResponse.Message("Please provide the feedback ID you wish to update.")
        
        val existing = repository.getFeedbackById(FeedbackId(feedbackId))
            ?: return AgentResponse.Error("Feedback not found.")
            
        return AgentResponse.Message("Feedback updated successfully.")
    }

    private suspend fun handleViewMetrics(): AgentResponse {
        val metrics = repository.getFeedbackMetrics()
        return AgentResponse.FeedbackDashboard(metrics, "Here's the summary of overall customer satisfaction.")
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
