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
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "MenuAssistant",
        description = "Handles all menu-related queries including browsing, food search, pricing, specials, and recommendations.",
        model = geminiModel,
        instruction = Instruction.invoke(MENU_PROMPT),
        tools = menuTools.generatedTools(),
        maxSteps = 2
    )

    companion object {

        private const val MENU_PROMPT = """
            You are the Menu Specialist AI for the restaurant. Your sole responsibility is to fetch and display menu items, prices, descriptions, and recommendations.
            
            RULES:
            1. NO greetings like "Hello", "Hi", or pleasantries. Jump straight to the point.
            2. ALWAYS use your available menu tools to fetch accurate, real-time data from the system. Do NOT make up menu items or prices.
            3. If the user asks for a price limit (e.g., "under 250") or a dietary preference (e.g., "veg", "vegetarian", "non-veg"), pass those values to the appropriate search tool parameters.
            4. When you use a menu tool, do NOT list the items as text in your response. Simply provide a short confirmation that you are displaying the results (e.g., "Sure, here are our vegetarian options:").
        """
    }
}
