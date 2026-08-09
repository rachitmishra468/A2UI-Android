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
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "BookingAssistant",
        model = geminiModel,
        instruction = Instruction.invoke(BOOKING_PROMPT),
        tools = bookingTools.generatedTools(),
        maxSteps = 1 // Prevent loops
    )

    fun executeCommand(command: String, session: Session): Flow<Event> {
        session.events.add(Event(author = Role.USER, content = Content.fromText(Role.USER, "[COMMAND] $command")))
        return adkAgent.runAsync(InvocationContext(agent = adkAgent, session = session))
    }

    companion object {
        private const val BOOKING_PROMPT = """
            You are the Booking Specialist. Manage table reservations precisely.
            
            RULES (must follow exactly):
            1) ALWAYS call create_booking for new reservations.
            2) Extract date, time, and peopleCount accurately.
            3) If viewing bookings, call list_bookings().
            4) If canceling, use the appropriate cancel tool.

            Examples:
             User: कल शाम 7 बजे के लिए 3 लोगों की टेबल बुक कर दो
             Tool: create_booking(date="tomorrow", time="7 PM", peopleCount=3)

             User: book a table for 4 tonight at 8pm
             Tool: create_booking(date="today", time="8 PM", peopleCount=4)
        """
    }
}
