package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantCartTools
import com.example.a2ui_sample.ai_assistant.tools.generatedTools
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.events.Event
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartAgent @Inject constructor(
    private val cartTools: AssistantCartTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "CartAssistant",
        description = "Handles all shopping cart operations including adding items, removing items, updating quantities, viewing the cart, and checkout.",
        model = geminiModel,
        tools = cartTools.generatedTools(),
        instruction = Instruction.invoke(CART_PROMPT),
        maxSteps = 1
    )



    companion object {
        private const val CART_PROMPT = """
            You are the Cart Specialist. Your job is to manage the user's shopping cart.
            
            CRITICAL RULES:
            1. ALWAYS call a tool for any modification or view request.
            2. After calling the tool and getting a result, STOP IMMEDIATELY.
            3. Do NOT ask follow-up questions.
            4. Do NOT try to do anything else after the tool returns.
            5. The Master Orchestrator will handle any other requests from the user.
            
            Your only job: Execute the cart operation using your tool and return the result.
            
            Examples:
             User: add 2 Maharaja Chicken burgers
             Tool: add_to_cart(itemName="Maharaja Chicken", quantity=2)
             Response: "Added 2 Maharaja Chicken burgers to your cart!"

             User: checkout
             Tool: checkout()
             Response: "Here is your order summary..."

             User: reorder my last meal
             Tool: transfer_to_agent(agent="OrderAssistant", query="reorder my last order")
        """
    }
}
