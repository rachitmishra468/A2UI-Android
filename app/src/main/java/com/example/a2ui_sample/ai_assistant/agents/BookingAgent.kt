package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantBookingTools
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
class BookingAgent @Inject constructor(
    private val bookingTools: AssistantBookingTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "BookingAssistant",
        description = "Handles table reservations, including creating new bookings, modifying existing ones, canceling reservations, and listing current bookings.",
        model = geminiModel,
        tools = bookingTools.generatedTools(),
        instruction = Instruction.invoke(BOOKING_PROMPT),
        maxSteps = 1
    )
    

    companion object {
        private const val BOOKING_PROMPT = """
            You are the Booking Specialist. Manage table reservations precisely.
            
            CRITICAL RULES:
            1. ALWAYS call a tool for bookings or inquiries.
            2. After calling the tool and getting a result, STOP IMMEDIATELY.
            3. Do NOT ask follow-up questions.
            4. Do NOT try to do anything else after the booking tool returns.
            5. The Master Orchestrator will handle any other requests from the user.
            
            Your only job: Process the booking using your tool and return the result.
        """
    }
}
