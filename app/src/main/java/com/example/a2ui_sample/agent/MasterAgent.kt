package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "A2UI_FLOW"

/**
 * ADKRestaurantMasterAgent
 * Acts as the Orchestrator for the Restaurant Master Agent flow.
 * 1. Decides if request is INTENT or TOOL_WORKFLOW via GeminiProvider.
 * 2. Executes each task using Specialist Agents.
 * 3. Builds A2UI responses.
 */
class ADKRestaurantMasterAgent(
    private val menuRepository: MenuRepository,
    private val feedbackRepository: FeedbackRepository,
    private val reservationRepository: ReservationRepository,
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }
    private val geminiProvider by lazy { GeminiProvider() }
    
    private val menuAgent by lazy { MenuAgent(menuRepository) }
    private val cartAgent by lazy { CartAgent(menuRepository) }
    private val bookingAgent by lazy { BookingAgent(reservationRepository) }
    private val deliveryAgent by lazy { DeliveryAgent(orderRepository, deliveryRepository, menuRepository) }
    private val feedbackAgent by lazy { FeedbackAgent(feedbackRepository) }

    suspend fun processQuery(
        userMessage: String, 
        history: List<String> = emptyList(),
        onProgress: (String) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Master Agent START: '$userMessage'")
        onProgress("🤖 Thinking...")

        val decision = geminiProvider.analyzeQuery(userMessage, history)
        Log.d(TAG, "Decision: ${decision.mode} (Tasks: ${decision.tasks?.size ?: 0})")

        val finalMessages = mutableListOf<String>()

        if (decision.mode == "INTENT" && decision.intent != null) {
            onProgress("⚙️ Processing ${decision.intent}...")
            val response = executeSingleIntent(decision)
            val uniqueId = "surf_${System.currentTimeMillis()}_0"
            // Return fragments as individual messages for better parsing
            finalMessages.addAll(responseBuilder.buildWithId(response, uniqueId))
        } else if (decision.mode == "TOOL_WORKFLOW" && decision.tasks != null) {
            decision.tasks.forEachIndexed { index, task ->
                onProgress("⚙️ Executing step ${index + 1}: ${task.tool}...")
                val response = executeTask(task)
                val uniqueId = "surf_${System.currentTimeMillis()}_$index"
                // Return fragments as individual messages
                finalMessages.addAll(responseBuilder.buildWithId(response, uniqueId))
            }
        } else {
            finalMessages.addAll(responseBuilder.buildWithId(AgentResponse.Message("I'm not sure how to handle that."), "surf_error"))
        }

        return@withContext finalMessages
    }

    private suspend fun executeSingleIntent(intentResult: IntentResult): AgentResponse {
        val mappedIntent = mapToolToUserIntent(intentResult.intent)
        val entities = mutableMapOf<String, Any>()
        intentResult.category?.let { entities["category"] = it }
        intentResult.diet?.let { entities["diet"] = it }
        intentResult.priceLimit?.let { entities["price_limit"] = it }
        return routeToAgent(mappedIntent, entities)
    }

    private suspend fun executeTask(task: MasterAgentTask): AgentResponse {
        val mappedIntent = mapToolToUserIntent(task.tool)
        val entities = mutableMapOf<String, Any>()
        
        // Smart mapping based on tool context
        if (mappedIntent == UserIntent.MENU_SEARCH || mappedIntent == UserIntent.MENU_RECOMMEND) {
            task.itemName?.let { entities["category"] = it }
            task.category?.let { entities["category"] = it }
            task.diet?.let { entities["diet"] = it }
            task.priceLimit?.let { entities["price_limit"] = it }
        } else {
            task.itemName?.let { entities["food_item"] = it }
        }

        task.quantity?.let { entities["quantity"] = it }
        task.date?.let { entities["date"] = it }
        task.time?.let { entities["time"] = it }
        task.peopleCount?.let { entities["peopleCount"] = it }
        task.rating?.let { entities["rating"] = it }
        task.comment?.let { entities["comment"] = it }
        
        return routeToAgent(mappedIntent, entities)
    }

    private suspend fun routeToAgent(intent: UserIntent, entities: Map<String, Any>): AgentResponse {
        val wrapper = IntentResultWrapper(intent, entities)
        return when (intent) {
            UserIntent.MENU_SEARCH, UserIntent.MENU_RECOMMEND -> menuAgent.execute(wrapper)
            UserIntent.CART_VIEW, UserIntent.CART_ADD, UserIntent.CART_REMOVE, UserIntent.CART_UPDATE, UserIntent.CART_CLEAR, UserIntent.CHECKOUT -> cartAgent.execute(wrapper)
            UserIntent.BOOKING_CREATE, UserIntent.BOOKING_CHECK, UserIntent.BOOKING_MODIFY, UserIntent.BOOKING_CANCEL, UserIntent.BOOKING_LIST -> bookingAgent.execute(wrapper)
            UserIntent.ORDER_HISTORY, UserIntent.ORDER_TRACKING, UserIntent.ORDER_REPEAT, UserIntent.ORDER_CANCEL -> deliveryAgent.execute(wrapper)
            UserIntent.FEEDBACK_SUBMIT, UserIntent.FEEDBACK_VIEW, UserIntent.FEEDBACK_UPDATE, UserIntent.FEEDBACK_METRICS -> feedbackAgent.execute(wrapper)
            else -> AgentResponse.Message("Action not supported yet.")
        }
    }

    private fun mapToolToUserIntent(tool: String?): UserIntent {
        if (tool == null) return UserIntent.UNKNOWN
        return when (tool.uppercase()) {
            "SEARCH_MENU", "SHOW_MENU" -> UserIntent.MENU_SEARCH
            "ADD_TO_CART" -> UserIntent.CART_ADD
            "REMOVE_FROM_CART" -> UserIntent.CART_REMOVE
            "SHOW_CART" -> UserIntent.CART_VIEW
            "CLEAR_CART" -> UserIntent.CART_CLEAR
            "CHECKOUT" -> UserIntent.CHECKOUT
            "BOOK_TABLE" -> UserIntent.BOOKING_CREATE
            "CANCEL_BOOKING" -> UserIntent.BOOKING_CANCEL
            "TRACK_ORDER", "SHOW_ORDER_STATUS" -> UserIntent.ORDER_TRACKING
            "ORDER_HISTORY" -> UserIntent.ORDER_HISTORY
            "SUBMIT_FEEDBACK" -> UserIntent.FEEDBACK_SUBMIT
            "SHOW_OFFERS" -> UserIntent.OFFER_LIST
            "REORDER_PREVIOUS" -> UserIntent.ORDER_REPEAT
            "SHOW_RECOMMENDATIONS" -> UserIntent.MENU_RECOMMEND
            else -> UserIntent.UNKNOWN
        }
    }

    fun buildOrderPlacedResponse(order: Order): String {
        val uniqueId = "surf_${System.currentTimeMillis()}_success"
        val responses = responseBuilder.buildWithId(AgentResponse.OrderPlaced(order), uniqueId)
        return responses.last()
    }
}
