# Complete Query Flow: A2UI ADK Agent System



```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER INPUT (Query)                          │
│                   "Show me pizza and add to cart"                   │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                     UI Layer: AssistantChatScreen                   │
│  • TextField captures user input                                    │
│  • Displays chat messages (bubbles)                                 │
│  • Shows different UI states (cards, lists, etc.)                  │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│               ViewModel: AssistantViewModel                         │
│  • Receives message via sendMessage(text)                          │
│  • Saves user message to database                                  │
│  • Shows loading state (isTyping = true)                           │
│  • Calls orchestrator.processQuery(text)                           │
│  • Saves AI responses to database                                  │
│  • Loads chat history from database                                │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│          Orchestrator: AssistantOrchestrator                        │
│  • Receives query from ViewModel                                   │
│  • Creates InMemoryRunner for RootAgent                            │
│  • Executes multi-agent system pipeline                            │
│  • Handles transfer_to_agent events (delegation)                   │
│  • Maps events to UI states                                        │
│  • Returns List<AssistantUiState> to ViewModel                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│           Multi-Agent System (powered by ADK)                       │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │  RootRestaurantAgent (LlmAgent: "RestaurantMaster")        │  │
│  │  • LLM: Gemini 3.1 Flash Lite                              │  │
│  │  • Role: Router & Orchestrator                             │  │
│  │  • Job: Analyze query & delegate to specialists            │  │
│  │  • Max Steps: 3 (can chain multiple transfers)             │  │
│  │  • Sub-Agents: 5 specialized agents                        │  │
│  └──────────────────┬──────────────────────────────────────────┘  │
│                     ↓                                               │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │         ROOT AGENT DECISION LOGIC                          │  │
│  │                                                             │  │
│  │  1. Analyze  user query                         │  │
│  │  2. Identify ALL separate tasks                            │  │
│  │  3. For each task: transfer_to_agent("SpecialistName")    │  │
│  │                                                             │  │
│  │  Example: User says "Show pizza AND add to cart"           │  │
│  │  • Task 1: transfer_to_agent("MenuAssistant", "Show pizza")│  │
│  │  • Task 2: transfer_to_agent("CartAssistant", "Add pizza") │  │
│  └──────────────────┬──────────────────────────────────────────┘  │
│                     ↓                                               │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │  SPECIALIZED SUB-AGENTS (Single Responsibility)             │  │
│  │                                                              │  │
│  │  1️⃣ MenuAssistant                                            │  │
│  │     • Search menu items by query/category/dietary type      │  │
│  │     • Get detailed info on specific items                   │  │
│  │     • Show recommendations                                  │  │
│  │     • Tools: search_menu, get_menu_details                  │  │
│  │                                                              │  │
│  │  2️⃣ CartAssistant                                            │  │
│  │     • Add items to cart with quantity                        │  │
│  │     • Remove items from cart                                │  │
│  │     • View current cart                                     │  │
│  │     • Update quantities                                     │  │
│  │     • Tools: add_to_cart, remove_from_cart, view_cart       │  │
│  │                                                              │  │
│  │  3️⃣ BookingAssistant                                         │  │
│  │     • Check table availability                              │  │
│  │     • Book table for specific date/time/party size          │  │
│  │     • Tools: check_availability, book_table                 │  │
│  │                                                              │  │
│  │  4️⃣ OrderAssistant                                           │  │
│  │     • Checkout and place order                              │  │
│  │     • Track order status                                    │  │
│  │     • Tools: checkout, get_order_status                     │  │
│  │                                                              │  │
│  │  5️⃣ FeedbackAssistant                                        │  │
│  │     • Collect user feedback/ratings                         │  │
│  │     • Submit feedback                                       │  │
│  │     • Tools: submit_feedback                                │  │
│  └──────────────────┬──────────────────────────────────────────┘  │
│                     ↓                                               │
└─────────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│              Tool Execution Layer                                   │
│                                                                     │
│  Each agent can call multiple tools to fulfill request              │
│  Tools interact with:                                               │
│  • Local Database (Room SQLite)                                     │
│  • Backend APIs (REST calls)                                        │
│  • Business Logic Layer (Use Cases)                                 │
│                                                                     │
│  Example Tool Chain:                                                │
│  search_menu("pizza") → Query DB/API → Returns MenuItems[]        │
│                     ↓                                               │
│  add_to_cart(itemId=1) → Add to Cart DB → Returns CartUpdate      │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│         Event Stream Processing (in Orchestrator)                  │
│                                                                     │
│  1. Collect all events from ADK runner                             │
│  2. For each event:                                                │
│     - If it's a transfer_to_agent: spawn sub-agent                │
│     - Extract function calls and responses                         │
│     - Map to AssistantUiState                                      │
│  3. Return ordered list of UI states                               │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│        UI State Mapping (AssistantUiMapper)                        │
│                                                                     │
│  Event ──→ AssistantUiState                                        │
│                                                                     │
│  Types of UI States:                                               │
│  • TextResponse: Simple text messages                              │
│  • MenuSearch: Horizontal list of menu items                       │
│  • Recommendations: Featured items list                            │
│  • MenuDetails: Full item details card                             │
│  • CartUpdate: Added/removed item confirmation                     │
│  • CartView: Full cart contents                                    │
│  • BookingResult: Booking confirmation                             │
│  • OrderStatus: Order tracking card                                │
│  • CheckoutSummary: Final order summary                            │
│  • FeedbackResult: Feedback confirmation                           │
│  • Error: Error message with recovery                              │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│           ViewModel Saves Responses to Database                     │
│                                                                     │
│  For each UI state:                                                │
│  • Serialize to JSON                                               │
│  • Wrap with type metadata                                         │
│  • Save to ChatMessageEntity                                       │
│  • Trigger database flow update                                    │
└──────────────────────────────┬──────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│           UI Renders Messages & Chat History                        │
│                                                                     │
│  AssistantChatScreen:                                              │
│  • Observes messages from ViewModel                                │
│  • LazyColumn displays all messages                                │
│  • Each message rendered via AssistantMessageBubble                │
│  • Bubble type based on UI state:                                  │
│    - TextBubble: Simple text in rounded container                  │
│    - MenuHorizontalList: Scrollable items                          │
│    - CartCard: Cart contents with prices                           │
│    - CheckoutCard: Order summary with total                        │
│  • History persisted in local SQLite                               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Detailed Step-by-Step Flow

### Step 1: User Types Query & Sends
```kotlin
// User in UI: Types "Show me pizza and add one to cart"
// Taps Send button

