package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.AgentResponse

class OrchestratorTools {
    private var lastResponse: AgentResponse? = null
    fun getLastResponse(): AgentResponse? = lastResponse
    fun setLastResponse(resp: AgentResponse) { lastResponse = resp }
    fun reset() { lastResponse = null }
}
