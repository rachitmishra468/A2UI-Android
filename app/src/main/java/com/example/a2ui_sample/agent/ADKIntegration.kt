package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.data.repository.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.example.a2ui_sample.BuildConfig
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.annotations.Tool
import kotlin.time.ExperimentalTime

/**
 * ADK-Based Restaurant Master Agent
 * Uses REAL Gemini LLM for reasoning + Tools for execution
 * Fully integrated with Clean Architecture use cases
 */
class ADKRestaurantMasterAgent(
    private val menuRepository: MenuRepository,
    private val agentTools: ADKRestaurantAgentTools
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }

    // Gemini LLM Configuration
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel by lazy { Gemini("gemini-3.6-flash", apiKey) }

    @OptIn(ExperimentalTime::class)
    private val sessionKey by lazy { SessionKey("RestaurantApp", "DefaultUser", "Session-Restaurant") }

    @OptIn(ExperimentalTime::class)
    private val session by lazy { Session(sessionKey) }

    /**
     * ADK LlmAgent with comprehensive tool set
     * Uses Gemini for intelligent reasoning about user intent
     */
    @OptIn(ExperimentalTime::class)
    private val adkAgent = LlmAgent(
        name = "RestaurantMasterAgent",
        model = geminiModel,
        instruction = Instruction(
            """
 You are a Restaurant Ordering Assistant.

            RULES (must follow exactly):
            1) Always return exactly one tool invocation and nothing else. Do NOT return plain text.
            2) If the user message contains any of the verbs: add, order, buy, put (case-insensitive) followed by a food phrase, ALWAYS call add_item_to_cart(itemName="<extracted item>").
               - Extract the item name after the verb. Remove words like "in my cart", "to my cart", "please", etc.
               - Normalize to Title Case (e.g., "masala dosa" -> "Masala Dosa").
               - Do NOT call get_full_menu for these add/order/buy/put intents.
               - If you cannot extract any food phrase after the verb, call search_menu(query="<original user text>").
            3) If the user asks to view or show cart (contains "cart" or "show cart"), call view_cart().
            4) If the user explicitly asks to see the entire menu (contains phrases like "show menu", "menu items", "what can I order", "show all items"), call get_full_menu().
            5) If the user requests filters/specific categories or ambiguous item names (e.g., "vegetarian", "pizza under 300", "spicy paneer"), call search_menu(query="<extracted phrase>").
            6) If the user asks for suggestions, call get_recommendations(criteria="<extracted>") or get_recommendations() when no criteria.
            7) If the user wants to book a table/reservation (contains "book", "reserve", "table"), call book_table_step(step="ask_people").
            8) If the user has provided a number of people, call book_table_step(step="ask_time", numberOfPeople="<number>").
            9) If the user has provided a time for booking, call book_table_step(step="complete", numberOfPeople="<number>", bookingTime="<time>").

            Examples (follow these EXACTLY):
              User: add masala dosa in my cart
              Tool: add_item_to_cart(itemName="Masala Dosa")

              User: show menu
              Tool: get_full_menu()

              User: show my cart
              Tool: view_cart()

              User: vegetarian items
              Tool: search_menu(type="veg")

              User: book a table
              Tool: book_table_step(step="ask_people")

              User: 4 people
              Tool: book_table_step(step="ask_time", numberOfPeople="4")

              User: 8:00 PM
              Tool: book_table_step(step="complete", numberOfPeople="4", bookingTime="8:00 PM")

            IMPORTANT: Under no circumstances return get_full_menu() in response to an add/order/buy/put user message.
            """.trimIndent()
        ),
        tools = agentTools.generatedTools(),
        maxSteps = 1
    )

    @OptIn(ExperimentalTime::class)
    suspend fun processQuery(userMessage: String): List<String> {
        Log.d("ADK_AGENT", "Step 1: Received user query: '$userMessage'")

        Log.d("ADK_AGENT", "Step 2: Resetting agent tools")
        agentTools.reset()

        Log.d("ADK_AGENT", "Step 3: Creating InvocationContext")
        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, userMessage)
        )

        var agentResponseText: String? = null

        try {
            Log.d("ADK_AGENT", "Step 4: Starting adkAgent.runAsync")
            adkAgent.runAsync(context).collect { event ->
                val text = event.content?.parts?.firstOrNull()?.text
                
                Log.d("ADK_AGENT", "Step 5: Received ADK Event: author=${event.author}, text=${text?.take(50)}")

                if (!text.isNullOrBlank() && event.author == "RestaurantMasterAgent") {
                    Log.d("ADK_AGENT", "Step 6: Captured agent response text")
                    agentResponseText = text
                }
            }
        } catch (e: Exception) {
            Log.e("ADK_AGENT", "Step ERROR: ADK Error occurred: ${e.message}", e)
            return listOf(responseBuilder.build(AgentResponse.Error("Error: ${e.message}")).last())
        }

        Log.d("ADK_AGENT", "Step 7: Checking for tool response")
        val toolResponse = agentTools.getLastResponse()
        
        Log.d("ADK_AGENT", "Step 8: Determining final response type")
        val finalResponse = when {
            toolResponse != null -> {
                Log.d("ADK_AGENT", "Step 9a: Using tool-generated response: $toolResponse")
                toolResponse
            }
            // If the agent replied with a generic success text or nothing, but the query 
            // is clearly for the cart/menu, use the manual intent as a safety fallback.
            userMessage.lowercase().contains(Regex("cart|basket|menu")) -> {
                Log.d("ADK_AGENT", "Step 9b: Using manual intent fallback for cart/menu")
                processManualIntent(userMessage)
            }
            !agentResponseText.isNullOrBlank() -> {
                Log.d("ADK_AGENT", "Step 9c: Using agent text response")
                AgentResponse.Message(agentResponseText)
            }
            else -> {
                Log.d("ADK_AGENT", "Step 9d: Fallback to manual intent matching (catch-all)")
                processManualIntent(userMessage)
            }
        }

        Log.d("ADK_AGENT", "Step 10: Building final A2UI messages")
        val result = responseBuilder.build(finalResponse)
        
        // Clean up lastResponse so it doesn't bleed into the next request
        agentTools.reset()
        
        Log.d("ADK_AGENT", "Step 11: Final result count: ${result.size}")
        return result
    }

    /**
     * Fallback intent matcher for cases where the ADK Agent doesn't call a tool
     * but the user's intent is clear (e.g., "my cart", "show menu").
     */
    private fun processManualIntent(query: String): AgentResponse {
        val q = query.lowercase().trim()
        return when {
            q.contains("cart") || q.contains("basket") -> {
                AgentResponse.CartView(menuRepository.getCart(), menuRepository.getCartTotal())
            }
            q.contains("menu") -> {
                AgentResponse.MenuResults(menuRepository.getMenuItems(), "Menu")
            }
            else -> AgentResponse.Message("I'm sorry, I couldn't process that. Try asking for the menu or viewing your cart.")
        }
    }
}