AssistantChatScreen:
    sendMessage(textState)  // Calls ViewModel
    textState = ""          // Clears input
```

### Step 2: ViewModel Captures & Processes
```kotlin
// AssistantViewModel.sendMessage(text)

1. Create user message:
   userMsg = AssistantChatMessage(
       content = TextResponse("Show me pizza and add one to cart"),
       isFromUser = true
   )

2. Save to database:
   chatMessageDao.insertMessage(ChatMessageEntity(...))

3. Show loading state:
   isTyping = true

4. Call orchestrator:
   val uiStates = orchestrator.processQuery(text)

5. For each UI state returned:
   - Create assistant message
   - Save to database
   - Flow observer updates UI

6. Hide loading:
   isTyping = false
```

### Step 3: Orchestrator Initializes ADK Runner
```kotlin
// AssistantOrchestrator.processQuery(query)

1. Create InMemoryRunner:
   val runner = InMemoryRunner(rootAgent.adkAgent)

2. Execute root agent:
   val events = runner.runAsync(
       userId = "user-1",
       sessionId = "session-1",
       newMessage = Content.fromText(Role.USER, query)
   ).toList()

3. The ADK engine:
   - Sends query to LLM (Gemini)
   - LLM analyzes intent
   - LLM generates tool calls or transfer_to_agent decisions
   - Returns stream of events
