package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.KnowledgeTools
import com.example.a2ui_sample.ai_assistant.tools.generatedTools
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeAgent @Inject constructor(
    private val knowledgeTools: KnowledgeTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "KnowledgeAssistant",
        description = "Handles general questions about the restaurant, policies, delivery, payments, and FAQs using the knowledge base.",
        model = geminiModel,
        tools = knowledgeTools.generatedTools(),
        instruction = Instruction.invoke(KNOWLEDGE_PROMPT),
        maxSteps = 1
    )

    companion object {
        private const val KNOWLEDGE_PROMPT = """
            You are the Knowledge Specialist. Your job is to answer general questions about the restaurant using the provided tools.
            
            "CRITICAL: Once you have successfully called your tool and executed the task,
             stop immediately and output the results. Do not ask follow-up questions."
             
            RULES:
            1. Use the `get_restaurant_guidelines` tool to fetch the latest policies and info.
            2. Answer the user's question based strictly on the information in the markdown file.
            3. Keep your answers concise and professional.
            4. If the information is not in the knowledge base, say you don't have that specific information and offer to connect them to a human manager.
        """
    }
}
