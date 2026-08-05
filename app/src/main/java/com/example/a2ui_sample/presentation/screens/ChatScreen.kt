package com.example.a2ui_sample.presentation.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import com.example.a2ui_sample.presentation.viewmodel.ChatLoadingState

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

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                textState = spokenText
                // Automatically send if needed, or just fill the box
                // viewModel.sendMessage(spokenText)
            }
        }
    }

    val onVoiceInputClick = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        voiceLauncher.launch(intent)
    }

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
            listState.animateScrollToItem(uiMessages.size)
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
                        val cartItems by viewModel.cartItems.collectAsState()
                        val cartCount = cartItems.sumOf { it.quantity }
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
                items(
                    items = uiMessages,
                    key = { it.id }
                ) { message ->
                    ChatBubble(message, viewModel.renderer)
                }

                // Loading State
                item {
                    val loadingState by viewModel.loadingState.collectAsState()
                    AnimatedVisibility(
                        visible = loadingState != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        loadingState?.let { state ->
                            ProcessingBubble(state)
                        }
                    }
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
                },
                onVoiceInput = onVoiceInputClick
            )
        }
    }
}

@Composable
fun ProcessingBubble(state: ChatLoadingState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypingIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.status,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // If we have skeleton data or multi-steps, we could show them here
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonCard()
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val dotCount = 3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until dotCount) {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 200)
                )
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun SkeletonCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .background(Color.LightGray.copy(alpha = alpha), RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(14.dp)
                .background(Color.LightGray.copy(alpha = alpha), RoundedCornerShape(4.dp))
        )
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
    // Extract surfaceId from JSON (handles single object or multi-line JSONL)
    val surfaceId = remember(json) {
        try {
            val firstLine = json.trim().split("\n").firstOrNull { it.isNotBlank() } ?: json
            val jsonObj = com.google.gson.JsonParser.parseString(firstLine).asJsonObject
            
            when {
                jsonObj.has("createSurface") -> jsonObj.getAsJsonObject("createSurface").get("surfaceId").asString
                jsonObj.has("updateComponents") -> jsonObj.getAsJsonObject("updateComponents").get("surfaceId").asString
                jsonObj.has("updateDataModel") -> jsonObj.getAsJsonObject("updateDataModel").get("surfaceId").asString
                else -> {
                    // Search all lines if not in first
                    val allLines = json.trim().split("\n")
                    var foundId: String? = null
                    for (line in allLines) {
                        try {
                            val obj = com.google.gson.JsonParser.parseString(line).asJsonObject
                            foundId = when {
                                obj.has("createSurface") -> obj.getAsJsonObject("createSurface").get("surfaceId").asString
                                obj.has("updateComponents") -> obj.getAsJsonObject("updateComponents").get("surfaceId").asString
                                obj.has("updateDataModel") -> obj.getAsJsonObject("updateDataModel").get("surfaceId").asString
                                else -> null
                            }
                            if (foundId != null) break
                        } catch (_: Exception) {}
                    }
                    foundId
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("A2UI_RESTORE", "Error parsing JSON ID: ${e.message}")
            null
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (surfaceId != null) {
            val surfaceState = renderer.getSurfaceState(surfaceId)
            
            if (surfaceState is org.a2ui.compose.rendering.A2UIRendererState.Loading) {
                SkeletonCard()
            } else {
                android.util.Log.d("A2UI_RESTORE", "Rendering Started for $surfaceId")
                val content = renderer.renderSurface(surfaceId)
                content.invoke()
                android.util.Log.d("A2UI_RESTORE", "Rendering Completed for $surfaceId")
            }
        } else {
            android.util.Log.e("A2UI_RESTORE", "Missing JSON or invalid surfaceId")
            Text("Error: Could not render UI component", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
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
        "Take me to checkout",
        "Show my previous feedback",
        "Rate my last order",
        "Feedback Dashboard"
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
fun ChatInput(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, onVoiceInput: () -> Unit) {
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
            IconButton(onClick = onVoiceInput) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(4.dp))
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
