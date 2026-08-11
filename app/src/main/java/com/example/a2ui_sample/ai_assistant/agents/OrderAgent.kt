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
        description = "Handles order tracking, order history retrieval, and status updates for previous and active orders.",
        model = geminiModel,
        tools = orderTools.generatedTools(),
        instruction = Instruction.invoke(ORDER_PROMPT),
        maxSteps = 1
    )

    companion object {
        private const val ORDER_PROMPT = """
            You are the Order Specialist. Track and retrieve order information.
            
            "CRITICAL: Once you have successfully called your tool and executed the task,
             stop immediately and output the results. Do not ask follow-up questions to the user, 
             allowing the Master Orchestrator to handle any other pending requests."
             
            RULES (must follow exactly):
            1) ALWAYS call a tool to retrieve order data.
            2) Once the tool returns results, explain the status or list the history clearly to the user.
        """
    }
}
