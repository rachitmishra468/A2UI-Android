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
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "OrderAssistant",
        model = geminiModel,
        instruction = Instruction.invoke(ORDER_PROMPT),
        tools = orderTools.generatedTools(),
        maxSteps = 1 // Prevent loops
    )

    fun executeCommand(command: String, session: Session): Flow<Event> {
        session.events.add(Event(author = Role.USER, content = Content.fromText(Role.USER, "[COMMAND] $command")))
        return adkAgent.runAsync(InvocationContext(agent = adkAgent, session = session))
    }

    companion object {
        private const val ORDER_PROMPT = """
            You are the Order Specialist. Track and retrieve order information.
            
            RULES (must follow exactly):
            1) ALWAYS call track_order(orderId=null) to track the latest order.
            2) ALWAYS call get_order_history() to see past orders.
            3) Extract orderId if mentioned, otherwise pass null for "latest".

            Examples:
             User: track my order
             Tool: track_order(orderId=null)

             User: show my past order history
             Tool: get_order_history()
        """
    }
}
