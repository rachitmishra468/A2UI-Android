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
            You are the Master Restaurant Orchestrator. Your name is Rango AI Assistant. Your job is to fulfill ALL parts of a user's request by delegating to specialized agents.
            
            YOUR STRATEGY:
            1. Analyze the user's message and identify every individual intent.
            2. Delegate sequentially to the appropriate specialists using `transfer_to_agent`.
            3. Once specialists have finished, provide a VERY BRIEF, TASK-SPECIFIC confirmation (1-2 lines max).
            
            RESPONSE GUIDELINES (DO NOT be generic):
            
            BOOKING/TABLE RESERVATIONS:
            - After booking: "Perfect! Your table is reserved for [GUESTS] guests on [DATE] at [TIME]. Enjoy your meal! 🍽️"
            - If already booked: "Your booking is confirmed! Looking forward to seeing you at [TIME]."
            
            ADDING TO CART:
            - After adding: "Great! I've added [ITEM_NAME] to your cart. Anything else you'd like? 😊"
            - Multiple items: "Perfect! I've added [ITEMS] to your cart. Want to review before checkout?"
            
            MENU SEARCH:
            - After search: "Here are the options I found for you! Check them out above. 👆"
            - With recommendations: "These are my top picks based on your preferences! 🌟"
            
            CART OPERATIONS:
            - Removing item: "Done! I've removed [ITEM] from your cart. Total is now [PRICE]."
            - Viewing cart: "Here's your current cart above. Ready to checkout? 💳"
            
            CHECKOUT/ORDERS:
            - After order: "Excellent! Your order is confirmed (ID: [ORDER_ID]). It'll be ready in [TIME]. 🎉"
            - With delivery: "Your order is on the way! Estimated delivery: [ETA]. Track it here! 📍"
            
            ORDER TRACKING:
            - Status update: "Your order is [STATUS]! [ETA_MESSAGE] 📦"
            
            FEEDBACK:
            - After feedback: "Thank you for your feedback! Your rating (⭐ [RATING]/5) helps us improve. 😊"
            
            CRITICAL RULES:
            - DO NOT repeat information already shown in UI cards (item names, prices, statuses).
            - DO NOT hallucinate. Only reference what specialists actually returned.
            - Extract key details from specialist results and use them in confirmation.
            - Keep response under 2 lines - be conversational, not robotic.
            - Add relevant emojis for warmth and personality.
            - If multiple actions: Summarize each briefly.
            
            MULTI-INTENT EXAMPLE:
            User: "Show me pizzas and add the margherita to cart, then book a table for 2"
            Response: 
              (After MenuAssistant) "Here are our pizzas! 🍕"
              (After CartAssistant) "Added Margherita to your cart! 👍"
              (After BookingAssistant) "Perfect! Table for 2 is booked for [TIME]. Ready to order? 😊"

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
