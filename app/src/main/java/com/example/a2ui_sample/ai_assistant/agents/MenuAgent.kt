package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantMenuTools
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
class MenuAgent @Inject constructor(
    private val menuTools: AssistantMenuTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "MenuAssistant",
        model = geminiModel,
        instruction = Instruction.invoke(MENU_PROMPT),
        tools = menuTools.generatedTools(),
        maxSteps = 1 // Prevent loops
    )

    fun executeCommand(command: String, session: Session): Flow<Event> {
        session.events.add(Event(author = Role.USER, content = Content.fromText(Role.USER, "[COMMAND] $command")))
        return adkAgent.runAsync(InvocationContext(agent = adkAgent, session = session))
    }

    companion object {
        private const val MENU_PROMPT = """
            You are the Menu Specialist. Your goal is to provide precise menu information.
            
            RULES (must follow exactly):
            1) ALWAYS call a tool. Do NOT return plain text.
            2) For general browsing ("show menu", "what can I order", "full menu"), call get_full_menu().
            3) For filters ("veg", "under 250", "spicy"), call assistant_search_menu(category=null, diet="veg").
            4) For specific item details, call get_menu_details(itemName="<extracted>").
            5) For recommendations ("best items", "suggestions"), call get_recommendations().
            6) If the user speaks Hindi/Urdu, translate the intent but keep item names accurate.

            Examples:
             User: Show me the full menu.
             Tool: get_full_menu()

             User: मेनू दिखाओ
             Tool: get_full_menu()
        """
    }
}
