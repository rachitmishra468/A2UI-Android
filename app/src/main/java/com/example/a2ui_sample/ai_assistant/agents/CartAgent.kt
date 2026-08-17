package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantCartTools
import com.example.a2ui_sample.ai_assistant.tools.CouponTools
import com.example.a2ui_sample.ai_assistant.tools.KnowledgeTools
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
    private val cartTools: AssistantCartTools,
    private val couponTools: CouponTools,
    private val knowledgeTools: KnowledgeTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "CartAssistant",
        description = "Handles all shopping cart operations including adding items, removing items, updating quantities, viewing the cart, applying coupons, and checkout.",
        model = geminiModel,
        tools = cartTools.generatedTools() + couponTools.generatedTools() + knowledgeTools.generatedTools(),
        instruction = Instruction.invoke(CART_PROMPT),
        maxSteps = 1
    )



    companion object {
        private const val CART_PROMPT = """
            You are the Cart Specialist. Your job is to manage the user's shopping cart and handle coupon/discount requests.
            
            CAPABILITIES:
            - Cart Operations: Add items, remove items, update quantities, view cart, checkout
            - Coupon Operations: Show available coupons, validate coupon codes, apply discounts
            - Policy Checks: Check delivery fees and cancellation rules using `get_restaurant_guidelines`.
            
            CRITICAL RULES:
            1. For any questions about delivery charges or minimum order for free delivery, use `get_restaurant_guidelines`.
            2. ALWAYS call a tool for any modification or view request.
            3. After calling the tool and getting a result, STOP IMMEDIATELY.
            4. Do NOT ask follow-up questions.
            
            Your only job: Execute the cart/coupon operation using your tools and return the result.
        """
    }
}
