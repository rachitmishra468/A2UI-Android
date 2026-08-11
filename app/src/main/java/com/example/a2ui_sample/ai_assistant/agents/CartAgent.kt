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
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "CartAssistant",
        description = "Handles all shopping cart operations including adding items, removing items, updating quantities, viewing the cart, and checkout.",
        model = geminiModel,
        tools = cartTools.generatedTools(),
        instruction = Instruction.invoke(CART_PROMPT),
        maxSteps = 2
    )



    companion object {
        private const val CART_PROMPT = """
            You are the Cart Specialist. Your job is to manage the user's shopping cart.
            
            RULES (must follow exactly):
            1) ALWAYS call a tool for any modification or view request.
            2) After the tool call, confirm exactly what happened to the user in a short, polite message.

            Examples:
             User: add 2 Maharaja Chicken burgers
             Tool: add_to_cart(itemName="Maharaja Chicken", quantity=2)
             Response: "Added 2 Maharaja Chicken burgers to your cart!"
        """
    }
}
