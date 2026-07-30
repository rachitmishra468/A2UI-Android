# Restaurant AI App - Complete Demo

A full-featured restaurant ordering app built with **clean architecture**, supporting both **manual UI** and **AI agent** flows using **A2UI** JSON rendering.

## 🎯 Key Features

### 1. **Dual-Mode Architecture**
- **Manual Mode**: Direct user interaction with menu, cart, and checkout
- **AI Agent Mode**: Natural language prompts via multi-agent orchestrator

### 2. **Multi-Agent System**
The app uses a **clean, modular multi-agent architecture**:
- **MenuAgent** - Search and filter menu items
- **CartAgent** - Add/remove/view items
- **BookingAgent** - Handle table reservations
- **PricingAgent** - Calculate totals and discounts
- **CheckoutAgent** - Process orders

### 3. **Beautiful Restaurant UI**
- Professional home screen with menu cards (images, prices, categories, description)
- Modern card-based design inspired by A2UI JSON format
- Seamless navigation between Home, AI Chat, and Cart screens
- Toggle button at top to switch between Manual and AI modes

### 4. **A2UI JSON Rendering**
All responses (menu items, cart updates, booking confirmations) are rendered using A2UI protocol inside chat bubbles, maintaining UI consistency.

### 5. **Shared State Across Flows**
- Both manual and AI flows share the same `MenuRepository` and use-cases
- Cart state is synchronized: items added in manual mode appear in cart screen and agent responses
- Checkout clears the shared cart for both flows

---

## 📁 Project Structure (Clean Architecture)

```
app/src/main/java/com/example/a2ui_sample/
├── domain/
│   └── usecases/
│       └── UseCases.kt              # Domain use case interfaces & implementations
│           ├── SearchMenuUseCase
│           ├── AddToCartUseCase
│           ├── ViewCartUseCase
│           ├── BookTableUseCase
│           ├── CalculatePriceUseCase
│           └── CheckoutUseCase
├── data/
│   ├── MenuRepository.kt            # Local data repository
│   ├── Models.kt                    # Data models (MenuItem, CartItem, AgentResponse, etc.)
│   └── AgentResponse.kt             # Sealed classes for structured responses
├── agent/
│   ├── AgentInterfaces.kt           # AgentK, TaskK, IntentK interfaces
│   ├── AgentsImpl.kt                 # Concrete agent implementations
│   ├── AgentRouterK.kt              # Routes tasks to appropriate agents
│   ├── OrchestratorK.kt             # Main orchestrator: intent detection → planning → execution
│   ├── A2UIResponseBuilder.kt       # Converts AgentResponse to A2UI JSON
│   ├── DummyRestaurantAgent.kt      # Fallback offline agent (for quick testing)
│   ├── RestaurantAgent.kt           # ADK-based agent (optional, requires API keys)
│   └── RestaurantTools.kt           # Tool implementations for ADK agent
├── ui/
│   ├── HomeScreen.kt                # Beautiful menu home screen (A2UI-inspired cards)
│   ├── AiRestaurantScreen.kt        # AI agent chat interface with A2UI rendering
│   ├── CartScreen.kt                # Cart management (view, edit, checkout)
│   ├── RestaurantViewModel.kt       # Unified ViewModel for both flows
│   └── MainActivity.kt              # Navigation setup (home → ai → cart)
└── (core app and dependencies)
```

---

## 🏃 How to Run

### Prerequisites
- Android Studio (latest)
- Android SDK 26+ (compileSdk 37)
- Gradle 8.0+
- (Optional) Gemini API key for ADK agent mode

### Build & Install

#### PowerShell
```powershell
cd C:\Users\rachmish\Documents\A2UI\A2UI_Android

# Assemble debug APK
.\gradlew.bat assembleDebug

# Install on emulator/device
.\gradlew.bat installDebug
```

#### Android Studio
1. Open the project in Android Studio
2. Press **Run** (green play button) or Shift+F10

---

## 📱 Usage Flows

### **Home Screen (Manual Mode)**
1. **View Menu**: Beautiful card layout showing all menu items
   - Image, name, category (Veg/Non-Veg), price
   - "Add to Cart" button on each item
2. **Quick Actions**:
   - `AI Agent` button (top right) → Switch to AI chat
   - `Cart` button (top right) → View cart

### **AI Agent Chat Screen**
1. **Toggle Modes** (top of screen):
   - `Manual` / `AI Agent` buttons to switch flows
   - `Use ADK` toggle (if you have API keys configured)
