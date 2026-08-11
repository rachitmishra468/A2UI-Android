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

    suspend fun processQuery(query: String): List<AssistantUiState> {
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
                 val eventType = when {
                     event.functionCalls().isNotEmpty() -> "FUNCTION_CALL (${event.functionCalls().joinToString { it.name }})"
                     event.functionResponses().isNotEmpty() -> "FUNCTION_RESPONSE (${event.functionResponses().joinToString { it.name }})"
                     event.content != null -> "CONTENT"
                     else -> "LIFECYCLE"
                 }
                 Log.d("AssistantFlow", "📥 Event: author=${event.author} type=$eventType")
             }

              // Process events and handle explicit transfer_to_agent events by executing
              // the named sub-agent separately. This preserves ADK/A2A semantics while
              // avoiding a single long run that exhausts model steps.
              val uiStates = mutableListOf<AssistantUiState>()
              val processedStateTypes = mutableSetOf<String>() // Track processed state types to avoid duplicates

              val eventQueue = events.toMutableList()

              while (eventQueue.isNotEmpty()) {
                  val event = eventQueue.removeAt(0)

                  // Detect a transfer request emitted by the root (either as a function response or call)
                  val funcResp = event.functionResponses().firstOrNull { it.name == "transfer_to_agent" }
                  if (funcResp != null) {
                      val payload = funcResp.response as? Map<*, *>
                      val agentName = payload?.get("agent") as? String ?: payload?.get("to") as? String ?: payload?.get("target") as? String
                      val agentQuery = payload?.get("query") as? String ?: payload?.get("message") as? String ?: payload?.get("text") as? String

                      if (agentName == null || agentQuery == null) {
                          Log.w("AssistantFlow", "transfer_to_agent missing agent or query payload: $payload")
                          continue
                      }

                      val subAgent = rootAgent.getAgentByName(agentName)
                      if (subAgent == null) {
                          Log.w("AssistantFlow", "Unknown agent in transfer: $agentName")
                          continue
                      }

                      Log.d("AssistantFlow", "Orchestrator: transfer_to_agent -> $agentName with query: '$agentQuery'")

                      // Run the sub-agent as a separate short session
                      val subRunner = InMemoryRunner(subAgent)
                      val subEvents = subRunner.runAsync(
                          userId = "user-1",
                          sessionId = "session-1-sub-${System.currentTimeMillis()}",
                          newMessage = Content.fromText(Role.USER, agentQuery)
                      ).toList()

                      // Map sub-agent events to UI states
                      subEvents.forEach { se ->
                          val isOrchestration = se.functionResponses().any { it.name == "transfer_to_agent" || it.name == "respond_to_user" }
                          if (!isOrchestration && (se.functionResponses().isNotEmpty() || se.content != null)) {
                              val state = uiMapper.mapEventToUi(se)
                              val stateType = state.javaClass.simpleName
                              if (state is AssistantUiState.TextResponse) {
                                  if (state.text.isNotBlank()) uiStates.add(state)
                              } else if (!processedStateTypes.contains(stateType)) {
                                  uiStates.add(state)
                                  processedStateTypes.add(stateType)
                              }
                          }
                      }

                      // Build a brief summary of the sub-agent run to inject back into root's session
                      val lastText = subEvents.lastOrNull { it.content != null }?.content?.parts?.firstOrNull()?.text
                      val lastToolMsg = subEvents.flatMap { it.functionResponses() }.firstOrNull()?.response?.get("message") as? String
                      val summary = lastToolMsg ?: lastText ?: "[sub-agent $agentName completed]"

                      // Inject the summary back into the root agent's session so it can decide next steps
                      val resumeEvents = runner.runAsync(
                          userId = "user-1",
                          sessionId = "session-1",
                          newMessage = Content.fromText(Role.SYSTEM, "SUBAGENT_RESULT:$agentName:$summary")
                      ).toList()

                      // Prepend new events produced by resuming the root into our processing queue
                      eventQueue.addAll(0, resumeEvents)
                  } else {
                      // Regular event — map to UI if it's not an orchestration-only event
                      val isOrchestration = event.functionResponses().any { it.name == "transfer_to_agent" || it.name == "respond_to_user" }
                      if (!isOrchestration && (event.functionResponses().isNotEmpty() || event.content != null)) {
                          val state = uiMapper.mapEventToUi(event)
                          val stateType = state.javaClass.simpleName
                          if (state is AssistantUiState.TextResponse) {
                              if (state.text.isNotBlank()) uiStates.add(state)
                          } else if (!processedStateTypes.contains(stateType)) {
                              uiStates.add(state)
                              processedStateTypes.add(stateType)
                          }
                      }
                  }
              }

             // Final Cleanup: If we have both structured UI and a TextResponse, 
             // and the TextResponse is very long (redundant), we could filter it.
             // But it's safer to rely on the prompt.

             // If no structured UI was added, ensure we at least have the final text
             if (uiStates.isEmpty()) {
                 val finalEvent = events.lastOrNull { it.content != null } ?: events.last()
                 Log.d("AssistantFlow", "Orchestrator: uiStates.isEmpty ${finalEvent} ")

                 uiStates.add(uiMapper.mapEventToUi(finalEvent))
             }

             Log.d("AssistantFlow", "Orchestrator: Returning ${uiStates.size} UI states")
             uiStates

         } catch (e: Exception) {
             Log.e("AssistantFlow", "❌ Orchestration Error", e)
             val errorMessage = when {
                 e.message?.contains("429") == true -> "RESOURCE_EXHAUSTED: You exceeded your current quota, please check your plan and billing details!"
                 e.message?.contains("503") == true -> "The AI service is temporarily unavailable. Please try again in a few seconds."
                 else -> "I encountered an issue. Could you please try again?"
             }
             listOf(uiMapper.mapErrorToUi(errorMessage))
         }
     }
}
