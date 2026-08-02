package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.annotations.Tool
import kotlin.time.ExperimentalTime

private const val TAG = "ADK_AGENT"

class CartAgent(private val repository: MenuRepository, private val model: Gemini) {
    private val tools = CartAgentTools(repository)

    @OptIn(ExperimentalTime::class)
    private val adkAgent = LlmAgent(
        name = "CartAgent",
        model = model,
        instruction = Instruction(
            """
            You are a Cart Assistant.
            
            RULES (must follow exactly):
            1) If the user wants to add an item (contains add, order, buy, put), ALWAYS call add_to_cart(itemName="...").
               - Extract the item name and normalize to Title Case.
               - Do NOT call view_cart for add intents.
            2) If the user wants to see their cart, call view_cart().
            3) If the user wants to checkout or place order, call checkout().
            
            Example:
              User: "add pizza" -> add_to_cart(itemName="Pizza")
            """.trimIndent()
        ),
        tools = tools.generatedTools(),
        maxSteps = 1
    )

    @OptIn(ExperimentalTime::class)
    suspend fun process(query: String, session: Session): AgentResponse {
        Log.d(TAG, ">>> CartAgent Processing: $query")
        tools.reset()
        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, query)
        )
        try { 
            adkAgent.runAsync(context).collect { event ->
                Log.v(TAG, "Cart Event: ${event.author}: ${event.content?.parts?.firstOrNull()?.text}")
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Cart Error: ${e.message}")
            return AgentResponse.Error("Cart Error: ${e.message}")
        }
        val response = tools.getLastResponse()
        Log.d(TAG, "<<< CartAgent Response: ${response?.javaClass?.simpleName}")
        return response ?: AgentResponse.Message("Cart operation failed.")
    }
}

class CartAgentTools(private val repository: MenuRepository) {
    private var lastResp: AgentResponse? = null
    fun getLastResponse() = lastResp
    fun reset() { lastResp = null }

    @Tool(name = "add_to_cart", description = "Add item by name")
    fun addToCart(itemName: String): String {
        Log.d(TAG, "Tool: add_to_cart called for: $itemName")
        val item = repository.getMenuItems().firstOrNull { it.name.contains(itemName, ignoreCase = true) }
        return if (item != null) {
            repository.addToCart(item.id)
            lastResp = AgentResponse.CartUpdate(item, repository.getCart().size)
            "Added ${item.name}"
        } else {
            Log.w(TAG, "Tool: add_to_cart FAILED - Item not found: $itemName")
            "Not found"
        }
    }

    @Tool(name = "view_cart", description = "Show cart items")
    fun viewCart(): String {
        Log.d(TAG, "Tool: view_cart called")
        lastResp = AgentResponse.CartView(repository.getCart(), repository.getCartTotal())
        return "Showed cart"
    }

    @Tool(name = "checkout", description = "Place order")
    fun checkout(): String {
        Log.d(TAG, "Tool: checkout called")
        val order = repository.placeOrder()
        lastResp = if (order != null) AgentResponse.OrderPlaced(order) else AgentResponse.Error("Cart empty")
        return "Checkout attempted"
    }
}
