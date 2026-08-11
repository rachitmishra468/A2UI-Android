package com.example.a2ui_sample.ai_assistant.orchestration

import android.util.Log
import com.example.a2ui_sample.ai_assistant.agents.RootRestaurantAgent
import com.example.a2ui_sample.ai_assistant.ui.mapper.AssistantUiMapper
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantUiState
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import kotlinx.coroutines.flow.toList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

/**
 * AssistantOrchestrator
 * High-level coordinator that bridges the UI with the Multi-Agent System.
 * Uses ADK's native InMemoryRunner for simplified agent execution and session management.
 */
@OptIn(ExperimentalTime::class)
@Singleton
class AssistantOrchestrator @Inject constructor(
    private val rootAgent: RootRestaurantAgent,
    private val uiMapper: AssistantUiMapper,
) {
    private val runner = InMemoryRunner(rootAgent.adkAgent)

    suspend fun processQuery(query: String): AssistantUiState {
        Log.d("AssistantFlow", "🎬 Enterprise Orchestrator START: '$query'")
        
        return try {
            // Use InMemoryRunner to execute the multi-agent system
            // It automatically handles session context and agent delegation
            val events = runner.runAsync(
                userId = "user-1",
                sessionId = "session-1",
                newMessage = Content.fromText(Role.USER, query)
            ).toList()

            events.forEach { event ->
                Log.d("AssistantFlow", "📥 Event Received: author=${event.author} hasContent=${event.content != null}")
            }

            // Find the last relevant event to map to UI
            // Prioritize events with function responses to ensure structured UI mapping
            val finalEvent = events.lastOrNull { it.functionResponses().isNotEmpty() }
                ?: events.lastOrNull { it.isFinalResponse }
                ?: events.last()
            
            Log.d("AssistantFlow", "Agent response " + finalEvent.content)

            uiMapper.mapEventToUi(finalEvent)
            
        } catch (e: Exception) {
            Log.e("AssistantFlow", "❌ Orchestration Error", e)
            val errorMessage = when {
                e.message?.contains("429") == true -> "RESOURCE_EXHAUSTED: You exceeded your current quota, please check your plan and billing details!"
                e.message?.contains("503") == true -> "The AI service is temporarily unavailable. Please try again in a few seconds."
                else -> "I encountered an issue. Could you please try again?"
            }
            uiMapper.mapErrorToUi(errorMessage)
        }
    }
}
