package com.example.a2ui_sample.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui_sample.agent.RestaurantAgent
import com.example.a2ui_sample.data.MenuRepository
import kotlinx.coroutines.launch
import org.a2ui.compose.rendering.A2UIRenderer
import java.util.UUID

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromAgent: Boolean,
    val isA2UI: Boolean = false,
    val a2uiPayloads: List<String> = emptyList()
)

/**
 * RestaurantViewModel
 * Orchestrates the chat flow using the REAL ADK RestaurantAgent.
 * Maintains complete chat history with unique message IDs.
 */
class RestaurantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MenuRepository(application)
    private val agent = RestaurantAgent(repository)
    
    val renderer = A2UIRenderer()

    private val _uiMessages = mutableStateListOf<UiMessage>()
    val uiMessages: List<UiMessage> = _uiMessages

    init {
        _uiMessages.add(
            UiMessage(
                content = "Hello! I'm your AI Food Assistant. Ask me to 'show veg burgers', 'book a table', or 'view cart'.",
                isFromAgent = true
            )
        )
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        Log.d("A2UI_FLOW", "1. User Input Received: $query")
        _uiMessages.add(UiMessage(content = query, isFromAgent = false))

        viewModelScope.launch {
            Log.d("A2UI_FLOW", "2. Calling ADK RestaurantAgent...")
            // Processing via ADK Agent
            val a2uiMessages = agent.processQuery(query)

            Log.d("A2UI_FLOW", "5. Received ${a2uiMessages.toString()} A2UI JSON messages from Agent")
            Log.d("A2UI_FLOW", "5. Received ${a2uiMessages.size} A2UI JSON messages from Agent")

            // (We keep the global renderer updated, but the UI will now use local ones per bubble)
            a2uiMessages.forEachIndexed { index, json ->
                Log.d("A2UI_FLOW", "6. Sending JSON message $index to Renderer: $json")
                renderer.processMessage(json)
            }

            _uiMessages.add(
                UiMessage(
                    content = "I've updated the view for you:",
                    isFromAgent = true,
                    isA2UI = true,
                    a2uiPayloads = a2uiMessages
                )
            )
        }
    }

    fun clearChat() {
        _uiMessages.clear()
        _uiMessages.add(
            UiMessage(
                content = "Chat cleared. Hello! I'm your AI Food Assistant. What would you like to order?",
                isFromAgent = true
            )
        )
    }
}