package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.models.LlmRequest
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.GenerateContentConfig
import com.google.adk.kt.types.Role
import com.google.gson.Gson
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import com.google.adk.kt.events.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RestaurantAgent
 * Principal Architect Implementation using Google ADK.
 */
@OptIn(ExperimentalTime::class)
@Singleton
class RestaurantAgent @Inject constructor(
    private val menuRepository: MenuRepository,
    private val restaurantTools: RestaurantTools
) {
    private val responseBuilder = A2UIResponseBuilder()
    private val gson = Gson()
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    private val sessionKey = SessionKey("RestaurantApp", "DefaultUser", "Session-Restaurant")
    private val session = Session(sessionKey)

    private val adkAgent = LlmAgent(
        name = "RestaurantAgent",
        model = geminiModel,
        instruction = Instruction.invoke(
            """
            You are a Restaurant Ordering Assistant.
            Your job is to help users manage their food orders and table bookings.
            Use the provided tools for:
            1. Searching the menu (search_menu)
            2. Adding items to cart (add_item_to_cart)
            3. Cart management (manage_cart)
            4. Checkout (checkout)
            5. Table reservations (book_table)
            6. Order tracking (track_order)
            
            Always confirm with the user after an action.
            """.trimIndent()
        ),
        tools = restaurantTools.generatedTools()
    )

    suspend fun processQuery(query: String): List<String> {
        Log.d("A2UI_FLOW", "RestaurantAgent processing query: '$query'")

        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, query)
        )

        var finalResponse: AgentResponse = AgentResponse.Message("I'm sorry, I couldn't process that.")

        try {
            adkAgent.runAsync(context).collect { event ->
                Log.d("A2UI_FLOW", "ADK Event: $event")
            }

            // ADK tools update the lastResponse in restaurantTools
            restaurantTools.lastResponse?.let {
                finalResponse = it
                restaurantTools.lastResponse = null
            }
        } catch (e: Exception) {
            Log.e("A2UI_FLOW", "ADK Error: ${e.message}", e)
            finalResponse = AgentResponse.Error("ADK Error: ${e.message}")
        }

        val uniqueId = "surf_${System.currentTimeMillis()}"
        return responseBuilder.buildWithId(finalResponse, uniqueId)
    }

    /**
     * Analyze the query to identify intent and extract entities.
     * Replaces the logic in GeminiProvider.
     */
    suspend fun analyzeIntent(query: String): IntentResult {
        Log.d("A2UI_FLOW", "RestaurantAgent analyzing intent for: '$query'")

        val request = LlmRequest(
            model = geminiModel,
            config = GenerateContentConfig(
                systemInstruction = Content.fromText(Role.SYSTEM, MASTER_AGENT_PROMPT)
            ),
            contents = listOf(Content.fromText(Role.USER, query))
        )

        return try {
            val response = geminiModel.generateContent(request).first()
            val jsonText = response.content?.parts?.firstOrNull()?.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: ""
            Log.d("A2UI_FLOW", "RestaurantAgent Intent JSON: $jsonText")
            gson.fromJson(jsonText, IntentResult::class.java).copy(rawQuery = query)
        } catch (e: Exception) {
            Log.e("A2UI_FLOW", "Intent Analysis Error: ${e.message}", e)
            IntentResult(mode = "INTENT", intent = "UNKNOWN", rawQuery = query)
        }
    }

    /**
     * Enhanced query analysis with full conversational memory using ADK Session
     */
    suspend fun analyzeQueryWithContext(
        query: String,
        chatHistory: List<ChatMessageEntity>
    ): IntentResult = withContext(Dispatchers.IO) {
        try {
            Log.d("A2UI_FLOW", "🧠 CONVERSATIONAL (ADK): Analyzing with ${chatHistory.size} messages context")
            Log.d("A2UI_FLOW", "Master Agent Reasoning: $query")

            // Create a temporary session to hold the history
            val tempSessionKey = SessionKey("RestaurantApp", "DefaultUser", "Temp-Session-${System.currentTimeMillis()}")
            val tempSession = Session(tempSessionKey)

            // Map ChatMessageEntity to ADK Events
            val historyEvents = chatHistory.takeLast(10).map { msg ->
                if (msg.isFromUser) {
                    Event(
                        author = Role.USER,
                        content = Content.fromText(Role.USER, msg.text)
                    )
                } else {
                    Event(
                        author = adkAgent.name,
                        content = Content.fromText(Role.MODEL, msg.text)
                    )
                }
            }
            tempSession.events.addAll(historyEvents)

            // In ADK, we use InvocationContext to wrap session and agent
            val context = InvocationContext(
                session = tempSession,
                agent = adkAgent,
                userContent = Content.fromText(Role.USER, query)
            )

            // However, we want the IntentResult JSON, not the tool execution.
            // So we manually build the LlmRequest with history from the session events.
            val historyContents = historyEvents.mapNotNull { it.content }
            val currentQueryContent = Content.fromText(Role.USER, query)

            val request = LlmRequest(
                model = geminiModel,
                config = GenerateContentConfig(
                    systemInstruction = Content.fromText(Role.SYSTEM, MASTER_AGENT_PROMPT)
                ),
                contents = historyContents + currentQueryContent
            )

            val response = geminiModel.generateContent(request).first()
            val jsonText = response.content?.parts?.firstOrNull()?.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: ""

            Log.d("A2UI_FLOW", "Master Agent Raw JSON: $jsonText")
            Log.d("A2UI_FLOW", "🧠 MEMORY: Agent has context of last ${historyEvents.size} messages")

            return@withContext gson.fromJson(jsonText, IntentResult::class.java).copy(rawQuery = query)
        } catch (e: Exception) {
            Log.e("A2UI_FLOW", "Reasoning Error: ${e.message}", e)
            // Fallback to simple analysis
            return@withContext analyzeIntent(query)
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
            - If user asks for something new (like "I want a combo") and "iced tea" is in SYSTEM CONTEXT from a previous turn, do NOT add it to cart again unless explicitly asked.
            - LANGUAGE & TRANSLATION RULE:
              - If the user speaks in a language other than English (e.g., Hindi, Hinglish), TRANSLATE the intent and entities to English.
              - ITEM NAMES MUST MATCH MENU EXACTLY: Refer to `last_menu_results` or `last_recommended_items` for exact spelling.
              - If item is not in history/recommendations, use common sense for translation (e.g., "पिज्जा" -> "Pizza", "कोला" -> "Coke").
              - DEDUCTION RULE: Do NOT add items that were not specifically requested. If you are unsure, do NOT guess.
              - Example: User says "2 महाराजा चिकन" -> itemName: "Maharaja Chicken" (NOT "Maharaja Chicken Burger" or "Maharaja Chicken burger").
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
