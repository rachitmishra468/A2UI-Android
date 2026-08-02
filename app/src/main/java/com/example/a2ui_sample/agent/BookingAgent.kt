package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.annotations.Tool
import kotlin.time.ExperimentalTime

private const val TAG = "ADK_AGENT"

class BookingAgent(private val repository: MenuRepository, private val model: Gemini) {
    private val tools = BookingAgentTools(repository)

    @OptIn(ExperimentalTime::class)
    private val adkAgent = LlmAgent(
        name = "BookingAgent",
        model = model,
        instruction = Instruction(
            """
            You are a Booking Assistant. Your job is to help users book a table.
            
            Information required:
            1. Number of people (count)
            2. Time of booking (e.g. 7 PM)
            
            Process:
            - If you have BOTH number of people and time, call book_table_step(step="complete", people="...", time="...").
            - If you are missing information, call book_table_step with the appropriate step:
              * step="ask_people" if you don't know the count.
              * step="ask_time" if you don't know the time.
            - Always pass whatever information you HAVE extracted so far into the parameters.
            - If the user provides a number (like "4") in response to "how many people", that is the 'people' count.
            - If the user provides a time (like "8pm"), that is the 'time'.
            - Always use the book_table_step tool.
            """.trimIndent()
        ),
        tools = tools.generatedTools(),
        maxSteps = 1
    )

    @OptIn(ExperimentalTime::class)
    suspend fun process(query: String, session: Session): AgentResponse {
        Log.d(TAG, ">>> BookingAgent Processing: $query")
        tools.resetLastResp() // Clear previous response but KEEP accumulated state
        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, query)
        )
        try { 
            adkAgent.runAsync(context).collect { event ->
                Log.v(TAG, "Booking Event: ${event.author}: ${event.content?.parts?.firstOrNull()?.text}")
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Booking Error: ${e.message}")
            return AgentResponse.Error("Booking Error: ${e.message}")
        }
        val response = tools.getLastResponse()
        Log.d(TAG, "<<< BookingAgent Response: ${response?.javaClass?.simpleName}")
        return response ?: AgentResponse.Message("Booking failed.")
    }
}

class BookingAgentTools(private val repository: MenuRepository) {
    private var lastResp: AgentResponse? = null
    
    // Accumulated state to handle Gemini potentially missing parameters in subsequent calls
    private var savedPeople: String? = null
    private var savedTime: String? = null

    fun getLastResponse() = lastResp
    fun resetLastResp() { lastResp = null }
    fun resetAll() { 
        lastResp = null
        savedPeople = null
        savedTime = null
    }

    @Tool(name = "book_table_step", description = "Handle booking steps")
    fun bookTableStep(step: String, people: String? = null, time: String? = null): String {
        Log.d(TAG, "Tool: book_table_step called (step=$step, people=$people, time=$time)")
        
        // Update accumulated state
        if (!people.isNullOrBlank()) savedPeople = people
        if (!time.isNullOrBlank()) savedTime = time
        
        Log.d(TAG, "   State: savedPeople=$savedPeople, savedTime=$savedTime")

        // Check if we are actually done (prefer accumulated state)
        if ((step == "complete" || (savedPeople != null && savedTime != null)) && !savedPeople.isNullOrBlank() && !savedTime.isNullOrBlank()) {
            val numPeople = savedPeople?.filter { it.isDigit() }?.toIntOrNull() ?: 2
            val booking = TableBooking(numberOfPeople = numPeople, bookingTime = savedTime!!)
            repository.addBooking(booking)
            lastResp = AgentResponse.BookingConfirmation(booking)
            
            // Clear state after success
            val finalTime = savedTime
            resetAll()
            return "Booking confirmed for $numPeople people at $finalTime."
        }
        
        lastResp = AgentResponse.BookingRequest(step, "Booking process")
        return "Step $step processed. Progress: People=$savedPeople, Time=$savedTime"
    }
}