```

### Step 4: Root Agent Analyzes & Routes
```
LLM Thinking (in RootRestaurantAgent):

Input: "Show me pizza and add one to cart"

Analysis:
✓ Detect Intent 1: SEARCH_MENU (pizza)
✓ Detect Intent 2: ADD_TO_CART (pizza, quantity=1)

Decision:
→ First, delegate to MenuAssistant with "Show me pizza"
→ Then, delegate to CartAssistant with "Add pizza to cart"

Output as function calls:
{
  "name": "transfer_to_agent",
  "to": "MenuAssistant",
  "query": "Show me pizza and recommend good options"
}
{
  "name": "transfer_to_agent",
  "to": "CartAssistant",
  "query": "Add pizza to cart"
}
```

### Step 5: Orchestrator Executes Sub-Agents Sequentially
```kotlin
// In AssistantOrchestrator while processing events

EVENT 1: transfer_to_agent detected → MenuAssistant

1. Get sub-agent:
   val subAgent = rootAgent.getAgentByName("MenuAssistant")

2. Create separate runner:
   val subRunner = InMemoryRunner(subAgent)

3. Execute MenuAssistant:
   val subEvents = subRunner.runAsync(
       userId = "user-1",
       sessionId = "session-1-sub-${timestamp}",
       newMessage = Content.fromText(Role.USER, "Show me pizza")
   ).toList()

4. Process sub-agent events:
   - MenuAssistant calls: search_menu(query="pizza")
   - Tool returns: List of pizza items with prices
   - LLM generates: Text summary + UI response
   - Events include: FunctionCall + FunctionResponse

5. Map events to UI states:
   - FunctionResponse contains: [MenuItem1, MenuItem2, MenuItem3, ...]
   - Map to: AssistantUiState.MenuSearch(items=[...])
   - Add to uiStates list

6. Inject summary back to root:
   - Root agent continues with context
   - Knows MenuAssistant finished

---

EVENT 2: transfer_to_agent detected → CartAssistant

1. Execute CartAssistant similarly:
   val subRunner = InMemoryRunner(cartAgent.adkAgent)

2. CartAssistant receives: "Add pizza to cart"

3. CartAssistant calls: add_to_cart(
       itemId = <parsed from previous context>,
       quantity = 1
   )

4. Tool returns: CartUpdateResult with success message

5. Map to UI state:
   - AssistantUiState.CartUpdate(
       item = pizzaItem,
       quantity = 1,
       message = "Added pizza to cart ✓"
   )

6. Orchestrator collects both UI states:
   uiStates = [
       MenuSearch(items=[...]),
       CartUpdate(...)
   ]
```

### Step 6: Events Map to UI States
```kotlin
// In AssistantOrchestrator.mapEventToUi()

Agent Event Type → UI State Transformation

┌─────────────────────────────────┐
│ MenuAssistant Returns Items     │
│ - FunctionResponse from tool    │
│ - Contains: List<MenuItem>      │
└────────────┬────────────────────┘
             ↓
    uiMapper.mapEventToUi(event)
             ↓
┌────────────────────────────────────────┐
│ AssistantUiState.MenuSearch            │
│ {                                      │
│   items: [                             │
│     MenuItem(id=1, name="Margherita",  │
│       price=250, image="..."),         │
│     MenuItem(id=2, name="Pepperoni",   │
│       price=300, image="..."),         │
│     ...                                │
│   ]                                    │
│ }                                      │
└────────────────────────────────────────┘

Repeat for each agent response...
```

### Step 7: ViewModel Saves All Responses
```kotlin
// In AssistantViewModel after orchestrator returns

