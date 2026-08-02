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
        instruction = Instruction("You are a Menu Specialist. Help users find food. Use get_full_menu, search_menu, or get_recommendations based on the query. Always use a tool."),
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
