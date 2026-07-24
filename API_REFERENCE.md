# API Reference - New & Modified Methods

## Overview
This document lists all new and modified methods/classes added during the feature implementation.

---

## 📦 Models (Models.kt)

### New Classes

#### `TableBooking`
Represents a confirmed table reservation.

```kotlin
data class TableBooking(
    val bookingId: String = generateBookingId(),
    val numberOfPeople: Int,
    val bookingTime: String,
    val bookingTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateBookingId(): String
        // Returns format: "TB-HHMMSS" (e.g., "TB-205632")
    }
}
```

**Properties:**
- `bookingId`: Unique identifier (auto-generated)
- `numberOfPeople`: Number of guests (1+)
- `bookingTime`: Booking time in user's format (e.g., "8:00 PM", "20:00")
- `bookingTimestamp`: System timestamp in milliseconds

---

### New Response Types

#### `AgentResponse.BookingRequest`
Represents a step in the booking flow.

```kotlin
data class BookingRequest(
    val step: String,           // "ask_people", "ask_time", "confirm"
    val query: String = ""      // Optional context
) : AgentResponse
```

**Steps:**
- `"ask_people"`: Asking for number of people
- `"ask_time"`: Asking for booking time
- `"confirm"`: Confirmation message

---

#### `AgentResponse.BookingConfirmation`
Contains confirmed booking details.

```kotlin
data class BookingConfirmation(
    val booking: TableBooking
) : AgentResponse
```

---

## 📱 ViewModel (RestaurantViewModel.kt)

### Updated Data Class

#### `UiMessage`
Chat message with unique identification.

```kotlin
data class UiMessage(
    val id: String = UUID.randomUUID().toString(),  // NEW: Unique identifier
    val content: String,
    val isFromAgent: Boolean,
    val isA2UI: Boolean = false,
    val a2uiPayloads: List<String> = emptyList()
)
```

**Changes:**
- Added `id` field with automatic UUID generation
- Ensures message uniqueness across app session
- Prevents message duplication

---

### New Methods

#### `RestaurantViewModel.clearChat()`
Clears all chat messages and resets conversation.

```kotlin
fun clearChat()
```

**Behavior:**
- Clears all messages from `_uiMessages`
- Adds fresh greeting message
- Resets conversation state
- No return value

**Usage:**
```kotlin
viewModel.clearChat()  // Call from UI or programmatically
```

---

## 📚 Repository (MenuRepository.kt)

### New Methods

#### `MenuRepository.addBooking(booking: TableBooking)`
Stores a table booking.

```kotlin
fun addBooking(booking: TableBooking)
```

**Parameters:**
- `booking`: TableBooking object to store

**Behavior:**
- Adds booking to internal mutable list
- Preserves chronological order
- No validation performed (add as needed)

**Usage:**
```kotlin
val booking = TableBooking(numberOfPeople = 4, bookingTime = "8:00 PM")
repository.addBooking(booking)
```

---

#### `MenuRepository.getBookings(): List<TableBooking>`
Retrieves all stored bookings.

```kotlin
fun getBookings(): List<TableBooking>
```

**Returns:**
- Immutable copy of bookings list
- Empty list if no bookings

**Usage:**
```kotlin
val bookings = repository.getBookings()
bookings.forEach { booking ->
    println("ID: ${booking.bookingId}, People: ${booking.numberOfPeople}")
}
```

---

## 🔧 Tools (RestaurantTools.kt)

### Modified Method

#### `RestaurantTools.searchMenu(category, type, maxPrice)`
Enhanced with keyword normalization.

```kotlin
fun searchMenu(
    category: String? = null,
    type: String? = null,
    maxPrice: Int? = null
): String
```

**New Feature - Type Normalization:**
```kotlin
val normalizedType = when {
    type?.lowercase() in listOf("veg", "vegetarian", "veggie") → "Veg"
    type?.lowercase() in listOf("non-veg", "non veg", ...) → "Non-Veg"
    else → type
}
```

**Supported Keywords:**
- Veg: "veg", "vegetarian", "veggie"
- Non-Veg: "non-veg", "non veg", "nonveg", "non-vegetarian", "meat"

**Example:**
```kotlin
// User says "show me veg items"
searchMenu(type = "veg")  // Automatically normalized to "Veg"

// User says "pizza under 300"
searchMenu(category = "pizza", maxPrice = 300)
```

---

### New Method

#### `RestaurantTools.bookTableStep(step, numberOfPeople, bookingTime)`
Handles table booking flow.

```kotlin
fun bookTableStep(
    step: String,
    numberOfPeople: String? = null,
    bookingTime: String? = null
): String
```

**Parameters:**
- `step`: Current booking step ("ask_people", "ask_time", "complete")
- `numberOfPeople`: Number of guests (optional, used in "ask_time" and "complete")
- `bookingTime`: Booking time (optional, used in "complete")

**Steps:**

**Step 1: "ask_people"**
```kotlin
bookTableStep("ask_people")
// Returns: "For how many people would you like to book a table?"
```

**Step 2: "ask_time"**
```kotlin
bookTableStep("ask_time", numberOfPeople = "4")
// Returns: "What time would you like to book the table?"
```

**Step 3: "complete"**
```kotlin
bookTableStep("complete", numberOfPeople = "4", bookingTime = "8:00 PM")
// Returns: "Table booking confirmed. Booking ID: TB-205632..."
// Also: Creates TableBooking and stores it
```

**Error Handling:**
```kotlin
bookTableStep("ask_time")  // Missing numberOfPeople
// Returns: "Please provide the number of people first."

bookTableStep("complete", numberOfPeople = "4")  // Missing bookingTime
// Returns: "Missing booking details. Please try again."
```

---

## 🎨 UI Components (Components.kt)

