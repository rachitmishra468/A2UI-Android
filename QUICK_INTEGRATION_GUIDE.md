# Quick Integration Guide - A2UI Restaurant Assistant Features

## Quick Start Examples

### 1. Table Booking using the UI

Simply type in the chat:
```
User: "Book a table"
Assistant: "For how many people would you like to book a table?"
User: "4"
Assistant: "What time would you like to book the table?"
User: "8:00 PM"
Assistant: "✓ Table Booking Confirmed
           Booking ID: TB-205632
           Number of People: 4
           Booking Time: 8:00 PM"
```

### 2. Accessing Bookings Programmatically

```kotlin
// In your code
val repository = MenuRepository(context)
val bookings = repository.getBookings()

bookings.forEach { booking ->
    println("Booking ID: ${booking.bookingId}")
    println("People: ${booking.numberOfPeople}")
    println("Time: ${booking.bookingTime}")
    println("Timestamp: ${booking.bookingTimestamp}")
}
```

### 3. Clear Chat

```kotlin
// In ViewModel or UI
viewModel.clearChat()  // Clears all messages and starts fresh
```

### 4. Advanced Search Examples

Try these search queries:

```
"Show all vegetarian items"
"Show non veg burgers"
"Pizza under 300"
"All veg meals"
"Non-vegetarian appetizers"
"Show me pizzas"
"Vegetarian biryani"
```

### 5. Vertical Menu Display

The menu automatically displays vertically in a scrollable list. Each card shows:
- Item image (thumbnail)
- Item name
- Type (Veg/Non-Veg)
- Category
- Price

---

## Understanding the Architecture

### Component Interaction Flow

```
User Input (Chat)
    ↓
RestaurantViewModel.sendMessage()
    ↓
RestaurantAgent.processQuery()
    ├─ Rule-based short-circuits (add, cart, booking)
    └─ ADK Agent reasoning with Gemini
    ↓
RestaurantTools.<method>() executes
    ├─ searchMenu()
    ├─ addItemToCart()
    ├─ viewCart()
    ├─ bookTableStep()
    └─ etc.
    ↓
AgentResponse created
    ├─ MenuResults
    ├─ CartView  
    ├─ BookingConfirmation
    └─ Message
    ↓
A2UIResponseBuilder.build()
    ↓
A2UI JSON → AiRestaurantScreen
    ↓
ChatBubble rendered with unique ID
```

### Message Immutability

Each message has a unique UUID:
```kotlin
UiMessage(
    id = UUID.randomUUID().toString(),  // Unique across app session
    content = "User input or bot response",
    isFromAgent = true/false,
    isA2UI = true if dynamic content,
    a2uiPayloads = listOf(json1, json2, ...)
)
```

---

## Common Integration Points

### Adding New Booking Status

```kotlin
// In RestaurantTools.kt - extend bookTableStep()
when (step.lowercase()) {
    "ask_people" -> { /* ... */ }
    "ask_time" -> { /* ... */ }
    "complete" -> { /* ... */ }
    "confirm_email" -> {  // NEW: Add email confirmation
        // Custom logic here
    }
}
```

### Custom Search Filters

```kotlin
// In RestaurantTools.kt - enhance searchMenu()
val normalizedType = when {
    type?.lowercase() in listOf("veg", "vegetarian") → "Veg"
    type?.lowercase() in listOf("spicy") → {
        // Add custom spicy logic
        "Spicy-Veg"
    }
    else → type
}
```

### Booking Storage Enhancement

```kotlin
// In MenuRepository.kt
private val bookings = mutableListOf<TableBooking>()

// Add persistence (SharedPreferences/Database)
fun saveBooking(booking: TableBooking) {
    bookings.add(booking)
    // TODO: Persist to database
}
```

---

## Debugging Tips

### View All Chat Messages

```kotlin
// In any composable with viewModel
LazyColumn {
    items(viewModel.uiMessages, key = { it.id }) { message ->
        // Each message has unique ID
        Log.d("CHAT", "Message ID: ${message.id}")
        Log.d("CHAT", "Content: ${message.content}")
        Log.d("CHAT", "From Agent: ${message.isFromAgent}")
    }
}
```

### Check Booking State

