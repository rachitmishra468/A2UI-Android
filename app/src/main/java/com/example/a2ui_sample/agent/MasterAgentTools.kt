package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.google.adk.kt.annotations.Tool
import com.google.adk.kt.sessions.Session
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime

private const val TAG = "ADK_AGENT"

@OptIn(ExperimentalTime::class)
class MasterAgentTools(
    private val menuAgent: MenuAgent,
    private val cartAgent: CartAgent,
    private val bookingAgent: BookingAgent,
    private val session: Session
) {
    private var lastResp: AgentResponse? = null
    private var userQuery: String = ""  // Store the original user query

    fun getLastResponse() = lastResp
    fun reset() { lastResp = null }
    fun setUserQuery(query: String) { userQuery = query }

    private fun getEffectiveQuery(llmQuery: String?): String {
        // Fallback to original user query if LLM passes meta-talk or null
        val isMetaTalk = llmQuery?.contains("System Instruction", ignoreCase = true) == true ||
                        llmQuery?.contains("specified in", ignoreCase = true) == true
        
        return if (llmQuery.isNullOrBlank() || isMetaTalk) {
            Log.d(TAG, "   Fallback used: llmQuery='$llmQuery', using userQuery='$userQuery'")
            userQuery
        } else {
            llmQuery
        }
    }

    @Tool(
        name = "delegate_to_menu_agent",
        description = "Delegate to the menu specialist agent for food, menu, or ingredient questions."
    )
    fun delegateMenu(query: String? = null): String {
        val effectiveQuery = getEffectiveQuery(query)
        Log.i(TAG, "🔴 TOOL CALLED: delegate_to_menu_agent (query: $effectiveQuery)")
        return try {
            val result = runBlocking {
                Log.d(TAG, "   Calling MenuAgent.process()...")
                menuAgent.process(effectiveQuery, session)
            }
            Log.d(TAG, "   ✅ MenuAgent returned: ${result.javaClass.simpleName}")
            lastResp = result
            "SUCCESS: Results retrieved from Menu Specialist. Task complete."
        } catch (e: Exception) {
            Log.e(TAG, "   ❌ Menu delegation error: ${e.message}", e)
            lastResp = AgentResponse.Error("Failed to get menu: ${e.message}")
            "ERROR: ${e.message}"
        }
    }

    @Tool(
        name = "delegate_to_cart_agent",
        description = "Delegate to the cart specialist agent for orders, cart items, or checkout."
    )
    fun delegateCart(query: String? = null): String {
        val effectiveQuery = getEffectiveQuery(query)
        Log.i(TAG, "🔴 TOOL CALLED: delegate_to_cart_agent (query: $effectiveQuery)")
        return try {
            val result = runBlocking {
                Log.d(TAG, "   Calling CartAgent.process()...")
                cartAgent.process(effectiveQuery, session)
            }
            Log.d(TAG, "   ✅ CartAgent returned: ${result.javaClass.simpleName}")
            lastResp = result
            "SUCCESS: Results retrieved from Cart Specialist. Task complete."
        } catch (e: Exception) {
            Log.e(TAG, "   ❌ Cart delegation error: ${e.message}", e)
            lastResp = AgentResponse.Error("Failed to process cart: ${e.message}")
            "ERROR: ${e.message}"
        }
    }

    @Tool(
        name = "delegate_to_booking_agent",
        description = "Delegate to the booking specialist agent for table reservations or availability."
    )
    fun delegateBooking(query: String? = null): String {
        val effectiveQuery = getEffectiveQuery(query)
        Log.i(TAG, "🔴 TOOL CALLED: delegate_to_booking_agent (query: $effectiveQuery)")
        return try {
            val result = runBlocking {
                Log.d(TAG, "   Calling BookingAgent.process()...")
                bookingAgent.process(effectiveQuery, session)
            }
            Log.d(TAG, "   ✅ BookingAgent returned: ${result.javaClass.simpleName}")
            lastResp = result
            "SUCCESS: Results retrieved from Booking Specialist. Task complete."
        } catch (e: Exception) {
            Log.e(TAG, "   ❌ Booking delegation error: ${e.message}", e)
            lastResp = AgentResponse.Error("Failed to process booking: ${e.message}")
            "ERROR: ${e.message}"
        }
    }
}
