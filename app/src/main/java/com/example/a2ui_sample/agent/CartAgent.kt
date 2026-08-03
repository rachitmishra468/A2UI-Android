package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.example.a2ui_sample.domain.valueobjects.OrderId
import com.example.a2ui_sample.domain.valueobjects.Price
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import kotlin.time.ExperimentalTime

private const val TAG = "CART_AGENT"

class CartAgent(
    private val repository: MenuRepository,
    private val model: Gemini
) {
    private val tools = CartAgentTools(repository)

    @OptIn(ExperimentalTime::class)
    private val adkAgent: LlmAgent by lazy {
        LlmAgent(
            name = "CartAgent",
            model = model,
            instruction = Instruction(
                """
                You are an expert Cart Management Specialist for a premium restaurant.
                
                # YOUR CAPABILITIES
                - Add items to cart by name or fuzzy match (e.g., "burger" matches "Veg Burger")
                - Remove specific items from cart
                - Update quantities: "add 2 more", "reduce to 1", "make it 3 pizzas"
                - View complete cart with items, quantities, and total price
                - Clear entire cart: "empty cart", "start fresh", "remove everything"
                - Apply discount coupons/promo codes: "apply WELCOME50", "use discount FIRST20"
                - Remove coupons: "remove coupon", "cancel discount"
                - Checkout and place orders
                - Track order status: "where is my order?", "order status", "track order #1234"
                - Reorder from history: "order again", "repeat last order", "same as before"
                
                # MULTILINGUAL SUPPORT
                Understand Hindi, Hinglish, Urdu, English:
                - "mera cart dikhao" → View cart
                - "2 pizza add karo" → Add 2 pizzas
                - "checkout karo" → Proceed to checkout
                - "dobara order karo" → Reorder previous
                
                # CART OPERATIONS GUIDE
                - Adding: Match item names intelligently. "burger" should find "Veg Burger" or ask which burger.
                - Quantities: Parse "2 burgers", "add 3 more", "increase to 5", "make it 2"
                - Removing: "remove burger", "delete dosa", "take out pizza"
                - Clearing: "clear cart", "empty", "start over", "remove all"
                - Checkout: Verify cart is not empty before proceeding
                
                # ERROR HANDLING
                - Empty cart checkout: "Your cart is empty. Would you like to browse the menu?"
                - Item not found: "I couldn't find 'samosa' in our menu. Did you mean something else?"
                - Ambiguous items: "We have Veg Burger and Chicken Burger. Which one?"
                - Invalid coupon: "That coupon code isn't valid. Try WELCOME10 or SAVE20."
                
                # RESPONSE QUALITY
                - Confirm every action: "Added 2 Veg Burgers to your cart. Total: ₹298"
                - Show cart summary after changes
                - Proactive next steps: "Ready to checkout?" / "Anything else?"
                - Handle edge cases gracefully
                
                # CRITICAL
                You must interpret user intent and perform the appropriate cart operation.
                Match the user's language in your response.
                Be helpful, clear, and efficient.
                """.trimIndent()
            ),
            tools = emptyList(),
            maxSteps = 2
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun process(query: String, session: Session): AgentResponse {
        Log.d(TAG, "Cart processing: $query")
        tools.reset()

        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, query)
        )

        var finalMessage = ""
        adkAgent.runAsync(context).collect { event ->
            val text = event.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank() && event.author == "CartAgent") {
                finalMessage = text
            }
        }

        return tools.getLastResponse() ?: AgentResponse.Message(finalMessage)
    }
}

class CartAgentTools(private val repository: MenuRepository) {
    private var lastResp: AgentResponse? = null

    fun getLastResponse(): AgentResponse? = lastResp
    fun reset() { lastResp = null }

    fun addToCart(itemName: String): String {
        val menu = repository.getMenuItems()
        val item = menu.find { it.name.contains(itemName, ignoreCase = true) }
            ?: return "I couldn't find '$itemName' in our menu."
        
        repository.addToCart(item.id)
        lastResp = AgentResponse.CartUpdate(item, repository.getCart().sumOf { it.quantity })
        return "Added ${item.name} to your cart."
    }

    fun viewCart(): String {
        val cartItems = repository.getCart()
        lastResp = AgentResponse.CartView(cartItems, repository.getCartTotal())
        return "Here is your cart with ${cartItems.size} items."
    }

    fun checkout(): String {
        val cartItems = repository.getCart()
        if (cartItems.isEmpty()) return "Your cart is empty."
        
        val subtotal = repository.getCartTotal()
        val tax = (subtotal * 0.05).toInt()
        val total = subtotal + tax
        
        val order = Order(
            id = OrderId("ORD-${System.currentTimeMillis() % 10000}"),
            items = cartItems.map { OrderItem(it.menuItem.id, it.menuItem.name, it.quantity, it.menuItem.price) },
            subtotal = Price(subtotal),
            tax = Price(tax),
            totalAmount = Price(total)
        )
        
        repository.placeOrder(order)
        lastResp = AgentResponse.OrderConfirmation(order)
        return "Order placed successfully. ID: ${order.id.value}"
    }
}
