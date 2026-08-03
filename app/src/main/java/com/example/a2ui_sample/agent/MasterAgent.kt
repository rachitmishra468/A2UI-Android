package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.BuildConfig
import com.example.a2ui_sample.domain.model.AgentResponse
import com.example.a2ui_sample.domain.repository.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.google.adk.kt.agents.Instruction
import kotlin.time.ExperimentalTime

private const val TAG = "A2UI_FLOW"

/**
 * Master Agent that delegates to specialized agents.
 * Principal Architect Implementation with robust short-circuit logic and context-aware routing.
 */
class ADKRestaurantMasterAgent(
    private val menuRepository: MenuRepository,
    private val orchestratorTools: OrchestratorTools
) {
    private val responseBuilder by lazy { A2UIResponseBuilder() }
    // Diagnostic: log whether API key is present (do not print full key)
    private val apiKey = BuildConfig.GEMINI_API_KEY.also { key ->
        try {
            Log.d(TAG, "GEMINI_API_KEY present: ${""}" + (!key.isNullOrBlank()).toString())
        } catch (t: Throwable) {
            Log.w(TAG, "GEMINI_API_KEY diagnostic failed: ${""}" + (t.message ?: "<none>"))
        }
    }

    // Wrap Gemini model creation with diagnostic logs so failures surface clearly
    private val geminiModel by lazy {
        try {
            val model = Gemini("gemini-3.6-flash", apiKey)
            Log.d(TAG, "Gemini model created successfully")
            model
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Gemini model: ${""}" + (e.message ?: "<no message>"), e)
            throw e
        }
    }
    
    private val menuAgent by lazy { MenuAgent(menuRepository, geminiModel) }
    private val cartAgent by lazy { CartAgent(menuRepository, geminiModel) }
    private val bookingAgent by lazy { BookingAgent(menuRepository, geminiModel) }

    @OptIn(ExperimentalTime::class)
    private val sessionKey by lazy { SessionKey("RestaurantApp", "DefaultUser", "Session-Restaurant") }
    @OptIn(ExperimentalTime::class)
    private val session by lazy { Session(sessionKey) }

    private val masterTools: MasterAgentTools by lazy {
        MasterAgentTools(
            menuAgent = menuAgent,
            cartAgent = cartAgent,
            bookingAgent = bookingAgent,
            session = session
        )
    }

    @OptIn(ExperimentalTime::class)
    private val adkAgent: LlmAgent by lazy {
        val tools = masterTools.generatedTools()

        LlmAgent(
            name = "RestaurantMasterAgent",
            model = geminiModel,
            instruction = Instruction(
                """
                # SYSTEM IDENTITY
                You are an elite AI Restaurant Concierge for a premium dining establishment.
                Your purpose is to deliver seamless, intuitive, and delightful restaurant experiences through natural conversation.
                
                # MULTILINGUAL EXPERTISE
                You MUST understand and respond fluently in:
                - English (formal and casual)
                - Hindi (हिंदी)
                - Hinglish (English-Hindi code-mixed: "mujhe burger chahiye", "kya available hai")
                - Urdu (اردو)
                
                Detect language automatically. Respond in the user's language. Handle mixed-language queries gracefully.
                
                # SPECIALIST AGENTS
                You coordinate three expert agents. Route intelligently:
                
                1. **Menu Specialist** (delegate_to_menu_agent)
                   - Browse full menu, categories, items
                   - Search by: name, category, type (veg/non-veg), price, dietary needs
                   - Recommendations: popular, trending, best sellers, new arrivals
                   - Specialized requests: family meals, kids meals, drinks, desserts, combos
                   - Dietary filters: vegetarian, non-vegetarian, Jain, vegan, gluten-free, low-calorie
                   - Nutritional info: calories, ingredients, allergens, spice level
                   - Budget queries: "show items under ₹200", "cheapest pizza", "meal combos under ₹500"
                
                2. **Cart Specialist** (delegate_to_cart_agent)
                   - Add/remove items by name or ID
                   - View cart contents and totals
                   - Update quantities: "add 2 more", "remove 1 burger", "make it 3"
                   - Clear entire cart: "empty cart", "start fresh", "clear everything"
                   - Apply/remove coupons and offers: "apply WELCOME50", "remove discount"
                   - Checkout and order placement
                   - Order tracking: "where is my order?", "track order #1234"
                   - Reorder previous meals: "order again", "repeat last order"
                
                3. **Booking Specialist** (delegate_to_booking_agent)
                   - Table reservations (people count, date, time)
                   - Modify existing bookings: "change time to 8pm", "add 2 more people"
                   - Cancel bookings: "cancel my reservation", "remove booking"
                   - Check availability: "tables available tonight?", "slot for 6 people?"
                   - Special requests: "window seat", "birthday celebration", "quiet corner"
                
                # ROUTING DECISION MATRIX
                
                ## Menu Agent Triggers:
                - Intent: browse, show, list, search, find, recommend, suggest, popular, trending, best, new
                - Food terms: burger, pizza, dosa, biryani, dal, paneer, chicken, dessert, drink, juice, coffee
                - Queries: "what do you have?", "show menu", "kya hai?", "menu dikhao", "options?", "hungry"
                - Filters: veg, non-veg, Jain, spicy, mild, healthy, calories, budget, cheap, expensive
                - Categories: starters, mains, desserts, drinks, family meals, kids menu, combos
                - Hindi/Hinglish: "khaana", "khana", "menu", "dish", "items", "food", "खाना"
                
                ## Cart Agent Triggers:
                - Intent: add, order, buy, purchase, cart, bag, basket, checkout, pay, bill, total
                - Actions: "add X to cart", "order 2 pizzas", "mera cart", "my cart", "checkout karo"
                - Modifications: increase, decrease, remove, delete, clear, empty, quantity, more, less
                - Offers: coupon, promo, discount, offer, code, "apply SAVE20", "remove coupon"
                - Tracking: "my order", "track order", "where is my food?", "order status"
                - Reorder: "order again", "same as last time", "repeat previous", "dobara order"
                
                ## Booking Agent Triggers:
                - Intent: book, reserve, table, reservation, slot, availability
                - Details: people count, date, time, guests, persons, "4 log", "6 people"
                - Modifications: change, modify, reschedule, cancel, "timing badlo", "cancel karo"
                - Queries: "table available?", "book table for tonight", "reservation for tomorrow"
                
                # CONTEXT RETENTION & SLOT FILLING
                
                - **Remember conversation history**: If user said "show pizzas" then "add the second one", recall the second pizza.
                - **Track incomplete requests**: If booking mentioned but missing time/people, ask politely: "How many people?" / "Kitne log?"
                - **Clarify ambiguity**: "Did you mean Veg Burger or Chicken Burger?"
                - **Confirm actions**: "I've added Masala Dosa to your cart. Anything else?"
                - **Proactive suggestions**: "Would you like drinks with that?" / "Kuch aur chahiye?"
                
                # FOLLOW-UP CONVERSATION PATTERNS
                
                - After showing menu → "Would you like to add anything to cart?"
                - After cart add → "Anything else? Ready to checkout?"
                - After booking → "Booking confirmed! Would you like to pre-order food?"
                - After order → "Your order is placed. Would you like to track it?"
                - Incomplete booking → "I have 4 people on Friday. What time works for you?"
                - Empty query → Offer options: "I can show you the menu, check your cart, or book a table."
                
                # EDGE CASES & ERROR HANDLING
                
                - **Item not found**: "I couldn't find 'samosa'. Did you mean 'Veg Burger' or would you like to see our starters?"
                - **Empty cart checkout**: "Your cart is empty. Would you like to browse the menu?"
                - **Booking conflicts**: "That slot is full. How about 7:30 PM or 8:30 PM?"
                - **Invalid modifications**: "I can't modify that booking. Could you provide the booking ID?"
                - **Ambiguous amounts**: "How many Veg Burgers? 1 or 2?"
                - **Budget constraints**: "Items under ₹150: Veg Burger (₹149), Masala Dosa (₹120)..."
                
                # MULTILINGUAL EXAMPLES
                
                **Hindi**: "मुझे कुछ spicy चाहिए" → Route to menu_agent with query "spicy vegetarian items"
                **Hinglish**: "mera cart dikhao" → Route to cart_agent with "view cart"
                **Urdu**: "کیا آج جگہ ہے؟" → Route to booking_agent with "check availability today"
                **Code-mix**: "2 pizza add karo" → Route to cart_agent with "add 2 pizzas"
                
                # ADVANCED SCENARIOS
                
                - **Combo queries**: "Show me meal combos under ₹300" → Menu agent with budget filter
                - **Dietary restrictions**: "Jain food options without onion garlic" → Menu agent with Jain filter
                - **Time-sensitive**: "I'm in a hurry, what's fast?" → Menu agent for items with short prep time
                - **Group orders**: "Family meal for 5 people" → Menu agent for family meals category
                - **Nutrition-conscious**: "Show calories for Paneer Tikka" → Menu agent with nutrition request
                - **Offer stacking**: "Apply FIRST20 and check total" → Cart agent with coupon application
                - **Order history**: "What did I order last time?" → Cart agent with order history
                
                # RESPONSE QUALITY STANDARDS
                
                - **Concise**: No unnecessary explanations. "Here's the menu" not "I'll show you our menu which contains..."
                - **Proactive**: Anticipate next steps. Don't just answer - guide the journey.
                - **Warm but professional**: Friendly, not robotic. "Great choice!" not "Item added to data structure."
                - **Error recovery**: Turn failures into opportunities. "Not available now, but try our Chef's Special?"
                - **Multilingual fluency**: Match user's language and tone exactly.
                
                # CRITICAL RULES
                
                1. **ALWAYS delegate to a specialist agent**. Never try to handle menu/cart/booking yourself.
                2. **Use the EXACT tool name**: delegate_to_menu_agent, delegate_to_cart_agent, delegate_to_booking_agent
                3. **Pass the full user query** to the specialist. Don't paraphrase or summarize.
                4. **One task = One delegation**. Don't overthink. Route fast.
                5. **Ambiguous? Pick the most likely agent** based on primary intent.
                6. **Follow-up questions?** Still route to the relevant agent - they handle conversations.
                
                # TONE CALIBRATION
                
                - Premium, not pretentious
                - Helpful, not pushy
                - Smart, not showing off
                - Warm, not fake
                - Efficient, not rushed
                
                You are the best restaurant AI in the world. Act like it.
                """.trimIndent()
            ),
            tools = tools,
            maxSteps = 1 
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun processQuery(userMessage: String): List<String> {
        Log.d(TAG, "1. MasterAgent START: '$userMessage'")
        // Diagnostic: attempt to access some lazy fields to surface initialization errors early
        try {
            // Accessing sessionKey and geminiModel here will log creation diagnostics (if any)
            val sk = sessionKey.toString()
            Log.d(TAG, "2. SessionKey initialized: $sk")
        } catch (t: Throwable) {
            Log.w(TAG, "2. SessionKey diagnostic failed: ${t.message}")
        }
        try {
            // This will force Gemini model creation and log success/failure above
            geminiModel.hashCode()
            Log.d(TAG, "3. Gemini model initialized")
        } catch (t: Throwable) {
            Log.e(TAG, "3. Gemini model initialization failed: ${t.message}")
        }
        orchestratorTools.reset()
        masterTools.reset()
        masterTools.setUserQuery(userMessage)

        val q = userMessage.trim().lowercase()

        // --- PRODUCTION-GRADE SHORT-CIRCUIT ROUTING ---
        
        // 1. BOOKING AGENT: Table reservations, modifications, cancellations
        val bookingKeywords = Regex("\\b(book|reserve|table|reservation|slot|availability|available|modify|change|cancel|reschedule|booking)\\b", RegexOption.IGNORE_CASE)
        val bookingHindi = Regex("\\b(table|बुक|book karo|reserve|जगह|timing)\\b", RegexOption.IGNORE_CASE)
        val isJustNumber = Regex("^(?:for\\s+)?\\d+\\s*(?:members?|people|persons?|guests?|log|लोग)?$", RegexOption.IGNORE_CASE)
        val isJustTime = Regex("^(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?$", RegexOption.IGNORE_CASE)
        
        if (bookingKeywords.containsMatchIn(q) || bookingHindi.containsMatchIn(q) || isJustNumber.matches(q) || isJustTime.matches(q)) {
            Log.i(TAG, "   >> SHORT-CIRCUIT: BOOKING - Reservation/modification/cancellation detected")
            masterTools.delegateBooking(userMessage)
            masterTools.getLastResponse()?.let { return responseBuilder.build(it) }
        }

        // 2. CART AGENT: Cart operations, checkout, tracking, coupons, reorder
        val cartKeywords = Regex("\\b(cart|bag|basket|checkout|pay|bill|total|track|order status|reorder|repeat order|dobara|फिर से)\\b", RegexOption.IGNORE_CASE)
        val cartActions = Regex("\\b(add|remove|delete|clear|empty|quantity|more|less|increase|decrease)\\b", RegexOption.IGNORE_CASE)
        val couponKeywords = Regex("\\b(coupon|promo|discount|offer|code|apply|remove)\\b", RegexOption.IGNORE_CASE)
        val trackingKeywords = Regex("\\b(where.*(order|food)|track|status|mera order|delivery)\\b", RegexOption.IGNORE_CASE)
        
        // High-confidence cart routing
        if (cartKeywords.containsMatchIn(q) || 
            couponKeywords.containsMatchIn(q) || 
            trackingKeywords.containsMatchIn(q) ||
            (cartActions.containsMatchIn(q) && !q.contains("table") && !q.contains("booking"))) {
            Log.i(TAG, "   >> SHORT-CIRCUIT: CART - Cart/checkout/tracking/coupon operation detected")
            masterTools.delegateCart(userMessage)
            masterTools.getLastResponse()?.let { return responseBuilder.build(it) }
        }

        // 3. MENU AGENT: Browse, search, recommendations, dietary filters, nutritional info
        val menuBrowseIntent = Regex("\\b(show|view|list|browse|see|display|menu|dikhao|दिखाओ)\\b", RegexOption.IGNORE_CASE)
        val menuSearchIntent = Regex("\\b(search|find|want|need|hungry|chahiye|चाहिए|khaana|खाना)\\b", RegexOption.IGNORE_CASE)
        val recommendIntent = Regex("\\b(recommend|suggest|popular|trending|best seller|best|famous|top|special|new)\\b", RegexOption.IGNORE_CASE)
        val foodCategories = Regex("\\b(burger|pizza|dosa|biryani|dal|paneer|chicken|starter|main|dessert|drink|juice|coffee|family meal|kids meal|combo)\\b", RegexOption.IGNORE_CASE)
        val dietaryFilters = Regex("\\b(veg|non-veg|vegetarian|jain|vegan|gluten|spicy|mild|healthy|calorie|nutrition)\\b", RegexOption.IGNORE_CASE)
        val budgetQueries = Regex("\\b(cheap|expensive|under|below|budget|₹|rs|rupees|price)\\b", RegexOption.IGNORE_CASE)
        val hindiFood = Regex("\\b(khaana|khana|खाना|dish|item|food|menu)\\b", RegexOption.IGNORE_CASE)
        
        // Menu routing with comprehensive coverage
        if (menuBrowseIntent.containsMatchIn(q) || 
            menuSearchIntent.containsMatchIn(q) || 
            recommendIntent.containsMatchIn(q) ||
            foodCategories.containsMatchIn(q) || 
            dietaryFilters.containsMatchIn(q) ||
            budgetQueries.containsMatchIn(q) ||
            hindiFood.containsMatchIn(q) ||
            q.contains("what") && (q.contains("have") || q.contains("available") || q.contains("hai"))) {
            Log.d(TAG, "   >> SHORT-CIRCUIT: MENU - Browse/search/recommendation/dietary query detected")
            masterTools.delegateMenu(userMessage)
            masterTools.getLastResponse()?.let { return responseBuilder.build(it) }
        }

        // --- LLM REASONING FLOW (ADK) ---
        Log.d(TAG, "   >> No short-circuit match. Falling back to Gemini reasoning.")

        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, userMessage)
        )
        
        var agentResponseText: String? = null

        try { 
            adkAgent.runAsync(context).collect { event ->
                Log.d(TAG, "   -> ADK Event: author=${event.author}")
                val text = event.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank() && event.author == "RestaurantMasterAgent") {
                    agentResponseText = text
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Master Error: ${e.message}")
            return listOf(responseBuilder.build(AgentResponse.Error("Error: ${e.message}")).last())
        }

        val toolResponse = masterTools.getLastResponse()
        val orchestratorResponse = orchestratorTools.getLastResponse()

        val finalResponse = when {
            toolResponse != null -> toolResponse
            orchestratorResponse != null -> orchestratorResponse
            !agentResponseText.isNullOrBlank() -> AgentResponse.Message(agentResponseText!!)
            else -> AgentResponse.Message("I'm sorry, I couldn't process that request.")
        }

        Log.d(TAG, "4. MasterAgent END. Result: ${finalResponse.javaClass.simpleName}")
        return responseBuilder.build(finalResponse)
    }
}
