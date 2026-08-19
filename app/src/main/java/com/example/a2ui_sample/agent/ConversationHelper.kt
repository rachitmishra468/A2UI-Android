package com.example.a2ui_sample.agent

import com.example.a2ui_sample.domain.model.UserIntent

/**
 * ConversationHelper
 * Utility for generating natural, helpful responses and satisfaction feedback prompts.
 */
object ConversationHelper {
    
    /**
     * Determines if we should ask for satisfaction feedback after this intent.
     * Ask after major actions: orders placed, bookings confirmed, feedback submitted
     */
    fun shouldAskForSatisfaction(intent: UserIntent): Boolean {
        return when (intent) {
            UserIntent.ORDER_PLACED,
            UserIntent.BOOKING_CREATE,
            UserIntent.FEEDBACK_SUBMIT,
            UserIntent.CHECKOUT -> true
            else -> false
        }
    }
    
    /**
     * Returns a natural satisfaction feedback prompt
     */
    fun getSatisfactionPrompt(intent: UserIntent): String {
        return when (intent) {
            UserIntent.ORDER_PLACED -> 
                "\n\nWas I able to help you place this order smoothly? 😊\n👍 Yes, great!  |  👎 Could be better"
            UserIntent.BOOKING_CREATE -> 
                "\n\nDid I make booking your table easy? 😊\n👍 Yes!  |  👎 Not really"
            UserIntent.FEEDBACK_SUBMIT -> 
                "\n\nWas the feedback process simple? 😊\n👍 Yes!  |  👎 Could improve"
            UserIntent.CHECKOUT -> 
                "\n\nHow was your ordering experience today? 😊\n👍 Excellent!  |  👎 Needs work"
            else -> ""
        }
    }
    
    /**
     * Returns helpful follow-up questions based on context
     */
    fun getFollowUpSuggestion(intent: UserIntent): String {
        return when (intent) {
            UserIntent.MENU_SEARCH -> 
                "Would you like to add any of these to your cart? 🛒"
            UserIntent.MENU_RECOMMEND -> 
                "Anything catching your eye? I can add it for you! 😊"
            UserIntent.CART_ADD -> 
                "Anything else you'd like to add?"
            UserIntent.CART_VIEW -> 
                "Ready to checkout, or shall we add more items?"
            UserIntent.ORDER_TRACKING -> 
                "Need help with anything else?"
            UserIntent.BOOKING_LIST -> 
                "Would you like to book another table?"
            UserIntent.ORDER_HISTORY -> 
                "Want to reorder any of these?"
            else -> ""
        }
    }
    
    /**
     * Returns contextual greeting based on time of day
     */
    fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good morning! ☀️"
            in 12..16 -> "Good afternoon! 🌤️"
            in 17..20 -> "Good evening! 🌆"
            else -> "Hey there! 🌙"
        }
    }
    
    /**
     * Returns friendly error messages
     */
    fun getFriendlyError(errorType: String): String {
        return when (errorType) {
            "not_found" -> "Hmm, I couldn't find that. 🤔 Let me help you search!"
            "empty_cart" -> "Your cart is empty right now. 🛒 Let's find something delicious!"
            "no_orders" -> "You don't have any orders yet. 📦 Ready to place your first one?"
            "network_error" -> "Oops! I'm having trouble connecting. 😔 Please try again!"
            "invalid_input" -> "I didn't quite get that. 🤔 Could you rephrase?"
            else -> "Something went wrong, but I'm here to help! 😊"
        }
    }
    
    /**
     * Adds personality to generic messages
     */
    fun enhanceMessage(baseMessage: String, context: UserIntent? = null): String {
        // Add emojis and friendly tone if not already present
        if (baseMessage.contains("😊") || baseMessage.contains("🛒") || baseMessage.contains("✅")) {
            return baseMessage // Already enhanced
        }
        
        return when {
            baseMessage.contains("success", ignoreCase = true) -> "$baseMessage ✅"
            baseMessage.contains("error", ignoreCase = true) -> "$baseMessage 😔"
            baseMessage.contains("thank", ignoreCase = true) -> "$baseMessage 😊"
            baseMessage.contains("sorry", ignoreCase = true) -> "$baseMessage 🙏"
            else -> "$baseMessage 😊"
        }
    }
}
