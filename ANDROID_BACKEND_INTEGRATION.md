# Android App ↔ Enterprise Backend - Integration Guide

## 🔗 Connecting Your Existing Android App to Enterprise Backend

Your current Android app can continue working with minimal changes by acting as a client to the new backend!

---

## Step 1: Create API Client in Android App

```kotlin
// File: app/src/main/java/com/example/a2ui_sample/api/RestaurantAPIClient.kt

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

// API Service Interface
interface RestaurantAPIService {
    
    // Menu Endpoints
    @GET("api/v1/menu")
    suspend fun searchMenu(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("dietaryType") dietaryType: String? = null
    ): ApiResponse<SearchMenuResponse>

    @GET("api/v1/menu/categories")
    suspend fun getCategories(): ApiResponse<GetCategoriesResponse>

    // Cart Endpoints
    @POST("api/v1/cart")
    suspend fun createCart(
        @Body request: CreateCartRequest
    ): ApiResponse<CreateCartResponse>

    @POST("api/v1/cart/{cartId}/items")
    suspend fun addToCart(
        @Path("cartId") cartId: String,
        @Body request: AddToCartRequest
    ): ApiResponse<AddToCartResponse>

    @GET("api/v1/cart/{cartId}")
    suspend fun viewCart(
        @Path("cartId") cartId: String
    ): ApiResponse<ViewCartResponse>

    @DELETE("api/v1/cart/{cartId}/items/{menuItemId}")
    suspend fun removeFromCart(
        @Path("cartId") cartId: String,
        @Path("menuItemId") menuItemId: String
    ): ApiResponse<RemoveFromCartResponse>

    // Booking Endpoints
    @POST("api/v1/bookings")
    suspend fun bookTable(
        @Body request: BookTableRequest
    ): ApiResponse<BookTableResponse>

    @POST("api/v1/bookings/check-availability")
    suspend fun checkTableAvailability(
        @Body request: CheckAvailabilityRequest
    ): ApiResponse<CheckAvailabilityResponse>

    // Pricing Endpoint
    @POST("api/v1/pricing/calculate")
    suspend fun calculatePrice(
        @Body request: CalculatePriceRequest
    ): ApiResponse<CalculatePriceResponse>

    // Order & Checkout
    @POST("api/v1/orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequest
    ): ApiResponse<CreateOrderResponse>

    @POST("api/v1/payments")
    suspend fun processPayment(
        @Body request: ProcessPaymentRequest
    ): ApiResponse<ProcessPaymentResponse>

    // AI Chat
    @POST("api/v1/chat")
    suspend fun sendChatMessage(
        @Body request: ChatMessageRequest
    ): ApiResponse<ChatMessageResponse>
}

// Data Models
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T?,
    @SerializedName("error")
    val error: String? = null
)

data class SearchMenuResponse(
    @SerializedName("items")
    val items: List<MenuItemDTO>,
    @SerializedName("totalCount")
    val totalCount: Int
)

data class MenuItemDTO(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("price")
    val price: Double,
    @SerializedName("category")
    val category: String,
    @SerializedName("dietaryType")
    val dietaryType: String,
    @SerializedName("imageUrl")
    val imageUrl: String,
    @SerializedName("rating")
    val rating: Double,
    @SerializedName("isAvailable")
    val isAvailable: Boolean
)

data class AddToCartRequest(
    @SerializedName("menuItemId")
    val menuItemId: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("specialInstructions")
    val specialInstructions: String? = null
)

data class AddToCartResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("cartId")
    val cartId: String,
    @SerializedName("itemCount")
    val itemCount: Int,
    @SerializedName("totalPrice")
    val totalPrice: Double,
    @SerializedName("message")
    val message: String
)

data class BookTableRequest(
    @SerializedName("customerId")
    val customerId: String,
    @SerializedName("partySize")
    val partySize: Int,
    @SerializedName("date")
    val date: String, // YYYY-MM-DD
    @SerializedName("time")
    val time: String, // HH:mm
    @SerializedName("specialRequests")
    val specialRequests: String? = null
)

data class BookTableResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("bookingId")
    val bookingId: String,
    @SerializedName("tableNumber")
    val tableNumber: Int,
    @SerializedName("partySize")
    val partySize: Int,
    @SerializedName("bookingTime")
    val bookingTime: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String
)

data class ChatMessageRequest(
    @SerializedName("message")
    val message: String,
    @SerializedName("sessionId")
    val sessionId: String,
    @SerializedName("customerId")
    val customerId: String? = null
)

data class ChatMessageResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("actions")
    val actions: List<AgentAction>,
    @SerializedName("data")
    val data: Any? = null
)

data class AgentAction(
    @SerializedName("agent")
    val agent: String,
    @SerializedName("action")
    val action: String,
    @SerializedName("result")
    val result: Map<String, Any>
)

// Factory to create Retrofit client
object RestaurantAPIFactory {
    private var instance: RestaurantAPIService? = null

    fun getInstance(baseUrl: String = "http://api.restaurant.com/"): RestaurantAPIService {
        if (instance == null) {
            val httpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    // Add auth token if available
                    val token = SharedPreferencesHelper.getAuthToken()
                    val request = chain.request().newBuilder().apply {
                        if (!token.isNullOrEmpty()) {
                            addHeader("Authorization", "Bearer $token")
                        }
                    }.build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            instance = retrofit.create(RestaurantAPIService::class.java)
        }
        return instance!!
    }
}
```

