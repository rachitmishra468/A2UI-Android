package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.*
import com.example.a2ui_sample.domain.repository.MenuRepository
import java.util.Calendar

/**
 * UserProfileBuilder - Builds AI-powered user profiles from behavior data
 */
class UserProfileBuilder(
    private val menuRepository: MenuRepository
) {
    
    /**
     * Build comprehensive user profile from orders, bookings, and feedback
     */
    fun buildProfile(
        orders: List<Order>,
        bookings: List<Reservation>,
        feedback: List<Feedback>
    ): UserProfile {
        if (orders.isEmpty()) {
            return UserProfile() // Return empty profile for new users
        }
        
        // Extract favorite items (most frequently ordered)
        val itemFrequency = mutableMapOf<Int, Int>()
        orders.forEach { order ->
            order.items.forEach { orderItem ->
                val currentCount = itemFrequency.getOrDefault(orderItem.menuItemId, 0)
                itemFrequency[orderItem.menuItemId] = currentCount + orderItem.quantity
            }
        }
        
        val allMenuItems = menuRepository.getMenuItems()
        val favoriteItems = itemFrequency.entries
            .sortedByDescending { it.value }
            .take(5)
            .mapNotNull { entry -> allMenuItems.find { it.id == entry.key } }
        
        // Extract category preferences
        val categoryFrequency = mutableMapOf<String, Int>()
        orders.forEach { order ->
            order.items.forEach { orderItem ->
                val menuItem = allMenuItems.find { it.id == orderItem.menuItemId }
                if (menuItem != null) {
                    val category = menuItem.category
                    categoryFrequency[category] = categoryFrequency.getOrDefault(category, 0) + 1
                }
            }
        }
        
        // Detect diet preference (veg vs non-veg)
        var vegCount = 0
        var nonVegCount = 0
        orders.forEach { order ->
            order.items.forEach { orderItem ->
                val menuItem = allMenuItems.find { it.id == orderItem.menuItemId }
                if (menuItem != null) {
                    if (menuItem.type.name == "VEG") vegCount++
                    else nonVegCount++
                }
            }
        }
        val preferredDiet = when {
            vegCount > nonVegCount * 2 -> "veg"
            nonVegCount > vegCount -> "non-veg"
            else -> null
        }
        
        // Calculate average budget
        val totalSpent = orders.sumOf { it.totalAmount.amount }
        val averageBudget = if (orders.isNotEmpty()) totalSpent / orders.size else 0
        
        // Detect order time pattern
        val orderHours = orders.mapNotNull { order ->
            try {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = order.orderTime
                calendar.get(Calendar.HOUR_OF_DAY)
            } catch (e: Exception) {
                null
            }
        }
        
        val orderTimePattern = when {
            orderHours.isEmpty() -> null
            orderHours.count { it in 6..10 } > orderHours.size / 2 -> "breakfast"
            orderHours.count { it in 11..15 } > orderHours.size / 2 -> "lunch"
            orderHours.count { it in 16..18 } > orderHours.size / 2 -> "evening"
            orderHours.count { it in 19..23 } > orderHours.size / 2 -> "dinner"
            else -> "dinner" // default
        }
        
        // Extract typical people count from bookings
        val peopleCounts: List<Int> = bookings.map { it.partySize }
        val typicalPeopleCount = if (peopleCounts.isNotEmpty()) {
            peopleCounts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 1
        } else 1
        
        // Analyze feedback patterns
        val feedbackPattern = feedback.groupingBy { it.overallRating.value }.eachCount()
        
        // Last order timestamp
        val lastOrderTimestamp = orders.maxOfOrNull { it.orderTime } ?: 0L
        
        return UserProfile(
            userId = "guest",
            favoriteItems = favoriteItems,
            frequentCategories = categoryFrequency,
            preferredDiet = preferredDiet,
            averageBudget = averageBudget,
            totalOrders = orders.size,
            spicePreference = null, // TODO: Could be extracted from item names containing "spicy"
            orderTimePattern = orderTimePattern,
            typicalPeopleCount = typicalPeopleCount,
            bookingFrequency = bookings.size,
            lastOrderTimestamp = lastOrderTimestamp,
            feedbackPattern = feedbackPattern
        )
    }
    
    /**
     * Generate proactive suggestions based on user profile
     */
    fun generateProactiveSuggestions(profile: UserProfile): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Suggest favorite items
        if (profile.favoriteItems.isNotEmpty() && profile.isTypicalOrderTime()) {
            val topItem = profile.favoriteItems.first()
            suggestions.add("It's ${profile.orderTimePattern} time! Would you like to order your favorite ${topItem.name}?")
        }
        
        // Suggest table booking for frequent bookers
        if (profile.bookingFrequency >= 2) {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
                suggestions.add("You usually book tables on weekends. Would you like to reserve one for ${profile.typicalPeopleCount} people?")
            }
        }
        
        // Suggest based on time since last order
        if (profile.lastOrderTimestamp > 0) {
            val daysSinceLastOrder = (System.currentTimeMillis() - profile.lastOrderTimestamp) / (1000 * 60 * 60 * 24)
            if (daysSinceLastOrder >= 7 && profile.favoriteItems.isNotEmpty()) {
                val topItem = profile.favoriteItems.first()
                suggestions.add("Haven't seen you in a while! Your favorite ${topItem.name} is waiting for you 😊")
            }
        }
        
        return suggestions
    }
}
