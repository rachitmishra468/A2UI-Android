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

class MenuAgent(private val repository: MenuRepository, private val model: Gemini) {
    private val tools = MenuAgentTools(repository)

    @OptIn(ExperimentalTime::class)
    private val adkAgent = LlmAgent(
        name = "MenuAgent",
        model = model,
        instruction = Instruction("""
            You are an elite Menu Specialist for a premium restaurant.
            
            # YOUR CAPABILITIES
            - Show full menu, categories, or filtered items
            - Search by: name, category (burger, pizza, dosa, etc.), type (veg/non-veg), price range, dietary preferences
            - Provide recommendations: popular items, trending dishes, best sellers, new arrivals, chef specials
            - Handle specialized requests: family meals, kids meals, drinks, desserts, combos, starters
            - Apply dietary filters: vegetarian, non-vegetarian, Jain (no onion/garlic), vegan, gluten-free, low-calorie, spicy levels
            - Show nutritional information: calories, ingredients, allergens, preparation time
            - Budget-aware searches: "under ₹200", "cheapest", "expensive options", "combos under ₹500"
            
            # MULTILINGUAL SUPPORT
            Understand Hindi, Hinglish, Urdu, English:
            - "mujhe spicy chahiye" → Show spicy items
            - "veg items dikhao" → Show vegetarian menu
            - "kya hai aaj?" → Show today's menu or specials
            - "burger under 200" → Budget-filtered search
            
            # DECISION LOGIC
            - General browsing ("show menu", "what do you have?") → Use get_full_menu
            - Specific search ("find burgers", "veg items under 150") → Use search_menu with filters
            - Recommendations ("popular items", "what's good?", "trending") → Use get_recommendations
            
            # RESPONSE QUALITY
            - Concise and helpful
            - Proactive: "Would you like to add any of these to your cart?"
            - Handle not-found gracefully: "We don't have samosas, but try our Veg Burger or Masala Dosa?"
            
            # CRITICAL
            ALWAYS call a tool. Never respond without using get_full_menu, search_menu, or get_recommendations.
            Match the user's language in your response.
        """.trimIndent()),
        tools = tools.generatedTools(),
        maxSteps = 1
    )

    @OptIn(ExperimentalTime::class)
    suspend fun process(query: String, session: Session): AgentResponse {
        Log.d(TAG, ">>> MenuAgent Processing: $query")
        tools.reset()
        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, query)
        )
        try { 
            adkAgent.runAsync(context).collect { event ->
                Log.v(TAG, "Menu Event: ${event.author}: ${event.content?.parts?.firstOrNull()?.text}")
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Menu Error: ${e.message}")
            return AgentResponse.Error("Menu Error: ${e.message}")
        }
        val response = tools.getLastResponse()
        Log.d(TAG, "<<< MenuAgent Response: ${response?.javaClass?.simpleName}")
        return response ?: AgentResponse.Message("No menu items found.")
    }
}

class MenuAgentTools(private val repository: MenuRepository) {
    private var lastResp: AgentResponse? = null
    fun getLastResponse() = lastResp
    fun reset() { lastResp = null }

    @Tool(name = "get_full_menu", description = "Get all items")
    fun getFullMenu(): String {
        Log.d(TAG, "Tool: get_full_menu called")
        lastResp = AgentResponse.MenuResults(repository.getMenuItems(), "Full Menu")
        return "Showed menu"
    }

    @Tool(name = "search_menu", description = "Search with filters")
    fun searchMenu(category: String? = null, type: String? = null, maxPrice: Int? = null): String {
        Log.d(TAG, "Tool: search_menu called (cat=$category, type=$type, maxPrice=$maxPrice)")
        val results = repository.searchMenu(category, type, maxPrice)
        lastResp = AgentResponse.MenuResults(results, "Search Results")
        return "Found ${results.size} items"
    }

    @Tool(name = "get_recommendations", description = "Get popular items")
    fun getRecommendations(): String {
        Log.d(TAG, "Tool: get_recommendations called")
        lastResp = AgentResponse.Recommendations(repository.getMenuItems().take(3))
        return "Showed recommendations"
    }
}
