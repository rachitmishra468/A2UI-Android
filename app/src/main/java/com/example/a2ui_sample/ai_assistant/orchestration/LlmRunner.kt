package com.example.a2ui_sample.ai_assistant.orchestration

import android.util.Log
import com.example.a2ui_sample.ai_assistant.agents.*
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.google.adk.kt.events.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LlmRunner
 * Master Orchestrator that routes intents to specialist agents.
 */
class LlmRunner(
    private val intentAgent: IntentAgent,
    private val menuAgent: MenuAgent,
    private val cartAgent: CartAgent,
    private val bookingAgent: BookingAgent,
    private val orderAgent: OrderAgent,
    private val feedbackAgent: FeedbackAgent,
    private val session: Session
) {
    private val mutex = Mutex()

    fun run(query: String): Flow<Event> = flow {
        mutex.withLock {
            Log.d("AssistantFlow", "🚀 LlmRunner: Starting for query: '$query'")
            
            // 1. Log incoming query to session
            val history = session.events.mapNotNull { it.content }
            
            // 2. Classify Intent
            Log.d("AssistantFlow", "🧠 Classifying intent...")
            val intent = intentAgent.analyzeIntent(query, history)
            
            // 3. Delegate based on intent type
            when (intent) {
                is AssistantIntent.SingleIntent -> {
                    Log.d("AssistantFlow", "🎯 Routing Single Intent: ${intent.result.intent}")
                    emitAll(routeToAgent(intent.result.intent ?: "UNKNOWN", query, session))
                }
                is AssistantIntent.Workflow -> {
                    Log.d("AssistantFlow", "⛓️ Executing Workflow with ${intent.tasks.size} tasks")
                    intent.tasks.forEach { task ->
                        emitAll(routeToAgent(task.tool ?: "UNKNOWN", task.toString(), session))
                    }
                }
                is AssistantIntent.Unknown -> {
                    Log.d("AssistantFlow", "❓ Unknown Intent, falling back to MenuAgent")
                    emitAll(menuAgent.executeCommand(query, session))
                }
            }
        }
    }

    private fun routeToAgent(intentName: String, command: String, session: Session): Flow<Event> {
        return when (intentName.uppercase()) {
            "SEARCH_MENU", "SHOW_MENU", "SHOW_RECOMMENDATIONS", "ITEM_DETAILS", "TODAY_SPECIALS", "SHOW_OFFERS" -> {
                menuAgent.executeCommand(command, session)
            }
            "ADD_TO_CART", "REMOVE_FROM_CART", "UPDATE_CART", "SHOW_CART", "CLEAR_CART", "CHECKOUT" -> {
                cartAgent.executeCommand(command, session)
            }
            "BOOK_TABLE", "SHOW_BOOKINGS", "CANCEL_BOOKING", "MODIFY_BOOKING" -> {
                bookingAgent.executeCommand(command, session)
            }
            "TRACK_ORDER", "ORDER_HISTORY", "CANCEL_ORDER", "REORDER_PREVIOUS" -> {
                orderAgent.executeCommand(command, session)
            }
            "SUBMIT_FEEDBACK" -> {
                feedbackAgent.executeCommand(command, session)
            }
            else -> menuAgent.executeCommand(command, session)
        }
    }
}