uiStates.forEach { uiState ->
    // Create message wrapper
    val assistantMsg = AssistantChatMessage(
        content = uiState,
        isFromUser = false
    )
    
    // Save to database with serialization
    saveMessageToDb(assistantMsg)
    
    // Serialize UI state to JSON:
    val wrapper = mapOf(
        "type" to uiState.javaClass.simpleName,  // "MenuSearch"
        "data" to uiState                          // Full object
    )
    val payload = gson.toJson(wrapper)
    
    // Store in ChatMessageEntity
    chatMessageDao.insertMessage(ChatMessageEntity(
        text = extractTextForPreview(uiState),
        isFromUser = false,
        isA2UI = true,
        a2uiPayload = payload,
        conversationId = "ai_assistant"
    ))
}

// Database observer triggers
messages.clear()
messages.addAll(deserializedEntities)
```

### Step 8: UI Layer Renders Messages
```kotlin
// In AssistantChatScreen

LazyColumn renders each message:
  
messages = [
    AssistantChatMessage(
        content = TextResponse("Hello! I'm AI Assistant..."),
        isFromUser = false
    ),
    AssistantChatMessage(
        content = TextResponse("Show me pizza and add one to cart"),
        isFromUser = true              ← Right aligned
    ),
    AssistantChatMessage(
        content = MenuSearch(items=[...]),
        isFromUser = false             ← Left aligned
    ),
    AssistantChatMessage(
        content = CartUpdate(message="Added!"),
        isFromUser = false
    )
]

For each message → AssistantMessageBubble():
    when (content) {
        is TextResponse → TextBubble(text, isFromUser)
        is MenuSearch → MenuHorizontalList(items)
        is CartUpdate → CartUpdateCard(item, quantity, message)
        is CheckoutSummary → CheckoutCard(items, total)
        // ... other types
    }

UI Components Rendered:
- TextBubble: Purple bubble (assistant), Orange bubble (user)
- MenuHorizontalList: Cards with image, name, price → tap to add
- CartUpdateCard: Checkmark + message
- CheckoutCard: Full order summary
```

---

## 🛠️ Tool System Explained

### What is a Tool?

A **Tool** is a function that an Agent can call to accomplish work. Tools bridge the AI/LLM world with the real system.

```
LLM Decision:
"User wants to search pizza"
           ↓
      Call Tool: search_menu
           ↓
   Tool Parameters:
   {
     query: "pizza",
     category: "food",
     priceRange: [0, 500]
   }
           ↓
   Tool Execution:
   • Query local database or API
   • Find matching items
   • Return structured result
           ↓
   Tool Response:
   {
     items: [MenuItem, MenuItem, ...],
     total: 5,
     executedAt: "2024-01-15T10:30:00Z"
   }
           ↓
   LLM Gets Result:
   "Here are 5 pizzas I found..."
```

### Built-in Tools in Your System

#### MenuAssistant Tools
```
1. search_menu(
     query: String,
     category?: String,
     dietaryType?: "veg"|"non-veg",
     priceRange?: [min, max]
   )
   Returns: List<MenuItem> with images, prices, ratings

2. get_menu_details(itemId: Int)
   Returns: MenuItem with full description, ingredients, 
            nutritional info, availability
```

#### CartAssistant Tools
```
1. add_to_cart(
     itemId: Int,
     quantity: Int,
     specialInstructions?: String
   )
   Returns: CartUpdate with item, quantity, total

2. remove_from_cart(itemId: Int)
   Returns: CartUpdate with success message

3. view_cart()
   Returns: CartView with all items, subtotal, tax, total

4. update_cart_quantity(itemId: Int, quantity: Int)
   Returns: CartUpdate
```

#### BookingAssistant Tools
```
1. check_availability(
     partySize: Int,
     date: String,
     time: String
   )
   Returns: List<TimeSlot> with availability

2. book_table(
     partySize: Int,
     date: String,
     time: String,
     specialRequests?: String
   )
   Returns: BookingResult with confirmation, reference ID