```kotlin
// In ViewModel
val bookings = repository.getBookings()
Log.d("BOOKINGS", "Total bookings: ${bookings.size}")
bookings.forEach {
    Log.d("BOOKINGS", "ID: ${it.bookingId}, People: ${it.numberOfPeople}")
}
```

### Monitor Agent Tool Calls

```
Look for logs with "A2UI_FLOW" tag:
- "3. Agent starting reasoning for query: ..."
- ">> Detected booking intent..."
- ">> Tool Executing: book_table_step(...)"
- "4. Reasoning finished..."
```

---

## Error Handling Examples

### Booking Validation

```kotlin
try {
    val booking = TableBooking(
        numberOfPeople = userInput.toIntOrNull() ?: 1,
        bookingTime = timeInput
    )
    repository.addBooking(booking)
    lastResponse = AgentResponse.BookingConfirmation(booking)
} catch (e: Exception) {
    lastResponse = AgentResponse.Error("Booking failed: ${e.message}")
}
```

### Search Error Handling

```kotlin
fun searchMenu(category: String?, type: String?, maxPrice: Int?): String {
    val results = repository.searchMenu(category, type, maxPrice)
    
    return when {
        results.isEmpty() → "No items found matching your criteria"
        results.size == 1 → "Found 1 item: ${results[0].name}"
        else → "${results.size} items found"
    }
}
```

---

## Performance Considerations

1. **Message IDs**: UUIDs are cheap to generate (use for all messages)
2. **Menu Rendering**: LazyColumn handles scrolling efficiently for 20+ items
3. **Agent Reasoning**: Short-circuits reduce unnecessary LLM calls
4. **State Updates**: All immutable, no accidental mutations
5. **A2UI Rendering**: Each chat bubble has isolated renderer state

---

## Testing the Features

### Test Table Booking

```
1. Open app
2. Say "book a table"
3. Provide number
4. Provide time
5. Verify confirmation appears
6. Check repository.getBookings() has entry
```

### Test Clear Chat

```
1. Have 5+ messages in chat
2. Click clear button (🗑️)
3. Verify all messages gone
4. Verify fresh greeting appears
5. Send new message - verify it works
```

### Test Menu Filters

```
1. Say "show veg items" - verify only veg items
2. Say "show pizza" - verify only pizzas
3. Say "show non-veg" - verify only non-veg
4. Say "vegetarian pizza" - verify veg + pizza filter
```

### Test Menu Display

```
1. Say "show menu"
2. Verify items in vertical list (not horizontal)
3. Scroll through list
4. Verify all 20 items visible
5. Verify images, names, prices visible
```

---

## API Reference

### RestaurantViewModel

```kotlin
fun sendMessage(query: String)  // Send chat message
fun clearChat()                 // Clear all messages, start fresh

val uiMessages: List<UiMessage> // Observable chat history
```

### MenuRepository

```kotlin
fun getMenuItems(): List<MenuItem>
fun searchMenu(category: String?, type: String?, maxPrice: Int?): List<MenuItem>
fun addToCart(itemId: Int): CartItem?
fun addBooking(booking: TableBooking)
fun getBookings(): List<TableBooking>
```

### RestaurantTools

```kotlin
fun searchMenu(category: String?, type: String?, maxPrice: Int?): String
fun addItemToCart(itemName: String): String
fun viewCart(): String
fun bookTableStep(step: String, numberOfPeople: String?, bookingTime: String?): String
fun getRecommendations(criteria: String?): String
fun getFullMenu(): String
```

---

## Common Issues & Solutions

### Issue: Messages duplicating

**Solution**: Ensure each message has unique ID (automatic with UUID)

### Issue: Booking not saved

**Solution**: Verify `repository.addBooking()` is called in `bookTableStep()` complete step

### Issue: Clear button doesn't work

**Solution**: Check `viewModel.clearChat()` is called, ensure state is properly cleared

### Issue: Menu not filtering

**Solution**: Verify keyword normalization in `searchMenu()` is handling the keyword

### Issue: Vertical menu not showing

**Solution**: Check A2UIResponseBuilder is calling `MenuListVertical()` instead of `MenuList()`

---

## Next Steps

1. **Test all features** using the examples above
2. **Monitor logs** with "A2UI_FLOW" tag while testing
3. **Check repository** for saved bookings after testing
4. **Deploy with confidence** - all changes are backward compatible

---

For detailed implementation see: `IMPLEMENTATION_SUMMARY.md`

