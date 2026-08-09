package com.example.a2ui_sample.ai_assistant.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui_sample.ai_assistant.orchestration.AssistantOrchestrator
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantChatMessage
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val orchestrator: AssistantOrchestrator
) : ViewModel() {

    val messages = mutableStateListOf<AssistantChatMessage>()
    var isTyping by mutableStateOf(false)
        private set

    init {
        messages.add(
            AssistantChatMessage(
                content = AssistantUiState.TextResponse("Hello! I'm your AI Menu Assistant. How can I help you today?"),
                isFromUser = false
            )
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        messages.add(
            AssistantChatMessage(
                content = AssistantUiState.TextResponse(text),
                isFromUser = true
            )
        )

        viewModelScope.launch {
            isTyping = true
            try {
                val uiState = orchestrator.processQuery(text)
                messages.add(
                    AssistantChatMessage(
                        content = uiState,
                        isFromUser = false
                    )
                )
            } catch (e: Exception) {
                messages.add(
                    AssistantChatMessage(
                        content = AssistantUiState.Error("Sorry, I encountered an error. Please try again."),
                        isFromUser = false
                    )
                )
            } finally {
                isTyping = false
            }
        }
    }
}
