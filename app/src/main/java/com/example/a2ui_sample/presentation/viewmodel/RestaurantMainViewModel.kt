package com.example.a2ui_sample.presentation.viewmodel

import android.util.Log
import com.example.a2ui_sample.agent.ADKRestaurantMasterAgent
import com.example.a2ui_sample.agent.ConversationHelper
import com.example.a2ui_sample.agent.UserProfileBuilder
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
    private val chatMessageDao: ChatMessageDao,
    private val memoryManager: com.example.a2ui_sample.agent.ConversationMemoryManager
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
    private var adkMasterAgent: ADKRestaurantMasterAgent? = null

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
            // Manual injection for adkMasterAgent
            adkMasterAgent?.memoryManager = memoryManager

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
                // Show proactive welcome with time-based greeting
                val greeting = ConversationHelper.getGreeting()
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val suggestion = when (hour) {
                    in 6..10 -> "Ready for a delicious breakfast? 🌅"
                    in 11..14 -> "Hungry for lunch? 🍽️"
                    in 18..21 -> "Perfect timing for dinner! 🌆"
                    else -> "Craving something tasty? 😋"
                }
                
                addMessage(UiMessage(
                    text = "$greeting I'm your AI Restaurant Assistant. $suggestion", 
                    isFromUser = false
                ))
                
                // Check for proactive suggestions based on history
                launch {
                    try {
                        val orders = orderRepository.getAllOrders().first()
                        val bookings = reservationRepository.getUpcomingReservations(CustomerId("guest")).first()
                        val feedback = feedbackRepository.getFeedbackFlow().first()
                        
                        if (orders.size >= 3) {
                            // User has order history - show proactive suggestions
                            val profileBuilder = UserProfileBuilder(repository)
                            val profile = profileBuilder.buildProfile(orders, bookings, feedback)
                            val suggestions = profileBuilder.generateProactiveSuggestions(profile)
                            
                            if (suggestions.isNotEmpty()) {
                                // Show first proactive suggestion after a small delay
                                kotlinx.coroutines.delay(1000)
                                addMessage(UiMessage(
                                    text = suggestions.first(),
                                    isFromUser = false
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("A2UI_RESTORE", "Error generating proactive suggestions", e)
                    }
                }
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
                    Log.d("A2UI_FLOW", "JSON Found for message ${msg.id}")
                    withContext(Dispatchers.Default) {
                        val jsonList = splitA2UICommand(msg.a2uiPayload!!)
                        jsonList.forEach { subJson ->
                            renderer.processMessage(subJson)
                        }
                    }
                    Log.d("A2UI_FLOW", "Rendering Completed for message ${msg.id}")
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
        Log.d("A2UI_FLOW", "Action received: $actionName with context: $context")
        when (actionName) {
            "addToCart" -> {
                val itemId = (context["itemId"] as? Number)?.toInt()
                if (itemId != null) {
                    addToCart(itemId)
                    // Persist to memory
                    viewModelScope.launch { memoryManager.save(com.example.a2ui_sample.agent.ConversationMemoryManager.LAST_SELECTED_ITEM, itemId) }
                }
            }
            "viewCart" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToCart) }
            }
            "openMenu" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToMenu) }
            }
            "openBooking" -> {
                // Update memory before navigating
                viewModelScope.launch { memoryManager.save(com.example.a2ui_sample.agent.ConversationMemoryManager.LAST_DISCUSSED_TOPIC, "booking_intent") }
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
                Log.d("A2UI_FLOW", "[CHECKOUT] COD button clicked")
                viewModelScope.launch {
                    try {
                        // 1. Show immediate text status
                        addMessage(UiMessage(text = "Placing your order...", isFromUser = false))
                        
                        val order = checkout()
                        if (order != null) {
                            Log.i("A2UI_FLOW", "[CHECKOUT] ✅ Order Created (COD): ${order.id.value}")
                            
                            // Persist to memory
                            adkMasterAgent?.updateOrderMemory(order)

                            val payload = adkMasterAgent?.buildOrderPlacedResponse(order)
                            if (payload != null) {
                                // 2. Process renderer on Main thread
                                withContext(Dispatchers.Main) {
                                    splitA2UICommand(payload).forEach { renderer.processMessage(it) }
                                    Log.d("A2UI_FLOW", "[CHECKOUT] Renderer processed multi-part A2UI payload for ${order.id.value}")
                                }
                                
                                // 3. Add the A2UI card bubble
                                val msg = UiMessage(
                                    text = "Order Confirmation",
                                    isFromUser = false,
                                    isA2UI = true,
                                    a2uiPayload = payload
                                )
                                withContext(Dispatchers.Main) {
                                    addMessage(msg)
                                }
                                
                                // 4. Add PREMIUM satisfaction feedback card with delay
                                kotlinx.coroutines.delay(1500)
                                val satisfactionPayload = adkMasterAgent?.buildPremiumFeedbackResponse(order)
                                if (satisfactionPayload != null) {
                                    withContext(Dispatchers.Main) {
                                        // Save to memory
                                        memoryManager.save(com.example.a2ui_sample.agent.ConversationMemoryManager.LAST_DISCUSSED_TOPIC, "checkout_completed")

                                        splitA2UICommand(satisfactionPayload).forEach { renderer.processMessage(it) }
                                        addMessage(UiMessage(
                                            text = "How was your experience today?",
                                            isFromUser = false,
                                            isA2UI = true,
                                            a2uiPayload = satisfactionPayload
                                        ))
                                    }
                                }
                                
                                Log.i("A2UI_FLOW", "[CHECKOUT] ✅ Success and Feedback cards added")
                            }
                        } else {
                            addMessage(UiMessage(text = "⚠️ Cart is empty. Please add items before checkout.", isFromUser = false))
                        }
                    } catch (e: Exception) {
                        Log.e("A2UI_FLOW", "[CHECKOUT] ❌ COD Error: ${e.message}", e)
                        addMessage(UiMessage(text = "❌ Checkout failed: ${e.message}", isFromUser = false))
                    }
                }
            }
            "selectRating" -> {
                val rating = (context["rating"] as? Number)?.toInt() ?: 0
                val label = context["label"] as? String ?: ""
                Log.d("A2UI_FLOW", "[FEEDBACK] User selected rating: $rating ($label)")
                // We could update the UI here to show selection, 
                // but for now we'll just log it until "submit" is clicked
            }
            "submit_premium_feedback" -> {
                val orderId = context["orderId"] as? String ?: "unknown"
                // Extract comment from data model via renderer
                val surfaceData = renderer.getDataModel(surfaceId)?.getDataSnapshot()
                val comment = (surfaceData?.get("comment") ?: "").toString()
                
                Log.i("A2UI_FLOW", "[FEEDBACK] Submitting for Order $orderId: Comment='$comment'")
                
                viewModelScope.launch {
                    // Save to repository
                    val ratingValue = com.example.a2ui_sample.domain.valueobjects.Rating(5)
                    val feedback = Feedback(
                        id = com.example.a2ui_sample.domain.valueobjects.FeedbackId(java.util.UUID.randomUUID().toString()),
                        orderId = com.example.a2ui_sample.domain.valueobjects.OrderId(orderId),
                        customerId = com.example.a2ui_sample.domain.valueobjects.CustomerId("guest"),
                        foodRating = ratingValue,
                        deliveryRating = ratingValue,
                        packagingRating = ratingValue,
                        overallRating = ratingValue,
                        comment = comment,
                        sentiment = Sentiment.POSITIVE
                    )
                    feedbackRepository.submitFeedback(feedback)
                    
                    // Update memory with last feedback
                    memoryManager.save(com.example.a2ui_sample.agent.ConversationMemoryManager.LAST_DISCUSSED_TOPIC, "feedback_submitted")

                    withContext(Dispatchers.Main) {
                        addMessage(UiMessage(
                            text = "Thank you for your feedback! 😊 Your response helps me improve future recommendations and service quality.",
                            isFromUser = false
                        ))
                    }
                }
            }
            "trackOrder" -> {
                val orderId = context["orderId"] as? String
                if (orderId != null) {
                    sendMessage("track order $orderId")
                }
            }
            "feedback_positive" -> {
                Log.d("A2UI_FLOW", "[FEEDBACK] User clicked Positive")
                addMessage(UiMessage(
                    text = "Thank you! 😊 We're so glad you had a great experience.",
                    isFromUser = false
                ))
            }
            "feedback_negative" -> {
                Log.d("A2UI_FLOW", "[FEEDBACK] User clicked Negative")
                addMessage(UiMessage(
                    text = "I'm sorry to hear that. 😔 Could you please explain what difficulty you faced? Your feedback helps us improve!",
                    isFromUser = false
                ))
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
        Log.d("A2UI_FLOW", "[$level] $message")
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
                
                // 3. Offload AI processing to IO thread with CONVERSATIONAL MEMORY
                val responses: List<String>? = withContext(Dispatchers.IO) {
                    // Get chat history from database for true conversational memory
                    val chatHistory = chatMessageDao.getRecentMessages(limit = 10)
                    
                    // Add local persistent memory context
                    val persistentContext = memoryManager.getAllContext()
                    val contextFormatted = if (persistentContext.isNotEmpty()) {
                        "\n\n[SYSTEM CONTEXT - DO NOT EXECUTE UNLESS REQUESTED]\n" + 
                        persistentContext.entries.joinToString("\n") { (key, value) -> "$key: $value" } +
                        "\n[END SYSTEM CONTEXT]\n"
                    } else ""

                    // Use new context-aware method
                    adkMasterAgent?.processQueryWithMemory(text + contextFormatted, chatHistory) { status: String ->
                        _loadingState.value = ChatLoadingState(status = status)
                    }
                }

                responses?.forEachIndexed { index: Int, response: String ->
                    Log.d("A2UI_FLOW", "[STEP] Processing response item $index. Payload hash: ${response.hashCode()}")
                    if (response.trim().startsWith("{") && response.contains("version")) {
                        _loadingState.value = ChatLoadingState(status = "🎨 Preparing view...")
                        
                        // Process the message through the renderer (split if multi-line JSONL)
                        val fragments = splitA2UICommand(response)
                        Log.d("A2UI_FLOW", "[STEP] Found ${fragments.size} JSON fragments in response $index")
                        
                        fragments.forEach { fragment ->
                            renderer.processMessage(fragment)
                        }

                        // Only add to chat if it's an updateComponents message (the UI part)
                        if (response.contains("updateComponents")) {
                            Log.d("A2UI_FLOW", "[STEP] Adding A2UI message bubble for response $index")
                            addMessage(UiMessage(
                                text = "I've updated the view for you:",
                                isFromUser = false,
                                isA2UI = true,
                                a2uiPayload = response
                            ))
                        }
                    } else {
                        Log.d("A2UI_FLOW", "[STEP] Adding text message bubble for response $index. Content: ${response.take(20)}...")
                        if (response.isNotBlank()) {
                             addMessage(UiMessage(text = response, isFromUser = false))
                        }
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

    fun bookTable(numberOfPeople: Int, date: String, time: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                Log.d("A2UI_FLOW", "[BOOKING] Attempting to book: $date at $time")
                
                val calendar = java.util.Calendar.getInstance()
                
                // Robust parsing for "Today, 05 Aug" etc.
                if (date.contains("Tomorrow", ignoreCase = true)) {
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                } else if (date.contains("Fri", ignoreCase = true)) {
                    while (calendar.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.FRIDAY) {
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                } else if (date.contains("Sat", ignoreCase = true)) {
                    while (calendar.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY) {
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                }

                // Parse time "07:00 PM"
                val timeSdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                val timeDate = try { timeSdf.parse(time) } catch (e: Exception) { null }
                if (timeDate != null) {
                    val timeCal = java.util.Calendar.getInstance()
                    timeCal.time = timeDate
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
                    calendar.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
                }

                val startMillis = calendar.timeInMillis

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
                Log.i("A2UI_FLOW", "[BOOKING] Table reserved successfully: ${reservation.id.value}")
                
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
                
                _uiMessages.add(UiMessage(
                    text = "Great! I've booked a table for $numberOfPeople on $date at $time. Your booking ID is ${reservation.id.value.take(8).uppercase()}.", 
                    isFromUser = false
                ))
            } catch (e: Exception) {
                Log.e("A2UI_FLOW", "[BOOKING] Failed to book table: ${e.message}", e)
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
