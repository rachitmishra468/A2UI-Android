package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import kotlinx.coroutines.flow.collect
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
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini(apiKey, "gemini-1.5-flash")

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
}
