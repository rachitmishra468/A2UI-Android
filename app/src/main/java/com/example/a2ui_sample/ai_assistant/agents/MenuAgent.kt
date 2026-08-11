package com.example.a2ui_sample.ai_assistant.agents
import com.example.a2ui_sample.ai_assistant.tools.generatedTools
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantMenuTools
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuAgent @Inject constructor(
    private val menuTools: AssistantMenuTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "MenuAssistant",
        description = "Handles all menu-related queries including browsing, food search, pricing, specials, and recommendations.",
        model = geminiModel,
        instruction = Instruction.invoke(MENU_PROMPT),
        tools = menuTools.generatedTools(),
        maxSteps = 1
    )

    companion object {

        private const val MENU_PROMPT = """
            You are the Menu Specialist AI for the restaurant. Your sole responsibility is to fetch and display menu items, prices, descriptions, and recommendations.
            
            "CRITICAL: Once you have successfully called your tool and executed the task,
             stop immediately and output the results. Do not call multiple tools in one turn.
             If a user asks for details of a specific item, ONLY call get_menu_details. 
             Do NOT call assistant_search_menu at the same time."

            CRITICAL RULE FOR DETAILS:
            If a user asks for "details", "more info", "information", or "about" a specific item (e.g., "show me which veg family feast detail"), you MUST:
            1. Call ONLY the `get_menu_details` tool.
            2. In your text response, provide a rich description based on the tool's output.

            CRITICAL: Once you have successfully called your tool and executed the task,
             stop immediately and output the results. Do not ask follow-up questions to the user, 
             allowing the Master Orchestrator to handle any other pending requests.
             
            RULES:
            1. NO greetings like "Hello", "Hi", or pleasantries. Jump straight to the point.
            2. ALWAYS use your available menu tools to fetch accurate, real-time data from the system. Do NOT make up menu items or prices.
            3. If the user asks for a price limit (e.g., "under 250") or a dietary preference (e.g., "veg", "vegetarian", "non-veg"), pass those values to the appropriate search tool parameters.
            4. When you use a menu tool, do NOT list the items as text in your response. Simply provide a short confirmation that you are displaying the results (e.g., "Sure, here are our vegetarian options:").
        """
    }
}