```

#### OrderAssistant Tools
```
1. checkout(
     paymentMethod: String,
     deliveryType: "pickup"|"delivery",
     promoCode?: String
   )
   Returns: CheckoutSummary with orderId, total, ETA

2. get_order_status(orderId: String)
   Returns: OrderStatus with current status, ETA, location
```

#### FeedbackAssistant Tools
```
1. submit_feedback(
     rating: Int,  // 1-5 stars
     review: String,
     orderId?: String
   )
   Returns: FeedbackResult with confirmation
```

---

## 🧠 Multi-Agent System Architecture

### Why Multi-Agent?

**Single Agent Problems:**
- ❌ Tries to do everything → Mistakes
- ❌ Long reasoning → Slow
- ❌ Can't parallelize
- ❌ Hard to update rules

**Multi-Agent Benefits:**
- ✅ Each agent = Expert at one task
- ✅ Agents specialize in domain
- ✅ Can handle complex multi-intent queries
- ✅ Easy to add/modify agents
- ✅ Better error handling
- ✅ Scalable architecture

### Agent Hierarchy in Your System

```
                 ROOT AGENT
              "RestaurantMaster"
         (Orchestrator & Router)
                    │
        ┌───────────┼───────────┬─────────────┬──────────────┐
        ↓           ↓           ↓             ↓              ↓
   SPECIALIST AGENTS:
   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────────┐
   │MenuAssistant │ │CartAssistant │ │BookingAgent  │ │OrderAgent    │ │FeedbackAgent  │
   ├──────────────┤ ├──────────────┤ ├──────────────┤ ├──────────────┤ ├───────────────┤
   │Search items  │ │Add to cart   │ │Check avail.  │ │Checkout     │ │Submit rating  │
   │Get details   │ │View cart     │ │Book table    │ │Order status │ │Write review   │
   │Recommend     │ │Update qty    │ │Cancel booking│ │Track delivery│ │View history   │
   │             │ │Remove item   │ │             │ │             │ │              │
   └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └───────────────┘

ROOT AGENT RESPONSIBILITIES:
├─ Parse user query
├─ Identify intents
├─ Route to correct specialist(s)
├─ Chain agents for multi-intent
└─ Generate final response

SPECIALIST RESPONSIBILITIES:
├─ Execute single domain task
├─ Call relevant tools
├─ Return formatted result
└─ Provide domain expertise
```

### Agent Communication Flow

```
USER: "Book a table for 2 tomorrow at 7pm, and show me the menu"

  ↓

ROOT AGENT PROCESSING:
  1. Parse: "Book table" + "Show menu"
  2. Intent Classification:
     - BOOKING_REQUEST → BookingAssistant
     - MENU_REQUEST → MenuAssistant
  3. Create transfer events

  ↓

BOOKING WORKFLOW:
  ┌─────────────────────────────────────┐
  │ BookingAssistant.execute()          │
  │ - Receives: partySize=2, date=..    │
  │ - Calls: check_availability()       │
  │ - Gets: available time slots        │
  │ - Calls: book_table()               │
  │ - Returns: BookingResult with ID    │
  └─────────────────────────────────────┘

MENU WORKFLOW:
  ┌─────────────────────────────────────┐
  │ MenuAssistant.execute()             │
  │ - Receives: query="menu"            │
  │ - Calls: search_menu()              │
  │ - Gets: List<MenuItem>              │
  │ - Returns: MenuSearch state         │
  └─────────────────────────────────────┘

  ↓

ORCHESTRATOR COMBINES RESULTS:
  uiStates = [
    BookingResult(message="Booked!", id="BK123"),
    MenuSearch(items=[...])
  ]

  ↓

