package com.example.a2ui_sample.presentation.viewmodel

import android.util.Log
import com.example.a2ui_sample.agent.ADKRestaurantMasterAgent
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.FeedbackRepository
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.repository.OrderRepository
import com.example.a2ui_sample.domain.repository.ReservationRepository
import com.example.a2ui_sample.domain.repository.DeliveryRepository
import com.example.a2ui_sample.domain.valueobjects.*
import com.example.a2ui_sample.infrastructure.persistence.dao.ChatMessageDao
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.a2ui.compose.rendering.A2UILogLevel
import org.a2ui.compose.rendering.A2UILogger
import org.a2ui.compose.rendering.A2UIRenderer
import org.a2ui.compose.rendering.ActionHandler
import javax.inject.Inject
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

@HiltViewModel
class RestaurantMainViewModel @Inject constructor(
    private val repository: MenuRepository,
    private val feedbackRepository: FeedbackRepository,
    private val orderRepository: OrderRepository,
    private val reservationRepository: ReservationRepository,
    private val deliveryRepository: DeliveryRepository,
    private val chatMessageDao: ChatMessageDao
) : ViewModel(), A2UILogger, ActionHandler {

    private val _featuredItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val featuredItems: StateFlow<List<MenuItem>> = _featuredItems

    private val _uiMessages = mutableStateListOf<UiMessage>()
    val uiMessages: List<UiMessage> = _uiMessages

    private val _loadingState = MutableStateFlow<ChatLoadingState?>(null)
    val loadingState: StateFlow<ChatLoadingState?> = _loadingState.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = repository.getCartFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    val allOrders: Flow<List<Order>> = orderRepository.getAllOrders()
    val allBookings: Flow<List<Reservation>> = reservationRepository.getUpcomingReservations(CustomerId("guest"))
    val allFeedback: Flow<List<Feedback>> = feedbackRepository.getFeedbackFlow()

    val renderer = A2UIRenderer(this)
    var adkMasterAgent: ADKRestaurantMasterAgent? = null

    init {
        Log.d("A2UI_INIT", "RestaurantMainViewModel init started")
        loadFeaturedItems()
        loadChatHistory()
        renderer.setActionHandler(this)
        
        try {
            Log.d("A2UI_INIT", "Creating ADKRestaurantMasterAgent")
            adkMasterAgent = ADKRestaurantMasterAgent(
                repository, 
                feedbackRepository, 
                reservationRepository,
                orderRepository,
                deliveryRepository
            )
            Log.d("A2UI_INIT", "ADKRestaurantMasterAgent created successfully")
        } catch (e: Exception) {
            Log.e("A2UI_INIT", "CRITICAL: ADKRestaurantMasterAgent initialization failed: ${e.message}", e)
        }
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            Log.d("A2UI_RESTORE", "History Loading Started")
            val history = chatMessageDao.getAllMessages().first()
            if (history.isEmpty()) {
                // Welcome message
                addMessage(UiMessage(text = "Hello! I'm your AI Restaurant Assistant. How can I help you today?", isFromUser = false))
            } else {
                val messages = history.map { entity ->
                    UiMessage(
                        id = entity.id.toString(),
                        text = entity.text,
                        isFromUser = entity.isFromUser,
                        timestamp = entity.timestamp,
                        isA2UI = entity.isA2UI,
                        a2uiPayload = entity.a2uiPayload
                    )
                }
                
                // Process A2UI payloads BEFORE adding to UI to ensure renderer is ready
                messages.filter { it.isA2UI && it.a2uiPayload != null }.forEach { msg ->
                    Log.d("A2UI_RESTORE", "JSON Found for message ${msg.id}")
                    withContext(Dispatchers.Default) {
                        val jsonList = splitA2UICommand(msg.a2uiPayload!!)
                        jsonList.forEach { subJson ->
                            renderer.processMessage(subJson)
                        }
                    }
                    Log.d("A2UI_RESTORE", "Rendering Completed for message ${msg.id}")
                }
                
                _uiMessages.addAll(messages)
            }
            Log.d("A2UI_RESTORE", "History Loaded: ${history.size} messages")
        }
    }

    private fun addMessage(message: UiMessage) {
        _uiMessages.add(message)
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.insertMessage(ChatMessageEntity(
                text = message.text,
                isFromUser = message.isFromUser,
                timestamp = message.timestamp,
                isA2UI = message.isA2UI,
                a2uiPayload = message.a2uiPayload
            ))
        }
    }

    override fun onAction(surfaceId: String, actionName: String, context: Map<String, Any>) {
        Log.d("A2UI_ACTION", "Action received: $actionName with context: $context")
        when (actionName) {
            "addToCart" -> {
                val itemId = (context["itemId"] as? Number)?.toInt()
                if (itemId != null) {
                    addToCart(itemId)
                }
            }
            "viewCart" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToCart) }
            }
            "openMenu" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToMenu) }
            }
            "openBooking" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToBookings) }
            }
            "checkout" -> {
                viewModelScope.launch {
                    val order = checkout()
                    if (order != null) {
                        addMessage(UiMessage(
                            text = "Order placed successfully! ID: ${order.id.value}",
                            isFromUser = false
                        ))
                        _navigationEvents.emit(NavigationEvent.NavigateToOrders)
                    } else {
                        addMessage(UiMessage(
                            text = "Your cart is empty. Please add some items before checking out.",
                            isFromUser = false
                        ))
                    }
                }
            }
            "viewBookings" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToBookings) }
            }
            "viewOrders" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToOrders) }
            }
            "payNow" -> {
                android.util.Log.d("A2UI_FLOW", "[CHECKOUT] Pay Now Selected")
                viewModelScope.launch {
                    _navigationEvents.emit(NavigationEvent.NavigateToCheckout)
                }
            }
            "payLater" -> {
                android.util.Log.d("A2UI_FLOW", "[CHECKOUT] COD Selected")
                viewModelScope.launch {
                    val order = checkout()
                    if (order != null) {
                        android.util.Log.d("A2UI_FLOW", "[CHECKOUT] Order Created (COD): ${order.id.value}")
                        android.util.Log.d("A2UI_FLOW", "[CHECKOUT] Cart Cleared")
                        
                        val payload = adkMasterAgent?.buildOrderPlacedResponse(order)
                        if (payload != null) {
                            val msg = UiMessage(
                                text = "Order placed successfully (COD)! ID: ${order.id.value}",
                                isFromUser = false,
                                isA2UI = true,
                                a2uiPayload = payload
                            )
                            addMessage(msg)
                            renderer.processMessage(payload)
                        }
                    }
                }
            }
            "trackOrder" -> {
                val orderId = context["orderId"] as? String
                if (orderId != null) {
                    sendMessage("track order $orderId")
                }
            }
        }
    }

    override fun openUrl(url: String) {
        // Handle URL if needed
    }

    override fun showToast(message: String) {
        // Handle toast if needed
    }

    override fun log(level: A2UILogLevel, message: String) {
        Log.d("A2UI_RENDERER", "[$level] $message")
    }

    private fun loadFeaturedItems() {
        viewModelScope.launch {
            val items = repository.getMenuItems().filter { it.isBestSeller }
            _featuredItems.value = items
        }
    }

    fun getAllMenuItems(): List<MenuItem> = repository.getMenuItems()

    fun sendMessage(text: String) {
        val startTime = System.currentTimeMillis()
        Log.d("A2UI_PERF", "Message sent at $startTime")
        
        // 1. Add user message IMMEDIATELY to UI
        val userMsg = UiMessage(text = text, isFromUser = true)
        addMessage(userMsg)
        
        // Start Global Loading
        _loadingState.value = ChatLoadingState(status = "🤖 Thinking...")

        viewModelScope.launch {
            try {
                // Prepare conversation history
                val historyContext = _uiMessages.takeLast(10).map { 
                    if (it.isFromUser) "User: ${it.text}" else "Assistant: ${it.text}"
                }

                // 2. Intent Analysis
                _loadingState.value = ChatLoadingState(status = "🔍 Analyzing intent...")
                
                // 3. Offload AI processing to IO thread
                val responses = withContext(Dispatchers.IO) {
                    adkMasterAgent?.processQuery(text, historyContext) { status ->
                        _loadingState.value = ChatLoadingState(status = status)
                    }
                }

                responses?.forEach { response ->
                    if (response.trim().startsWith("{") && response.contains("version")) {
                        _loadingState.value = ChatLoadingState(status = "🎨 Preparing view...")
                        
                        // Process the message through the renderer
                        renderer.processMessage(response)

                        // Only add to chat if it's an updateComponents message (the UI part)
                        if (response.contains("updateComponents")) {
                            Log.d("A2UI_RESTORE", "UI JSON Found for message")
                            addMessage(UiMessage(
                                text = "I've updated the view for you:",
                                isFromUser = false,
                                isA2UI = true,
                                a2uiPayload = response
                            ))
                        }
                    } else {
                        addMessage(UiMessage(text = response, isFromUser = false))
                    }
                }
            } catch (e: Exception) {
                Log.e("A2UI_FLOW", "Error in sendMessage: ${e.message}")
                addMessage(UiMessage(text = "⚠️ Oops, I encountered an error: ${e.message}", isFromUser = false))
            } finally {
                _loadingState.value = null // Hide loader
            }
        }
    }

    fun addToCart(menuItemId: Int) {
        viewModelScope.launch {
            repository.addToCart(menuItemId)
        }
    }

    fun updateCartQuantity(menuItemId: Int, quantity: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(menuItemId, quantity)
        }
    }

    fun removeFromCart(menuItemId: Int) {
        viewModelScope.launch {
            repository.removeFromCart(menuItemId)
        }
    }

    fun bookTable(numberOfPeople: Int, date: String, time: String) {
        viewModelScope.launch {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                val parsedDate = sdf.parse("$date $time")
                val startMillis = parsedDate?.time ?: System.currentTimeMillis()

                val reservation = Reservation(
                    id = ReservationId(),
                    customerId = CustomerId("guest"),
                    restaurantId = RestaurantId("rest_1"),
                    restaurantName = "The Grand Kitchen",
                    tableId = TableId((1..20).random()),
                    timeSlot = TimeSlot(startMillis, startMillis + 3600000),
                    partySize = numberOfPeople,
                    status = ReservationStatus.CONFIRMED,
                    source = BookingSource.APP
                )
                
                reservationRepository.createReservation(reservation)
                
                _uiMessages.add(UiMessage(
                    text = "Great! I've booked a table for $numberOfPeople on $date at $time. Your booking ID is ${reservation.id.value.take(8).uppercase()}.", 
                    isFromUser = false
                ))
            } catch (e: Exception) {
                android.util.Log.e("A2UI_VIEWMODEL", "Failed to book table", e)
                _uiMessages.add(UiMessage(text = "⚠️ Sorry, I couldn't save your booking. Please try again.", isFromUser = false))
            }
        }
    }

    suspend fun checkout(): Order? {
        val cartItems = repository.getCart()
        if (cartItems.isEmpty()) return null

        val subtotal = repository.getCartTotal()
        val tax = (subtotal * 0.05).toInt()
        val total = subtotal + tax

        val orderItems = cartItems.map {
            OrderItem(
                menuItemId = it.menuItem.id,
                menuItemName = it.menuItem.name,
                quantity = it.quantity,
                unitPrice = it.menuItem.price
            )
        }

        val order = Order(
            id = OrderId("ORD-${System.currentTimeMillis() % 10000}"),
            items = orderItems,
            subtotal = Price(subtotal),
            tax = Price(tax),
            totalAmount = Price(total)
        )

        orderRepository.placeOrder(order)
        repository.clearCart()
        return order
    }

    fun clearChat() {
        _uiMessages.clear()
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.clearHistory()
        }
        addMessage(UiMessage(text = "Chat cleared. How can I help you now?", isFromUser = false))
    }

    private fun splitA2UICommand(json: String): List<String> {
        return try {
            // Handle multiple JSON objects separated by newlines (JSONL format)
            val lines = json.trim().split("\n").filter { it.isNotBlank() }
            if (lines.size > 1) {
                val allResults = mutableListOf<String>()
                lines.forEach { line ->
                    allResults.addAll(splitA2UICommand(line))
                }
                return allResults
            }

            val jsonObj = com.google.gson.JsonParser.parseString(json).asJsonObject
            val version = if (jsonObj.has("version")) jsonObj.get("version").asString else "v0.10"
            val result = mutableListOf<String>()

            listOf("createSurface", "updateComponents", "updateDataModel", "deleteSurface").forEach { key ->
                if (jsonObj.has(key)) {
                    val sub = com.google.gson.JsonObject()
                    sub.addProperty("version", version)
                    sub.add(key, jsonObj.get(key))
                    result.add(com.google.gson.Gson().toJson(sub))
                }
            }
            if (result.isEmpty()) listOf(json) else result
        } catch (_: Exception) {
            listOf(json)
        }
    }
}

sealed class NavigationEvent {
    object NavigateToCart : NavigationEvent()
    object NavigateToMenu : NavigationEvent()
    object NavigateToCheckout : NavigationEvent()
    object NavigateToBookings : NavigationEvent()
    object NavigateToOrders : NavigationEvent()
}

data class ChatLoadingState(
    val status: String,
    val progress: List<String> = emptyList()
)
