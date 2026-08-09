package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.*
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import org.a2ui.compose.rendering.A2UIRenderer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

private const val TAG = "A2UI_FLOW"

/**
 * ADKRestaurantMasterAgent
 * Acts as the Orchestrator for the Restaurant Master Agent flow.
 * 1. Decides if request is INTENT or TOOL_WORKFLOW via GeminiProvider.
 * 2. Executes each task using Specialist Agents.
 * 3. Builds A2UI responses.
 */
@Singleton
class ADKRestaurantMasterAgent @Inject constructor(
    private val menuRepository: MenuRepository,
    private val feedbackRepository: FeedbackRepository,
    private val reservationRepository: ReservationRepository,
    private val orderRepository: OrderRepository,
    private val deliveryRepository: DeliveryRepository,
    private val memoryManager: ConversationMemoryManager,
    private val restaurantAgent: RestaurantAgent
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }
    
    private val menuAgent by lazy { MenuAgent(menuRepository) }
    private val cartAgent by lazy { CartAgent(menuRepository) }
    private val bookingAgent by lazy { BookingAgent(reservationRepository) }
    private val deliveryAgent by lazy { DeliveryAgent(orderRepository, deliveryRepository, menuRepository) }
    private val feedbackAgent by lazy { FeedbackAgent(feedbackRepository) }

    /**
     * Process query with conversational memory (NEW - AI-POWERED)
     * Handles multi-turn conversations and contextual references
     */
    suspend fun processQueryWithMemory(
        userMessage: String,
        chatHistory: List<ChatMessageEntity>,
        onProgress: (String) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🧠 Master Agent START (with memory): '$userMessage'")
        Log.d(TAG, "🧠 Context: ${chatHistory.size} messages in history")
        onProgress("🤖 Understanding with context...")

        val decision = restaurantAgent.analyzeQueryWithContext(userMessage, chatHistory)


        Log.d(TAG, "Decision: ${decision.mode} (Tasks: ${decision.tasks?.size ?: 0})")

        val finalMessages = mutableListOf<String>()

        if (decision.mode == "INTENT" && decision.intent != null) {
            onProgress("⚙️ Processing ${decision.intent}...")
            val response = executeSingleIntent(decision)
            
            val uniqueId = "surf_${System.currentTimeMillis()}_0"
            val fragments = responseBuilder.buildWithId(response, uniqueId)
            
            Log.d(TAG, "📦 INTENT Fragments: ${fragments.size} items")
            fragments.forEachIndexed { fIndex, fragment ->
                Log.d(TAG, "   [Fragment $fIndex]: $fragment")
            }

            // Join fragments with newlines so they are treated as one logical UI update
            val payload = fragments.joinToString("\n")
            finalMessages.add(payload)
            Log.d(TAG, "A2UI_FLOW: Added INTENT payload to finalMessages")
            
            // Persist important data to memory
            updateMemory(decision, response)
            Log.d(TAG, "🧠 Context resolved: LAST_INTENT=${decision.intent}")
        } else if (decision.mode == "TOOL_WORKFLOW" && decision.tasks != null) {
            Log.d(TAG, "📋 MULTITASK: ${decision.tasks.size} tasks planned")
            onProgress("📋 Planning ${decision.tasks.size} tasks...")
            
            decision.tasks.forEachIndexed { index, task ->
                Log.d(TAG, "⚙️ TASK ${index + 1}/${decision.tasks.size}: ${task.tool}")
                onProgress("⚙️ Task ${index + 1}: ${task.tool}")
                
                val taskStart = System.currentTimeMillis()
                val response = executeTask(task)
                val taskDuration = System.currentTimeMillis() - taskStart
                
                val uniqueId = "surf_${System.currentTimeMillis()}_$index"
                val fragments = responseBuilder.buildWithId(response, uniqueId)
                
                Log.d(TAG, "📦 TASK ${index + 1} Fragments: ${fragments.size} items")
                fragments.forEachIndexed { fIndex, fragment ->
                    Log.d(TAG, "   [Fragment $fIndex]: $fragment")
                }

                // Join fragments with newlines per task
                val payload = fragments.joinToString("\n")
                finalMessages.add(payload)
                
                Log.d(TAG, "✅ TASK ${index + 1} complete (${taskDuration}ms)")
            }
            Log.d(TAG, "🎉 MULTITASK complete: ${decision.tasks.size} tasks executed")
            onProgress("✅ All tasks complete!")
        }

        return@withContext finalMessages
    }

    private suspend fun updateMemory(intent: IntentResult, response: AgentResponse) {
        memoryManager.save(ConversationMemoryManager.LAST_INTENT, intent.intent)
        
        when (response) {
            is AgentResponse.OrderPlaced -> {
                memoryManager.save(ConversationMemoryManager.LAST_ORDER_ID, response.order.id.value)
                memoryManager.save(ConversationMemoryManager.LAST_ORDER, response.order)
            }
            is AgentResponse.BookingConfirmation -> {
                memoryManager.save(ConversationMemoryManager.LAST_BOOKING_ID, response.booking.id)
                memoryManager.save(ConversationMemoryManager.LAST_BOOKING, response.booking)
            }
            is AgentResponse.MenuResults -> {
                memoryManager.save(ConversationMemoryManager.LAST_MENU_RESULTS, response.items)
            }
            is AgentResponse.Recommendations -> {
                memoryManager.save(ConversationMemoryManager.LAST_RECOMMENDED_ITEMS, response.items)
            }
            else -> {}
        }
    }
    
    suspend fun processQuery(
        userMessage: String, 
        history: List<String> = emptyList(),
        onProgress: (String) -> Unit = {}
    ): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Master Agent START: '$userMessage'")
        onProgress("🤖 Thinking...")

        val decision = restaurantAgent.analyzeIntent(userMessage)
        Log.d(TAG, "Decision: ${decision.mode} (Tasks: ${decision.tasks?.size ?: 0})")

        val finalMessages = mutableListOf<String>()

        if (decision.mode == "INTENT" && decision.intent != null) {
            onProgress("⚙️ Processing ${decision.intent}...")
            val response = executeSingleIntent(decision)
            val uniqueId = "surf_${System.currentTimeMillis()}_0"
            // Join fragments with newlines (JSONL) so they are treated as one logical UI update
            val payload = responseBuilder.buildWithId(response, uniqueId).joinToString("\n")
            finalMessages.add(payload)
        } else if (decision.mode == "TOOL_WORKFLOW" && decision.tasks != null) {
            Log.i(TAG, "📋 MULTITASK: ${decision.tasks.size} tasks queued")
            decision.tasks.forEachIndexed { index, task ->
                Log.i(TAG, "⚙️ TASK ${index + 1}/${decision.tasks.size}: ${task.tool} START")
                onProgress("⚙️ Executing step ${index + 1}: ${task.tool}...")
                val response = executeTask(task)
                val uniqueId = "surf_${System.currentTimeMillis()}_$index"
                // Join fragments with newlines (JSONL)
                val payload = responseBuilder.buildWithId(response, uniqueId).joinToString("\n")
                finalMessages.add(payload)
                Log.i(TAG, "✅ TASK ${index + 1}/${decision.tasks.size}: ${task.tool} COMPLETE (${response::class.simpleName})")
            }
            Log.i(TAG, "🎉 MULTITASK: All ${decision.tasks.size} tasks completed successfully")
        } else {
            finalMessages.addAll(responseBuilder.buildWithId(AgentResponse.Message("I'm not sure how to handle that."), "surf_error"))
        }

        return@withContext finalMessages
    }

    private suspend fun executeSingleIntent(intentResult: IntentResult): AgentResponse {
        val mappedIntent = mapToolToUserIntent(intentResult.intent)
        
        val entities = mutableMapOf<String, Any>()
        intentResult.itemName?.let { entities["food_item"] = it }
        intentResult.category?.let { entities["category"] = it }
        intentResult.diet?.let { entities["diet"] = it }
        intentResult.occasion?.let { entities["category"] = (entities["category"] as? String ?: "") + " " + it }
        intentResult.priceLimit?.let { entities["price_limit"] = it }
        intentResult.quantity?.let { entities["quantity"] = it }
        
        // Add Booking Entities
        intentResult.peopleCount?.let { entities["people_count"] = it }
        intentResult.date?.let { entities["date"] = it }
        intentResult.time?.let { entities["time"] = it }
        intentResult.target?.let { entities["target"] = it }
        intentResult.rating?.let { entities["rating"] = it }
        intentResult.comment?.let { entities["comment"] = it }
        
        Log.d(TAG, "🎯 INTENT: ${intentResult.intent} → ${mappedIntent.name}")
        Log.d(TAG, "📦 ENTITIES: ${entities.entries.joinToString { "${it.key}=${it.value}" }}")
        
        val result = routeToAgent(mappedIntent, entities)
        Log.d(TAG, "✅ INTENT RESULT: ${result::class.simpleName}")
        
        return result
    }

    private suspend fun executeTask(task: MasterAgentTask): AgentResponse {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🔴 TOOL EXEC: ${task.tool} | Params: itemName=${task.itemName}, qty=${task.quantity}, date=${task.date}, time=${task.time}")
        
        val mappedIntent = mapToolToUserIntent(task.tool)
        val entities = mutableMapOf<String, Any>()
        
        // Smart mapping based on tool context
        if (mappedIntent == UserIntent.MENU_SEARCH || mappedIntent == UserIntent.MENU_RECOMMEND) {
            task.itemName?.let { entities["category"] = it }
            task.category?.let { entities["category"] = it }
            task.diet?.let { entities["diet"] = it }
            task.occasion?.let { entities["category"] = (entities["category"] as? String ?: "") + " " + it }
            task.priceLimit?.let { entities["price_limit"] = it }
        } else {
            task.itemName?.let { entities["food_item"] = it }
        }

        task.quantity?.let { entities["quantity"] = it }
        task.date?.let { entities["date"] = it }
        task.time?.let { entities["time"] = it }
        task.peopleCount?.let { entities["people_count"] = it } // Use underscored key for specialist
        task.target?.let { entities["target"] = it }
        task.rating?.let { entities["rating"] = it }
        task.comment?.let { entities["comment"] = it }
        
        val result = routeToAgent(mappedIntent, entities)
        val duration = System.currentTimeMillis() - startTime
        
        Log.d(TAG, "✅ TOOL COMPLETE: ${task.tool} | Duration: ${duration}ms | Result: ${result::class.simpleName}")
        
        return result
    }

    private suspend fun routeToAgent(intent: UserIntent, entities: Map<String, Any>): AgentResponse {
        // Context Resolution: If intent is repeat/cancel/track without ID, fetch from memory
        val finalEntities = entities.toMutableMap()
        if (intent == UserIntent.ORDER_TRACKING || intent == UserIntent.ORDER_REPEAT || intent == UserIntent.ORDER_CANCEL) {
             if (!entities.containsKey("order_id")) {
                  memoryManager.get(ConversationMemoryManager.LAST_ORDER_ID, String::class.java)?.let {
                       Log.d(TAG, "🧠 RESOLVED: Using last order ID from memory: $it")
                       finalEntities["order_id"] = it
                  }
             }
        }
        
        if (intent == UserIntent.BOOKING_CANCEL || intent == UserIntent.BOOKING_MODIFY) {
             if (!entities.containsKey("booking_id")) {
                  memoryManager.get(ConversationMemoryManager.LAST_BOOKING_ID, String::class.java)?.let {
                       Log.d(TAG, "🧠 RESOLVED: Using last booking ID from memory: $it")
                       finalEntities["booking_id"] = it
                  }
             }
        }

        val wrapper = IntentResultWrapper(intent, finalEntities)

        // Special handling for personalized recommendations
        if (intent == UserIntent.MENU_RECOMMEND) {
            Log.d(TAG, "🎯 Checking for personalized recommendations...")
            
            // Check if user has order history (3+ orders = personalized)
            val orders = orderRepository.getAllOrders().first()
            
            if (orders.size >= 3) {
                Log.d(TAG, "✨ User has ${orders.size} orders - building profile for personalized recs")
                
                try {
                    // Build user profile from history
                    val bookings = reservationRepository.getUpcomingReservations(
                        com.example.a2ui_sample.domain.valueobjects.CustomerId("guest")
                    ).first()
                    val feedback = feedbackRepository.getFeedbackFlow().first()
                    
                    val profileBuilder = UserProfileBuilder(menuRepository)
                    val profile = profileBuilder.buildProfile(orders, bookings, feedback)
                    
                    Log.d(TAG, "📊 Profile: favorites=${profile.favoriteItems.size}, " +
                            "avgBudget=${profile.averageBudget}, diet=${profile.preferredDiet}")
                    
                    // Get AI-powered personalized recommendations
                    return menuAgent.getPersonalizedRecommendations(profile, "")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error building profile, falling back to generic", e)
                    // Fall back to generic recommendations
                    return menuAgent.execute(wrapper)
                }
            } else {
                Log.d(TAG, "📝 User has ${orders.size} orders - showing generic recommendations")
                // New user or not enough history - show generic
                return menuAgent.execute(wrapper)
            }
        }
        
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
            "UPDATE_CART" -> UserIntent.CART_UPDATE
            "SHOW_CART" -> UserIntent.CART_VIEW
            "CLEAR_CART" -> UserIntent.CART_CLEAR
            "CHECKOUT" -> UserIntent.CHECKOUT
            "BOOK_TABLE" -> UserIntent.BOOKING_CREATE
            "SHOW_BOOKINGS", "BOOKING_HISTORY" -> UserIntent.BOOKING_LIST
            "CANCEL_BOOKING" -> UserIntent.BOOKING_CANCEL
            "MODIFY_BOOKING", "MODIFIY_BOOKING" -> UserIntent.BOOKING_MODIFY // Typo in Gemini prompt
            "TRACK_ORDER", "SHOW_ORDER_STATUS" -> UserIntent.ORDER_TRACKING
            "ORDER_HISTORY" -> UserIntent.ORDER_HISTORY
            "CANCEL_ORDER" -> UserIntent.ORDER_CANCEL
            "REORDER_PREVIOUS" -> UserIntent.ORDER_REPEAT
            "SUBMIT_FEEDBACK" -> UserIntent.FEEDBACK_SUBMIT
            "SHOW_OFFERS" -> UserIntent.OFFER_LIST
            "APPLY_COUPON" -> UserIntent.OFFER_APPLY
            "SHOW_RECOMMENDATIONS" -> UserIntent.MENU_RECOMMEND
            else -> UserIntent.UNKNOWN
        }
    }

    fun buildOrderPlacedResponse(order: Order): String {
        val uniqueId = "surf_${System.currentTimeMillis()}_success"
        val responses = responseBuilder.buildWithId(AgentResponse.OrderPlaced(order), uniqueId)
        
        // Join all fragments (CreateSurface, UpdateComponents, UpdateDataModel) with newlines
        return responses.joinToString("\n")
    }

    suspend fun updateOrderMemory(order: Order) {
        memoryManager.save(ConversationMemoryManager.LAST_ORDER_ID, order.id.value)
        memoryManager.save(ConversationMemoryManager.LAST_ORDER, order)
        memoryManager.save(ConversationMemoryManager.LAST_DISCUSSED_TOPIC, "order_placed")
    }

    fun buildSatisfactionResponse(intent: UserIntent): String? {
        val prompt = ConversationHelper.getSatisfactionPrompt(intent)
        if (prompt.isEmpty()) return null
        
        // Use the new Premium Feedback card
        val uniqueId = "surf_${System.currentTimeMillis()}_feedback"
        val responses = responseBuilder.buildWithId(
            AgentResponse.FeedbackRequest("", prompt.substringBefore("😊").trim() + " 😊"), 
            uniqueId
        )
        return responses.joinToString("\n")
    }

    fun buildPremiumFeedbackResponse(order: Order): String {
        val uniqueId = "surf_${System.currentTimeMillis()}_feedback"
        val prompt = "How was your experience with order ${order.id.value}?"
        val responses = responseBuilder.buildWithId(
            AgentResponse.FeedbackRequest(order.id.value, prompt),
            uniqueId
        )
        return responses.joinToString("\n")
    }
}
