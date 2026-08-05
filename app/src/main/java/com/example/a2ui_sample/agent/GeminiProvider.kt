package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.IntentResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
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

    companion object {
        private val MASTER_AGENT_PROMPT = """
            You are the Restaurant Master Agent.
            Your purpose is to decide whether a user request is:
            1. SIMPLE INTENT
            2. MULTI STEP TOOL WORKFLOW

            RULES
            If user request can be handled by a single intent then return ONLY JSON.
            Example:
            User: show menu
            Output: {"mode": "INTENT", "intent": "SHOW_MENU"}

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

            Always break compound requests into independent tasks.
            Never combine tasks. Never skip tasks.
            Always preserve the execution order.

            SUPPORTED TOOLS
            SEARCH_MENU, ADD_TO_CART, REMOVE_FROM_CART, UPDATE_CART, SHOW_CART, CLEAR_CART, CHECKOUT, 
            BOOK_TABLE, CANCEL_BOOKING, MODIFIY_BOOKING, TRACK_ORDER, ORDER_HISTORY, SEARCH_RESTAURANTS, 
            SUBMIT_FEEDBACK, SHOW_OFFERS, APPLY_COUPON, REMOVE_COUPON, REORDER_PREVIOUS, SHOW_RECOMMENDATIONS

            ENTITY EXTRACTION
            Menu Search: category, diet, priceLimit
            Booking: date, time, peopleCount
            Cart: itemName, quantity
            Feedback: rating, comment

            Output must always be valid JSON. Never return markdown. Never return natural language. JSON only.
        """.trimIndent()
    }
}