UI DISPLAYS BOTH RESULTS:
  ┌──────────────────────────────────────┐
  │ ✅ Table booked for 2 people         │
  │ Confirmation: BK123                  │
  │ Time: Tomorrow 7:00 PM               │
  ├──────────────────────────────────────┤
  │ [Menu Item 1] [Menu Item 2] [Item 3]│
  │ $ 250         $ 300         $ 180    │
  └──────────────────────────────────────┘
```

---

## 🎯 A2UI (Agent-to-UI) Concept

A2UI bridges the gap between agents and UI by providing **structured, typed UI states** instead of just text.

### Without A2UI (Traditional LLM)
```
User: "Show me pizzas"

LLM Output:
"Here are some pizzas we have:
1. Margherita - $250
2. Pepperoni - $300
3. Veggie Supreme - $280

Would you like to add any to your cart?"

UI: Displays as plain text bubble
Problem:
- Not interactive
- Can't easily add to cart
- No images or structure
```

### With A2UI (Your System)
```
User: "Show me pizzas"

Agent executes:
1. search_menu("pizza")
2. Returns MenuItem[] with images, prices, ratings

Generates UI State:
AssistantUiState.MenuSearch(
  items = [
    MenuItem(id=1, name="Margherita", price=250, image="url1", rating=4.5),
    MenuItem(id=2, name="Pepperoni", price=300, image="url2", rating=4.7),
    MenuItem(id=3, name="Veggie", price=280, image="url3", rating=4.3)
  ]
)

UI Renders:
[Horizontal scrolling gallery]
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ Margherita  │  │ Pepperoni   │  │ Veggie      │
│ ⭐ 4.5      │  │ ⭐ 4.7      │  │ ⭐ 4.3      │
│ $250        │  │ $300        │  │ $280        │
│ [+ ADD]     │  │ [+ ADD]     │  │ [+ ADD]     │
└─────────────┘  └─────────────┘  └─────────────┘

Fully interactive:
- Tap image → See details
- Tap [+ADD] → Add to cart instantly
- Swipe → Browse more pizzas
```

### A2UI Benefits
```
✅ Structured Data:
   Not ambiguous text, but typed objects
   
✅ Rich UI:
   Images, ratings, prices, stock status
   
✅ Interactive:
   Users can interact without followup queries
   
✅ Type Safe:
   Compile-time checks for UI rendering
   
✅ Persistent:
   Can be stored and replay from history
```

### A2UI State Hierarchy (Your System)
```
AssistantUiState (sealed class)
├─ TextResponse(text: String)
│  └─ Simple text messages
│
├─ MenuSearch(items: List<MenuItem>)
│  └─ Search results as scrollable list
│
├─ Recommendations(items: List<MenuItem>)
│  └─ Featured/recommended items
│
├─ MenuDetails(item: MenuItem)
│  └─ Full details of single item
│
├─ CartUpdate(item: MenuItem, quantity: Int, message: String)
│  └─ Confirmation when adding/removing
│
├─ CartView(items: List<CartItem>, total: Float, message: String)
│  └─ Current cart contents
│
├─ BookingResult(message: String, date: String, time: String, guests: Int)
│  └─ Booking confirmation
│
├─ CheckoutSummary(items: List<CartItem>, total: Float, message: String)
│  └─ Order summary before payment
│
├─ OrderStatus(message: String, status: String, eta: String)
│  └─ Order tracking
│
├─ FeedbackResult(message: String, rating: Int)
│  └─ Feedback confirmation
│
└─ Error(message: String)
   └─ Error states with recovery
```

---

## 💾 Data Persistence Flow

```
User Message
     ↓
AssistantViewModel saves to DB:
     ↓
ChatMessageEntity {
  id: Long,
  text: String,              // "Show me pizza"
  isFromUser: Boolean,       // true
  timestamp: Long,
  conversationId: String,    // "ai_assistant"
  isA2UI: Boolean,           // false (text)
  a2uiPayload: String?       // null
}

―――――――――――――――――――――――――――――

Agent Response (MenuSearch)
     ↓
