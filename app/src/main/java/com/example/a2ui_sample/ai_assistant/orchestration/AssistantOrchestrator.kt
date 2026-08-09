package com.example.a2ui_sample.ai_assistant.orchestration

import android.util.Log
import com.example.a2ui_sample.ai_assistant.agents.*
import com.example.a2ui_sample.ai_assistant.ui.mapper.AssistantUiMapper
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantUiState
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey
import kotlinx.coroutines.flow.last
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Singleton
class AssistantOrchestrator @Inject constructor(
    private val intentAgent: IntentAgent,
    private val menuAgent: MenuAgent,
    private val cartAgent: CartAgent,
    private val bookingAgent: BookingAgent,
    private val orderAgent: OrderAgent,
    private val feedbackAgent: FeedbackAgent,
    private val uiMapper: AssistantUiMapper,
) {
    private val sessionKey = SessionKey("AssistantApp", "User-1", "Enterprise-Session")
    private val session = Session(sessionKey)
    
    private val runner = LlmRunner(
        intentAgent = intentAgent,
        menuAgent = menuAgent,
        cartAgent = cartAgent,
        bookingAgent = bookingAgent,
        orderAgent = orderAgent,
        feedbackAgent = feedbackAgent,
        session = session,
    )

    suspend fun processQuery(query: String): AssistantUiState {
        Log.d("AssistantFlow", "🎬 Enterprise Orchestrator START: '$query'")
        
        return try {
            // Collect events from the ADK flow
            val events = mutableListOf<com.google.adk.kt.events.Event>()
            runner.run(query).collect { event ->
                events.add(event)
                Log.d("AssistantFlow", "📥 Event Received: author=${event.author} hasContent=${event.content != null}")
            }

            // Find the last relevant event to map to UI
            // Usually the final response containing text or the tool result
            val finalEvent = events.lastOrNull { it.isFinalResponse } 
                ?: events.lastOrNull { it.functionResponses().isNotEmpty() }
                ?: events.last()

            uiMapper.mapEventToUi(finalEvent)
            
        } catch (e: Exception) {
            Log.e("AssistantFlow", "❌ Orchestration Error", e)
            uiMapper.mapErrorToUi("I encountered an issue. Could you please try again?")
        }
    }
}
