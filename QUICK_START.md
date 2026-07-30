# 🚀 Quick Start Guide - Restaurant AI App

## Setup & Installation (5 minutes)

### 1. Prerequisites
- ✅ Android Studio (latest version)
- ✅ Android SDK 26+ (compileSdk 37 configured)
- ✅ Java 11 or higher
- ✅ Min 4GB RAM for emulator
- (Optional) Gemini API key for LLM features

### 2. Build & Install

#### Via Command Line (PowerShell/Bash)
```bash
cd C:\Users\rachmish\Documents\A2UI\A2UI_Android

# Assemble debug APK
.\gradlew.bat assembleDebug

# Install on connected emulator/device
.\gradlew.bat installDebug
```

#### Via Android Studio
1. Open project: `File → Open → A2UI_Android`
2. Let Gradle sync (wait for index complete)
3. Press ▶️ **Run** (Shift+F10) or `Run → Run 'app'`
4. Select emulator/device
5. App starts on Home screen

### 3. First Run Checklist
- [ ] App launches on Home screen
- [ ] Menu items display with images and prices
- [ ] "AI Agent" button visible (top-right)
- [ ] "View Cart" button in menu navigation
- [ ] Can tap "Add to Cart" on any item

---

## 🎮 Quick Test (5 minutes)

### Test Path 1: Manual Mode (Add to Cart)
```
1. Home Screen
2. Tap "Add to Cart" on "Masala Dosa"
3. See A2UI cart update in bubble
4. Tap "View Cart"
5. See Masala Dosa in cart
6. Tap "Checkout"
7. See checkout confirmation (A2UI bubble)
```

### Test Path 2: AI Agent Mode (Book Table)
```
1. From Home, tap "AI Agent" button (top-right)
2. In chat, type: "book a table for 5 people at 4 pm"
3. See A2UI booking confirmation:
   ✓ Table Booking Confirmed
   Booking ID: TB-xxxxx
   Number of People: 5
   Booking Time: 4 pm
4. Tap "Home" button (top-left) to return to menu
```

### Test Path 3: Agent Adding to Shared Cart
```
1. From AI Agent screen, type: "add masala dosa to my cart"
2. See A2UI cart update message
3. Tap "Cart" button (top-right) to view cart screen
4. Verify Masala Dosa is in shared cart
5. Can increment/decrement/remove from this screen
6. Tap "Checkout" to complete
```

### Test Path 4: Natural Language Menu Search
```
AI Screen → type any of:
- "show veg items"
- "what desserts do you have"
- "show me non-veg items"
- "what pizzas are available"
→ See A2UI menu cards with filtered results
```

---

## 🎯 Key Interactions

### Manual Mode (Default on Home Screen)
| Action | Result |
|--------|--------|
| Tap item "Add to Cart" | Item added to local cart, A2UI bubble confirms |
| Tap "View Cart" | Navigate to Cart screen |
| Tap "AI Agent" | Switch to AI chat screen |

### AI Agent Mode (Chat Screen)
| Command | Result |
|---------|--------|
| "show menu" | A2UI menu cards displayed |
| "add [item] to my cart" | Item added to shared cart, A2UI confirms |
| "view cart" / "show my cart" | A2UI cart view in bubble |
| "book a table for X people at Y time" | A2UI booking confirmation |
| "checkout" | A2UI order confirmation |

### Buttons & Navigation
- **Top-Left**: Logo ("RestaurantAI")
- **Top-Right** (Home): "AI Agent" button to switch to chat
- **Top-Right** (AI): Home icon, Cart icon, Clear button
- **Bottom**: Input field for messages/search

---

## 📊 Architecture Overview

