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
        Log.d("AssistantFlow", "🗺️ Mapping event. Content: '${text.take(50)}...'")
        
        val responses = event.functionResponses()
        if (responses.isNotEmpty()) {
            val toolName = responses.first().name
            val rawData = responses.first().response
            
            // ADK wraps tool return Map in a "result" key. Handle both Map and JSON String.
            val data: Map<*, *> = when (val result = rawData["result"]) {
                is Map<*, *> -> result
                is String -> try {
                    gson.fromJson(result, object : TypeToken<Map<String, Any?>>() {}.type)
                } catch (e: Exception) {
                    rawData
                }
                else -> rawData
            }
            
            Log.d("AssistantFlow", "Mapper: Mapping tool $toolName with data keys: ${data.keys}")

            return when (toolName) {
                "assistant_search_menu", "get_recommendations", "get_today_specials", "get_full_menu", "search_menu" -> {
                    val rawItems = data["result"] ?: data // handle different repository return styles
                    val items = convertToMenuItems(rawItems)
                    Log.d("AssistantFlow", "Mapper: Found ${items.size} items for $toolName")
                    
                    if (toolName == "get_recommendations") {
                        AssistantUiState.Recommendations(items)
                    } else {
                        AssistantUiState.MenuSearch(items)
                    }
                }
                "get_menu_details" -> {
                    val rawItem = data["result"] ?: data
                    val item = convertToSingleMenuItem(rawItem)
                    item?.let { AssistantUiState.MenuDetails(it) } ?: AssistantUiState.TextResponse(text)
                }
                "add_to_cart" -> {
                    val rawItem = data["item"]
                    val item = convertToSingleMenuItem(rawItem)
                    val qty = (data["quantity"] as? Number)?.toInt() ?: 1
                    val msg = data["message"] as? String ?: text
                    item?.let { AssistantUiState.CartUpdate(it, qty, msg) } ?: AssistantUiState.TextResponse(msg)
                }
                "view_cart" -> {
                    // In some cases it might be data["result"]["items"]
                    val innerData = (data["result"] as? Map<*, *>) ?: data
                    val rawItems = innerData["items"] ?: data["items"]
                    val items = convertToCartItems(rawItems)
                    val total = (innerData["total"] as? Number)?.toInt() 
                        ?: (data["total"] as? Number)?.toInt() 
                        ?: 0
                    val msg = innerData["message"] as? String ?: data["message"] as? String ?: text
                    Log.d("AssistantFlow", "Mapper: CartView result -> ${items.size} items, total: $total")
                    AssistantUiState.CartView(items, total, msg)
                }
                "checkout" -> {
                    val rawItems = data["items"]
                    val items = convertToCartItems(rawItems)
                    val total = (data["total"] as? Number)?.toInt() ?: 0
                    val msg = data["message"] as? String ?: text
                    if (items.isNotEmpty()) {
                        AssistantUiState.CheckoutSummary(items, total, msg)
                    } else {
                        AssistantUiState.TextResponse(msg)
                    }
                }
                "remove_from_cart", "update_cart_quantity" -> {
                    val rawItem = data["item"]
                    val item = convertToSingleMenuItem(rawItem)
                    val qty = (data["quantity"] as? Number)?.toInt() ?: 0
                    val msg = data["message"] as? String ?: text
                    item?.let { AssistantUiState.CartUpdate(it, qty, msg) } ?: AssistantUiState.TextResponse(msg)
                }
                "clear_cart" -> {
                    val msg = data["message"] as? String ?: text
                    AssistantUiState.TextResponse(msg)
                }
                "create_booking" -> {
                    val msg = data["message"] as? String ?: text
                    val date = data["date"] as? String
                    val time = data["time"] as? String
                    val guests = (data["guests"] as? Number)?.toInt()
                    AssistantUiState.BookingResult(msg, date, time, guests)
                }
                "list_bookings" -> {
                    val msg = data["message"] as? String ?: text
                    // For POC, we'll map the list to a TextResponse if it's just info
                    // or we could add a BookingList state. For now, let's use the message.
                    AssistantUiState.TextResponse(msg)
                }
                "cancel_booking" -> {
                    val msg = data["message"] as? String ?: text
                    AssistantUiState.TextResponse(msg)
                }
                "submit_feedback" -> {
                    val msg = data["message"] as? String ?: text
                    val rating = (data["rating"] as? Number)?.toInt()
                    AssistantUiState.FeedbackResult(msg, rating)
                }
                "get_order_history" -> {
                    val isLatestOnly = (data["isLatestOnly"] as? Boolean) ?: false
                    val msg = data["message"] as? String ?: text
                    
                    if (isLatestOnly) {
                        val latestMap = data["latestOrder"] as? Map<*, *>
                        val statusStr = latestMap?.get("status") as? String ?: "PENDING"
                        
                        // Principle Engineer Sync: Match dots (0, 0.33, 0.66, 1.0)
                        val progress = when(statusStr.uppercase()) {
                            "PENDING", "CONFIRMED" -> 0.01f
                            "PREPARING" -> 0.33f
                            "READY", "PICKED_UP" -> 0.66f
                            "DELIVERED", "COMPLETED" -> 1.0f
                            else -> 0f
                        }
                        
                        val friendlyStatus = statusStr.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                        val fullMsg = if (msg.contains(friendlyStatus, ignoreCase = true)) msg else "$msg Current status is $friendlyStatus."

                        AssistantUiState.OrderStatus(fullMsg, statusStr, progress = progress)
                    } else {
                        AssistantUiState.TextResponse(msg)
                    }
                }
                "track_order" -> {
                    val msg = data["message"] as? String ?: text
                    val status = data["deliveryStatus"] as? String ?: data["orderStatus"] as? String ?: "PENDING"
                    val eta = data["eta"] as? String
                    
                    val progress = when(status.uppercase()) {
                        "PENDING", "CONFIRMED" -> 0.01f
                        "PREPARING" -> 0.33f
                        "READY", "PICKED_UP" -> 0.66f
                        "DELIVERED", "COMPLETED" -> 1.0f
                        else -> 0f
                    }

                    val friendlyStatus = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    val fullMsg = if (msg.contains(friendlyStatus, ignoreCase = true)) msg else "$msg Current status is $friendlyStatus."

                    AssistantUiState.OrderStatus(fullMsg, status, eta, progress = progress)
                }
                "cancel_order" -> {
                    val msg = data["message"] as? String ?: text
                    AssistantUiState.TextResponse(msg)
                }
                "get_available_coupons" -> {
                    val innerData = (data["result"] as? Map<*, *>) ?: data
                    val msg = innerData["message"] as? String ?: data["message"] as? String ?: text
                    val rawCoupons = innerData["coupons"] as? List<*> ?: data["coupons"] as? List<*>
                    
                    val coupons = convertToCoupons(rawCoupons)

                    if (coupons.isEmpty()) {
                        AssistantUiState.TextResponse(msg.ifBlank { "No active coupons available right now." })
                    } else {
                        AssistantUiState.CouponList(coupons, msg)
                    }
                }
                "get_restaurant_guidelines" -> {
                    val content = data["content"] as? String ?: ""
                    val msg = data["message"] as? String ?: text
                    
                    if (text.isBlank() || text == "I processed your request.") {
                        // If the model hasn't commented yet, don't show a bubble for the raw guidelines
                        // unless we want to show an InfoCard.
                        // Let's return blank so the Orchestrator doesn't add a redundant bubble.
                        AssistantUiState.TextResponse("")
                    } else {
                        AssistantUiState.TextResponse(text)
                    }
                }
                "validate_coupon" -> {
                    val msg = data["message"] as? String ?: text
                    AssistantUiState.TextResponse(msg.ifBlank { "Coupon validation result is unavailable." })
                }
                "apply_coupon" -> {
                    val msg = data["message"] as? String ?: text
                    AssistantUiState.TextResponse(msg.ifBlank { "Coupon was not applied." })
                }
                else -> {
                    if (text.isBlank() || text == "I processed your request.") {
                        val fallback = data["message"] as? String ?: ""
                        if (fallback.isNotBlank() && fallback != "I processed your request.") {
                            AssistantUiState.TextResponse(fallback)
                        } else {
                            // If everything is blank or generic, return blank to avoid double bubbles
                            AssistantUiState.TextResponse("")
                        }
                    } else {
                        AssistantUiState.TextResponse(text)
                    }
                }
            }
        }

        if (text.isBlank() || text == "I processed your request.") {
            val fallback = event.functionResponses().firstOrNull()?.response?.get("message") as? String ?: ""
            return AssistantUiState.TextResponse(if (fallback == "I processed your request.") "" else fallback)
        }

        return AssistantUiState.TextResponse(text)
    }

    private fun buildCouponSummary(coupons: List<*>, fallbackMessage: String): String {
        val items = coupons.mapNotNull { coupon ->
            val map = coupon as? Map<*, *> ?: return@mapNotNull null
            val code = map["code"] as? String ?: return@mapNotNull null
            val desc = map["description"] as? String ?: ""
            val percent = map["discount_percentage"] as? Number ?: return@mapNotNull null
            val minOrder = map["min_order_amount"] as? Number
            val suffix = minOrder?.let { " • Min order ₹${it.toDouble()}" } ?: ""
            "• $code: ${percent.toInt()}% off$suffix${if (desc.isNotBlank()) " • $desc" else ""}"
        }

        return if (items.isNotEmpty()) {
            "${fallbackMessage.ifBlank { "Here are the available coupons." }}\n${items.joinToString("\n")}"
        } else {
            fallbackMessage.ifBlank { "No active coupons available right now." }
        }
    }

    private fun convertToMenuItems(data: Any?): List<MenuItem> {
        if (data == null) {
            Log.d("AssistantFlow", "Mapper: Menu items data is null")
            return emptyList()
        }

        // Handle case where data is a JSON string
        if (data is String) {
            return try {
                val listType = object : TypeToken<List<MenuItem>>() {}.type
                gson.fromJson(data, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        return try {
            // Try direct cast
            if (data is List<*> && data.isNotEmpty() && data.firstOrNull() is MenuItem) {
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

    private fun convertToCartItems(data: Any?): List<com.example.a2ui_sample.domain.model.CartItem> {
        if (data == null) {
            Log.d("AssistantFlow", "Mapper: Cart items data is null")
            return emptyList()
        }
        
        Log.d("AssistantFlow", "Mapper: Converting cart items. Raw type: ${data.javaClass.simpleName}")

        // Handle case where data is a JSON string
        if (data is String) {
            return try {
                val listType = object : TypeToken<List<com.example.a2ui_sample.domain.model.CartItem>>() {}.type
                gson.fromJson(data, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Try direct cast
        if (data is List<*>) {
            if (data.isEmpty()) {
                Log.d("AssistantFlow", "Mapper: Cart items list is empty")
                return emptyList()
            }
            
            val first = data.firstOrNull()
            Log.d("AssistantFlow", "Mapper: First item type: ${first?.javaClass?.simpleName}")
            
            if (first is com.example.a2ui_sample.domain.model.CartItem) {
                Log.d("AssistantFlow", "Mapper: Using direct cast for ${data.size} CartItems")
                @Suppress("UNCHECKED_CAST")
                return data as List<com.example.a2ui_sample.domain.model.CartItem>
            }
        }

        return try {
            val json = gson.toJson(data)
            Log.d("AssistantFlow", "Mapper: Cart JSON: $json")
            val listType = object : TypeToken<List<com.example.a2ui_sample.domain.model.CartItem>>() {}.type
            val result: List<com.example.a2ui_sample.domain.model.CartItem> = gson.fromJson(json, listType) ?: emptyList()
            Log.d("AssistantFlow", "Mapper: GSON conversion produced ${result.size} items")
            result
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Error converting cart items: ${e.message}")
            emptyList()
        }
    }

    /**
     * Safely converts raw tool data to a single MenuItem.
     */
    private fun convertToSingleMenuItem(data: Any?): MenuItem? {
        if (data == null) return null
        if (data is MenuItem) return data
        
        // Handle JSON string
        if (data is String) {
            return try {
                gson.fromJson(data, MenuItem::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        return try {
            val json = gson.toJson(data)
            gson.fromJson(json, MenuItem::class.java)
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Error converting menu item: ${e.message}")
            null
        }
    }

    private fun convertToCoupons(data: Any?): List<com.example.a2ui_sample.domain.model.Coupon> {
        if (data == null) return emptyList()
        return try {
            val json = gson.toJson(data)
            val listType = object : TypeToken<List<com.example.a2ui_sample.domain.model.Coupon>>() {}.type
            gson.fromJson(json, listType) ?: emptyList()
        } catch (e: Exception) {
            Log.e("AssistantFlow", "Error converting coupons: ${e.message}")
            emptyList()
        }
    }

    fun mapTextToUi(text: String): AssistantUiState = AssistantUiState.TextResponse(text)
    fun mapErrorToUi(error: String): AssistantUiState = AssistantUiState.Error(error)
}
