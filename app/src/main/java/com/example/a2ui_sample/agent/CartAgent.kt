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
                You manage the shopping cart. 
                - Add items by name.
                - Show the cart contents.
                - Handle checkout.
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