ViewModel saves to DB:
     ↓
ChatMessageEntity {
  id: Long,
  text: String,              // "Found 5 pizzas"
  isFromUser: Boolean,       // false
  timestamp: Long,
  conversationId: String,
  isA2UI: Boolean,           // true (structured)
  a2uiPayload: String {      // JSON serialized:
    {
      "type": "MenuSearch",
      "data": {
        "items": [
          {
            "id": 1,
            "name": "Margherita",
            "price": 250.0,
            "image": "...",
            "rating": 4.5
          },
          ...
        ]
      }
    }
  }
}

―――――――――――――――――――――――――――――

On App Restart:
     ↓
ViewModel observes database:
     ↓
deserializeUiState(a2uiPayload):
  1. Parse JSON wrapper
  2. Check "type" field
  3. Deserialize data based on type
  4. Return AssistantUiState object
     ↓
messages list updated:
     ↓
UI automatically refreshes (Flow observer)
     ↓
Chat history fully restored!
```

---

## 🔧 Key Components Summary

| Component | Responsibility | Key Methods |
|-----------|-----------------|-------------|
| **AssistantChatScreen** | UI rendering | Compose layout, message bubbles |
| **AssistantViewModel** | State management | sendMessage, saveMessageToDb, observeCart |
| **AssistantOrchestrator** | Agent coordination | processQuery, mapEventToUi |
| **RootRestaurantAgent** | Intent routing | Orchestrates sub-agents |
| **MenuAgent** | Menu operations | search_menu, get_menu_details |
| **CartAgent** | Cart operations | add_to_cart, remove_from_cart, view_cart |
| **BookingAgent** | Table booking | check_availability, book_table |
| **OrderAgent** | Order management | checkout, get_order_status |
| **FeedbackAgent** | Feedback collection | submit_feedback |
| **AssistantUiMapper** | Event-to-UI mapping | mapEventToUi |
| **ChatMessageDao** | Database access | insertMessage, getMessagesByConversation |

---

## 📈 Complete Query Example: Step-by-Step

### User Request:
```
"I want a pizza and a coke. Can you also book a table for 2 tomorrow at 7pm?"
```

### Step-by-Step Execution:

#### 1️⃣ UI Captures Input
```
User types message → Taps Send
textState = "I want a pizza and a coke. Can you also book a table for 2 tomorrow at 7pm?"
```

#### 2️⃣ ViewModel Processes
```
sendMessage("I want a pizza and a coke. Can you also book a table for 2 tomorrow at 7pm?")

Actions:
├─ saveMessageToDb(userMsg)           // Persist user message
├─ isTyping = true                    // Show loading
├─ orchestrator.process Query(text)   // Send to orchestrator
└─ isTyping = false (when done)       // Hide loading
```

#### 3️⃣ Orchestrator Analyzes
```
Root Agent Receives Query
LLM Analyzes:
  - Intent 1: ADD_PIZZA (implies SEARCH)
  - Intent 2: ADD_COKE
  - Intent 3: BOOK_TABLE

Routing Decision:
  Decision 1: transfer_to_agent("MenuAssistant", 
              "Find me a pizza")
  Decision 2: transfer_to_agent("MenuAssistant", 
              "Find me a coke")
  Decision 3: transfer_to_agent("BookingAssistant",
              "Book table for 2 tomorrow at 7pm")

Why separate calls?
- Two different searches (pizza + drink)
- Reservation is independent
- Can execute in sequence
```

#### 4️⃣ Execute MenuAssistant for Pizza
```
MenuAssistant("Find me a pizza")

Execution:
1. LLM calls: search_menu(query="pizza")
2. Tool returns:  [Margherita, Pepperoni, Veggie]
3. LLM generates: MenuSearch.state
4. Events captured

