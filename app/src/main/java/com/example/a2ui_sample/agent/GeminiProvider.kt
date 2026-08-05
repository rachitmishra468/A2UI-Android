package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.model.UserIntent
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "A2UI_FLOW"

/**
 * GeminiProvider
 * The primary reasoning engine that classifies intent and extracts entities.
 */
class GeminiProvider {
    private val gson = Gson()
    private val model = GenerativeModel(
        modelName = "gemini-3.6-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val systemInstruction = """
        You are a high-performance Restaurant AI Reasoning Engine.
        Your task is to analyze user queries and output ONLY a structured JSON object.
        
        SUPPORTED INTENTS:
        - MENU_SEARCH: User wants to find food, browse categories, or search by name/type.
        - MENU_RECOMMEND: User is hungry, wants suggestions, or asks for popular items.
        - CART_VIEW: User wants to see their bag/cart (e.g., "show my cart", "what's in my bag").
        - CART_ADD: User wants to add items to cart.
        - CART_REMOVE: User wants to remove items from cart.
        - CART_UPDATE: User wants to change quantity of items.
        - CART_CLEAR: User wants to empty the cart.
        - BOOKING_CREATE: User wants to book a table.
        - BOOKING_CHECK: User wants to check availability or slots.
        - BOOKING_MODIFY: User wants to change time/people of reservation.
        - BOOKING_CANCEL: User wants to cancel a booking.
        - BOOKING_LIST: User wants to see their reservations.
        - ORDER_HISTORY: User wants to see past orders.
        - ORDER_TRACKING: User wants to track their order status, delivery time, or courier info.
        - ORDER_REPEAT: User wants to order the same thing again.
        - ORDER_CANCEL: User wants to cancel an existing order.
        - OFFER_LIST: User wants to see discounts, coupons, or deals.
        - OFFER_APPLY: User wants to apply a specific code or discount.
        - CHECKOUT: User wants to pay, finish ordering, or proceed to checkout (e.g., "checkout", "pay now", "proceed").
        - FEEDBACK_SUBMIT: User wants to rate their order, leave a review, or provide feedback.
        - FEEDBACK_VIEW: User wants to see their past reviews or feedback history.
        - FEEDBACK_UPDATE: User wants to modify their existing feedback.
        - FEEDBACK_METRICS: User (likely admin/manager) wants to see average ratings or trends.
        
        MULTILINGUAL SUPPORT:
        Detect language: English, Hindi, Hinglish (English-Hindi mix), Urdu, or others.
        
        ENTITY EXTRACTION:
        - food_item: name of the dish
        - category: dish category
        - quantity: number of items
        - price_limit: budget constraint
        - people_count: number of people or guests
        - date: booking date
        - time: booking time
        - order_id: the unique ID of an order (e.g., "ORD-1234")
        - feedback_id: the unique ID of a feedback entry
        - rating: numeric rating (1-5)
        - payment_method: "cod" or "online"
        - food_rating: rating for food quality (1-5)
        - delivery_rating: rating for delivery speed/experience (1-5)
        - packaging_rating: rating for packaging quality (1-5)
        - comment: textual review or feedback
        - coupon_code: e.g., "SAVE20"
        - diet: (e.g., "veg", "non-veg")
        
        Output JSON Format:
        {
          "intent": "INTENT_NAME",
          "language": "detected_language",
          "entities": { "key": "value" },
          "confidence": 0.0-1.0,
          "rawQuery": "original_query"
        }
        
        If intent is unclear, return UNKNOWN. Do not add any text outside the JSON.
    """.trimIndent()

    suspend fun analyzeQuery(query: String, history: List<String> = emptyList()): IntentResult = withContext(Dispatchers.IO) {
        val currentDate = java.time.LocalDate.now().toString()
        val currentTime = java.time.LocalTime.now().toString().take(5)
        
        val contextPrompt = """
            CONTEXT:
            - Current Date: $currentDate
            - Current Time: $currentTime
            - App: Luxe Dining AI Assistant
            
            CONVERSATION HISTORY (Last 5 turns):
            ${history.takeLast(5).joinToString("\n")}
            
            NEW USER QUERY:
            "$query"
        """.trimIndent()

        try {
            val response = model.generateContent(
                content {
                    text(systemInstruction)
                    text(contextPrompt)
                }
            )
            
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: ""
            Log.d(TAG, "Gemini Raw Output: $jsonText")
            
            val result = gson.fromJson(jsonText, IntentResult::class.java)
            result.copy(rawQuery = query)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing query: ${e.message}", e)
            IntentResult(
                intent = UserIntent.UNKNOWN,
                language = "unknown",
                confidence = 0.0,
                rawQuery = query
            )
        }
    }
}
