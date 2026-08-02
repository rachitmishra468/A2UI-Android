package com.example.a2ui_sample.presentation.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui_sample.agent.ADKRestaurantMasterAgent
import com.example.a2ui_sample.agent.OrchestratorTools
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.Price
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import org.a2ui.compose.rendering.A2UIRenderer
import org.a2ui.compose.rendering.A2UILogger
import org.a2ui.compose.rendering.A2UILogLevel
import org.a2ui.compose.rendering.ActionHandler
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class RestaurantMainViewModel @Inject constructor(
    private val repository: MenuRepository
) : ViewModel(), A2UILogger, ActionHandler {

    private val _featuredItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val featuredItems: StateFlow<List<MenuItem>> = _featuredItems

    private val _uiMessages = mutableStateListOf<UiMessage>()
    val uiMessages: List<UiMessage> = _uiMessages

    private val _cartUpdateTrigger = mutableStateOf(0)
    val cartUpdateTrigger: MutableState<Int> = _cartUpdateTrigger

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    val renderer = A2UIRenderer(this)
    var adkMasterAgent: ADKRestaurantMasterAgent? = null

    init {
        Log.d("A2UI_INIT", "RestaurantMainViewModel init started")
        loadFeaturedItems()
        renderer.setActionHandler(this)
        
        try {
            Log.d("A2UI_INIT", "Creating ADKRestaurantMasterAgent")
            adkMasterAgent = ADKRestaurantMasterAgent(repository, OrchestratorTools())
            Log.d("A2UI_INIT", "ADKRestaurantMasterAgent created successfully")
        } catch (e: Exception) {
            Log.e("A2UI_INIT", "CRITICAL: ADKRestaurantMasterAgent initialization failed: ${e.message}", e)
        }

        // Welcome message
        _uiMessages.add(UiMessage("Hello! I'm your AI Restaurant Assistant. How can I help you today?", isFromUser = false))
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
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToCheckout) }
            }
            "viewBookings" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToBookings) }
            }
            "viewOrders" -> {
                viewModelScope.launch { _navigationEvents.emit(NavigationEvent.NavigateToOrders) }
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
        _uiMessages.add(UiMessage(text, isFromUser = true))
        
        viewModelScope.launch {
            try {
                adkMasterAgent?.let { agent ->
                    val responses = agent.processQuery(text)
                    responses.forEach { response ->
                        Log.d("A2UI_FLOW", "6. Checking response item: '${response.take(50).replace("\n", " ")}...'")
                        if (response.trim().startsWith("{") && response.contains("version")) {
                            Log.d("A2UI_FLOW", "7. SUCCESS: Detected A2UI Payload in response")
                            
                            // Split multi-command JSON if needed
                            val jsonList = splitA2UICommand(response)
                            jsonList.forEachIndexed { index, subJson ->
                                Log.d("A2UI_FLOW", "7.$index. Processing sub-command: ${subJson.take(100)}...")
                                renderer.processMessage(subJson)
                            }

                            _uiMessages.add(UiMessage(
                                text = "I've updated the view for you:",
                                isFromUser = false,
                                isA2UI = true,
                                a2uiPayload = response // Keep original for reference if needed
                            ))
                        } else {
                            Log.d("A2UI_FLOW", "7. Regular text response: ${response.take(50).replace("\n", " ")}...")
                            _uiMessages.add(UiMessage(response, isFromUser = false))
                        }
                    }
                } ?: run {
                    _uiMessages.add(UiMessage("I'm sorry, I'm still initializing. Please try again in a moment.", isFromUser = false))
                }
            } catch (e: Exception) {
                _uiMessages.add(UiMessage("Oops, I encountered an error: ${e.message}", isFromUser = false))
            }
        }
    }

    fun addToCart(menuItemId: Int) {
        repository.addToCart(menuItemId)
        _cartUpdateTrigger.value++
    }

    fun updateCartQuantity(menuItemId: Int, quantity: Int) {
        repository.updateCartQuantity(menuItemId, quantity)
        _cartUpdateTrigger.value++
    }

    fun removeFromCart(menuItemId: Int) {
        repository.removeFromCart(menuItemId)
        _cartUpdateTrigger.value++
    }

    fun getCartItems(): List<CartItem> = repository.getCart()

    fun getCartTotal(): Int = repository.getCartTotal()

    fun bookTable(numberOfPeople: Int, date: String, time: String) {
        val booking = TableBooking(
            numberOfPeople = numberOfPeople,
            bookingDate = date,
            bookingTime = time
        )
        repository.addBooking(booking)
        _uiMessages.add(UiMessage("Great! I've booked a table for $numberOfPeople on $date at $time. Your booking ID is ${booking.id}.", isFromUser = false))
    }

    fun getBookings(): List<TableBooking> = repository.getBookings()

    fun getCurrentOrders(): List<Order> = repository.getCurrentOrders()

    fun getPastOrders(): List<Order> = repository.getPastOrders()

    fun checkout(): Order? {
        val cartItems = repository.getCart()
        if (cartItems.isEmpty()) return null

        val subtotal = getCartTotal()
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

        repository.placeOrder(order)
        _cartUpdateTrigger.value++
        return order
    }

    fun clearChat() {
        _uiMessages.clear()
        _uiMessages.add(UiMessage("Chat cleared. How can I help you now?", isFromUser = false))
    }

    private fun splitA2UICommand(json: String): List<String> {
        return try {
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

data class UiMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isA2UI: Boolean = false,
    val a2uiPayload: String? = null
)
