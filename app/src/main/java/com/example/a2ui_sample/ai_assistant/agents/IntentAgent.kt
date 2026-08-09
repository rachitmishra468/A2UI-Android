package com.example.a2ui_sample.ai_assistant.agents

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.orchestration.AssistantIntent
import com.example.a2ui_sample.domain.model.IntentResult
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.models.LlmRequest
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.GenerateContentConfig
import com.google.adk.kt.types.Role
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IntentAgent
 * Highly-optimized Router for restaurant domain.
 * Identifies if a query is a single intent or a multi-step workflow.
 */
@Singleton
class IntentAgent @Inject constructor() {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)
    private val gson = Gson()

    val adkAgent = LlmAgent(
        name = "IntentAssistant",
        model = geminiModel,
        instruction = Instruction.invoke(MASTER_AGENT_PROMPT)
    )

    suspend fun analyzeIntent(query: String, history: List<Content>): AssistantIntent {
        val request = LlmRequest(
            model = geminiModel,
            config = GenerateContentConfig(
                systemInstruction = Content.fromText(Role.SYSTEM, MASTER_AGENT_PROMPT)
            ),
            contents = history + Content.fromText(Role.USER, query)
        )

        return try {
            val response = geminiModel.generateContent(request).first()
            val jsonText = response.content?.parts?.firstOrNull()?.text
                ?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: ""
            
            Log.d("AssistantFlow", "🧠 IntentAgent JSON: $jsonText")
            val result = gson.fromJson(jsonText, IntentResult::class.java)
            
            when (result.mode) {
                "TOOL_WORKFLOW" -> {
                    if (!result.tasks.isNullOrEmpty()) {
                        AssistantIntent.Workflow(result.tasks)
                    } else {
                        AssistantIntent.Unknown("Tool workflow requested but no tasks provided.")
                    }
                }
                "INTENT" -> AssistantIntent.SingleIntent(result)
                else -> AssistantIntent.Unknown("Unknown mode: ${result.mode}")
            }
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Intent Analysis Error: ${e.message}", e)
            AssistantIntent.Unknown(e.message)
        }
    }

    companion object {
        private val MASTER_AGENT_PROMPT = """
            You are the Restaurant Master Agent (Principal Architect). 
            Your only job is to decide the execution plan: "INTENT" or "TOOL_WORKFLOW".

            CONVERSATIONAL CONTEXT RULES:
            - ITEM NAMES: Match menu exactly. Resolves "it", "that", "the first one".
            - MULTILINGUAL: Translate Hindi, Urdu, Hinglish, Arabic, etc. to English intent/entities.
            - COMPOUND REQUESTS: If user asks for multiple things, ALWAYS return "TOOL_WORKFLOW".

            WORKFLOW EXAMPLES (FOLLOW EXACTLY):
            1. User: "Add a Big Mac to my cart, then show my past order history and track the latest one"
               Output: {
                 "mode": "TOOL_WORKFLOW",
                 "tasks": [
                   {"tool": "ADD_TO_CART", "itemName": "Big Mac", "quantity": 1},
                   {"tool": "ORDER_HISTORY"},
                   {"tool": "TRACK_ORDER"}
                 ]
               }

            2. User: "2 महाराजा चिकन बर्गर और 1 लार्ज फ्राइज़ कार्ट में डालो और फिर मेरा कार्ट दिखाओ"
               Output: {
                 "mode": "TOOL_WORKFLOW",
                 "tasks": [
                   {"tool": "ADD_TO_CART", "itemName": "Maharaja Chicken Burger", "quantity": 2},
                   {"tool": "ADD_TO_CART", "itemName": "Large Fries", "quantity": 1},
                   {"tool": "SHOW_CART"}
                 ]
               }

            3. User: "Show me all vegetarian burgers and sides under ₹250"
               Output: {
                 "mode": "INTENT",
                 "intent": "SEARCH_MENU",
                 "diet": "veg",
                 "priceLimit": 250,
                 "category": "burgers and sides"
               }

            INTENT TYPES:
            SEARCH_MENU, ADD_TO_CART, REMOVE_FROM_CART, UPDATE_CART, SHOW_CART, CHECKOUT, 
            BOOK_TABLE, LIST_BOOKINGS, TRACK_ORDER, ORDER_HISTORY, SUBMIT_FEEDBACK

            Output must be JSON only. No markdown.
        """.trimIndent()
    }
}
