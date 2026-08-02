package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.presentation.viewmodel.NavigationEvent
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.viewmodel.UiMessage
import kotlinx.coroutines.flow.collectLatest
import org.a2ui.compose.rendering.A2UIRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToCheckout: () -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToMenu: () -> Unit = {},
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val uiMessages = viewModel.uiMessages
    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }

    // Observe navigation events from ViewModel (A2UI actions)
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToCart -> onNavigateToCart()
                is NavigationEvent.NavigateToCheckout -> onNavigateToCheckout()
                is NavigationEvent.NavigateToBookings -> onNavigateToBookings()
                is NavigationEvent.NavigateToOrders -> onNavigateToOrders()
                is NavigationEvent.NavigateToMenu -> onNavigateToMenu()
            }
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(uiMessages.size) {
        if (uiMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Assistant")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) { Icon(Icons.Default.DeleteSweep, contentDescription = "Clear") }
                    Box {
                        IconButton(onClick = onNavigateToCart) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                        val cartCount = viewModel.getCartItems().sumOf { it.quantity }
                        if (cartCount > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                containerColor = MaterialTheme.colorScheme.error
                            ) { Text(cartCount.toString()) }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF0F2F5))
                .imePadding() // Fix for keyboard hiding input
        ) {
            // 1. Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiMessages) { message ->
                    ChatBubble(message, viewModel.renderer)
                }
            }

            // 2. Quick Actions
            QuickActions(onAction = { viewModel.sendMessage(it) })

            // 3. Input Area
            ChatInput(
                text = textState,
                onTextChange = { textState = it },
                onSend = {
                    if (textState.isNotBlank()) {
                        viewModel.sendMessage(textState)
                        textState = ""
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: UiMessage, renderer: A2UIRenderer) {
    val isUser = message.isFromUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val color = if (isUser) MaterialTheme.colorScheme.primary else Color.White
    val textColor = if (isUser) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        if (!isUser && message.isA2UI && message.a2uiPayload != null) {
            // Render structured A2UI payload
            A2UIPayloadRenderer(json = message.a2uiPayload, renderer = renderer)
        } else {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 0.dp,
                    bottomEnd = if (isUser) 0.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = color),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun A2UIPayloadRenderer(json: String, renderer: A2UIRenderer) {
    // Extract surfaceId from JSON to only render what's relevant to this message
    val surfaceId = remember(json) {
        try {
            val jsonObj = com.google.gson.JsonParser.parseString(json).asJsonObject
            when {
                jsonObj.has("createSurface") -> jsonObj.getAsJsonObject("createSurface").get("surfaceId").asString
                jsonObj.has("updateComponents") -> jsonObj.getAsJsonObject("updateComponents").get("surfaceId").asString
                jsonObj.has("updateDataModel") -> jsonObj.getAsJsonObject("updateDataModel").get("surfaceId").asString
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Note: processMessage is now handled in RestaurantMainViewModel for better sequencing
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        surfaceId?.let { id ->
            android.util.Log.d("A2UI_FLOW", "9. Rendering surface: $id")
            val content = renderer.renderSurface(id)
            content.invoke()
        } ?: run {
            android.util.Log.w("A2UI_FLOW", "9. No surfaceId found in JSON")
            // Fallback: render all if surfaceId not found
            renderer.getAllSurfaceIds().forEach { id ->
                val content = renderer.renderSurface(id)
                content.invoke()
            }
        }
    }
}

@Composable
fun QuickActions(onAction: (String) -> Unit) {
    val actions = listOf(
        "I'm hungry, what do you recommend?",
        "Build a meal under ₹300",
        "Suggest something spicy",
        "What's your most popular combo?",
        "Help me order lunch",
        "I want a vegetarian meal",
        "Show today's offers",
        "Book a table for tonight",
        "Show my cart",
        "Repeat my last order",
        "Take me to checkout"
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            SuggestionChip(
                onClick = { onAction(action) },
                label = { Text(action) },
                shape = CircleShape
            )
        }
    }
}

@Composable
fun ChatInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Type a message...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = Color(0xFFF0F2F5),
                    focusedContainerColor = Color(0xFFF0F2F5)
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}