2. **Chat Interface**:
   - Type natural language prompts
   - Receive A2UI-rendered responses in chat bubbles
3. **Example Prompts**:
   ```
   # Search menu
   "show veg burgers"
   "what desserts do you have?"
   
   # Add to cart
   "add masala dosa to my cart"
   "order 2 pizzas"
   
   # Booking
   "book a table for 5 people at 4 pm"
   "reserve a seat for 10 at 8 pm"
   
   # Checkout
   "checkout my order"
   "place my order"
   
   # View cart
   "show my cart"
   "where is my cart"
   ```
4. **Navigation** (top bar):
   - Home button → Back to menu
   - Cart button → View cart
   - Clear button → Clear chat history

### **Cart Screen**
- Lists all items with quantities
- Increment (+) / Decrement (−) buttons per item
- Remove item button
- Total price display
- **Checkout** button → Completes transaction and clears cart
- **Back to AI Agent** → Return to chat (cart state preserved)

---

## 🤖 How the AI Agent Works

### Intent Detection (Regex-based)
The orchestrator uses regex patterns to detect user intents:
- `add|order|buy|put` → **ADD_TO_CART**
- `show|view|cart` → **VIEW_CART**
- `book|reserve|table` → **BOOK_TABLE**
- `menu|what can i order` → **SEARCH_MENU**
- `checkout|place order|pay` → **CHECKOUT**

### Multi-Agent Orchestration
1. **Detect Intents** from user message
2. **Plan Tasks** (extract parameters like item name, party size, time)
3. **Route to Agents** (MenuAgent, CartAgent, BookingAgent, etc.)
4. **Execute in Parallel** where applicable
5. **Aggregate Responses** using `A2UIResponseBuilder`
6. **Render A2UI JSON** in chat bubble

### Example Flow: "Book a table for 5 people at 4 pm"
```
User Input
  ↓
Intent Detection: BOOK_TABLE
  ↓
Task Planning: Extract partySize=5, time="4 pm"
  ↓
Route to BookingAgent
  ↓
BookingAgent calls BookTableUseCase with params
  ↓
Use case creates TableBooking entity
  ↓
A2UIResponseBuilder creates booking confirmation JSON
  ↓
Chat bubble renders: "✓ Table Booking Confirmed"
   - Booking ID: TB-123456
   - Number of People: 5
   - Booking Time: 4 pm
```

---

## 🔄 Shared State Between Flows

Both manual and AI flows update the **same repository**:

```
HomeScreen (Manual)  ──┐
                       ├─→ MenuRepository (Singleton)
AiRestaurantScreen ────┤                     ↓
                       ├─→ Domain Use Cases (Shared)
CartScreen ────────────┤                     ↓
                       └─→ A2UIResponseBuilder (A2UI JSON)
```

**Example Scenario**:
1. User adds item via manual "Add to Cart" button on Home
2. User navigates to AI Agent screen
3. User types "view my cart" in AI agent chat
4. Orchestrator calls ViewCartUseCase
5. Cart shows the same item added manually

---

## 🏗️ Clean Architecture Benefits

| Layer | Responsibility | Benefit |
|-------|---|---|
| **Domain** (Use Cases) | Business logic | Reusable by both manual UI and AI agents |
| **Data** (Repository) | Data access | Single source of truth |
| **Agent** | AI reasoning & routing | Modular, testable agents |
| **UI** (Compose) | User interface | Multiple UIs can use same logic |

---

## ⚙️ Configuration

### Offline Mode (Default)
```kotlin
// In RestaurantViewModel.kt
useDummyAgent = true      // Use local orchestrator (no API required)
manualMode = false        // Start in AI agent mode
```

### ADK/LLM Mode (Optional)
1. Get Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Add to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_key_here
   ```
3. In app, toggle "Use ADK" switch to enable ADK RestaurantAgent
4. Or in code: `viewModel.setUseDummyAgent(false)`

---

## 📊 Data Flow Diagram

```
User Input (Manual or AI)
  ↓
ViewModel (RestaurantViewModel)
  ├─ Manual Mode? 
  │  ├─ YES: Direct use-case call + A2UIResponseBuilder
  │  └─ Render in chat bubble (A2UI JSON)
  │
  └─ AI Mode?
     ├─ Use Orchestrator or ADK Agent
     ├─ Intent Detection + Task Planning
     ├─ Parallel Agent Execution
     ├─ Response Aggregation
     └─ A2UIResponseBuilder + Chat Bubble Render