### New Composable

#### `MenuCardVertical(item: MenuItem)`
Renders a menu item as a horizontal card (vertical list).

```kotlin
@Composable
fun MenuCardVertical(item: MenuItem)
```

**Visual Layout:**
```
┌────────────────────────────────┐
│ [Image]  Name                  │
│ 80×80dp  Type • Category       │
│          ₹Price                │
└────────────────────────────────┘
```

**Usage:**
```kotlin
MenuCardVertical(item = MenuItem(...))
```

---

#### `MenuListVertical(items: List<MenuItem>)`
Renders a vertical scrollable list of menu items.

```kotlin
@Composable
fun MenuListVertical(items: List<MenuItem>)
```

**Features:**
- Full-width cards
- Smooth scrolling
- 8dp spacing between items
- 8dp top/bottom padding

**Usage:**
```kotlin
MenuListVertical(items = repository.getMenuItems())
```

---

## 🔨 Response Builder (A2UIResponseBuilder.kt)

### New Method

#### `A2UIResponseBuilder.buildBookingConfirmation(response: BookingConfirmation)`
Generates A2UI JSON for booking confirmation.

```kotlin
private fun buildBookingConfirmation(
    response: AgentResponse.BookingConfirmation
): String
```

**Returns:**
- A2UI-formatted JSON string with:
  - Confirmation header (✓ Table Booking Confirmed)
  - Booking ID
  - Number of people
  - Booking time

**Generated A2UI Structure:**
```json
{
  "version": "v0.10",
  "updateComponents": {
    "surfaceId": "restaurant_surface",
    "components": [
      {"id": "root", "component": "Column", "children": [...]},
      {"id": "booking_header", "component": "Text", "text": "✓ Table Booking Confirmed"},
      {"id": "booking_id", "component": "Text", "text": "Booking ID: TB-205632"},
      {"id": "booking_people", "component": "Text", "text": "Number of People: 4"},
      {"id": "booking_time", "component": "Text", "text": "Booking Time: 8:00 PM"}
    ]
  }
}
```

---

### Modified Method

#### `A2UIResponseBuilder.build(response: AgentResponse)`
Updated to handle booking responses.

```kotlin
fun build(response: AgentResponse): List<String>
```

**New Response Type Handling:**
```kotlin
is AgentResponse.BookingRequest → buildMessage(...)
is AgentResponse.BookingConfirmation → buildBookingConfirmation(...)
```

---

## 🎯 Agent (RestaurantAgent.kt)

### Modified Agent Instruction

The LLM agent instruction now includes booking rules:

```
Rule 7: If user wants to book table → call book_table_step(step="ask_people")
Rule 8: If user provided number → call book_table_step(step="ask_time", numberOfPeople="<number>")
Rule 9: If user provided time → call book_table_step(step="complete", numberOfPeople="<number>", bookingTime="<time>")
```

---

### New Short-Circuit Handler

#### Booking Intent Detection
```kotlin
val bookingKeywords = Regex("\\b(book|reserve|reservation|table)\\b", RegexOption.IGNORE_CASE)
if (bookingKeywords.containsMatchIn(query)) {
    restaurantTools.bookTableStep("ask_people")
    // ... return response immediately
}
```

---

## 🎨 UI Changes (AiRestaurantScreen.kt)

### TopAppBar Actions
```kotlin
TopAppBar(
    // ... existing code ...
    actions = {
        IconButton(onClick = { viewModel.clearChat() }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear Chat")
        }
    }
)
```

---

### LazyColumn Message Keying
```kotlin
LazyColumn(
    // ... existing code ...
) {
    items(viewModel.uiMessages, key = { it.id }) { message ->
        // Each message identified by unique ID
        ChatBubble(message, viewModel)
    }
}
```

---

## 📊 Summary Table

| Component | Change Type | Method/Class |
|-----------|------------|--------------|
| Models | New | TableBooking, BookingRequest, BookingConfirmation |
| ViewModel | Modified | UiMessage (added id field) |
| ViewModel | New | clearChat() |
| Repository | New | addBooking(), getBookings() |
| Tools | Modified | searchMenu() (keyword normalization) |
| Tools | New | bookTableStep() |
| Components | New | MenuCardVertical(), MenuListVertical() |
| ResponseBuilder | New | buildBookingConfirmation() |
| ResponseBuilder | Modified | build() (handles booking responses) |
| Screen | Modified | TopAppBar (clear button), LazyColumn (message keys) |
| Agent | Modified | Instruction (booking rules), Short-circuits |

---

## 🔗 Integration Flow

```
User Input
    ↓
RestaurantViewModel.sendMessage()
    ↓
RestaurantAgent.processQuery()
    ├─ Check booking keywords
    ├─ Call RestaurantTools.bookTableStep()
    └─ Get AgentResponse.BookingRequest/BookingConfirmation
    ↓
A2UIResponseBuilder.build()
    ├─ Check response type
    ├─ Call buildBookingConfirmation() for bookings
    └─ Generate A2UI JSON
    ↓
ChatBubble (with unique id as key)
    ↓
A2UISurface renders JSON
```

---

## 💡 Best Practices

1. **Always use unique IDs for messages** - Automatic via UUID
2. **Store bookings persistently** - Currently in-memory only
3. **Validate user input** - Add before calling tools
4. **Normalize keywords** - Already done in searchMenu()
5. **Handle errors gracefully** - All tools return String

---

## 🔄 Backward Compatibility

All new methods are **additive only**:
- Existing methods unchanged
- New optional parameters with defaults
- No signature changes to existing APIs
- All old workflows still work

---

For detailed implementation examples, see:
- `QUICK_INTEGRATION_GUIDE.md` - Usage examples
- `IMPLEMENTATION_SUMMARY.md` - Architecture details


