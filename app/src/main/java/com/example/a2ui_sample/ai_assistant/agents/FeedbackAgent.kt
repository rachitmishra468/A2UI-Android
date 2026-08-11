package com.example.a2ui_sample.ai_assistant.agents

import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.ai_assistant.tools.AssistantFeedbackTools
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
class FeedbackAgent @Inject constructor(
    private val feedbackTools: AssistantFeedbackTools
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.1-flash-lite", apiKey)

    val adkAgent = LlmAgent(
        name = "FeedbackAssistant",
        description = "Handles all customer feedback, ratings, reviews, complaints, and suggestions regarding food or service.",
        model = geminiModel,
        tools = feedbackTools.generatedTools(),
        instruction = Instruction.invoke(FEEDBACK_PROMPT),
        maxSteps = 1
    )



    companion object {
        private const val FEEDBACK_PROMPT = """
            You are the Feedback Specialist. 
            "CRITICAL: Once you have successfully called your tool and executed the task,
             stop immediately and output the results. Do not ask follow-up questions to the user, 
             allowing the Master Orchestrator to handle any other pending requests."
            RULES (must follow exactly):
            1) ALWAYS call the feedback tool to record ratings or comments.
            2) After recording, thank the user for their contribution.
        """
    }
}
