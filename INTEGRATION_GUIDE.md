/**
 * INTEGRATION GUIDE - Complete Restaurant Management System
 * 
 * This guide shows how to integrate the new Clean Architecture + ADK Multi-Agent System
 * with the existing Presentation Layer (UI).
 */

// ==============================================================================
// STEP 1: Update Intent Enum in AgentCore.kt
// ==============================================================================

Add this to the Intent enum:

    CHECK_AVAILABILITY,


// ==============================================================================
// STEP 2: Update RestaurantViewModel to use new Agent System
// ==============================================================================

Replace the content of RestaurantViewModel.kt with:

```kotlin
package com.example.a2ui_sample.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui_sample.data.MenuItem
import com.example.a2ui_sample.data.CartItem
import com.example.a2ui_sample.data.MenuRepository
import kotlinx.coroutines.launch
import java.util.UUID

// Import new agent system
import com.example.restaurant.application.agents.*
import com.example.restaurant.application.agents.core.*
import com.example.restaurant.application.usecases.*
import com.example.restaurant.domain.repositories.*
import com.example.restaurant.domain.services.*
import com.example.restaurant.domain.valueobjects.*
import com.example.restaurant.infrastructure.database.AppDatabase
import com.example.restaurant.infrastructure.repositories.*
import com.example.restaurant.infrastructure.logging.Logger
import com.example.restaurant.infrastructure.logging.RequestTracer

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromAgent: Boolean,
    val isA2UI: Boolean = false,
    val a2uiPayloads: List<String> = emptyList()
)

class RestaurantViewModel(application: Application) : AndroidViewModel(application) {
    // Database and repositories
    private val database by lazy { AppDatabase.getInstance(application) }
    
    private val customerRepo by lazy { 
        CustomerRepositoryImpl(database.customerDao())
    }
    private val menuRepo by lazy { 
        MenuRepositoryImpl(database.menuItemDao())
    }
    private val reservationRepo by lazy { 
        ReservationRepositoryImpl(database.reservationDao())
    }
    private val tableRepo by lazy { 
        TableRepositoryImpl(database.tableDao())
    }
    private val orderRepo by lazy { 
        OrderRepositoryImpl(database.orderDao(), database.orderItemDao())
    }
    private val deliveryRepo by lazy { 
        DeliveryRepositoryImpl(database.deliveryDao())
    }
    private val feedbackRepo by lazy { 
        FeedbackRepositoryImpl(database.feedbackDao())
    }

    // Domain services
    private val priceCalculator by lazy { PriceCalculator() }
    private val orderValidator by lazy { OrderValidator() }
    private val reservationValidator by lazy { 
        ReservationValidator(reservationRepo, tableRepo)
    }
    private val availabilityService by lazy { 
        AvailabilityService(tableRepo, reservationRepo)
    }

    // Use cases
    private val bookTableUseCase by lazy { 
        BookTableUseCaseImpl(reservationRepo, tableRepo, reservationValidator)
    }
    private val cancelReservationUseCase by lazy { 
        CancelReservationUseCaseImpl(reservationRepo, tableRepo, reservationValidator)
    }
    private val checkAvailabilityUseCase by lazy { 
        CheckAvailabilityUseCaseImpl(availabilityService)
    }
    private val getMenuItemsUseCase by lazy { 
        GetMenuItemsUseCaseImpl(menuRepo)
    }
    private val searchMenuUseCase by lazy { 
        SearchMenuUseCaseImpl(menuRepo)
    }
    private val createOrderUseCase by lazy { 
        CreateOrderUseCaseImpl(orderRepo, menuRepo, priceCalculator, orderValidator)
    }
    private val getOrdersUseCase by lazy { 
        GetOrdersUseCaseImpl(orderRepo)
    }
    private val trackDeliveryUseCase by lazy { 
        TrackDeliveryUseCaseImpl(deliveryRepo)
    }
    private val submitFeedbackUseCase by lazy { 
        SubmitFeedbackUseCaseImpl(feedbackRepo)
    }

    // Agents
    private val reservationAgent by lazy {
        ReservationAgent(
            bookTableUseCase,
            cancelReservationUseCase,
            checkAvailabilityUseCase
        )
    }
    private val menuAgent by lazy {
        MenuAgent(getMenuItemsUseCase, searchMenuUseCase)
    }
    private val orderAgent by lazy {
        OrderAgent(createOrderUseCase, getOrdersUseCase)
    }
    private val deliveryAgent by lazy {
        DeliveryAgent(trackDeliveryUseCase)
    }
    private val feedbackAgent by lazy {
        FeedbackAgent(submitFeedbackUseCase)
    }

    // Orchestrator
    val orchestrator by lazy {
        AgentSystemFactory.createAgentSystem(
            reservationAgent,
            menuAgent,
            orderAgent,
            deliveryAgent,
            feedbackAgent
        )
    }

    // Local repository (for existing cart functionality)
    private val localRepository by lazy { MenuRepository(application) }

    // UI state
    private val _uiMessages = mutableStateListOf<UiMessage>()
    val uiMessages: List<UiMessage> = _uiMessages

    private val _cartUpdateTrigger = mutableStateOf(0)
    val cartUpdateTrigger = _cartUpdateTrigger

    // Current customer (mock for demo)
    private val currentCustomerId: CustomerId?
        get() = CustomerId("demo-customer-123")

    init {
        _uiMessages.add(
            UiMessage(
                content = "Hi! 👋 I'm your Restaurant AI Assistant. You can ask me to:\n" +
                    "📅 Book a table\n" +
                    "🍽️ Search menu items\n" +
                    "🛒 Add items to cart\n" +
                    "📍 Track your order\n" +
                    "⭐ Submit feedback",
                isFromAgent = true
            )
        )
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // Start request tracing
        val correlationId = RequestTracer.startRequest()
        Logger.logRequestReceived(correlationId, query)

        // Add user message
        _uiMessages.add(UiMessage(content = query, isFromAgent = false))

        viewModelScope.launch {
            try {
                Logger.debug(correlationId, "ViewModel", "Processing message through agent orchestrator")

                // Process through agent orchestrator
                val response = orchestrator.handleUserInput(query, currentCustomerId)

                Logger.debug(correlationId, "ViewModel", "Agent response received: $response")

                // Add agent response
                val agentMessage = when (response) {
                    is AgentResponse.Success -> {
                        UiMessage(
                            content = response.message,
                            isFromAgent = true,
                            isA2UI = response.a2uiPayload != null,
                            a2uiPayloads = response.a2uiPayload?.let { listOf(it) } ?: emptyList()
                        )
                    }
                    is AgentResponse.Error -> {
                        UiMessage(
                            content = "❌ ${response.message}",
                            isFromAgent = true
                        )
                    }
                    is AgentResponse.MultiStep -> {
                        UiMessage(
                            content = response.message,
                            isFromAgent = true
                        )
                    }
                    is AgentResponse.Redirect -> {
                        UiMessage(
                            content = "Redirecting to ${response.screen}...",
                            isFromAgent = true
                        )
                    }
                }

                _uiMessages.add(agentMessage)
                Logger.info(correlationId, "ViewModel", "Message added to UI")

                RequestTracer.endRequest()
            } catch (e: Exception) {
                Logger.error(correlationId, "ViewModel", "Error processing message", e)
                _uiMessages.add(
                    UiMessage(
                        content = "❌ Error: ${e.message}",
                        isFromAgent = true
                    )
                )
                RequestTracer.endRequest()
            }
        }
    }

    // Manual UI operations (dual execution - also call use cases or agents)
    fun addItemToCartById(itemId: Int) {
        viewModelScope.launch {
            try {
                val correlationId = RequestTracer.startRequest()
                val item = localRepository.getMenuItems().find { it.id == itemId }
                if (item != null) {
                    localRepository.addToCart(itemId)
                    _cartUpdateTrigger.value++
                    Logger.info(correlationId, "ViewModel", "Item added to cart: $itemId")
                    RequestTracer.endRequest()
                }
            } catch (e: Exception) {
                Logger.error(RequestTracer.getCurrentCorrelationId(), "ViewModel", "Error adding item", e)
            }
        }
    }

    fun getMenuItems(): List<MenuItem> = localRepository.getMenuItems()

    fun getCartItems(): List<CartItem> = localRepository.getCart()

    fun getCartTotal(): Int = localRepository.getCartTotal()

    fun updateCartQuantity(menuItemId: Int, quantity: Int) {
        localRepository.updateCartQuantity(menuItemId, quantity)
        _cartUpdateTrigger.value++
    }

    fun removeCartItem(menuItemId: Int) {
        localRepository.removeFromCart(menuItemId)
        _cartUpdateTrigger.value++
    }

    fun checkoutCart() {
        localRepository.clearCart()
        _cartUpdateTrigger.value++
    }

    fun clearChat() {
        _uiMessages.clear()
        init()
    }
}
```

// ==============================================================================
// STEP 3: Update build.gradle.kts dependencies (if using Room)
// ==============================================================================

Add these dependencies to app/build.gradle.kts:

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")


// ==============================================================================
// STEP 4: Install and Test
// ==============================================================================

Build and run:
    ./gradlew.bat clean assembleDebug
    ./gradlew.bat installDebug

The app will now:
✅ Use clean architecture
✅ ✅ Have complete database integration
✅ Have multi-agent orchestration
✅ Support dual execution (manual UI + AI chat)
✅ Have comprehensive logging with correlation IDs
✅ Route all requests through agents

All existing UI functionality remains intact while adding powerful AI capabilities!