Result:
AssistantUiState.MenuSearch(
  items = [MenuItem, MenuItem, MenuItem]
)
```

#### 5️⃣ Execute MenuAssistant for Coke
```
MenuAssistant("Find me a coke")

Execution:
1. LLM calls: search_menu(query="coke", category="beverages")
2. Tool returns: [Coke, Coke Zero, Diet Coke]
3. LLM generates: MenuSearch state
4. Events captured

Result:
AssistantUiState.MenuSearch(
  items = [Coke, CokZero, DietCoke]
)
```

#### 6️⃣ Execute BookingAssistant
```
BookingAssistant("Book table for 2 tomorrow at 7pm")

Execution:
1. LLM calls: check_availability(partySize=2, date="tomorrow", time="19:00")
2. Tool returns: [19:00 available, 19:30 available, ...]
3. LLM calls: book_table(partySize=2, date="tomorrow", time="19:00")
4. Tool returns: BookingResult {id: "BK12345", confirmed: true}
5. LLM generates: BookingResult state

Result:
AssistantUiState.BookingResult(
  message = "Table booked!",
  date = "Tomorrow",
  time = "7:00 PM",
  guests = 2
)
```

#### 7️⃣ Orchestrator Combines Results
```
uiStates = [
  MenuSearch(pizzas),          // Step 4
  MenuSearch(drinks),          // Step 5
  BookingResult(confirmed)     // Step 6
]

Returned to ViewModel
```

#### 8️⃣ ViewModel Saves & Updates
```
For each uiState:
  ├─ saveMessageToDb(assistantMsg)
  ├─ Serialize to JSON
  └─ Trigger database update

Observer fires:
  ├─ messages.clear()
  ├─ messages.addAll(deserializedEntities)
  └─ UI automatically refreshes
```

#### 9️⃣ UI Renders Complete Response
```
┌─────────────────────────────────────┐
│          ASSISTANT SCREEN           │
├─────────────────────────────────────┤
│ User: "I want a pizza and a coke... │
│ Can you also book a table for 2..." │
├─────────────────────────────────────┤
│          🍕 PIZZAS                  │
│ [Margherita] [Pepperoni] [Veggie]  │
│   $250        $300       $280       │
│                                     │
│          🥤 DRINKS                  │
│ [Coke] [Coke Zero] [Diet Coke]     │
│  $40     $40        $40             │
│                                     │
│ ✅ TABLE BOOKED                     │
│ 2 guests • Tomorrow • 7:00 PM       │
│ Confirmation: BK12345               │
└─────────────────────────────────────┘
```

#### 🔟 User Can Interact
```
User taps [Margherita] [+ADD]
  ├─ Calls: addToCart(itemId=1, quantity=1)
  ├─ CartAgent.add_to_cart() executes
  ├─ Tool updates database
  ├─ UI state: CartUpdate generated
  ├─ Saved to database
  └─ Shows confirmation: "Added to cart ✓"

User continues conversation naturally...
```

---

## 🎓 Key Takeaways

1. **Layered Architecture**: UI → ViewModel → Orchestrator → Agents → Tools → Database
2. **Multi-Agent**: Each specialist (MenuAgent, CartAgent, etc.) handles one domain
3. **Event-Driven**: ADK generates events → Orchestrator collects → Maps to UI states
4. **A2UI States**: Rich, typed, interactive UI representations (not just text)
5. **Persistent**: All messages saved to database → Survives app restart
6. **Parallel Execution**: Multiple agents can run sequentially in single query
7. **Tools**: Bridge between agents and business logic
8. **LLM-Powered**: Gemini 3.1 Flash Lite makes decisions and generates responses

---

## 🚀 Production Improvements

Your system is production-ready with:
- ✅ Error handling with fallbacks
- ✅ Database persistence
- ✅ Loading states
- ✅ Multi-agent orchestration
- ✅ Type-safe UI states
- ✅ Session management
- ✅ Cloud LLM integration
- ✅ Dependency injection (Hilt)