```
┌─────────────────────────────────────┐
│      Compose UI Layer               │
│  ┌─────────────────────────────┐   │
│  │ HomeScreen   ChatScreen    │   │
│  │ CartScreen   Components    │   │
│  └─────────────────────────────┘   │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│   ViewModel (RestaurantViewModel)   │
│  ┌─────────────────────────────┐   │
│  │ Use Cases + Agents          │   │
│  │ Toggles + Helpers           │   │
│  └─────────────────────────────┘   │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│    Multi-Agent Orchestrator         │
│  ┌─────────────────────────────┐   │
│  │ MenuAgent, CartAgent,       │   │
│  │ BookingAgent, etc.          │   │
│  └─────────────────────────────┘   │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│   Domain Use Cases (Reusable)       │
│  ┌─────────────────────────────┐   │
│  │ SearchMenu, AddToCart,      │   │
│  │ ViewCart, BookTable, etc.   │   │
│  └─────────────────────────────┘   │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│   MenuRepository (Local Data)       │
│  ┌─────────────────────────────┐   │
│  │ Menu Items | Cart State     │   │
│  │ Bookings   | Local Storage  │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 🐛 Troubleshooting

### Issue: App doesn't build
**Solution**:
```powershell
# Clear build cache and rebuild
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

### Issue: Menu items show blank images
- Ensure `assets/menu.json` has valid image URLs
- App uses Coil for async image loading
- Placeholder color is light gray while loading

### Issue: AI Agent doesn't respond
- Check if in manual mode (switch to AI mode via toggle)
- Verify intent detection works (check Logcat for "A2UI_FLOW" logs)
- Try simpler prompts like "show menu"

### Issue: Cart doesn't update across screens
- Both screens use same `MenuRepository` singleton
- If not working, check ViewModel initialization
- Clear app data: Settings → Apps → A2UI Sample → Clear Data

### Issue: Logs not showing
- Open Logcat: `View → Tool Windows → Logcat`
- Filter for "A2UI_FLOW" or "Restaurant"
- Restart app (Kill Process)

---

## 📋 App Features Checklist

- [x] Beautiful home screen with menu cards
- [x] A2UI rendering in chat bubbles
- [x] Manual mode: direct UI interactions
- [x] AI Agent mode: natural language chat
- [x] Multi-agent orchestrator (MenuAgent, CartAgent, BookingAgent)
- [x] Shared cart state across flows
- [x] Image loading for menu items
- [x] Table booking flow with date/time parsing
- [x] Checkout with order confirmation
- [x] Navigation between Home, AI Chat, Cart screens
- [x] Clean architecture (Domain → Data → Agent → UI)

---

## 🔐 Security & Considerations

1. **API Keys**:
   - Gemini key stored in `local.properties` (git-ignored)
   - Never commit real keys to repository

2. **Local Storage**:
   - Cart state in-memory (lost on app restart)
   - Can add SharedPreferences or Room DB for persistence

3. **Network**:
   - App works entirely offline (default "dummy" agent)
   - Optional LLM integration requires internet

---

## 📱 Device Requirements

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 37 (Android 14)
- **Max APK Size**: ~150 MB (with dependencies)
- **RAM**: Min 256 MB, Recommended 512 MB+
- **Screen**: Phone or tablet (Compose responsive)

---

## 🚀 Next Steps

1. ✅ Run the app (see "Build & Install" section)
2. ✅ Test both manual and AI flows (see "Quick Test" section)
3. ✅ Review code structure (see `DEMO_APP_README.md`)
4. ✅ Customize menu (edit `assets/menu.json`)
5. ✅ Add real backend API calls
6. ✅ Implement proper authentication
7. ✅ Add payment integration

---

## 📞 Support

**For issues**:
1. Check Logcat (filter "A2UI_FLOW")
2. Review error messages in app
3. See "Troubleshooting" section
4. Check `DEMO_APP_README.md` for architecture

**For customization**:
- Menu items: Edit `assets/menu.json`
- Colors: Modify theme in `ui/theme/Color.kt`
- Fonts: Update in `ui/theme/Type.kt`
- Agents: Extend `AgentsImpl.kt`

---

**🎉 Ready to Order! Have fun testing the app!**

