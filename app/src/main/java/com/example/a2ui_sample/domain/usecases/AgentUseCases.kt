package com.example.a2ui_sample.domain.usecases

import com.example.a2ui_sample.agent.ADKRestaurantMasterAgent
import com.example.a2ui_sample.domain.model.Order
import com.example.a2ui_sample.infrastructure.persistence.entity.ChatMessageEntity
import javax.inject.Inject

/**
 * ProcessRestaurantQueryUseCase
 * Orchestrates the AI processing of a user query.
 */
class ProcessRestaurantQueryUseCase @Inject constructor(
    private val agent: ADKRestaurantMasterAgent
) {
    suspend operator fun invoke(
        query: String,
        history: List<ChatMessageEntity>,
        onStatusUpdate: (String) -> Unit
    ): List<String> {
        return agent.processQueryWithMemory(query, history, onStatusUpdate)
    }
}

/**
 * GetOrderConfirmationUiUseCase
 * Generates the A2UI payload for an order confirmation.
 */
class GetOrderConfirmationUiUseCase @Inject constructor(
    private val agent: ADKRestaurantMasterAgent
) {
    fun execute(order: Order): String {
        return agent.buildOrderPlacedResponse(order)
    }
}

/**
 * GetPremiumFeedbackUiUseCase
 * Generates the premium feedback request UI payload.
 */
class GetPremiumFeedbackUiUseCase @Inject constructor(
    private val agent: ADKRestaurantMasterAgent
) {
    fun execute(order: Order): String {
        return agent.buildPremiumFeedbackResponse(order)
    }
}

/**
 * UpdateAgentOrderMemoryUseCase
 * Syncs the order state with the agent's memory.
 */
class UpdateAgentOrderMemoryUseCase @Inject constructor(
    private val agent: ADKRestaurantMasterAgent
) {
    suspend fun execute(order: Order) {
        agent.updateOrderMemory(order)
    }
}
