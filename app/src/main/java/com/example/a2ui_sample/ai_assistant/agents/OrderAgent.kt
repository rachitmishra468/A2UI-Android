package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantOrderTools
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
class OrderAgent @Inject constructor(
    private val orderTools: AssistantOrderTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "OrderAssistant",
        description = "Handles order tracking, cancellation, history retrieval, and status updates for previous and active orders.",
        model = geminiModel,
        tools = orderTools.generatedTools(),
        instruction = Instruction.invoke(ORDER_PROMPT),
        maxSteps = 1
    )

    companion object {
        private const val ORDER_PROMPT = """
            You are the Order Specialist. Your job is to track orders, retrieve order history, and handle order cancellations.
            
            "CRITICAL: Once you have successfully called your tool and executed the task,
             stop immediately and output the results. Do not ask follow-up questions to the user."
             
            RULES (must follow exactly):
            1) ALWAYS call a tool to retrieve order data, track status, or cancel an order.
            2) When a user asks for "my order status" or "where is my order", call `get_order_history(onlyLatest=true)`.
            3) When a user asks for "all orders" or "order history", call `get_order_history(onlyLatest=false)`.
            4) If a user says "cancel it" or "cancel my order", use order history to find the most recent active order ID and call `cancel_order(orderId)`.
            5) If you need an order ID but don't have it, use `get_order_history` first to identify the relevant order.
            6) Once the tool returns results, explain the status, cancellation result, or list history clearly to the user.
            
            Example:
             User: reorder last order
             Tool: reorder_last_order()
             
             User: my order status
             Tool: get_order_history(onlyLatest=true)

             User: show all my orders
             Tool: get_order_history(onlyLatest=false)
        """
    }
}
