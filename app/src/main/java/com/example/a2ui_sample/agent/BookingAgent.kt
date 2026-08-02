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
                You are a table reservation specialist. 
                Collect: number of people, date, and time.
                If any info is missing, ask for it politely. 
                Once you have all three, book the table.
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
