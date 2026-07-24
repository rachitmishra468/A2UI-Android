package com.example.a2ui_sample.agent

import android.util.Log
import com.example.a2ui_sample.data.AgentResponse
import com.example.a2ui_sample.data.MenuRepository
import com.google.adk.kt.agents.InvocationContext
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.sessions.Session
import com.google.adk.kt.sessions.SessionKey

import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Role
import com.example.a2ui_sample.BuildConfig
import com.google.adk.kt.agents.Instruction
import kotlin.time.ExperimentalTime


/**
 * RestaurantAgent
 * Principal Architect Implementation using Google ADK.
 * Orchestrates reasoning via Gemini and execution via Tools.
 */
class RestaurantAgent(private val repository: MenuRepository) {

    private val restaurantTools = RestaurantTools(repository)
    private val responseBuilder = A2UIResponseBuilder()

    // Gemini Engine Configuration
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val geminiModel = Gemini("gemini-3.6-flash", apiKey)

    @OptIn(ExperimentalTime::class)
    private val sessionKey = SessionKey("RestaurantApp", "DefaultUser", "Session-Restaurant")
    @OptIn(ExperimentalTime::class)
    private val session = Session(sessionKey)


     /**
      * ADK LlmAgent Definition
      */
     private val adkAgent = LlmAgent(
         name = "RestaurantAgent",
         model = geminiModel,
         instruction = Instruction(
             """
            You are a Restaurant Ordering Assistant.

            RULES (must follow exactly):
            1) Always return exactly one tool invocation and nothing else. Do NOT return plain text.
            2) If the user message contains any of the verbs: add, order, buy, put (case-insensitive) followed by a food phrase, ALWAYS call add_item_to_cart(itemName="<extracted item>").
               - Extract the item name after the verb. Remove words like "in my cart", "to my cart", "please", etc.
               - Normalize to Title Case (e.g., "masala dosa" -> "Masala Dosa").
               - Do NOT call get_full_menu for these add/order/buy/put intents.
               - If you cannot extract any food phrase after the verb, call search_menu(query="<original user text>").
            3) If the user asks to view or show cart (contains "cart" or "show cart"), call view_cart().
            4) If the user explicitly asks to see the entire menu (contains phrases like "show menu", "menu items", "what can I order", "show all items"), call get_full_menu().
            5) If the user requests filters/specific categories or ambiguous item names (e.g., "vegetarian", "pizza under 300", "spicy paneer"), call search_menu(query="<extracted phrase>").
            6) If the user asks for suggestions, call get_recommendations(criteria="<extracted>") or get_recommendations() when no criteria.
            7) If the user wants to book a table/reservation (contains "book", "reserve", "table"), call book_table_step(step="ask_people").
            8) If the user has provided a number of people, call book_table_step(step="ask_time", numberOfPeople="<number>").
            9) If the user has provided a time for booking, call book_table_step(step="complete", numberOfPeople="<number>", bookingTime="<time>").

            Examples (follow these EXACTLY):
              User: add masala dosa in my cart
              Tool: add_item_to_cart(itemName="Masala Dosa")

              User: show menu
              Tool: get_full_menu()

              User: show my cart
              Tool: view_cart()

              User: vegetarian items
              Tool: search_menu(type="veg")

              User: book a table
              Tool: book_table_step(step="ask_people")

              User: 4 people
              Tool: book_table_step(step="ask_time", numberOfPeople="4")

              User: 8:00 PM
              Tool: book_table_step(step="complete", numberOfPeople="4", bookingTime="8:00 PM")

            IMPORTANT: Under no circumstances return get_full_menu() in response to an add/order/buy/put user message.
             """.trimIndent()
          ),
          tools = listOf(
              AddItemToCartTool(restaurantTools),
              ViewCartTool(restaurantTools),
              SearchMenuTool(restaurantTools),
              GetRecommendationsTool(restaurantTools),
              GetFullMenuTool(restaurantTools),
              BookTableStepTool(restaurantTools)
          ),
         maxSteps = 1
     )

