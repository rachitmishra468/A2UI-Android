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
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    val adkAgent = LlmAgent(
        name = "FeedbackAssistant",
        model = geminiModel,
        instruction = Instruction.invoke(FEEDBACK_PROMPT),
        tools = feedbackTools.generatedTools(),
        maxSteps = 1 // Prevent loops
    )

    fun executeCommand(command: String, session: Session): Flow<Event> {
        session.events.add(Event(author = Role.USER, content = Content.fromText(Role.USER, "[COMMAND] $command")))
        return adkAgent.runAsync(InvocationContext(agent = adkAgent, session = session))
    }

    companion object {
        private const val FEEDBACK_PROMPT = """
            You are the Feedback Specialist. 
            
            RULES (must follow exactly):
            1) ALWAYS call submit_feedback(rating=<num>, comment="<text>", orderId=null).
            2) Extract the star rating (1-5) and the user's comment precisely.
            3) If the user doesn't provide a rating, ask for one while calling the tool with the comment.

            Example:
             User: food was great, 5 stars!
             Tool: submit_feedback(rating=5, comment="food was great")
        """
    }
}
