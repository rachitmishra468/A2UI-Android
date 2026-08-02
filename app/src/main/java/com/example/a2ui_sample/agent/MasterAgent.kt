package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.google.adk.kt.agents.Instruction
import kotlin.time.ExperimentalTime

private const val TAG = "ADK_AGENT"

/**
 * Master Agent that delegates to specialized agents.
 * Principal Architect Implementation with robust short-circuit logic and context-aware routing.
 */
class ADKRestaurantMasterAgent(
    private val menuRepository: MenuRepository,
    private val orchestratorTools: OrchestratorTools
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel by lazy { Gemini("gemini-3.6-flash", apiKey) }
    
    private val menuAgent by lazy { MenuAgent(menuRepository, geminiModel) }
    private val cartAgent by lazy { CartAgent(menuRepository, geminiModel) }
    private val bookingAgent by lazy { BookingAgent(menuRepository, geminiModel) }

    @OptIn(ExperimentalTime::class)
    private val sessionKey by lazy { SessionKey("RestaurantApp", "DefaultUser", "Session-Restaurant") }
    @OptIn(ExperimentalTime::class)
    private val session by lazy { Session(sessionKey) }

    private val masterTools: MasterAgentTools by lazy {
        MasterAgentTools(
            menuAgent = menuAgent,
            cartAgent = cartAgent,
            bookingAgent = bookingAgent,
            session = session
        )
    }

    @OptIn(ExperimentalTime::class)
    private val adkAgent: LlmAgent by lazy {
        val tools = masterTools.generatedTools()

        LlmAgent(
            name = "RestaurantMasterAgent",
            model = geminiModel,
            instruction = Instruction(
                """
                You are a strict Routing Agent for a Restaurant. 
                Your ONLY purpose is to delegate requests to the correct specialist.
                
                CRITICAL - CONTEXT DETECTION:
                1. If the user provides a NUMBER (e.g. "4", "for 5", "two people"), a TIME (e.g. "8 PM", "tomorrow at 7"), or answers a question about booking, ALWAYS use 'delegate_to_booking_agent'.
                2. If the user mentions food, cravings, ingredients, or "menu", use 'delegate_to_menu_agent'.
                3. If the user mentions "cart", "add", "order", "checkout", or "buy", use 'delegate_to_cart_agent'.
                
                RULES:
                - Always return exactly one tool invocation.
                - Do NOT respond in plain text.
                - Just call the tool and pass the user's input.
                """.trimIndent()
            ),
            tools = tools,
            maxSteps = 1 
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun processQuery(userMessage: String): List<String> {
        Log.d(TAG, "======== MasterAgent Reasoning START: '$userMessage' ========")
        orchestratorTools.reset()
        masterTools.reset()
        masterTools.setUserQuery(userMessage)

        val q = userMessage.trim().lowercase()

        // --- ENHANCED QUICK RULE-BASED SHORT-CIRCUITS ---

        // 1. Booking Follow-up Detection (Extremely common turns)
        val bookingKeywords = Regex("\\b(book|reserve|table|reservation)\\b", RegexOption.IGNORE_CASE)
        val isJustNumber = Regex("^(?:for\\s+)?\\d+\\s*(?:members?|people|persons?|guests?)?$", RegexOption.IGNORE_CASE)
        val isJustTime = Regex("^(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?$", RegexOption.IGNORE_CASE)
        
        if (bookingKeywords.containsMatchIn(q) || isJustNumber.matches(q) || isJustTime.matches(q)) {
            Log.i(TAG, "   >> Routing to BOOKING: Keyword/Number/Time detected.")
            masterTools.delegateBooking(userMessage)
            masterTools.getLastResponse()?.let { return responseBuilder.build(it) }
        }

        // 2. Add/Order Short-Circuit
        val addIntent = Regex("\\b(add|order|buy|put)\\b", RegexOption.IGNORE_CASE)
        if (addIntent.containsMatchIn(q) && !q.contains("table")) { // Avoid "order a table" confusion
            Log.i(TAG, "   >> Routing to CART: Add/Order intent detected.")
            masterTools.delegateCart(userMessage)
            masterTools.getLastResponse()?.let { return responseBuilder.build(it) }
        }

        // 3. View Cart Short-Circuit
        val cartViewIntent = Regex("\\b(show|view|my)\\s+(cart|shopping)\\b", RegexOption.IGNORE_CASE)
        if (cartViewIntent.containsMatchIn(q) || q == "cart") {
            Log.i(TAG, "   >> Routing to CART: View intent detected.")
            masterTools.delegateCart(userMessage)
            masterTools.getLastResponse()?.let { return responseBuilder.build(it) }
        }

        // 4. Menu / Food Search Short-Circuit
        val menuIntent = Regex("\\b(show|view|get|list|search|find|want|crave|need)\\b", RegexOption.IGNORE_CASE)
        val commonFoodItems = Regex("\\b(burger|pizza|dosa|idli|paneer|veg|chicken|meal|drink|beverage|spicy|dessert)\\b", RegexOption.IGNORE_CASE)
        
        if (menuIntent.containsMatchIn(q) || commonFoodItems.containsMatchIn(q) || q.contains("what can i order") || q.contains("menu items")) {
            Log.i(TAG, "   >> Routing to MENU: Menu/Food intent detected.")
            masterTools.delegateMenu(userMessage)
            masterTools.getLastResponse()?.let { return responseBuilder.build(it) }
        }

        // --- LLM REASONING FLOW (ADK) ---
        Log.d(TAG, "   >> No short-circuit match. Falling back to Gemini reasoning.")

        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, userMessage)
        )
        
        var agentResponseText: String? = null

        try { 
            adkAgent.runAsync(context).collect { event ->
                Log.d(TAG, "   -> ADK Event: author=${event.author}")
                val text = event.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank() && event.author == "RestaurantMasterAgent") {
                    agentResponseText = text
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Master Error: ${e.message}")
            return listOf(responseBuilder.build(AgentResponse.Error("Error: ${e.message}")).last())
        }

        val toolResponse = masterTools.getLastResponse()
        val orchestratorResponse = orchestratorTools.getLastResponse()

        val finalResponse = when {
            toolResponse != null -> toolResponse
            orchestratorResponse != null -> orchestratorResponse
            !agentResponseText.isNullOrBlank() -> AgentResponse.Message(agentResponseText!!)
            else -> AgentResponse.Message("I'm sorry, I couldn't process that request.")
        }

        Log.d(TAG, "======== MasterAgent Reasoning END. Result: ${finalResponse.javaClass.simpleName} ========")
        return responseBuilder.build(finalResponse)
    }
}
