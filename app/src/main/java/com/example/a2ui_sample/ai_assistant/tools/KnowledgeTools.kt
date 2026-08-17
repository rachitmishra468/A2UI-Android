package com.example.a2ui_sample.ai_assistant.tools

import android.content.Context
import android.util.Log
import com.google.adk.kt.annotations.Tool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeTools @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Tool(
        name = "get_restaurant_guidelines",
        description = "Read the restaurant's business rules, delivery policies, payment info, and general FAQs from the knowledge base."
    )
    fun getGuidelines(): Map<String, Any?> {
        Log.d("AssistantFlow", "TOOL: get_restaurant_guidelines() called")
        return try {
            val content = context.assets.open("knowledge.md").bufferedReader().use { it.readText() }
            mapOf(
                "content" to content,
                "success" to true,
                "message" to "Rango guidelines"
            )
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Error reading knowledge.md", e)
            mapOf(
                "message" to "I'm sorry, I couldn't access my knowledge base right now.",
                "success" to false
            )
        }
    }
}