/**
 * ADK Tools for Restaurant Operations
 * These are the actual tools that Gemini calls during reasoning
 */
class ADKRestaurantAgentTools(internal val repository: MenuRepository) {
    private var lastResponse: AgentResponse? = null
    private var bookingState: MutableMap<String, Any> = mutableMapOf()

    fun getLastResponse(): AgentResponse? = lastResponse

    fun reset() {
        lastResponse = null
    }

    @Tool(
        name = "add_item_to_cart",
        description = "Add a food item to the customer's cart. Requires exact item name."
    )
    fun addItemToCart(
        /** Exact name of the food item (e.g., 'Masala Dosa', 'Paneer Butter Masala') */
        itemName: String
    ): String {
        Log.d("ADK_AGENT", "TOOL: addItemToCart started for '$itemName'")

        val query = itemName.trim().replace(Regex("\\s+"), " ")
        val items = repository.getMenuItems()

        var item = items.firstOrNull { it.name.equals(query, ignoreCase = true) }

        if (item == null) {
            Log.d("ADK_AGENT", "TOOL: Exact match not found, trying fuzzy match")
            val containsMatches = items.filter {
                it.name.contains(itemName, ignoreCase = true) || itemName.contains(it.name, ignoreCase = true)
            }
            if (containsMatches.size == 1) {
                Log.d("ADK_AGENT", "TOOL: Found single fuzzy match: ${containsMatches.first().name}")
                item = containsMatches.first()
            } else if (containsMatches.size > 1) {
                Log.d("ADK_AGENT", "TOOL: Multiple matches found (${containsMatches.size})")
                lastResponse = AgentResponse.MenuResults(containsMatches, "Multiple matches")
                return "Multiple items match '$itemName'. Which one?"
            }
        }

        if (item == null) {
            Log.d("ADK_AGENT", "TOOL: Item not found at all")
            return "❌ Item '$itemName' not found in menu"
        }

        Log.d("ADK_AGENT", "TOOL: Adding item ID ${item.id} to cart")
        repository.addToCart(item.id)
        lastResponse = AgentResponse.CartUpdate(item, repository.getCart().sumOf { it.quantity })
        return "✅ ${item.name} added to cart (₹${item.price})"
    }

