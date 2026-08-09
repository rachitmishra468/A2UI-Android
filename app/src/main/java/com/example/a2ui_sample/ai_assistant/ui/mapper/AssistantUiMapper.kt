package com.example.a2ui_sample.ai_assistant.ui.mapper

import android.util.Log
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantUiState
import com.example.a2ui_sample.domain.model.MenuItem
import com.google.adk.kt.events.Event
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AssistantUiMapper
 * Event-based UI mapping.
 * Optimized to handle ADK tool response data types (Maps/Lists).
 */
@Singleton
class AssistantUiMapper @Inject constructor() {
    private val gson = Gson()

    fun mapEventToUi(event: Event): AssistantUiState {
        val text = event.content?.parts?.firstOrNull()?.text ?: ""
        
        val responses = event.functionResponses()
        if (responses.isNotEmpty()) {
            val toolName = responses.first().name
            val data = responses.first().response
            Log.d("AssistantFlow", "Mapper: Mapping tool $toolName")

            return when (toolName) {
                "assistant_search_menu", "get_recommendations", "get_today_specials", "get_full_menu" -> {
                    val rawItems = data["result"]
                    val items = convertToMenuItems(rawItems)
                    Log.d("AssistantFlow", "Mapper: Found ${items.size} items for $toolName")
                    
                    if (toolName == "get_recommendations") {
                        AssistantUiState.Recommendations(items)
                    } else {
                        AssistantUiState.MenuSearch(items)
                    }
                }
                "get_menu_details" -> {
                    val rawItem = data["result"]
                    val item = convertToSingleMenuItem(rawItem)
                    item?.let { AssistantUiState.MenuDetails(it) } ?: AssistantUiState.TextResponse(text)
                }
                "add_to_cart" -> {
                    val rawItem = data["item"]
                    val item = convertToSingleMenuItem(rawItem)
                    val qty = (data["quantity"] as? Number)?.toInt() ?: 1
                    item?.let { AssistantUiState.CartUpdate(it, qty, text) } ?: AssistantUiState.TextResponse(text)
                }
                else -> AssistantUiState.TextResponse(text)
            }
        }

        return AssistantUiState.TextResponse(text)
    }

    /**
     * Safely converts raw tool data to a List of MenuItems.
     * Handles cases where data is returned as List<Map> instead of List<MenuItem>.
     */
    private fun convertToMenuItems(data: Any?): List<MenuItem> {
        if (data == null) return emptyList()
        
        return try {
            // Try direct cast
            if (data is List<*> && data.firstOrNull() is MenuItem) {
                @Suppress("UNCHECKED_CAST")
                return data as List<MenuItem>
            }
            
            // Fallback: Convert via JSON to ensure correct object mapping
            val json = gson.toJson(data)
            val element: JsonElement = try {
                JsonParser.parseString(json)
            } catch (pe: Exception) {
                Log.e("AssistantFlow", "Error parsing JSON for menu items: ${pe.message}")
                return emptyList()
            }

            val listType = object : TypeToken<List<MenuItem>>() {}.type

            // If the JSON is an array, parse directly
            if (element.isJsonArray) {
                return gson.fromJson(element, listType) ?: emptyList()
            }

            // If the JSON is an object, try common wrapper fields that may contain the array
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                val candidateFields = listOf("items", "results", "result", "menu", "data")
                for (field in candidateFields) {
                    if (obj.has(field) && obj.get(field).isJsonArray) {
                        return gson.fromJson(obj.get(field), listType) ?: emptyList()
                    }
                }

                // If object looks like a single MenuItem, parse it as single and wrap in a list
                try {
                    val single = gson.fromJson(element, MenuItem::class.java)
                    // Basic validation: name should not be blank
                    if (single != null && single.name.isNotBlank()) return listOf(single)
                } catch (_: Exception) {
                    // fallthrough to empty list
                }
            }

            emptyList()
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Error converting menu items: ${e.message}")
            emptyList()
        }
    }

    /**
     * Safely converts raw tool data to a single MenuItem.
     */
    private fun convertToSingleMenuItem(data: Any?): MenuItem? {
        if (data == null) return null
        if (data is MenuItem) return data
        
        return try {
            val json = gson.toJson(data)
            gson.fromJson(json, MenuItem::class.java)
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Error converting menu item: ${e.message}")
            null
        }
    }

    fun mapTextToUi(text: String): AssistantUiState = AssistantUiState.TextResponse(text)
    fun mapErrorToUi(error: String): AssistantUiState = AssistantUiState.Error(error)
}
