package com.example.a2ui_sample.presentation.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.presentation.viewmodel.NavigationEvent
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.viewmodel.UiMessage
import com.example.a2ui_sample.presentation.viewmodel.ChatLoadingState
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.theme.PremiumSpacing
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

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                textState = results[0]
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

    // Observe navigation events
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
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = PremiumColors.Accent.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PremiumColors.Accent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Rango AI Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Elite Support", style = MaterialTheme.typography.labelSmall, color = PremiumColors.Success)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) { Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear") }
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Outlined.LocalMall, contentDescription = "Cart")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            // 1. Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiMessages,
                    key = { it.id }
                ) { message ->
                    PremiumChatBubble(message, viewModel.renderer)
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
                            PremiumProcessingBubble(state)
                        }
                    }
                }
            }

            // 2. Suggestion Pills
            PremiumQuickActions(onAction = { viewModel.sendMessage(it) })

            // 3. Modern Input Area
            PremiumChatInput(
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
fun PremiumChatBubble(message: UiMessage, renderer: A2UIRenderer) {
    val isUser = message.isFromUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isUser && message.isA2UI && message.a2uiPayload != null) {
            A2UIPayloadRenderer(json = message.a2uiPayload, renderer = renderer)
        } else {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) PremiumColors.Accent else PremiumColors.Gray100,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isUser) Color.White else PremiumColors.Gray900,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun PremiumProcessingBubble(state: ChatLoadingState) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(PremiumColors.Gray100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = PremiumColors.Accent
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = state.status,
            fontSize = 13.sp,
            color = PremiumColors.Gray500,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PremiumQuickActions(onAction: (String) -> Unit) {
    val actions = listOf(
        "Suggest a meal",
        "Book a table",
        "Show my cart",
        "Active offers",
        "Track order"
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            Surface(
                modifier = Modifier.clickable { onAction(action) },
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, PremiumColors.Gray200),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = action,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumColors.Gray700
                )
            }
        }
    }
}

@Composable
fun PremiumChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoiceInput) {
                Icon(Icons.Outlined.Mic, contentDescription = null, tint = PremiumColors.Gray400)
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Rango...", color = PremiumColors.Gray400) },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge
            )
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .background(if (text.isNotBlank()) PremiumColors.Accent else PremiumColors.Gray100, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) Color.White else PremiumColors.Gray400,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun A2UIPayloadRenderer(json: String, renderer: A2UIRenderer) {
    val surfaceId = remember(json) {
        try {
            val firstLine = json.trim().split("\n").firstOrNull { it.isNotBlank() } ?: json
            val jsonObj = com.google.gson.JsonParser.parseString(firstLine).asJsonObject

            when {
                jsonObj.has("createSurface") -> jsonObj.getAsJsonObject("createSurface").get("surfaceId").asString
                jsonObj.has("updateComponents") -> jsonObj.getAsJsonObject("updateComponents").get("surfaceId").asString
                jsonObj.has("updateDataModel") -> jsonObj.getAsJsonObject("updateDataModel").get("surfaceId").asString
                else -> {
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
            null
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (surfaceId != null) {
            val surfaceState = renderer.getSurfaceState(surfaceId)

            if (surfaceState is org.a2ui.compose.rendering.A2UIRendererState.Loading) {
                SkeletonCard()
            } else {
                val content = renderer.renderSurface(surfaceId)
                content.invoke()
            }
        } else {
            Text("Error: Could not render UI component", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
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

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