    @Tool(
        name = "view_cart",
        description = "Display all items currently in the shopping cart"
    )
    fun viewCart(): String {
        Log.d("ADK_TOOLS", "view_cart called")

        val cartItems = repository.getCart()
        val total = repository.getCartTotal()

        lastResponse = AgentResponse.CartView(cartItems, total)

        if (cartItems.isEmpty()) {
            return "Your cart is empty"
        }

        return buildString {
            appendLine("📦 Your Cart:")
            cartItems.forEach {
                appendLine("  • ${it.menuItem.name} (₹${it.menuItem.price}) x ${it.quantity}")
            }
            appendLine("Total: ₹$total")
        }
    }

    @Tool(
        name = "search_menu",
        description = "Search menu items by category, type (veg/non-veg), or price range"
    )
    fun searchMenu(
        category: String? = null,
        type: String? = null,
        maxPrice: Int? = null
    ): String {
        Log.d("ADK_TOOLS", "search_menu: category=$category, type=$type, maxPrice=$maxPrice")

        val normalizedType = when {
            type?.lowercase() in listOf("veg", "vegetarian") -> "Veg"
            type?.lowercase() in listOf("non-veg", "nonveg") -> "Non-Veg"
            else -> type
        }

        val results = repository.searchMenu(category, normalizedType, maxPrice)
        lastResponse = AgentResponse.MenuResults(results, "Search Results")

        if (results.isEmpty()) {
            return "No items found matching your criteria"
        }

        return buildString {
            appendLine("🔍 Found ${results.size} items:")
            results.take(5).forEach {
                appendLine("  • ${it.name} - ₹${it.price}")
            }
        }
    }

    @Tool(
        name = "get_full_menu",
        description = "Display the complete restaurant menu with all available items"
    )
    fun getFullMenu(): String {
        Log.d("ADK_TOOLS", "get_full_menu called")

        val items = repository.getMenuItems()
        lastResponse = AgentResponse.MenuResults(items, "Full Menu")

        if (items.isEmpty()) {
            return "Menu is currently empty"
        }

        return buildString {
            appendLine("📋 Full Menu (${items.size} items):")
            items.forEach {
                appendLine("  • ${it.name} (${it.type}) - ₹${it.price}")
            }
        }
    }

    @Tool(
        name = "get_recommendations",
        description = "Get personalized food recommendations based on criteria"
    )
    fun getRecommendations(criteria: String? = "popular"): String {
        Log.d("ADK_TOOLS", "get_recommendations: $criteria")

        val all = repository.getMenuItems()
        val items = when (criteria?.lowercase()) {
            "spicy" -> all.filter { it.name.contains("Masala", ignoreCase = true) }
            "veg" -> all.filter { it.type == "Veg" }
            else -> all.take(5)
        }

        lastResponse = AgentResponse.Recommendations(items)
        return "⭐ Recommended: ${items.joinToString(", ") { it.name }}"
    }

