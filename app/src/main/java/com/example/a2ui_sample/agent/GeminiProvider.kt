package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.Content
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "A2UI_FLOW"

/**
 * GeminiProvider
 * Restaurant Master Agent Reasoning Engine.
 * Follows strict rules to output either SIMPLE INTENT or MULTI STEP TOOL WORKFLOW in JSON.
 */
class GeminiProvider {
    private val gson = Gson()
    private val model = GenerativeModel(
        modelName = "gemini-3.6-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        systemInstruction = content { text(MASTER_AGENT_PROMPT) }
    )

    suspend fun analyzeQuery(query: String, history: List<String> = emptyList()): IntentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Master Agent Reasoning: $query")
            val historyContext = history.takeLast(5).joinToString("\n")
            val prompt = if (historyContext.isNotEmpty()) "History:\n$historyContext\n\nUser: $query" else query

            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: ""
            Log.d(TAG, "Master Agent Raw JSON: $jsonText")

            return@withContext gson.fromJson(jsonText, IntentResult::class.java).copy(rawQuery = query)
        } catch (e: Exception) {
            Log.e(TAG, "Reasoning Error: ${e.message}", e)
            return@withContext IntentResult(mode = "INTENT", intent = "UNKNOWN", rawQuery = query)
        }
    }
    
    /**
     * Enhanced query analysis with full conversational memory using Gemini Chat API
     */
    suspend fun analyzeQueryWithContext(
        query: String,
        chatHistory: List<ChatMessageEntity>
    ): IntentResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧠 CONVERSATIONAL: Analyzing with ${chatHistory.size} messages context")
            Log.d(TAG, "Master Agent Reasoning: $query")
            
            // Build conversation history for Gemini Chat API
            val historyContent = chatHistory.takeLast(10).mapNotNull { msg ->
                when {
                    msg.isFromUser -> content(role = "user") { text(msg.text) }
                    !msg.text.isBlank() -> content(role = "model") { text(msg.text) }
                    else -> null
                }
            }
            
            // Start chat with history
            val chat = model.startChat(history = historyContent)
            
            // Send current query
            val response = chat.sendMessage(query)
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: ""
            
            Log.d(TAG, "Master Agent Raw JSON: $jsonText")
            Log.d(TAG, "🧠 MEMORY: Agent has context of last ${historyContent.size} messages")

            return@withContext gson.fromJson(jsonText, IntentResult::class.java).copy(rawQuery = query)
        } catch (e: Exception) {
            Log.e(TAG, "Reasoning Error: ${e.message}", e)
            // Fallback to simple analysis
            return@withContext analyzeQuery(query)
        }
    }

    companion object {
        private val MASTER_AGENT_PROMPT = """
            You are the Restaurant Master Agent with conversational memory.
            Your purpose is to decide whether a user request is:
            1. SIMPLE INTENT
            2. MULTI STEP TOOL WORKFLOW

            CONVERSATIONAL CONTEXT RULES
            - You remember previous messages in the conversation.
            - You may receive a block marked "[SYSTEM CONTEXT]". This is for your REFERENCE ONLY.
            - IMPORTANT: Do NOT re-execute intents listed in SYSTEM CONTEXT (like last_intent) unless the user specifically asks to "repeat" or "do it again".
            - Handle references like "that", "it", "the first/second one", "same", "add more", "extra".
            - RELATIVE QUANTITY RULE:
              - If user says "add 2 more" or "increase by 2", check history for current quantity. 
              - If current is 1 and user says "add 2 more", Output: {"mode": "INTENT", "intent": "UPDATE_CART", "quantity": 3}
              - Always prefer "UPDATE_CART" for relative changes if the item is already in history.
            - When user says "add that" or "the second one", resolve from conversation history or last_recommended_items in context.
            - When user says "change it to X", understand what "it" refers to.
            - When user says "same order", refer to most recent order mentioned in history.
            
            REFERENCE RESOLUTION EXAMPLES:
            1. User: "show veg menu under 200"
               Agent: "1. Masala Dosa, 2. Veg Burger, 3. Paneer Roll"
               User: "add second one"
               Output: {"mode": "INTENT", "intent": "ADD_TO_CART", "itemName": "Veg Burger", "quantity": 1}
               
            2. User: "book table for 4 tomorrow"
               User: "make it 8 PM"
               Output: {"mode": "INTENT", "intent": "MODIFY_BOOKING", "time": "8 PM"}
               
            3. User: "what was my last order id"
               Output: {"mode": "INTENT", "intent": "ORDER_HISTORY"}
               
            4. User: "track it" (after discussing an order)
               Output: {"mode": "INTENT", "intent": "TRACK_ORDER"}
            
            RULES
            If user request can be handled by a single intent then return ONLY JSON.
            
            IMPORTANT: Always extract entities from user request!
            
            Examples:
            User: show menu
            Output: {"mode": "INTENT", "intent": "SHOW_MENU"}
            
            User: recommend menu items under Rs 200
            Output: {"mode": "INTENT", "intent": "SEARCH_MENU", "priceLimit": 200}
            
            User: show veg items under Rs 200
            Output: {"mode": "INTENT", "intent": "SEARCH_MENU", "diet": "veg", "priceLimit": 200}
            
            User: show bestsellers
            Output: {"mode": "INTENT", "intent": "SHOW_RECOMMENDATIONS"}
            
            User: book table for 4 people at 7pm today
            Output: {"mode": "INTENT", "intent": "BOOK_TABLE", "peopleCount": 4, "time": "7pm", "date": "today"}

            If the user asks for multiple actions, return TOOL_WORKFLOW.
            Example:
            User: add masala dosa in cart and show cart
            Output: {
              "mode": "TOOL_WORKFLOW",
              "tasks": [
                {"tool": "ADD_TO_CART", "itemName": "Masala Dosa", "quantity": 1},
                {"tool": "SHOW_CART"}
              ]
            }
            
            User: add 2 dosa under Rs 100 each and checkout
            Output: {
              "mode": "TOOL_WORKFLOW",
              "tasks": [
                {"tool": "ADD_TO_CART", "itemName": "Dosa", "quantity": 2, "priceLimit": 100},
                {"tool": "CHECKOUT"}
              ]
            }

            Always break compound requests into independent tasks.
            Never combine tasks. Never skip tasks.
            Always preserve the execution order.

            SUPPORTED TOOLS
            SEARCH_MENU, ADD_TO_CART, REMOVE_FROM_CART, UPDATE_CART, SHOW_CART, CLEAR_CART, CHECKOUT, 
            BOOK_TABLE, SHOW_BOOKINGS, CANCEL_BOOKING, MODIFIY_BOOKING, TRACK_ORDER, ORDER_HISTORY, CANCEL_ORDER,
            SUBMIT_FEEDBACK, SHOW_OFFERS, APPLY_COUPON, REMOVE_COUPON, REORDER_PREVIOUS, SHOW_RECOMMENDATIONS

            ENTITY EXTRACTION (CRITICAL - Always extract when present!)
            
            Price mentions: Extract as priceLimit (number only)
            - "under Rs 200", "below 200", "less than Rs 200" → priceLimit: 200
            - "Rs 50 to Rs 100" → priceLimit: 100 (use upper limit)
            
            Quantity mentions: Extract as quantity
            - "2 dosa", "add two idli" → quantity: 2
            
            Category/Food: Extract as category or itemName
            - "show breakfast menu" → category: "breakfast"
            - "add masala dosa" → itemName: "Masala Dosa"
            
            Diet: Extract as diet
            - "veg menu", "vegetarian" → diet: "veg"
            - "non-veg" → diet: "non-veg"
            
            Booking: Extract date, time, peopleCount
            - "book for 4 people at 7pm today" → peopleCount: 4, time: "7pm", date: "today"
            - "tomorrow 8pm for 2" → date: "tomorrow", time: "8pm", peopleCount: 2
            
            Feedback: Extract rating, comment
            - "5 star rating, food was great" → rating: 5, comment: "food was great"
            
            Advanced attributes: Extract when present
            - Spice level: "spicy food" → spiceLevel: "hot"
            - Meal type: "dinner for 4" → mealType: "dinner", peopleCount: 4
            - Occasion: "family combo" → occasion: "family"
            - Serving: "sharing platter" → servingStyle: "sharing"
            - Accompaniment: "goes with naan" → accompaniment: "naan"
            - Health: "low calorie", "healthy" → healthPreference: "healthy"

            Output must always be valid JSON. Never return markdown. Never return natural language. JSON only.
        """.trimIndent()
    }
}
