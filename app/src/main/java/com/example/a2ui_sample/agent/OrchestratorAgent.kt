package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.repository.*
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.defineFunction
import com.google.ai.client.generativeai.type.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ADK_ORCHESTRATOR"

/**
 * OrchestratorAgent
 * Implementation of a proper ADK-based agent using Gemini's native Tool Calling.
 */
@Singleton
class OrchestratorAgent @Inject constructor(
    private val menuRepository: MenuRepository,
    private val reservationRepository: ReservationRepository,
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository,
    private val feedbackRepository: FeedbackRepository,
    private val memoryManager: ConversationMemoryManager
) {
    private val responseBuilder = A2UIResponseBuilder()
    
    private val restaurantTools = RestaurantTools(
        menuRepository,
        reservationRepository,
        orderRepository,
        deliveryRepository,
        feedbackRepository
    )

    private val tools = Tool(
        listOf(
            defineFunction(
                name = "search_menu",
                description = "Search for food items by category, diet (veg/non-veg), or price limit.",
                parameters = listOf(
                    Schema.str("category", "Category like Pizza, Pasta"),
                    Schema.str("diet", "veg or non-veg"),
                    Schema.int("priceLimit", "Maximum price per person"),
                    Schema.int("peopleCount", "Number of people")
                )
            ),
            defineFunction(
                name = "add_item_to_cart",
                description = "Add a specific food item to the cart.",
                parameters = listOf(
                    Schema.str("itemName", "Exact name of the item from the menu")
                )
            ),
            defineFunction(
                name = "manage_cart",
                description = "View, clear, or remove items from the cart.",
                parameters = listOf(
                    Schema.str("action", "VIEW, CLEAR, or REMOVE"),
                    Schema.str("itemName", "Item name to remove (only for REMOVE action)")
                )
            ),
            defineFunction(
                name = "checkout",
                description = "Proceed to checkout and show order summary."
            ),
            defineFunction(
                name = "book_table",
                description = "Book a table at the restaurant.",
                parameters = listOf(
                    Schema.str("date", "Date in YYYY-MM-DD or 'today'/'tomorrow'"),
                    Schema.str("time", "Time like '7:00 PM'"),
                    Schema.int("peopleCount", "Number of guests")
                )
            ),
            defineFunction(
                name = "track_order",
                description = "Track the status of an order.",
                parameters = listOf(
                    Schema.str("orderId", "Order ID to track")
                )
            ),
            defineFunction(
                name = "submit_feedback",
                description = "Submit a rating and comment for an order.",
                parameters = listOf(
                    Schema.int("rating", "Rating from 1 to 5"),
                    Schema.str("comment", "Feedback comment"),
                    Schema.str("orderId", "Optional Order ID")
                )
            )
        )
    )

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        tools = listOf(tools),
        systemInstruction = content {
            text("""
                You are a Restaurant Orchestrator Assistant. 
                Your job is to help users manage their food orders and table bookings.
                Use the provided tools for:
                1. Searching the menu (search_menu)
                2. Adding items to cart (add_item_to_cart)
                3. Cart management (manage_cart)
                4. Checkout (checkout)
                5. Table reservations (book_table)
                6. Order tracking (track_order)
                
                Always confirm with the user after an action. 
                If multiple steps are needed (e.g., 'add pizza and checkout'), call tools sequentially.
            """.trimIndent())
        }
    )

    suspend fun processQuery(
        userMessage: String,
        chatHistory: List<ChatMessageEntity>
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            val history = chatHistory.takeLast(10).map { msg ->
                content(role = if (msg.isFromUser) "user" else "model") { text(msg.text) }
            }

            val chat = model.startChat(history = history)
            var response = chat.sendMessage(userMessage)
            val finalMessages = mutableListOf<String>()

            // ADK/Gemini Tool Execution Loop
            while (response.functionCalls.isNotEmpty()) {
                for (functionCall in response.functionCalls) {
                    Log.d(TAG, "ADK: Executing tool '${functionCall.name}' with args: ${functionCall.args}")
                    
                    val result = handleToolCall(functionCall.name, functionCall.args)
                    
                    // Capture A2UI response if generated by the tool
                    restaurantTools.lastResponse?.let { agentResponse ->
                        val uniqueId = "surf_${System.currentTimeMillis()}"
                        val payload = responseBuilder.buildWithId(agentResponse, uniqueId).joinToString("\n")
                        finalMessages.add(payload)
                        restaurantTools.lastResponse = null
                    }

                    // Feed tool result back to Gemini
                    response = chat.sendMessage(content("user") { text(result) })
                }
            }

            // Fallback: If model has a final textual response and no A2UI cards were generated
            response.text?.let { text ->
                if (text.isNotBlank() && finalMessages.isEmpty()) {
                    val uniqueId = "msg_${System.currentTimeMillis()}"
                    val payload = responseBuilder.buildWithId(AgentResponse.Message(text), uniqueId).joinToString("\n")
                    finalMessages.add(payload)
                }
            }
            
            finalMessages
        } catch (e: Exception) {
            Log.e(TAG, "ADK Orchestrator Error: ${e.message}", e)
            val errorPayload = responseBuilder.buildWithId(AgentResponse.Error("Something went wrong: ${e.message}"), "err").joinToString("\n")
            listOf(errorPayload)
        }
    }

    private fun handleToolCall(name: String, args: Map<String, Any?>): String {
        return when (name) {
            "search_menu" -> restaurantTools.searchMenu(
                args["category"] as? String, 
                args["diet"] as? String, 
                args["priceLimit"]?.toString()?.toIntOrNull(), 
                args["peopleCount"]?.toString()?.toIntOrNull() ?: 1
            )
            "add_item_to_cart" -> restaurantTools.addItemToCart(args["itemName"] as? String ?: "")
            "manage_cart" -> restaurantTools.manageCart(
                args["action"] as? String ?: "VIEW", 
                args["itemName"] as? String
            )
            "checkout" -> restaurantTools.checkout()
            "book_table" -> restaurantTools.bookTable(
                args["date"] as? String ?: "today", 
                args["time"] as? String ?: "", 
                args["peopleCount"]?.toString()?.toIntOrNull() ?: 2
            )
            "track_order" -> restaurantTools.trackOrder(args["orderId"] as? String)
            "submit_feedback" -> restaurantTools.submitFeedback(
                args["rating"]?.toString()?.toIntOrNull() ?: 5,
                args["comment"] as? String ?: "",
                args["orderId"] as? String
            )
            else -> "Unknown tool called: $name"
        }
    }
    
    // Maintain references for external checks
    fun getMenuRepository() = menuRepository
    fun getMemoryManager() = memoryManager
}