    @Tool(
        name = "book_table_step",
        description = "Multi-step table booking: ask for people count, then time, then confirm"
    )
    fun bookTableStep(
        step: String,
        numberOfPeople: String? = null,
        bookingTime: String? = null
    ): String {
        Log.d("ADK_AGENT", "TOOL: bookTableStep - step=$step, people=$numberOfPeople, time=$bookingTime")

        return when (step.lowercase()) {
            "ask_people" -> {
                Log.d("ADK_AGENT", "TOOL: Booking Step - ask_people")
                bookingState.clear()
                lastResponse = AgentResponse.BookingRequest("ask_people", "")
                "How many people will be dining?"
            }
            "ask_time" -> {
                Log.d("ADK_AGENT", "TOOL: Booking Step - ask_time")
                if (numberOfPeople.isNullOrBlank()) return "Please provide number of people"
                bookingState["numberOfPeople"] = numberOfPeople
                lastResponse = AgentResponse.BookingRequest("ask_time", "")
                "What time would you like to book?"
            }
            "complete" -> {
                Log.d("ADK_AGENT", "TOOL: Booking Step - complete")
                if (numberOfPeople.isNullOrBlank() || bookingTime.isNullOrBlank()) {
                    Log.d("ADK_AGENT", "TOOL: Booking failed - missing details")
                    return "Missing booking details"
                }
                val booking = com.example.a2ui_sample.domain.model.TableBooking(
                    numberOfPeople = numberOfPeople.toIntOrNull() ?: 1,
                    bookingTime = bookingTime
                )
                repository.addBooking(booking)
                lastResponse = AgentResponse.BookingConfirmation(booking)
                "✅ Table booked! ID: ${booking.bookingId} for ${booking.numberOfPeople} at ${booking.bookingTime}"
            }
            else -> {
                Log.d("ADK_AGENT", "TOOL: Booking Step - invalid step: $step")
                "Invalid booking step"
            }
        }
    }

    @Tool(
        name = "book_table",
        description = "Book a table IMMEDIATELY in a single call when the user's message already contains BOTH the number of people AND the time (e.g. 'book table for 5 members at 4pm'). Do NOT use this if either value is missing."
    )
    fun bookTable(
        numberOfPeople: Int,
        bookingTime: String
    ): String {
        Log.d("ADK_AGENT", "TOOL: bookTable - people=$numberOfPeople, time=$bookingTime")
        val booking = com.example.a2ui_sample.domain.model.TableBooking(
            numberOfPeople = numberOfPeople,
            bookingTime = bookingTime
        )
        repository.addBooking(booking)
        lastResponse = AgentResponse.BookingConfirmation(booking)
        return "✅ Table booked! ID: ${booking.bookingId} for ${booking.numberOfPeople} at ${booking.bookingTime}"
    }

    @Tool(
        name = "checkout_cart",
        description = "Place an order using everything currently in the cart (checkout). Use when the user says 'checkout', 'place my order', or 'confirm order'."
    )
    fun checkoutCart(): String {
        Log.d("ADK_AGENT", "TOOL: checkoutCart")
        val order = repository.placeOrder()
        if (order == null) {
            lastResponse = AgentResponse.Error("Your cart is empty. Add items before checking out.")
            return "Cart is empty. Nothing to check out."
        }
        lastResponse = AgentResponse.OrderPlaced(order)
        return "✅ Order placed! Order ID: ${order.orderId}. Total: ₹${order.totalAmount}."
    }

    @Tool(
        name = "track_order_status",
        description = "Track the delivery status of an order"
    )
    fun trackOrderStatus(orderId: String): String {
        Log.d("ADK_TOOLS", "track_order_status: $orderId")
        return "📍 Order #$orderId - Status: Out for delivery (ETA 30 mins)"
    }

    @Tool(
        name = "submit_feedback",
        description = "Submit customer feedback and rating for an order"
    )
    fun submitFeedback(
        orderId: String,
        rating: Int,
        comment: String? = null
    ): String {
        Log.d("ADK_TOOLS", "submit_feedback: orderId=$orderId, rating=$rating")
        if (rating !in 1..5) return "Rating must be between 1-5"
        return "✅ Thank you for rating us $rating stars! $comment"
    }
}
