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

private const val TAG = "A2UI_FLOW"

/**
 * Master Agent that delegates to specialized agents.
 * Principal Architect Implementation with robust short-circuit logic and context-aware routing.
 */
class ADKRestaurantMasterAgent(
    private val menuRepository: MenuRepository,
    private val orchestratorTools: OrchestratorTools
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }
    // Diagnostic: log whether API key is present (do not print full key)
    private val apiKey = BuildConfig.GEMINI_API_KEY.also { key ->
        try {
            Log.d(TAG, "GEMINI_API_KEY present: ${""}" + (!key.isNullOrBlank()).toString())
        } catch (t: Throwable) {
            Log.w(TAG, "GEMINI_API_KEY diagnostic failed: ${""}" + (t.message ?: "<none>"))
        }
    }

    // Wrap Gemini model creation with diagnostic logs so failures surface clearly
    private val geminiModel by lazy {
        try {
            val model = Gemini("gemini-3.6-flash", apiKey)
            Log.d(TAG, "Gemini model created successfully")
            model
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Gemini model: ${""}" + (e.message ?: "<no message>"), e)
            throw e
        }
    }
    
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
                You are a premium Restaurant Concierge and Ordering Assistant. 
                Your job is to provide a seamless, high-end experience for Luxe Dining customers.
                
                You have three specialist agents at your disposal:
                1. Menu Specialist: For browsing, searching, and recommending dishes.
                2. Cart Specialist: For managing the shopping cart, updating quantities, and checking out.
                3. Booking Specialist: For reserving tables and checking availability.
                
                CRITICAL - CONTEXT DETECTION:
                - If the user provides details for a booking (people, time), use 'delegate_to_booking_agent'.
                - If the user wants to add items, view their cart, or pay, use 'delegate_to_cart_agent'.
                - If the user wants to see the menu, wants recommendations, or has food questions, use 'delegate_to_menu_agent'.
                
                TONE:
                Professional, helpful, and sophisticated.
                """.trimIndent()
            ),
            tools = tools,
            maxSteps = 1 
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun processQuery(userMessage: String): List<String> {
        Log.d(TAG, "1. MasterAgent START: '$userMessage'")
        // Diagnostic: attempt to access some lazy fields to surface initialization errors early
        try {
            // Accessing sessionKey and geminiModel here will log creation diagnostics (if any)
            val sk = sessionKey.toString()
            Log.d(TAG, "2. SessionKey initialized: $sk")
        } catch (t: Throwable) {
            Log.w(TAG, "2. SessionKey diagnostic failed: ${t.message}")
        }
        try {
            // This will force Gemini model creation and log success/failure above
            geminiModel.hashCode()
            Log.d(TAG, "3. Gemini model initialized")
        } catch (t: Throwable) {
            Log.e(TAG, "3. Gemini model initialization failed: ${t.message}")
        }
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
        val menuIntent = Regex("\\b(show|view|get|list|search|find|want|crave|need|menu|food|order|eat)\\b", RegexOption.IGNORE_CASE)
        val commonFoodItems = Regex("\\b(burger|pizza|dosa|idli|paneer|veg|chicken|meal|drink|beverage|spicy|dessert|item)\\b", RegexOption.IGNORE_CASE)
        
        if (menuIntent.containsMatchIn(q) || commonFoodItems.containsMatchIn(q) || q.contains("order") || q.contains("menu")) {
            Log.d(TAG, "   >> Routing to MENU: Menu/Food intent detected (Short-circuit).")
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

        Log.d(TAG, "4. MasterAgent END. Result: ${finalResponse.javaClass.simpleName}")
        return responseBuilder.build(finalResponse)
    }
}