```

---

## 🧪 Testing the App

### Quick Manual Test Sequence
1. **Start App** → Home screen with menu
2. **Manual Flow**:
   - Tap "Add to Cart" on a menu item
   - Verify cart shows item
   - Tap "+"/"-" to change quantity
   - Tap "Checkout" → See A2UI confirmation
3. **AI Agent Flow**:
   - Tap "AI Agent" button (top right)
   - Type "show menu" → See A2UI menu
   - Type "add masala dosa" → See A2UI cart update
   - Navigate to Cart → See the added item
   - Type "book a table for 3 people at 7 pm" → See A2UI booking confirmation
   - Type "checkout" → See A2UI checkout message

### Example Test Prompts
```
# All should work and return A2UI rendered responses:
- "show veg items"
- "what's the price of masala dosa"
- "add 2 pizzas to my cart"
- "show my cart"
- "remove pizza from cart"
- "book a table for 4 people"
- "can i book a table for 6 people at 8 pm"
- "checkout"
- "place my order"
```

---

## 🚀 Future Enhancements

1. **Better Intent Detection**:
   - Fuzzy matching for menu item names (Levenshtein distance)
   - Natural time parsing ("this evening", "tomorrow at noon")
   - Multi-turn conversation memory

2. **UI Improvements**:
   - Cart badge with item count on top bar
   - Product images with proper image URLs in menu
   - Animated transitions between screens
   - Order history

3. **Backend Integration**:
   - Real REST API instead of local repo
   - Server-side multi-agent orchestrator
   - Real payment processing

4. **Testing**:
   - Unit tests for use cases
   - Integration tests for agent flows
   - UI tests for Compose screens

---

## 📝 File Modifications Summary

| File | Status | Changes |
|------|--------|---------|
| `domain/usecases/UseCases.kt` | **Created** | Domain use cases for search, cart, booking, checkout |
| `agent/AgentInterfaces.kt` | **Created** | AgentK, TaskK, IntentK interfaces |
| `agent/AgentsImpl.kt` | **Created** | MenuAgentK, CartAgentK, BookingAgentK, etc. |
| `agent/AgentRouterK.kt` | **Created** | Routers tasks to agents |
| `agent/OrchestratorK.kt` | **Created** | Main orchestrator logic |
| `agent/DummyRestaurantAgent.kt` | **Created** | Offline agent fallback |
| `ui/HomeScreen.kt` | **Modified** | Professional A2UI-inspired menu cards |
| `ui/CartScreen.kt` | **Created** | Cart management screen |
| `ui/AiRestaurantScreen.kt` | **Modified** | Added navigation, Home/Cart buttons |
| `ui/RestaurantViewModel.kt` | **Modified** | Integrated use cases, orchestrator, toggles |
| `data/MenuRepository.kt` | **Modified** | Added cart management helpers |
| `MainActivity.kt` | **Modified** | Navigation setup (home, ai, cart routes) |

---

## 🎨 UI Screenshots (Description)

### Home Screen
- Header: "RestaurantAI" with AI Agent button + Cart icon
- Menu grid: Cards with image, name, category tags, price, Add button
- Colors: Professional primary blue, green for Veg, red for Non-Veg

### AI Agent Screen
- Top bar: Home button, Cart button, Clear button
- Chat Area: User messages (right-aligned, blue), Agent responses (left-aligned, A2UI rendered)
- Input: Text field + Send button

### Cart Screen
- Header: "Your Cart"
- Items: Cards showing name, price × quantity, +/− buttons, Remove button
- Total: Display total amount
- Buttons: Checkout (completes order), Back to AI Agent

---

## 💡 Key Design Decisions

1. **Shared Domain Use Cases**: Both manual UI and AI agents use the same business logic
2. **A2UI Rendering**: Consistent UI across all agent responses
3. **Local-First**: App works offline with dummy agent; optional API support
4. **Clean Architecture**: Clear separation of concerns (domain, data, agent, UI)
5. **Navigation**: Single NavHost manages all screen transitions with shared state

---

## 📞 Support

For issues or questions:
1. Check logs: `Logcat` in Android Studio (filter "A2UI_FLOW")
2. Verify `menu.json` exists in `assets/` folder
3. Ensure all dependencies in `build.gradle.kts` are installed
4. Try "Clear Cache" in Android Studio Build menu

---

**Happy Ordering! 🍕🍔🍜**

