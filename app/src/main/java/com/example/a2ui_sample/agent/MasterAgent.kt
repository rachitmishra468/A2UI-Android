package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.UserIntent
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.repository.MenuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "A2UI_FLOW"

/**
 * Master Agent that orchestrates the flow:
 * 1. GeminiProvider for Intent Analysis (Reasoning)
 * 2. Specialist Agents for Action (Execution)
 * 3. A2UIResponseBuilder for UI (Presentation)
 */
class ADKRestaurantMasterAgent(
    private val menuRepository: MenuRepository,
    private val feedbackRepository: FeedbackRepository,
    private val reservationRepository: com.example.a2ui_sample.domain.repository.ReservationRepository,
    private val orderRepository: com.example.a2ui_sample.domain.repository.OrderRepository,
    private val deliveryRepository: com.example.a2ui_sample.domain.repository.DeliveryRepository
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }
    private val geminiProvider by lazy { GeminiProvider() }
    
    private val menuAgent by lazy { MenuAgent(menuRepository) }
    private val cartAgent by lazy { CartAgent(menuRepository) }
    private val bookingAgent by lazy { BookingAgent(reservationRepository) }
    private val deliveryAgent by lazy { DeliveryAgent(orderRepository, deliveryRepository, menuRepository) }
    private val feedbackAgent by lazy { FeedbackAgent(feedbackRepository) }

    /**
     * processQuery
     * The entry point for all user queries.
     * history: Previous messages for context.
     */
    suspend fun processQuery(
        userMessage: String, 
        history: List<String> = emptyList(),
        onProgress: (String) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "1. MasterAgent START: '$userMessage'")
        
        onProgress("🤖 Thinking...")
        
        val truncatedHistory = history.takeLast(10)

        // Step 1: Reasoning
        onProgress("🔍 Analyzing intent...")
        val intentResult = geminiProvider.analyzeQuery(userMessage, truncatedHistory)
        Log.d(TAG, "2. Gemini Intent: ${intentResult.intent} (Confidence: ${intentResult.confidence})")

        // Map intent to user-friendly status
        val status = when(intentResult.intent) {
            UserIntent.MENU_SEARCH, UserIntent.MENU_RECOMMEND -> "🔍 Looking for menu items..."
            UserIntent.CART_VIEW, UserIntent.CART_ADD, UserIntent.CART_REMOVE, UserIntent.CART_UPDATE -> "🛒 Updating your cart..."
            UserIntent.CHECKOUT -> "💳 Processing checkout..."
            UserIntent.BOOKING_CREATE, UserIntent.BOOKING_CHECK -> "📅 Checking table availability..."
            UserIntent.ORDER_HISTORY, UserIntent.ORDER_TRACKING, UserIntent.ORDER_CANCEL -> "📜 Loading your orders..."
            else -> "🤖 Processing request..."
        }
        onProgress(status)

        // Step 2: Execution (Delegate to Specialist)
        val executionResponse = when (intentResult.intent) {
            UserIntent.MENU_SEARCH, 
            UserIntent.MENU_RECOMMEND -> menuAgent.execute(intentResult)
            
            UserIntent.CART_VIEW,
            UserIntent.CART_ADD,
            UserIntent.CART_REMOVE,
            UserIntent.CART_UPDATE,
            UserIntent.CART_CLEAR,
            UserIntent.CHECKOUT -> cartAgent.execute(intentResult)
            
            UserIntent.BOOKING_CREATE,
            UserIntent.BOOKING_CHECK,
            UserIntent.BOOKING_MODIFY,
            UserIntent.BOOKING_CANCEL,
            UserIntent.BOOKING_LIST -> bookingAgent.execute(intentResult)
            
            UserIntent.ORDER_HISTORY,
            UserIntent.ORDER_TRACKING,
            UserIntent.ORDER_REPEAT,
            UserIntent.ORDER_CANCEL -> deliveryAgent.execute(intentResult)
            
            UserIntent.FEEDBACK_SUBMIT,
            UserIntent.FEEDBACK_VIEW,
            UserIntent.FEEDBACK_UPDATE,
            UserIntent.FEEDBACK_METRICS -> feedbackAgent.execute(intentResult)
            
            UserIntent.OFFER_LIST,
            UserIntent.OFFER_APPLY -> AgentResponse.Message("Offers and discounts are coming soon!")
            
            UserIntent.AI_LIMIT_REACHED -> AgentResponse.Message("⚠️ AI service limit has been reached.\n\n" +
                    "You have exhausted the available AI tokens/requests for now.\n" +
                    "Please wait a few minutes and try again.")
            
            UserIntent.UNKNOWN -> AgentResponse.Message("I couldn't understand your request. Please try again with different wording.")
        }

        Log.d(TAG, "3. Execution Result: ${executionResponse.javaClass.simpleName}")

        onProgress("🎨 Preparing response...")
        val uniqueSurfaceId = "surf_${System.currentTimeMillis()}_${(0..999).random()}"
        return@withContext responseBuilder.buildWithId(executionResponse, uniqueSurfaceId)
    }

    /**
     * buildOrderPlacedResponse
     * Direct helper to generate Order Placed UI from ViewModel after checkout.
     */
    fun buildOrderPlacedResponse(order: com.example.a2ui_sample.domain.model.Order): String {
        val uniqueId = "surf_${System.currentTimeMillis()}_${(0..999).random()}"
        val responses = responseBuilder.buildWithId(AgentResponse.OrderPlaced(order), uniqueId)
        return responses.last() // The updateComponents JSON
    }
}
