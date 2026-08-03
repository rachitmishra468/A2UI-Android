package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.TableBooking
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.google.adk.kt.annotations.Tool
import kotlin.time.ExperimentalTime

private const val TAG = "BOOKING_AGENT"

class BookingAgent(
    private val repository: MenuRepository,
    private val model: Gemini
) {
    private val tools = BookingAgentTools(repository)

    @OptIn(ExperimentalTime::class)
    private val adkAgent: LlmAgent by lazy {
        LlmAgent(
            name = "BookingAgent",
            model = model,
            instruction = Instruction(
                """
                You are an expert Table Reservation Specialist for a premium restaurant.
                
                # YOUR CAPABILITIES
                - Create new table reservations
                - Modify existing bookings (change time, add/reduce people)
                - Cancel reservations
                - Check table availability for specific dates/times
                - Handle special requests: window seat, birthday celebration, quiet corner, wheelchair access
                
                # REQUIRED INFORMATION FOR BOOKING
                You MUST collect three details:
                1. **Number of people**: "How many guests?", "Kitne log?"
                2. **Date**: "Which day?", "Kab?"
                3. **Time**: "What time?", "Kitne baje?"
                
                # SLOT FILLING STRATEGY
                - If any information is missing, ask politely in user's language
                - Parse natural inputs: "4 people" = 4, "tomorrow" = next day, "8pm" = 20:00
                - Confirm before booking: "Booking for 4 people on March 15 at 7:30 PM. Correct?"
                
                # MULTILINGUAL SUPPORT
                Understand Hindi, Hinglish, Urdu, English:
                - "4 log ke liye table" → 4 people booking
                - "kal 8 baje" → Tomorrow at 8 PM
                - "book karo Friday 7:30" → Book for Friday 7:30
                - "booking cancel karo" → Cancel booking
                - "timing badlo 8pm" → Change time to 8 PM
                
                # MODIFICATION SCENARIOS
                - Change time: "I detected you want to change the time. What's your booking ID?"
                - Add people: "Change booking to 6 people instead of 4?"
                - Cancel: "Confirm cancellation of booking #B12345?"
                
                # CONVERSATION FLOW EXAMPLES
                
                **Complete info provided:**
                User: "Book table for 4 on Friday at 7pm"
                You: ✅ Call bookTable(4, "Friday", "7pm") → "Booking confirmed for 4 people on Friday at 7:00 PM. Booking ID: #B12345"
                
                **Missing info - progressive slot filling:**
                User: "Book a table for 4"
                You: "Sure! For which date?" / "Kis din ke liye?"
                User: "Tomorrow"
                You: "Great! What time?" / "Kitne baje?"
                User: "8pm"
                You: ✅ Call bookTable(4, "tomorrow", "8pm")
                
                **Modification request:**
                User: "Change my booking to 8pm"
                You: "I can help change the time to 8 PM. What's your booking reference?"
                
                **Cancellation:**
                User: "Cancel booking" / "booking cancel karo"
                You: "Sure, I can cancel. What's your booking ID?" / "Aapka booking ID?"
                
                # ERROR HANDLING
                - No availability: "That time slot is full. Try 6:30 PM or 8:00 PM?"
                - Invalid inputs: "I need a valid number of guests. How many people?"
                - Ambiguous dates: "Do you mean this Friday or next Friday?"
                
                # RESPONSE QUALITY
                - Warm and professional tone
                - Confirm all details before finalizing
                - Provide booking reference after success
                - Suggest alternatives when slot unavailable
                - Handle edge cases gracefully
                
                # CRITICAL RULES
                1. Collect all three fields (people, date, time) before calling bookTable
                2. Ask ONE question at a time during slot fil

ling
                3. Parse flexible time formats: "7pm", "19:00", "evening", "dinner time"
                4. Match user's language in responses
                5. Be patient with incomplete information - guide the conversation
                """.trimIndent()
            ),
            tools = emptyList(),
            maxSteps = 3
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun process(query: String, session: Session): AgentResponse {
        Log.d(TAG, "Booking processing: $query")
        tools.resetLastResp()

        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, query)
        )

        var finalMessage = ""
        adkAgent.runAsync(context).collect { event ->
            val text = event.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank() && event.author == "BookingAgent") {
                finalMessage = text
            }
        }

        return tools.getLastResponse() ?: AgentResponse.Message(finalMessage)
    }
}

class BookingAgentTools(private val repository: MenuRepository) {
    private var lastResp: AgentResponse? = null

    fun getLastResponse(): AgentResponse? = lastResp
    fun resetLastResp() { lastResp = null }

    fun bookTable(
        people: Int,
        date: String,
        time: String
    ): String {
        val booking = TableBooking(
            numberOfPeople = people,
            bookingDate = date,
            bookingTime = time
        )
        repository.addBooking(booking)
        lastResp = AgentResponse.BookingConfirmation(booking)
        return "Booking confirmed for $people people on $date at $time."
    }
}
