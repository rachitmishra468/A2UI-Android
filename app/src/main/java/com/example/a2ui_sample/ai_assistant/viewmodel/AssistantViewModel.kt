package com.example.a2ui_sample.ai_assistant.viewmodel

import android.util.Log
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
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.model.CartItem
import com.example.a2ui_sample.infrastructure.persistence.dao.ChatMessageDao
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.StringReader
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val orchestrator: AssistantOrchestrator,
    private val menuRepository: MenuRepository,
    private val chatMessageDao: ChatMessageDao
) : ViewModel() {

    private val gson = Gson()
    val messages = mutableStateListOf<AssistantChatMessage>()
    
    var isTyping by mutableStateOf(false)
        private set
        
    var cartItems by mutableStateOf<List<CartItem>>(emptyList())
        private set

    private val conversationId = "ai_assistant"

    init {
        observeCart()
        observeChatHistory()
    }

    private fun observeChatHistory() {
        viewModelScope.launch {
            chatMessageDao.getMessagesByConversation(conversationId).collectLatest { entities ->
                if (entities.isEmpty()) {
                    messages.clear()
                    // Add welcome message if empty
                    val welcome = AssistantChatMessage(
                        content = AssistantUiState.TextResponse("Hello! I'm your AI Menu Assistant. How can I help you today?"),
                        isFromUser = false
                    )
                    saveMessageToDb(welcome)
                } else {
                    messages.clear()
                    messages.addAll(entities.map { entity ->
                        val content = if (entity.isA2UI && entity.a2uiPayload != null) {
                            deserializeUiState(entity.a2uiPayload, entity.text)
                        } else {
                            AssistantUiState.TextResponse(entity.text)
                        }
                        AssistantChatMessage(
                            id = entity.id.toString(),
                            content = content,
                            isFromUser = entity.isFromUser,
                            timestamp = entity.timestamp
                        )
                    })
                }
            }
        }
    }

    private fun deserializeUiState(payload: String, fallbackText: String): AssistantUiState {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            // Use a lenient JsonReader to tolerate minor malformed JSON (unquoted keys, trailing commas, etc.)
            val wrapperReader = JsonReader(StringReader(payload))
            // setLenient is required for older/newer gson versions; call method
            wrapperReader.setLenient(true)
            val wrapper: Map<String, Any> = gson.fromJson(wrapperReader, type)

            // Check if it's our wrapper format
            if (wrapper.containsKey("type") && wrapper.containsKey("data")) {
                val typeName = wrapper["type"] as? String ?: ""
                val data = wrapper["data"]

                val dataJson = if (data is String) data else gson.toJson(data)

                try {
                    when (typeName) {
                        "MenuSearch" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.MenuSearch::class.java)
                        }
                        "Recommendations" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.Recommendations::class.java)
                        }
                        "MenuDetails" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.MenuDetails::class.java)
                        }
                        "CartUpdate" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.CartUpdate::class.java)
                        }
                        "Error" -> AssistantUiState.Error(fallbackText)
                        else -> AssistantUiState.TextResponse(fallbackText)
                    }
                } catch (e: Exception) {
                    Log.e("AssistantFlow", "Inner payload deserialization error: ${e.message}")
                    AssistantUiState.TextResponse(fallbackText)
                }
            } else {
                // It's likely a raw A2UI payload from the other ViewModel
                // We don't have a specific state for this in AssistantUiState yet,
                // so we fallback to text for now to avoid crashes.
                AssistantUiState.TextResponse(fallbackText)
            }
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Deserialization error: ${e.message}")
            AssistantUiState.TextResponse(fallbackText)
        }
    }

    private fun saveMessageToDb(message: AssistantChatMessage) {
        viewModelScope.launch {
            val content = message.content
            val typeName = content.javaClass.simpleName
            
            // Put the object directly to avoid double serialization
            val wrapper = mapOf("type" to typeName, "data" to content)
            val fullPayload = gson.toJson(wrapper)
            
            val text = when (content) {
                is AssistantUiState.TextResponse -> content.text
                is AssistantUiState.Error -> content.message
                is AssistantUiState.CartUpdate -> content.message
                else -> "I've updated the view for you:"
            }
            
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    text = text,
                    isFromUser = message.isFromUser,
                    timestamp = message.timestamp,
                    isA2UI = content !is AssistantUiState.TextResponse,
                    a2uiPayload = fullPayload,
                    conversationId = conversationId
                )
            )
        }
    }

    private fun observeCart() {
        viewModelScope.launch {
            menuRepository.getCartFlow().collectLatest { items ->
                cartItems = items
            }
        }
    }

    fun addToCart(itemId: Int) {
        viewModelScope.launch {
            menuRepository.addToCart(itemId)
        }
    }

    fun updateCartQuantity(itemId: Int, quantity: Int) {
        viewModelScope.launch {
            if (quantity <= 0) {
                menuRepository.removeFromCart(itemId)
            } else {
                menuRepository.updateCartQuantity(itemId, quantity)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatMessageDao.clearHistoryByConversation(conversationId)
            // After clearing, the flow observer will re-trigger and add welcome message
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = AssistantChatMessage(
            content = AssistantUiState.TextResponse(text),
            isFromUser = true
        )
        saveMessageToDb(userMsg)

        viewModelScope.launch {
            isTyping = true
            try {
                val uiState = orchestrator.processQuery(text)
                val assistantMsg = AssistantChatMessage(
                    content = uiState,
                    isFromUser = false
                )
                saveMessageToDb(assistantMsg)
            } catch (e: Exception) {
                Log.e("AssistantFlow", "SendMessage error", e)
                val errorMsg = AssistantChatMessage(
                    content = AssistantUiState.Error("Sorry, I encountered an error. Please try again."),
                    isFromUser = false
                )
                saveMessageToDb(errorMsg)
            } finally {
                isTyping = false
            }
        }
    }
}
