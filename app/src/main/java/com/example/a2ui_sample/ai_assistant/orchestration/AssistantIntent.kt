package com.example.a2ui_sample.ai_assistant.orchestration

import com.example.a2ui_sample.domain.model.IntentResult
import com.example.a2ui_sample.domain.model.MasterAgentTask

/**
 * AssistantIntent
 * Sealed class representing structured intents extracted by the IntentAgent.
 * Updated to support both single and multi-step (Workflow) execution.
 */
sealed class AssistantIntent {
    // Wrap the full IntentResult for flexibility
    data class SingleIntent(val result: IntentResult) : AssistantIntent()
    data class Workflow(val tasks: List<MasterAgentTask>) : AssistantIntent()
    
    data class Unknown(
        val reasoning: String? = null
    ) : AssistantIntent()
}
