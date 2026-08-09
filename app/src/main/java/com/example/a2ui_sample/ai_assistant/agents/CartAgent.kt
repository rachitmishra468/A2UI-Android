package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantCartTools
import com.example.a2ui_sample.ai_assistant.tools.generatedTools
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.events.Event
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartAgent @Inject constructor(
    private val cartTools: AssistantCartTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "CartAssistant",
        model = geminiModel,
        instruction = Instruction.invoke(CART_PROMPT),
        tools = cartTools.generatedTools(),
        maxSteps = 1 // Prevent loops
    )

    fun executeCommand(command: String, session: Session): Flow<Event> {
        session.events.add(Event(author = Role.USER, content = Content.fromText(Role.USER, "[COMMAND] $command")))
        return adkAgent.runAsync(InvocationContext(agent = adkAgent, session = session))
    }

    companion object {
        private const val CART_PROMPT = """
            You are the Cart Specialist. Your job is to manage the user's shopping cart.
            
            RULES (must follow exactly):
            1) ALWAYS call a tool. Do NOT return plain text.
            2) If adding ("add", "order", "put", "ڈالیں"), call add_to_cart(itemName="<item>", quantity=<num>).
            3) If viewing ("show cart", "what's in my cart"), call view_cart().
            4) If removing ("remove", "delete"), call remove_from_cart(itemName="<item>").
            5) If updating ("change quantity", "make it 3"), call update_cart_quantity(itemName="<item>", quantity=<num>).
            6) If checking out ("checkout", "pay now"), call clear_cart() or appropriate checkout tool.

            Examples:
             User: add 2 Maharaja Chicken burgers
             Tool: add_to_cart(itemName="Maharaja Chicken", quantity=2)

             User: 1 large fries cart mein daalo
             Tool: add_to_cart(itemName="Large Fries", quantity=1)

             User: show my cart
             Tool: view_cart()
        """
    }
}
