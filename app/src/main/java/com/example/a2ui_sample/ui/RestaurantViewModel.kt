package com.example.a2ui_sample.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui_sample.agent.A2UIResponseBuilder
import com.example.a2ui_sample.domain.usecases.AddToCartUseCaseImpl
import com.example.a2ui_sample.domain.usecases.SearchMenuUseCaseImpl
import com.example.a2ui_sample.domain.usecases.ViewCartUseCaseImpl
import com.example.a2ui_sample.domain.usecases.BookTableUseCaseImpl
import com.example.a2ui_sample.domain.usecases.CalculatePriceUseCaseImpl
import com.example.a2ui_sample.domain.usecases.CheckoutUseCaseImpl
import com.example.a2ui_sample.data.repository.MenuRepositoryImpl
import com.example.a2ui_sample.domain.repository.MenuRepository
import kotlinx.coroutines.launch
import org.a2ui.compose.rendering.A2UIRenderer
import java.util.UUID
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.agent.ADKRestaurantMasterAgent
import com.example.a2ui_sample.agent.OrchestratorTools


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

    private val repository: MenuRepository by lazy { MenuRepositoryImpl.getInstance(application) }
    private lateinit var adkMasterAgent: ADKRestaurantMasterAgent

    // Domain use-cases
    private val searchUseCase by lazy { SearchMenuUseCaseImpl(repository) }
    private val addToCartUseCase by lazy { AddToCartUseCaseImpl(repository) }
    private val viewCartUseCase by lazy { ViewCartUseCaseImpl(repository) }
    private val bookTableUseCase by lazy { BookTableUseCaseImpl(repository) }
    private val calculatePriceUseCase by lazy { CalculatePriceUseCaseImpl(repository) }
    private val checkoutUseCase by lazy { CheckoutUseCaseImpl(repository) }

    // When true, the UI will use manual use-case calls instead of agent orchestration
    private var manualMode: Boolean = false

    val renderer = A2UIRenderer()

    private val _uiMessages = mutableStateListOf<UiMessage>()
    val uiMessages: List<UiMessage> = _uiMessages

    // Observable cart state for UI recomposition
    private val _cartUpdateTrigger = mutableStateOf(0)
    val cartUpdateTrigger = _cartUpdateTrigger

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
            try {
                Log.d("A2UI_FLOW", "2. Processing message with ADK Master Agent")

                // Initialize ADK agent on first use
                if (!::adkMasterAgent.isInitialized) {
                    adkMasterAgent = ADKRestaurantMasterAgent(
                        repository,
                        OrchestratorTools()
                    )
                }

                val a2uiMessages = adkMasterAgent.processQuery(query)

                Log.d("A2UI_FLOW", "5. Received ${a2uiMessages.size} A2UI JSON messages from ADK Agent")

                _uiMessages.add(
                    UiMessage(
                        content = "🤖 ",
                        isFromAgent = true,
                        isA2UI = true,
                        a2uiPayloads = a2uiMessages
                    )
                )


                // Trigger cart update trigger so UI recomposes with new cart count
                _cartUpdateTrigger.value++
            } catch (e: Exception) {
                Log.e("VM", "Error: ${e.message}", e)
                _uiMessages.add(
                    UiMessage(
                        content = "❌ Error: ${e.message}",
                        isFromAgent = true
                    )
                )

            }
        }
    }

    private fun processManualCommand(query: String): List<String> {
        val builder = A2UIResponseBuilder()
        val q = query.trim()

        val addIntent = Regex("\\b(add|order|buy|put)\\b", RegexOption.IGNORE_CASE)
        val cartViewIntent = Regex("\\b(show|view|my)\\s+(cart|shopping)\\b", RegexOption.IGNORE_CASE)
        val bookingIntent = Regex("\\b(book|reserve|reservation|table)\\b", RegexOption.IGNORE_CASE)
        val checkoutIntent = Regex("\\b(checkout|place order|pay)\\b", RegexOption.IGNORE_CASE)

        return when {
            addIntent.containsMatchIn(q) -> {
                val extract = Regex("(?i)\\b(add|order|buy|put)\\b\\s+(.*)")
                val m = extract.find(q)
                val rawItem = m?.groupValues?.get(2)?.replace("in my cart", "", ignoreCase = true)
                    ?.replace("to my cart", "", ignoreCase = true)
                    ?.replace("please", "", ignoreCase = true)
                    ?.trim() ?: ""

                if (rawItem.isNotBlank()) {
                    val found = repository.getMenuItems().firstOrNull { it.name.contains(rawItem, ignoreCase = true) }
                    if (found != null) {
                        val resp = addToCartUseCase.execute(found.id)
                        resp?.let { builder.build(it) } ?: listOf(builder.build(AgentResponse.Error("Item not found")).first())
                    } else {
                        val res = searchUseCase.execute(rawItem, null, null, null)
                        builder.build(AgentResponse.MenuResults(res, rawItem))
                    }
                } else {
                    listOf(builder.build(AgentResponse.Message("Please specify an item to add")).first())
                }
            }
            cartViewIntent.containsMatchIn(q) || q.equals("cart", ignoreCase = true) -> {
                builder.build(viewCartUseCase.execute())
            }
            bookingIntent.containsMatchIn(q) -> {
                val people = Regex("(\\d{1,2})\\s*(?:people|persons|guests)?", RegexOption.IGNORE_CASE).find(q)?.groupValues?.get(1)?.toIntOrNull()
                val time = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)", RegexOption.IGNORE_CASE).find(q)?.groupValues?.get(1)
                if (people != null && !time.isNullOrBlank()) {
                    val booking = bookTableUseCase.execute(people, time)
                    builder.build(booking)
                } else {
                    builder.build(AgentResponse.BookingRequest(step = "ask_people"))
                }
            }
            checkoutIntent.containsMatchIn(q) -> {
                builder.build(checkoutUseCase.execute(null))
            }
            else -> {
                val res = searchUseCase.execute(q, null, null, null)
                builder.build(AgentResponse.MenuResults(res, q))
            }
        }
    }

    // Trigger recomposition for cart changes from agent
    fun triggerCartUpdate() {
        _cartUpdateTrigger.value++
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

    // Expose menu items for the UI
    fun getMenuItems(): List<MenuItem> = repository.getMenuItems()

    // Manual add-to-cart helper that returns the A2UI payloads and updates UI
    fun addItemToCartById(menuItemId: Int): List<String> {
        val resp = addToCartUseCase.execute(menuItemId)
        val builder = A2UIResponseBuilder()
        val msgs = resp?.let { builder.build(it) } ?: listOf(builder.build(AgentResponse.Error("Item not found")).first())
        // Render and add a system bubble summarizing the update
        msgs.forEach { renderer.processMessage(it) }
        // Trigger UI recomposition
        _cartUpdateTrigger.value++
        _uiMessages.add(
            UiMessage(
                content = "I've updated the view for you:",
                isFromAgent = true,
                isA2UI = true,
                a2uiPayloads = msgs
            )
        )
        return msgs
    }

    // Cart helpers for UI
    fun getCartItems() = repository.getCart()

    fun getCartTotal(): Int = repository.getCartTotal()
    
    fun getCartTotalPrice(): Int = calculatePriceUseCase.execute()

    fun updateCartQuantity(menuItemId: Int, quantity: Int): List<String> {
        val updated = repository.updateCartQuantity(menuItemId, quantity)
        val builder = A2UIResponseBuilder()
        val msgs = if (updated != null) builder.build(AgentResponse.CartView(repository.getCart(), repository.getCartTotal()))
        else listOf(builder.build(AgentResponse.Message("Item removed from cart")).first())
        msgs.forEach { renderer.processMessage(it) }
        // Trigger UI recomposition
        _cartUpdateTrigger.value++
        _uiMessages.add(UiMessage(content = "Cart updated", isFromAgent = true, isA2UI = true, a2uiPayloads = msgs))
        return msgs
    }

    fun removeCartItem(menuItemId: Int): List<String> {
        val removed = repository.removeFromCart(menuItemId)
        val builder = A2UIResponseBuilder()
        val msgs = if (removed) builder.build(AgentResponse.CartView(repository.getCart(), repository.getCartTotal()))
        else listOf(builder.build(AgentResponse.Error("Item not in cart")).first())
        msgs.forEach { renderer.processMessage(it) }
        // Trigger UI recomposition
        _cartUpdateTrigger.value++
        _uiMessages.add(UiMessage(content = "Cart updated", isFromAgent = true, isA2UI = true, a2uiPayloads = msgs))
        return msgs
    }

    fun checkoutCart(): List<String> {
        val result = checkoutUseCase.execute(null)
        val builder = A2UIResponseBuilder()
        val msgs = builder.build(result)
        msgs.forEach { renderer.processMessage(it) }
        // Trigger UI recomposition
        _cartUpdateTrigger.value++
        _uiMessages.add(UiMessage(content = "Checkout complete", isFromAgent = true, isA2UI = true, a2uiPayloads = msgs))
        return msgs
    }

    fun setManualMode(enabled: Boolean) {
        manualMode = enabled
        _uiMessages.add(
            UiMessage(
                content = if (enabled) "Manual mode enabled. Using direct use-cases." else "Agent mode enabled. Using ADK.",
                isFromAgent = true
            )
        )
    }

    fun isManualMode(): Boolean = manualMode

    // ---- Table Booking (manual UI) ----

    /** Books a table manually (from the Table Booking screen form), reusing the same use case the agent uses. */
    fun bookTableManually(numberOfPeople: Int, bookingTime: String): List<String> {
        val resp = bookTableUseCase.execute(numberOfPeople, bookingTime)
        val builder = A2UIResponseBuilder()
        val msgs = builder.build(resp)
        msgs.forEach { renderer.processMessage(it) }
        _cartUpdateTrigger.value++
        _uiMessages.add(
            UiMessage(
                content = "Table booked",
                isFromAgent = true,
                isA2UI = true,
                a2uiPayloads = msgs
            )
        )
        return msgs
    }

    fun getBookings() = repository.getBookings()

    // ---- Orders (Current / Past) ----

    fun getCurrentOrders() = repository.getCurrentOrders()

    fun getPastOrders() = repository.getPastOrders()

    /** Marks a current order as delivered/completed, moving it to the Past Orders list. */
    fun completeOrder(orderId: String) {
        repository.completeOrder(orderId)
        _cartUpdateTrigger.value++
    }
}