---

## Step 2: Refactor ViewModel to Use API Client

```kotlin
// File: app/src/main/java/com/example/a2ui_sample/ui/RestaurantViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2ui_sample.api.RestaurantAPIFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromAgent: Boolean = false,
    val a2uiPayloads: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatState(
    val messages: List<UiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentCart: CartState? = null,
    val sessionId: String = UUID.randomUUID().toString()
)

data class CartState(
    val cartId: String?,
    val items: List<CartItem> = emptyList(),
    val itemCount: Int = 0,
    val totalPrice: Double = 0.0
)

data class CartItem(
    val menuItemId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

class RestaurantViewModel : ViewModel() {
    private val apiService = RestaurantAPIFactory.getInstance()
    
    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState

    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser

    init {
        _currentUser.value = SharedPreferencesHelper.getCurrentUserId()
    }

    // === MENU OPERATIONS ===
    fun searchMenu(query: String = "", category: String? = null, dietaryType: String? = null) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            try {
                val response = apiService.searchMenu(query, category, dietaryType)
                if (response.success && response.data != null) {
                    // Format menu items as A2UI JSON
                    val a2uiMessage = formatMenuAsA2UI(response.data.items)
                    
                    addMessage(
                        content = "Found ${response.data.totalCount} items",
                        isFromAgent = true,
                        a2uiPayloads = listOf(a2uiMessage)
                    )
                } else {
                    updateState { it.copy(error = response.error) }
                }
            } catch (e: Exception) {
                updateState { it.copy(error = e.message) }
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    // === CART OPERATIONS ===
    fun createCart() {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value ?: return@launch
                val response = apiService.createCart(CreateCartRequest(userId))
                
                if (response.success && response.data != null) {
                    updateState { currentState ->
                        currentState.copy(
                            currentCart = CartState(cartId = response.data.cartId)
                        )
                    }
                    SharedPreferencesHelper.saveCartId(response.data.cartId)
                }
            } catch (e: Exception) {
                updateState { it.copy(error = e.message) }
            }
        }
    }

    fun addToCart(menuItemId: String, quantity: Int = 1, specialInstructions: String? = null) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            try {
                val cartId = _chatState.value.currentCart?.cartId 
                    ?: SharedPreferencesHelper.getCartId() 
                    ?: return@launch

                val response = apiService.addToCart(
                    cartId,
                    AddToCartRequest(menuItemId, quantity, specialInstructions)
                )

                if (response.success && response.data != null) {
                    addMessage(
                        content = response.data.message,
                        isFromAgent = true
                    )
                    // Refresh cart
                    viewCart()
                } else {
                    updateState { it.copy(error = response.error) }
                }
            } catch (e: Exception) {
                updateState { it.copy(error = e.message) }
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    fun viewCart() {
        viewModelScope.launch {
            try {
                val cartId = _chatState.value.currentCart?.cartId 
                    ?: SharedPreferencesHelper.getCartId() 
                    ?: return@launch

                val response = apiService.viewCart(cartId)
                
                if (response.success && response.data != null) {
                    updateState { currentState ->
                        currentState.copy(
                            currentCart = CartState(
                                cartId = cartId,
                                items = response.data.items.map { item ->
                                    CartItem(
                                        menuItemId = item.menuItemId,
                                        name = item.name,
                                        quantity = item.quantity,
                                        unitPrice = item.unitPrice,
                                        totalPrice = item.totalPrice
                                    )
                                },
                                itemCount = response.data.itemCount,
                                totalPrice = response.data.total
                            )
                        )
                    }

                    val cartSummary = "Cart: ${response.data.itemCount} items, Total: ₹${response.data.total}"
                    addMessage(content = cartSummary, isFromAgent = true)
                }
            } catch (e: Exception) {
                updateState { it.copy(error = e.message) }
            }
        }
    }

    // === BOOKING OPERATIONS ===
    fun bookTable(partySize: Int, date: String, time: String, specialRequests: String? = null) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            try {
                val userId = _currentUser.value ?: return@launch
                val response = apiService.bookTable(
                    BookTableRequest(userId, partySize, date, time, specialRequests)
                )

                if (response.success && response.data != null) {
                    val message = """
                        ✅ ${response.data.message}
                        Table ${response.data.tableNumber} | ${response.data.bookingTime} | ${response.data.partySize} people
                        Booking ID: ${response.data.bookingId}
                    """.trimIndent()
                    
                    addMessage(content = message, isFromAgent = true)
                } else {
                    updateState { it.copy(error = response.error) }
                }
            } catch (e: Exception) {
                updateState { it.copy(error = e.message) }
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    // === AI CHAT ===
    fun sendChatMessage(message: String) {
        // Add user message
        addMessage(content = message, isFromAgent = false)

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            try {
                val response = apiService.sendChatMessage(
                    ChatMessageRequest(
                        message = message,
                        sessionId = _chatState.value.sessionId,
                        customerId = _currentUser.value
                    )
                )

                if (response.success) {
                    // Add agent response
                    addMessage(
                        content = response.data?.message ?: "Processing...",
                        isFromAgent = true,
                        a2uiPayloads = formatActionsAsA2UI(response.data?.actions)
                    )

                    // Update cart if any cart action was performed
                    response.data?.actions?.find { it.agent == "CartAgent" }?.let {
                        viewCart()
                    }
                } else {
                    updateState { it.copy(error = "Failed to process chat message") }
                }
            } catch (e: Exception) {
                updateState { it.copy(error = e.message) }
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    // === UI MESSAGE MANAGEMENT ===
    private fun addMessage(
        content: String,
        isFromAgent: Boolean = false,
        a2uiPayloads: List<String> = emptyList()
    ) {
        val newMessage = UiMessage(
            content = content,
            isFromAgent = isFromAgent,
            a2uiPayloads = a2uiPayloads
        )
        updateState { currentState ->
            currentState.copy(messages = currentState.messages + newMessage)
        }
    }

    fun clearChat() {
        updateState { it.copy(messages = emptyList()) }
    }

    private fun updateState(update: (ChatState) -> ChatState) {
        _chatState.value = update(_chatState.value)
    }

    // === HELPERS ===
    private fun formatMenuAsA2UI(items: List<MenuItemDTO>): String {
        // Convert menu items to A2UI JSON format
        val json = """
        {
            "version": "v0.10",
            "updateComponents": {
                "surfaceId": "restaurant_surface",
                "components": [
                    ${items.mapIndexed { index, item ->
                        """
                        {
                            "id": "item_card_${index + 1}",
                            "component": "Card",
                            "child": "item_col_${index + 1}"
                        },
                        {
                            "id": "item_col_${index + 1}",
                            "component": "Column",
                            "children": ["item_img_${index + 1}", "item_name_${index + 1}", "item_price_${index + 1}"]
                        },
                        {
                            "id": "item_name_${index + 1}",
                            "component": "Text",
                            "text": "${item.name}",
                            "variant": "h4"
                        },
                        {
                            "id": "item_price_${index + 1}",
                            "component": "Text",
                            "text": "₹${item.price}",
                            "variant": "body"
                        }
                        """
                    }.joinToString(",")}
                ]
            }
        }
        """.trimIndent()
        return json
    }

    private fun formatActionsAsA2UI(actions: List<AgentAction>?): List<String> {
        // Convert agent actions to A2UI JSON format
        return emptyList() // Implementation depends on action types
    }
}
```

