# 🎯 QUICK IMPLEMENTATION - ADK SETUP (15 minutes)

## Step 1: Get Gemini API Key (2 minutes)

1. Go to [Google AI Studio](https://ai.google.dev/tutorials/setup)
2. Click "Get API Key"
3. Copy your key

## Step 2: Local Configuration (2 minutes)

```properties
# local.properties (add this line)
GEMINI_API_KEY=your_api_key_here
```

## Step 3: Copy New File (1 minute)

Copy **ADKIntegration.kt** to:
```
app/src/main/java/com/example/restaurant/application/agents/
```

## Step 4: Update ViewModel (5 minutes)

Replace your `RestaurantViewModel.kt` sendMessage with:

```kotlin
fun sendMessage(query: String) {
    if (query.isBlank()) return

    val correlationId = RequestTracer.startRequest()
    Logger.logRequestReceived(correlationId, query)

    _uiMessages.add(UiMessage(content = query, isFromAgent = false))

    viewModelScope.launch {
        try {
            // NEW: Use ADK Agent with Gemini
            Log.d("VM", "Sending to ADK agent: $query")
            
            // Initialize ADK agent on first use
            if (!::adkMasterAgent.isInitialized) {
                adkMasterAgent = ADKRestaurantMasterAgent(
                    localRepository,
                    ADKRestaurantAgentTools(localRepository)
                )
            }
            
            val a2uiMessages = adkMasterAgent.processQuery(query)

            _uiMessages.add(
                UiMessage(
                    content = "🤖 ",
                    isFromAgent = true,
                    isA2UI = true,
                    a2uiPayloads = a2uiMessages
                )
            )

            Logger.info(correlationId, "ViewModel", "Response added to UI")
            RequestTracer.endRequest()
        } catch (e: Exception) {
            Log.e("VM", "Error: ${e.message}", e)
            _uiMessages.add(
                UiMessage(
                    content = "❌ Error: ${e.message}",
                    isFromAgent = true
                )
            )
            RequestTracer.endRequest()
        }
    }
}
```

Add to top of class:

```kotlin
private lateinit var adkMasterAgent: ADKRestaurantMasterAgent

// Imports needed:
import com.example.restaurant.application.agents.ADKRestaurantMasterAgent
import com.example.restaurant.application.agents.ADKRestaurantAgentTools
```

## Step 5: Build & Test (5 minutes)

```bash
# Clean build
./gradlew.bat clean

# Build
./gradlew.bat build

# Install
./gradlew.bat installDebug
```

## Test Cases

### Test 1: Simple Menu Search
```
User: "Show menu"
Expected: Menu items appear (using Gemini)
```

### Test 2: Add Item
```
User: "Add paneer tikka to cart"
Expected: Item added (Gemini understands even with typos)
```

### Test 3: Book Table
```
User: "Book table for 4"
Expected: Multi-step dialog
  Bot: "What time?"
User: "5 PM"
Expected: Booking confirmed
```

### Test 4: Complex Query
```
User: "Show me vegetarian items under 200"
Expected: Filtered menu using Gemini
```

---

## ✅ Verification Checklist

- [ ] API Key added to local.properties
- [ ] ADKIntegration.kt copied
- [ ] ViewModel updated with new code
- [ ] Build succeeds: `./gradlew.bat build`
- [ ] App starts without crashes
- [ ] Chat screen loads
- [ ] Logcat shows "ADK_AGENT" logs
- [ ] User message sent
- [ ] Gemini reasoning happens (~1-2 seconds)
- [ ] Response appears in chat
- [ ] AI2UI rendering works
- [ ] Multiple turns work (multi-step dialog)

---

## 🔍 Troubleshooting

### "ADK Error: API Key invalid"
→ Check local.properties has correct key
→ Verify key is from Google AI Studio

### "Module not found" for ADKIntegration
→ Check package path: `com.example.restaurant.application.agents`
→ File should be in: `app/src/main/java/com/example/restaurant/application/agents/ADKIntegration.kt`

### "Slow response" (3+ seconds)
→ Normal for first call (Gemini warm-up)
→ Subsequent calls faster (~500ms)

### "Only plain text, no A2UI"
→ Check A2UIResponseBuilder is imported
→ Verify response types match AgentResponse

---

## 🎯 What Happens Now

**User Types**: "Book table for 5 at 5 PM"
```
1. Chat captures message
2. Calls: adkMasterAgent.processQuery()
3. ADK Framework:
   - Creates InvocationContext
   - Sends to Gemini LLM
   - Gemini: "This is a booking request"
   - Gemini selects: book_table_step tool
   - Gemini extracts: people=5, time=5PM
4. Tool execution:
   - Calls: bookTableStep("complete", "5", "5PM")
   - Creates booking in database
   - Sets response
5. Response rendering:
   - A2UIResponseBuilder formats
   - Returns A2UI JSON
6. Chat display:
   - Shows confirmation card
   - "✅ Table booked! ID: RES-12345"
```

---

## 💯 You Now Have

✅ Real Google ADK
✅ Gemini LLM reasoning  
✅ Professional AI assistant
✅ Multi-turn dialog support
✅ Structured tool execution
✅ A2UI rendering
✅ Production-ready system

**Deploy-ready in 15 minutes!** 🚀

---

**Next: Follow ADK_REAL_INTEGRATION.md for detailed setup**

