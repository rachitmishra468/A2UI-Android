package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootRestaurantAgent @Inject constructor(
    private val menuAgent: MenuAgent,
    private val cartAgent: CartAgent,
    private val bookingAgent: BookingAgent,
    private val orderAgent: OrderAgent,
    private val feedbackAgent: FeedbackAgent
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "RestaurantMaster",
        model = geminiModel,
        description = "Router agent that decides which specialist to invoke based on the user query.",
        instruction = Instruction.invoke(ROOT_AGENT_PROMPT),
        subAgents = listOf(
            menuAgent.adkAgent,
            cartAgent.adkAgent,
            bookingAgent.adkAgent,
            orderAgent.adkAgent,
            feedbackAgent.adkAgent
        ),
        maxSteps = 3
    )

    companion object {

        private const val ROOT_AGENT_PROMPT = """
            You are the Master Restaurant Router Agent. Your job is to analyze the user's query and delegate (transfer) the conversation to the most appropriate sub-agent.
            
            CRITICAL: Do not answer the query yourself. Use your sub-agent transfer tools immediately.
            
            Routing Rules:
            - If query is about menu, food, veg, burger, pizza, specials, price, or under ₹ -> Delegate to MenuAssistant.
            - If query is about cart, adding items, removing items, checkout, or quantity -> Delegate to CartAssistant.
            - If query is about booking, tables, or reservations -> Delegate to BookingAssistant.
            - If query is about tracking, order numbers, or delivery status -> Delegate to OrderAssistant.
            - If query is about feedback, ratings, or reviews -> Delegate to FeedbackAssistant.
            - If no clear match -> Delegate to MenuAssistant.
            
            Ensure the sub-agent names match exactly with their registered names.
        """
    }
}