---

## Step 3: Update Environment Configuration

```kotlin
// File: app/src/main/java/com/example/a2ui_sample/config/ApiConfig.kt

object ApiConfig {
    // Development
    const val DEV_BASE_URL = "http://10.0.2.2:3000/" // Android emulator localhost

    // Staging
    const val STAGING_BASE_URL = "https://api-staging.restaurant.com/"

    // Production
    const val PROD_BASE_URL = "https://api.restaurant.com/"

    fun getBaseUrl(environment: String = BuildConfig.FLAVOR): String {
        return when (environment) {
            "dev" -> DEV_BASE_URL
            "staging" -> STAGING_BASE_URL
            "prod" -> PROD_BASE_URL
            else -> DEV_BASE_URL
        }
    }
}

// Update RestaurantAPIFactory
val apiService = RestaurantAPIFactory.getInstance(ApiConfig.getBaseUrl())
```

---

## Step 4: Dependency Injection Setup

```kotlin
// File: app/src/main/java/com/example/a2ui_sample/di/AppContainer.kt

import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@dagger.Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Provides
    @Singleton
    fun provideRestaurantAPIService(): RestaurantAPIService {
        return RestaurantAPIFactory.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideRestaurantViewModel(
        apiService: RestaurantAPIService
    ): RestaurantViewModel {
        return RestaurantViewModel(apiService)
    }
}
```

