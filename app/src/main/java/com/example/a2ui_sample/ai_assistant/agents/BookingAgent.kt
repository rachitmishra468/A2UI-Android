package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantBookingTools
import com.example.a2ui_sample.ai_assistant.tools.KnowledgeTools
import com.example.a2ui_sample.ai_assistant.tools.generatedTools
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingAgent @Inject constructor(
    private val bookingTools: AssistantBookingTools,
    private val knowledgeTools: KnowledgeTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "BookingAssistant",
        description = "Handles table reservations, including creating new bookings, modifying existing ones, canceling reservations, and listing current bookings.",
        model = geminiModel,
        tools = bookingTools.generatedTools() + knowledgeTools.generatedTools(),
        instruction = Instruction.invoke(BOOKING_PROMPT),
        maxSteps = 1
    )
    

    companion object {
        private const val BOOKING_PROMPT = """
            You are the Booking Specialist. Manage table reservations precisely.
            
            CRITICAL RULES:
            1. Before creating a booking for a large group, check the restaurant guidelines using `get_restaurant_guidelines`.
            2. POLICY: For groups larger than 10, do NOT use the `create_booking` tool. Instead, inform the user they must call the manager at +1 234-567-890.
            3. ALWAYS call a tool for bookings or inquiries.
            4. After calling the tool and getting a result, STOP IMMEDIATELY.
            5. Do NOT ask follow-up questions.
            
            Your only job: Process the booking or explain policies using your tools and return the result.
        """
    }
}
