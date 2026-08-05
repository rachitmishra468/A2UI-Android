package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.model.IntentResultWrapper
import com.example.a2ui_sample.domain.model.UserProfile
import com.example.a2ui_sample.domain.repository.MenuRepository

private const val TAG = "MenuAgent"

/**
 * MenuAgent
 * Specialist for menu-related operations with AI-powered recommendations.
 */
class MenuAgent(private val repository: MenuRepository) {
    
    private val profileBuilder by lazy { UserProfileBuilder(repository) }

    fun execute(intent: IntentResultWrapper): AgentResponse {
        return when (intent.intent) {
            com.example.a2ui_sample.domain.model.UserIntent.MENU_SEARCH -> {
                val category = intent.entities["category"] as? String
                val diet = intent.entities["diet"] as? String
                val rawMaxPrice = (intent.entities["price_limit"] as? Number)?.toInt()
                val peopleCount = (intent.entities["people_count"] as? Number)?.toInt() ?: 1
                
                val maxPrice = if (rawMaxPrice != null && peopleCount > 1 && rawMaxPrice > 500) {
                    rawMaxPrice / peopleCount
                } else {
                    rawMaxPrice
                }

                val items = repository.searchMenu(category, diet, maxPrice)
                
                val message = when {
                    items.isEmpty() && maxPrice != null -> {
                        // Find closest alternatives above the budget
                        val alternatives = repository.searchMenu(category, diet, null).filter { it.price.amount > maxPrice!! }.take(3)
                        if (alternatives.isNotEmpty()) {
                            "I couldn't find items under ₹$maxPrice, but these are close! Would any of these work? 😊"
                        } else {
                            "Hmm, I couldn't find anything matching those filters. Can I suggest something else? 🤔"
                        }
                    }
                    items.isEmpty() -> "I couldn't find exactly what you're looking for. How about I show you our bestsellers? 🌟"
                    rawMaxPrice != null && peopleCount > 1 -> "Perfect! For $peopleCount people within ₹$rawMaxPrice, here are some great dishes (around ₹$maxPrice per person): 👥"
                    rawMaxPrice != null -> "Great choice! Here are delicious options under ₹$rawMaxPrice: 💰"
                    peopleCount > 1 -> "Nice! For a group of $peopleCount, I'd recommend 3-4 of these ${category ?: "dishes"} to share: 🍽️"
                    diet != null && category != null -> "Here are some tasty $diet $category options just for you: 😋"
                    diet != null -> "Here are our best $diet dishes: 🥗"
                    category != null -> "I've got some amazing $category options: ✨"
                    else -> "Let me show you some delicious options: 🍴"
                }
                
                val finalItems = if (items.isEmpty() && maxPrice != null) {
                    repository.searchMenu(category, diet, null).filter { it.price.amount > maxPrice }.take(3)
                } else {
                    items
                }

                AgentResponse.MenuResults(finalItems, message)
            }
            com.example.a2ui_sample.domain.model.UserIntent.MENU_RECOMMEND -> {
                Log.d(TAG, "🎯 Generating AI-powered recommendations")
                
                val category = intent.entities["category"] as? String
                val diet = intent.entities["diet"] as? String
                val rawMaxPrice = (intent.entities["price_limit"] as? Number)?.toInt()
                val peopleCount = (intent.entities["people_count"] as? Number)?.toInt() ?: 1
                
                val perPersonBudget = if (rawMaxPrice != null && peopleCount > 0) rawMaxPrice / peopleCount else null
                
                // Get bestsellers filtered by constraints
                val items = repository.getMenuItems().filter { item ->
                    val matchesCategory = category == null || item.category.contains(category, ignoreCase = true)
                    val matchesDiet = diet == null || item.type.name.equals(diet, ignoreCase = true)
                    val matchesPrice = perPersonBudget == null || item.price.amount <= perPersonBudget
                    item.isBestSeller && matchesCategory && matchesDiet && matchesPrice
                }.take(5)

                if (items.isEmpty() && category != null) {
                    return execute(intent.copy(intent = com.example.a2ui_sample.domain.model.UserIntent.MENU_SEARCH))
                }
                
                val message = if (items.isNotEmpty()) {
                    "Here are my top picks for you today! 🌟 Would you like to add any?"
                } else {
                    "Let me show you our most popular dishes! 🔥"
                }

                AgentResponse.Recommendations(items, message)
            }
            else -> AgentResponse.Message("I'd be happy to help you explore our menu! What are you craving today? 😊")
        }
    }
    
    /**
     * Generate personalized recommendations using user profile and AI reasoning
     */
    fun getPersonalizedRecommendations(
        profile: UserProfile,
        context: String = ""
    ): AgentResponse {
        Log.d(TAG, "🧠 Generating personalized recommendations for user")
        Log.d(TAG, "📊 Profile: ${profile.totalOrders} orders, ${profile.favoriteItems.size} favorites")
        
        // If new user, show bestsellers
        if (!profile.isFrequentCustomer()) {
            val bestsellers = repository.getMenuItems().filter { it.isBestSeller }.take(5)
            return AgentResponse.Recommendations(
                bestsellers,
                "Welcome! Here are our most popular dishes to get you started:"
            )
        }
        
        // For frequent customers, build personalized recommendations
        val allItems = repository.getMenuItems()
        
        // Score items based on user preferences
        val scoredItems = allItems.map { item ->
            var score = 0.0
            
            // Boost if similar to favorites
            if (profile.favoriteItems.any { fav -> fav.category == item.category }) {
                score += 10.0
            }
            
            // Boost if matches diet preference
            profile.preferredDiet?.let {
                if ((it == "veg" && item.type.name == "VEG") || 
                    (it == "non-veg" && item.type.name != "VEG")) {
                    score += 8.0
                }
            }
            
            // Boost if in typical budget range
            if (profile.averageBudget > 0) {
                val priceDiff = Math.abs(item.price.amount - profile.averageBudget)
                val priceDiffPercent = (priceDiff.toDouble() / profile.averageBudget) * 100
                if (priceDiffPercent < 30) {
                    score += 7.0 - (priceDiffPercent / 10)
                }
            }
            
            // Boost if bestseller
            if (item.isBestSeller) {
                score += 5.0
            }
            
            // Penalize if already a favorite (show new items)
            if (profile.favoriteItems.any { it.id == item.id }) {
                score -= 15.0
            }
            
            item to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        // Build personalized message
        val message = buildPersonalizedMessage(profile, scoredItems)
        
        return AgentResponse.Recommendations(scoredItems, message)
    }
    
    private fun buildPersonalizedMessage(profile: UserProfile, items: List<com.example.a2ui_sample.domain.model.MenuItem>): String {
        val parts = mutableListOf<String>()
        
        if (profile.isTypicalOrderTime()) {
            parts.add("Perfect timing for ${profile.orderTimePattern}!")
        }
        
        if (profile.totalOrders >= 5) {
            parts.add("Welcome back!")
        }
        
        if (profile.favoriteItems.isNotEmpty() && items.isNotEmpty()) {
            val favCategory = profile.favoriteItems.first().category
            val hasMatchingCategory = items.any { it.category.contains(favCategory, ignoreCase = true) }
            if (hasMatchingCategory) {
                parts.add("Based on your love for $favCategory, I've got some great suggestions:")
            } else {
                parts.add("Since you enjoy our $favCategory dishes, you might also like these:")
            }
        } else {
            parts.add("Here are some dishes I think you'll love:")
        }
        
        return parts.joinToString(" ")
    }
}