---

## Step 5: Network Configuration

```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.restaurant.com</domain>
        <pin-set expiration="2026-07-24">
            <!-- SSL pinning for production -->
            <pin digest="SHA-256">your-certificate-pin-here</pin>
        </pin-set>
    </domain-config>
    
    <!-- Allow cleartext for development on emulator -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

```xml
<!-- AndroidManifest.xml -->
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
    ...
</application>
```

---

## Step 6: UI - Replace Agent Calls with API Calls

```kotlin
// Before: Local agent call
class AiRestaurantScreen (
    viewModel: RestaurantViewModel
) {
    // ...
    fun sendMessage(message: String) {
        // OLD: viewModel.agent.processQuery(message)
        
        // NEW: API call through ViewModel
        viewModel.sendChatMessage(message)
    }
}
```

---

## Step 7: Migration Testing

```kotlin
// tests/integration/ApiIntegrationTest.kt
class ApiIntegrationTest {
    
    @Test
    fun testSearchMenu_ReturnsItems() = runBlocking {
        val response = apiService.searchMenu("pizza")
        
        assertThat(response.success).isTrue()
        assertThat(response.data?.items).isNotEmpty()
    }

    @Test
    fun testBookTable_Success() = runBlocking {
        val response = apiService.bookTable(
            BookTableRequest(
                customerId = "user-1",
                partySize = 4,
                date = "2025-08-15",
                time = "19:00"
            )
        )
        
        assertThat(response.success).isTrue()
        assertThat(response.data?.bookingId).isNotEmpty()
    }

    @Test
    fun testChatMessage_DetectsIntent() = runBlocking {
        val response = apiService.sendChatMessage(
            ChatMessageRequest(
                message = "Add 2 pizzas",
                sessionId = "session-1"
            )
        )
        
        assertThat(response.success).isTrue()
        assertThat(response.data?.actions?.size).isGreaterThan(0)
    }
}
```

---

## Benefits of This Integration

✅ **Minimal Changes to Existing App**
- Keep UI components as-is
- Only replace RestaurantAgent with API calls

✅ **Gradual Migration**
- Migrate screens one by one
- Phase 1: Search & Menu
- Phase 2: Cart & Checkout
- Phase 3: Booking
- Phase 4: Full AI integration

✅ **Shared Backend**
- Web, Mobile, AI all use same API
- Business logic centralized
- Easier to maintain

✅ **Future-Proof**
- Ready for microservices
- Ready for offline-first architecture
- Ready for voice integration

✅ **Testing**
- Mock API responses easily
- Test all flows independently

---

## Deployment Checklist

- [ ] Backend deployed to staging
- [ ] API endpoints tested
- [ ] Android app points to staging API
- [ ] All CRUD operations working
- [ ] Search & filtering working
- [ ] Cart operations working
- [ ] Booking working
- [ ] AI chat working
- [ ] Load testing passed
- [ ] Security audit passed
- [ ] Deployed to production
- [ ] Monitor logs & metrics

---

**Your Android app + Enterprise Backend = 🚀 Production-Ready System!**

