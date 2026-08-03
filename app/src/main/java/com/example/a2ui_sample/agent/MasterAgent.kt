package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.UserIntent
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
    private val menuRepository: MenuRepository
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }
    private val geminiProvider by lazy { GeminiProvider() }
    
    private val menuAgent by lazy { MenuAgent(menuRepository) }
    private val cartAgent by lazy { CartAgent(menuRepository) }
    private val bookingAgent by lazy { BookingAgent(menuRepository) }

    /**
     * processQuery
     * The entry point for all user queries.
     * history: Previous messages for context.
     */
    suspend fun processQuery(userMessage: String, history: List<String> = emptyList()): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "1. MasterAgent START: '$userMessage'")
        
        // Step 1: Reasoning (Intent Classification with Context)
        val intentResult = geminiProvider.analyzeQuery(userMessage, history)
        Log.d(TAG, "2. Gemini Intent: ${intentResult.intent} (Confidence: ${intentResult.confidence})")

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
            UserIntent.ORDER_TRACK,
            UserIntent.ORDER_REPEAT -> AgentResponse.Message("Order history and tracking is coming soon!")
            
            UserIntent.OFFER_LIST,
            UserIntent.OFFER_APPLY -> AgentResponse.Message("Offers and discounts are coming soon!")
            
            UserIntent.UNKNOWN -> AgentResponse.Message("I'm sorry, I couldn't understand that request. Could you please rephrase?")
        }

        Log.d(TAG, "3. Execution Result: ${executionResponse.javaClass.simpleName}")

        // Step 3: Presentation (A2UI Generation)
        // Every response now generates a UNIQUE surfaceId to prevent overwriting
        val uniqueSurfaceId = "surf_${System.currentTimeMillis()}_${(0..999).random()}"
        return@withContext responseBuilder.buildWithId(executionResponse, uniqueSurfaceId)
    }
}
