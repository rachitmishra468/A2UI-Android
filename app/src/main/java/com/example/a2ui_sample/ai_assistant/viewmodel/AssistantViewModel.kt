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
import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.model.CartItem
import com.example.a2ui_sample.domain.model.Feedback
import com.example.a2ui_sample.domain.model.Sentiment
import com.example.a2ui_sample.domain.model.OrderItem
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.Price
import com.example.a2ui_sample.domain.valueobjects.CustomerId
import com.example.a2ui_sample.domain.valueobjects.Rating
import com.example.a2ui_sample.domain.valueobjects.OrderStatus as DomainOrderStatus
import com.example.a2ui_sample.infrastructure.persistence.dao.ChatMessageDao
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.StringReader
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

sealed class AssistantNavigationEvent {
    object NavigateToCheckout : AssistantNavigationEvent()
}

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val orchestrator: AssistantOrchestrator,
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val feedbackRepository: FeedbackRepository,
    private val chatMessageDao: ChatMessageDao
) : ViewModel() {

    private val gson = Gson()
    val messages = mutableStateListOf<AssistantChatMessage>()
    
    var isTyping by mutableStateOf(false)
        private set
        
    var cartItems by mutableStateOf<List<CartItem>>(emptyList())
        private set

    private val _navigationEvents = kotlinx.coroutines.flow.MutableSharedFlow<AssistantNavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    private val conversationId = "ai_assistant"

    init {
        observeCart()
        observeChatHistory()
        observeOrderUpdates()
    }

    private fun observeOrderUpdates() {
        viewModelScope.launch {
            orderRepository.getActiveOrders().collectLatest { orders ->
                orders.forEach { order ->
                    // For each active order, if it's currently tracked in our UI, we could update it.
                    // However, our UI is list-based (chat). To show live updates, we can either:
                    // 1. Add a new message (noisy)
                    // 2. Update existing message (preferred but needs ID tracking in ViewModel)
                    
                    // Simple POC: Add a message if status changes for the latest order
                    // In a real app, you'd use a more sophisticated state management.
                    Log.d("AssistantFlow", "Live update for order ${order.id.value}: ${order.status}")
                }
            }
        }
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
                        "CartView" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.CartView::class.java)
                        }
                        "BookingResult" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.BookingResult::class.java)
                        }
                        "FeedbackResult" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.FeedbackResult::class.java)
                        }
                        "OrderStatus" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.OrderStatus::class.java)
                        }
                        "CheckoutSummary" -> {
                            val rdr = JsonReader(StringReader(dataJson))
                            rdr.setLenient(true)
                            gson.fromJson(rdr, AssistantUiState.CheckoutSummary::class.java)
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
        Log.d("AssistantFlow", "💾 Saving message to DB: isFromUser=${message.isFromUser}, type=${message.content.javaClass.simpleName}")
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
                is AssistantUiState.CartView -> content.message
                is AssistantUiState.BookingResult -> content.message
                is AssistantUiState.FeedbackResult -> content.message
                is AssistantUiState.OrderStatus -> content.message
                is AssistantUiState.CheckoutSummary -> content.message
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

    fun navigateToCheckout() {
        viewModelScope.launch {
            _navigationEvents.emit(AssistantNavigationEvent.NavigateToCheckout)
        }
    }

    fun placeOrder(isCod: Boolean) {
        viewModelScope.launch {
            val total = menuRepository.getCartTotal()
            if (total == 0) return@launch
            
            Log.d("AssistantFlow", "🛒 Placing order: isCod=$isCod, total=$total")
            
            // Generate dummy order items for the new order
            val cart = menuRepository.getCart()
            val orderItems = cart.map { 
                OrderItem(
                    menuItemId = it.menuItem.id,
                    menuItemName = it.menuItem.name,
                    quantity = it.quantity,
                    unitPrice = it.menuItem.price
                )
            }

            val orderId = OrderId()
            val newOrder = com.example.a2ui_sample.domain.model.Order(
                id = orderId,
                items = orderItems,
                subtotal = Price(total),
                tax = Price((total * 0.05).toInt()),
                totalAmount = Price((total * 1.05).toInt()),
                status = DomainOrderStatus.PENDING
            )

            // Save order to repository
            orderRepository.placeOrder(newOrder)

            // Clear cart in repository
            menuRepository.clearCart()
            
            // Show Live Order Status Card immediately
            val statusMsg = AssistantChatMessage(
                content = AssistantUiState.OrderStatus(
                    message = "Order placed successfully! (ID: ${orderId.value.take(8).uppercase()}). I'm tracking your order live! 🚀",
                    status = "PENDING",
                    progress = 0.1f
                ),
                isFromUser = false
            )
            saveMessageToDb(statusMsg)

            // Show success message in chat
            val msg = if (isCod) {
                "Your delicious meal is being prepared. It will be delivered soon!"
            } else {
                "Payment successful! Your delicious meal is being prepared."
            }
            
            val assistantMsg = AssistantChatMessage(
                content = AssistantUiState.TextResponse(msg),
                isFromUser = false
            )
            saveMessageToDb(assistantMsg)
        }
    }

    fun submitFeedback(orderId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val feedback = Feedback(
                id = com.example.a2ui_sample.domain.valueobjects.FeedbackId(),
                orderId = OrderId(orderId),
                customerId = CustomerId("guest"),
                foodRating = Rating(rating),
                deliveryRating = Rating(rating),
                packagingRating = Rating(rating),
                overallRating = Rating(rating),
                comment = comment,
                sentiment = if (rating >= 4) Sentiment.POSITIVE else Sentiment.NEGATIVE
            )
            feedbackRepository.submitFeedback(feedback)
            Log.d("AssistantFlow", "Feedback submitted for $orderId: $rating stars")
        }
    }

    fun addSystemMessage(text: String) {
        val msg = AssistantChatMessage(
            content = AssistantUiState.TextResponse(text),
            isFromUser = false
        )
        saveMessageToDb(msg)
    }

    fun sendMessage(text: String) {
        Log.d("AssistantFlow", "✉️ User sending message: '$text'")
        if (text.isBlank()) return

        val userMsg = AssistantChatMessage(
            content = AssistantUiState.TextResponse(text),
            isFromUser = true
        )
        saveMessageToDb(userMsg)

        viewModelScope.launch {
            isTyping = true
            try {
                val uiStates = orchestrator.processQuery(text)
                uiStates.forEach { uiState ->
                    val assistantMsg = AssistantChatMessage(
                        content = uiState,
                        isFromUser = false
                    )
                    saveMessageToDb(assistantMsg)
                }
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
