package com.example.a2ui_sample.domain.model

/**
 * UserProfile - AI-powered user preference learning
 * Built from order history, feedback, and behavior patterns
 */
data class UserProfile(
    val userId: String = "guest",
    val favoriteItems: List<MenuItem> = emptyList(),
    val frequentCategories: Map<String, Int> = emptyMap(), // category -> frequency
    val preferredDiet: String? = null, // "veg" or "non-veg" based on pattern
    val averageBudget: Int = 0,
    val totalOrders: Int = 0,
    val spicePreference: String? = null, // "mild", "medium", "hot"
    val orderTimePattern: String? = null, // "lunch", "dinner", "evening"
    val typicalPeopleCount: Int = 1,
    val bookingFrequency: Int = 0,
    val lastOrderTimestamp: Long = 0L,
    val feedbackPattern: Map<Int, Int> = emptyMap() // rating -> count
) {
    /**
     * Convert profile to natural language prompt for Gemini
     */
    fun toPrompt(): String {
        val parts = mutableListOf<String>()
        
        if (totalOrders > 0) {
            parts.add("Total orders: $totalOrders")
        }
        
        if (favoriteItems.isNotEmpty()) {
            val topItems = favoriteItems.take(5).joinToString(", ") { it.name }
            parts.add("Favorite items: $topItems")
        }
        
        if (frequentCategories.isNotEmpty()) {
            val topCategories = frequentCategories.entries
                .sortedByDescending { it.value }
                .take(3)
                .joinToString(", ") { "${it.key} (${it.value}x)" }
            parts.add("Frequent categories: $topCategories")
        }
        
        preferredDiet?.let {
            parts.add("Diet preference: $it")
        }
        
        if (averageBudget > 0) {
            parts.add("Average budget: ₹$averageBudget")
        }
        
        spicePreference?.let {
            parts.add("Spice preference: $it")
        }
        
        orderTimePattern?.let {
            parts.add("Typical order time: $it")
        }
        
        if (typicalPeopleCount > 1) {
            parts.add("Usually orders for: $typicalPeopleCount people")
        }
        
        if (bookingFrequency > 0) {
            parts.add("Table bookings: $bookingFrequency times")
        }
        
        return parts.joinToString("\n- ", prefix = "- ")
    }
    
    /**
     * Check if user is likely to be interested in an item
     */
    fun wouldLikeItem(item: MenuItem): Boolean {
        // Check if item matches user's diet preference
        preferredDiet?.let {
            if (it == "veg" && item.type.name != "VEG") return false
            if (it == "non-veg" && item.type.name == "VEG") return false
        }
        
        // Check if item is in budget range (±30%)
        if (averageBudget > 0) {
            val minBudget = (averageBudget * 0.7).toInt()
            val maxBudget = (averageBudget * 1.3).toInt()
            if (item.price.amount !in minBudget..maxBudget) return false
        }
        
        return true
    }
    
    /**
     * Check if user is a frequent customer
     */
    fun isFrequentCustomer(): Boolean = totalOrders >= 3
    
    /**
     * Check if it's user's typical order time
     */
    fun isTypicalOrderTime(): Boolean {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (orderTimePattern) {
            "breakfast" -> currentHour in 6..10
            "lunch" -> currentHour in 11..15
            "evening" -> currentHour in 16..18
            "dinner" -> currentHour in 19..23
            else -> false
        }
    }
}