    @OptIn(ExperimentalTime::class)
    suspend fun processQuery(query: String): List<String> {
        Log.d("A2UI_FLOW", "3. Agent starting reasoning for query: '$query'")
        
        // NOTE: Do not reset tools here; keep tool state between turns to support
        // multi-step flows (e.g., table booking). Previously this reset cleared
        // bookingState and caused continuation turns like "4 people" to lose context.
        // restaurantTools.reset()

        // -- QUICK RULE-BASED CONTINUATION: If we have an ongoing booking step,
        // handle the user's reply locally to continue the booking flow.
        val prev = restaurantTools.getLastResponse()
        if (prev is AgentResponse.BookingRequest) {
            try {
                when (prev.step) {
                    "ask_people" -> {
                        // Expecting a number of people in the user's reply
                        val numMatch = Regex("(\\d{1,2})\\s*(?:people|persons|guests)?", RegexOption.IGNORE_CASE).find(query)
                        val number = numMatch?.groupValues?.get(1)
                        if (!number.isNullOrBlank()) {
                            Log.d("A2UI_FLOW", "   >> Continuing booking: received numberOfPeople=$number")
                            val res = restaurantTools.bookTableStep("ask_time", numberOfPeople = number)
                            val toolResponse = restaurantTools.getLastResponse() ?: AgentResponse.Message(res)
                            return responseBuilder.build(toolResponse)
                        }
                    }
                    "ask_time" -> {
                        // Expecting a booking time (e.g., "8:00 PM" or "8 pm")
                        val timeMatch = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)", RegexOption.IGNORE_CASE).find(query)
                        val time = timeMatch?.groupValues?.get(1)
                        val pendingNumber = restaurantTools.getPendingBookingNumber()
                        if (!time.isNullOrBlank() && !pendingNumber.isNullOrBlank()) {
                            Log.d("A2UI_FLOW", "   >> Continuing booking: completing for $pendingNumber at $time")
                            val res = restaurantTools.bookTableStep("complete", numberOfPeople = pendingNumber, bookingTime = time)
                            val toolResponse = restaurantTools.getLastResponse() ?: AgentResponse.Message(res)
                            return responseBuilder.build(toolResponse)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("A2UI_FLOW", "   !! Error continuing booking flow: ${e.message}")
                // fall through to normal agent flow
            }
        }

        // -- QUICK RULE-BASED SHORT-CIRCUIT: if the user clearly intends to add an item,
        // -- handle it locally instead of relying on the LLM to pick the right tool.
        // This prevents the model from accidentally calling get_full_menu for add-item intents.
        val addIntent = Regex("\\b(add|order|buy|put)\\b", RegexOption.IGNORE_CASE)
        if (addIntent.containsMatchIn(query)) {
            // Extract probable item phrase after the verb
            val extract = Regex("(?i)\\b(add|order|buy|put)\\b\\s+(.*)")
            val m = extract.find(query.trim())
            val rawItem = m?.groupValues?.get(2)?.replace("in my cart", "", ignoreCase = true)
                ?.replace("to my cart", "", ignoreCase = true)
                ?.replace("please", "", ignoreCase = true)
                ?.trim() ?: ""

            if (rawItem.isNotBlank()) {
                Log.d("A2UI_FLOW", "   >> Detected add-item intent. Extracted item='$rawItem' - calling tool directly.")
                try {
                    val result = restaurantTools.addItemToCart(rawItem)
                    val toolResponse = restaurantTools.getLastResponse() ?: AgentResponse.Message(result)
                    return responseBuilder.build(toolResponse)
                } catch (e: Exception) {
                    Log.e("A2UI_FLOW", "   !! Error executing addItemToCart directly: ${e.message}")
                    // fall through to normal agent flow
                }
            }
        }

         // -- QUICK RULE-BASED SHORT-CIRCUIT: if the user asks to view/show cart,
         // -- handle it directly to prevent the LLM from calling get_full_menu.
         val cartIntent = Regex("\\b(show|view|my)\\s+(cart|shopping)\\b", RegexOption.IGNORE_CASE)
         if (cartIntent.containsMatchIn(query) || query.trim().lowercase() == "cart") {
             Log.d("A2UI_FLOW", "   >> Detected cart view intent - calling view_cart directly.")
             try {
                 restaurantTools.viewCart()
                 val toolResponse = restaurantTools.getLastResponse() ?: AgentResponse.Message("No items in cart")
                 return responseBuilder.build(toolResponse)
             } catch (e: Exception) {
                 Log.e("A2UI_FLOW", "   !! Error executing viewCart directly: ${e.message}")
                 // fall through to normal agent flow
             }
         }

         // -- QUICK RULE-BASED SHORT-CIRCUIT: if the user asks to book a table,
         // -- handle the booking flow step-by-step.
         val bookingKeywords = Regex("\\b(book|reserve|reservation|table)\\b", RegexOption.IGNORE_CASE)
         if (bookingKeywords.containsMatchIn(query)) {
             Log.d("A2UI_FLOW", "   >> Detected booking intent - starting table booking flow.")
             try {
                 restaurantTools.bookTableStep("ask_people")
                 val toolResponse = restaurantTools.getLastResponse() ?: AgentResponse.Message("How many people?")
                 return responseBuilder.build(toolResponse)
             } catch (e: Exception) {
                 Log.e("A2UI_FLOW", "   !! Error starting booking: ${e.message}")
                 // fall through to normal agent flow
             }
         }

          // -- QUICK RULE-BASED SHORT-CIRCUIT: menu/category/type filters
          // If user explicitly requests filtered menu (veg/non-veg/categories/price),
          // handle it locally to avoid LLM calling get_full_menu.
          try {
              val filterKeywords = listOf("veg","vegetarian","veggie","non-veg","non veg","nonvegetarian","non vegetarian","meat","dessert","desserts","beverage","beverages","snack","snacks","pizza","burger","biryani","dosa","rice","appetizer","meal","curry","curries","south indian")
              if (filterKeywords.any { kw -> query.contains(kw, ignoreCase = true) }) {
                  Log.d("A2UI_FLOW", "   >> Detected menu filter intent - extracting filters from query: '$query'")

                  // Determine type param
                  val typeParam = when {
                      Regex("\\b(veg|vegetarian|veggie)\\b", RegexOption.IGNORE_CASE).containsMatchIn(query) -> "Veg"
                      Regex("\\b(non[- ]?veg|nonvegetarian|non vegetarian|meat)\\b", RegexOption.IGNORE_CASE).containsMatchIn(query) -> "Non-Veg"
                      else -> null
                  }

                  // Determine category param (find first matching category)
                  val categories = listOf("pizza","burger","biryani","dosa","rice","appetizer","meal","dessert","beverage","snack","curries","south indian","meal","burger")
                  val categoryParam = categories.firstOrNull { c -> query.contains(c, ignoreCase = true) }

                  // Extract max price if present (e.g., "under 300")
                  val priceRegex = Regex("(?:under|below|less than|up to|upto)\\s*(\\d{2,5})", RegexOption.IGNORE_CASE)
                  val priceMatch = priceRegex.find(query)
                  val maxPrice = priceMatch?.groupValues?.get(1)?.toIntOrNull()

                  Log.d("A2UI_FLOW", "   >> Filters parsed: category=$categoryParam type=$typeParam maxPrice=$maxPrice")

                  val result = restaurantTools.searchMenu(categoryParam, typeParam, maxPrice)
                  val toolResponse = restaurantTools.getLastResponse() ?: AgentResponse.Message(result)
                  return responseBuilder.build(toolResponse)
              }
          } catch (e: Exception) {
              Log.e("A2UI_FLOW", "   !! Error executing menu filter short-circuit: ${e.message}")
              // fall through to normal agent flow
          }

        // 1. Create ADK Invocation Context
        val context = InvocationContext(
            session = session,
            agent = adkAgent,
            userContent = Content.fromText(Role.USER, query)
        )

        var agentResponseText: String? = null

        // 3. Run the Agent Loop
        try {
            // Collecting events to trigger reasoning and tool execution
            adkAgent.runAsync(context).collect { event ->
                Log.d("A2UI_FLOW", "   -> ADK Event: author=${event.author}")
                Log.d("A2UI_FLOW", event.toString())

                // Capture the text response from the agent
                val text = event.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank() && event.author == "RestaurantAgent") {
                    agentResponseText = text
                }


            }
        } catch (e: Exception) {
            Log.e("A2UI_FLOW", "   !! ADK Error during reasoning: ${e.message}")
            return listOf(responseBuilder.build(AgentResponse.Error("ADK Error: ${e.message}")).last())
        }

        // 4. Retrieve the structured response captured by the tools OR the model's text
        val toolResponse = restaurantTools.getLastResponse()
        val finalResponse = when {
            toolResponse != null -> toolResponse
            !agentResponseText.isNullOrBlank() -> AgentResponse.Message(agentResponseText)
            else -> AgentResponse.Message("I'm sorry, I couldn't process that request. Try asking for the menu.")
        }
        
        Log.d("A2UI_FLOW", "4. Reasoning finished. Response generated: $finalResponse")
        
        // 5. Build A2UI JSON messages
        return responseBuilder.build(finalResponse)
    }
}
