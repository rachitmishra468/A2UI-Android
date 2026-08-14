package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootRestaurantAgent @Inject constructor(
    private val menuAgent: MenuAgent,
    private val cartAgent: CartAgent,
    private val bookingAgent: BookingAgent,
    private val orderAgent: OrderAgent,
    private val feedbackAgent: FeedbackAgent
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)
    val adkAgent = LlmAgent(
        name = "RestaurantMaster",
        model = geminiModel,
        description = "Router agent that orchestrates and chains specialized sub-agents for multi-intent requests. and root agent name is Rango AI Assistant",
        instruction = Instruction.invoke(ROOT_AGENT_PROMPT),
        subAgents = listOf(
            menuAgent.adkAgent,
            cartAgent.adkAgent,
            bookingAgent.adkAgent,
            orderAgent.adkAgent,
            feedbackAgent.adkAgent
        ),
        // Allow multiple delegation steps so the Root agent can sequentially call
        // multiple specialists for multi-intent user requests (e.g., "book a table and show my cart").
        // Increased from 1 to 3 to permit several transfers within a single execution.
        maxSteps = 3
    )

    companion object {

        private const val ROOT_AGENT_PROMPT = """
            You are the Master Restaurant Orchestrator.your name is Rango AI Assistant ,  Your job is to fulfill ALL parts of a user's request by delegating to specialized agents.
            
            YOUR STRATEGY:
            1. Analyze the user's message and identify every individual intent.
            2. Delegate sequentially to the appropriate specialists using `transfer_to_agent`.
            3. Once specialists have finished, provide a VERY BRIEF (one line) confirmation.
            
            CRITICAL RULES:
            - DO NOT repeat information already shown in UI cards (like item names, prices, or status).
            - DO NOT hallucinate about availability. If a tool returns items, they ARE available unless explicitly stated otherwise in the data.
            - If a specialist has already provided a response or called a tool, just say something like "Done! I've handled your request." or "Here is what I found for you:".
            - Keep your final response extremely concise to avoid redundant double-responses.

            Routing:
            - Booking/Table -> BookingAssistant
            - Cart/Add/Remove/View -> CartAssistant
            - Menu/Search/Details -> MenuAssistant
            - Orders/Tracking -> OrderAssistant
            - Feedback -> FeedbackAssistant
        """
    }

    // Expose sub-agents by name so the orchestrator can run them individually when following transfer events.
    fun getAgentByName(name: String): LlmAgent? = when (name) {
        "MenuAssistant" -> menuAgent.adkAgent
        "CartAssistant" -> cartAgent.adkAgent
        "BookingAssistant" -> bookingAgent.adkAgent
        "OrderAssistant" -> orderAgent.adkAgent
        "FeedbackAssistant" -> feedbackAgent.adkAgent
        else -> null
    }
}
