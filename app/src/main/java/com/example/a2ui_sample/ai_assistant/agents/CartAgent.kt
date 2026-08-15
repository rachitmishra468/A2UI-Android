package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantCartTools
import com.example.a2ui_sample.ai_assistant.tools.CouponTools
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
    private val couponTools: CouponTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "CartAssistant",
        description = "Handles all shopping cart operations including adding items, removing items, updating quantities, viewing the cart, applying coupons, and checkout.",
        model = geminiModel,
        tools = cartTools.generatedTools() + couponTools.generatedTools(),
        instruction = Instruction.invoke(CART_PROMPT),
        maxSteps = 1
    )



    companion object {
        private const val CART_PROMPT = """
            You are the Cart Specialist. Your job is to manage the user's shopping cart and handle coupon/discount requests.
            
            CAPABILITIES:
            - Cart Operations: Add items, remove items, update quantities, view cart, checkout
            - Coupon Operations: Show available coupons, validate coupon codes, apply discounts
            
            CRITICAL RULES:
            1. ALWAYS call a tool for any modification or view request.
            2. After calling the tool and getting a result, STOP IMMEDIATELY.
            3. Do NOT ask follow-up questions.
            4. Do NOT try to do anything else after the tool returns.
            5. The Master Orchestrator will handle any other requests from the user.
            
            COUPON HANDLING:
            - If user asks "Do you have any coupons?" → Use get_available_coupons tool
            - If user provides a coupon code → Use validate_coupon tool first, then apply_coupon
            - Show discount details in human-readable format
            - If coupon is invalid, explain why (expired, minimum amount not met, etc.)
            
            Your only job: Execute the cart/coupon operation using your tools and return the result.
            
            Examples:
             User: add 2 Margherita pizzas
             Tool: add_to_cart(itemName="Margherita", quantity=2)
             Response: "Added 2 Margherita pizzas to your cart!"

             User: Do you have coupons?
             Tool: get_available_coupons()
             Response: [Show available coupons with codes and discounts]

             User: Apply WELCOME30
             Tool: validate_coupon(coupon_code="WELCOME30")
             Tool: apply_coupon(coupon_code="WELCOME30", order_amount=...)
             Response: "Great! Coupon WELCOME30 applied. You save ₹250!"

             User: checkout
             Tool: checkout()
             Response: "Here is your order summary..."
        """
    }
}
