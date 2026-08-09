package com.example.a2ui_sample.agent

import com.example.a2ui_sample.infrastructure.persistence.dao.ConversationMemoryDao
import com.example.a2ui_sample.infrastructure.persistence.entity.ConversationMemoryEntity
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationMemoryManager @Inject constructor(
    private val memoryDao: ConversationMemoryDao
) {
    private val gson = Gson()

    companion object {
        const val LAST_INTENT = "last_intent"
        const val LAST_MENU_RESULTS = "last_menu_results"
        const val LAST_ORDER = "last_order"
        const val LAST_ORDER_ID = "last_order_id"
        const val LAST_BOOKING = "last_booking"
        const val LAST_BOOKING_ID = "last_booking_id"
        const val LAST_RECOMMENDED_ITEMS = "last_recommended_items"
        const val LAST_CART_ITEMS = "last_cart_items"
        const val LAST_SELECTED_ITEM = "last_selected_item"
        const val LAST_DISCUSSED_TOPIC = "last_discussed_topic"
        const val LAST_AI_RESPONSE = "last_ai_response"
    }

    suspend fun save(key: String, value: Any?) {
        if (value == null) {
            memoryDao.deleteMemory(key)
            return
        }
        val stringValue = if (value is String) value else gson.toJson(value)
        memoryDao.insertMemory(ConversationMemoryEntity(key, stringValue))
    }

    suspend fun <T> get(key: String, type: Class<T>): T? {
        val memory = memoryDao.getMemory(key) ?: return null
        return try {
            if (type == String::class.java) {
                memory.value as T
            } else {
                gson.fromJson(memory.value, type)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getString(key: String): String? = get(key, String::class.java)

    suspend fun clear() {
        memoryDao.clearMemory()
    }
    
    suspend fun getAllContext(): Map<String, String> {
        return memoryDao.getAllMemory().associate { it.memoryKey to it.value }
    }
}
