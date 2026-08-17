package com.example.a2ui_sample.ai_assistant.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.ai_assistant.ui.components.*
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantChatMessage
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantUiState
import com.example.a2ui_sample.ai_assistant.viewmodel.AssistantNavigationEvent
import com.example.a2ui_sample.ai_assistant.viewmodel.AssistantViewModel
import com.example.a2ui_sample.presentation.theme.PremiumColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChatScreen(
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val messages = viewModel.messages
    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val totalCartQuantity = viewModel.cartItems.sumOf { it.quantity }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is AssistantNavigationEvent.NavigateToCheckout -> onNavigateToCheckout()
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isRecording = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!spokenText.isNullOrEmpty()) {
                textState = spokenText
                viewModel.sendMessage(spokenText)
                textState = ""
            }
        }
    }

    val onVoiceInputClick = {
        isRecording = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "How can I help?")
        }
        voiceLauncher.launch(intent)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                            Text("Rango Assistant ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Elite AI ", style = MaterialTheme.typography.labelSmall, color = PremiumColors.Success)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear Chat")
                    }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = onNavigateToCart) {
                            Icon(Icons.Outlined.LocalMall, contentDescription = "Cart")
                        }
                        if (totalCartQuantity > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp)
                            ) {
                                Text(totalCartQuantity.toString())
                            }
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
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .animateContentSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { message ->
                    AssistantMessageBubble(message, viewModel)
                }
                
                if (viewModel.isTyping) {
                    item {
                        PremiumProcessingBubble("Assistant is thinking...")
                    }
                }
            }

            // Quick Suggestions
            AssistantQuickActions(onAction = { viewModel.sendMessage(it) })

            // Modern Input Area
            AssistantPremiumInput(
                text = textState,
                onTextChange = { textState = it },
                onSend = {
                    if (textState.isNotBlank()) {
                        viewModel.sendMessage(textState)
                        textState = ""
                    }
                },
                onVoiceInput = onVoiceInputClick,
                isRecording = isRecording
            )
        }
    }
}

@Composable
fun AssistantPremiumInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    isRecording: Boolean
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
                Icon(
                    Icons.Outlined.Mic, 
                    contentDescription = null, 
                    tint = if (isRecording) Color.Red else PremiumColors.Gray400
                )
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Rango Assistant...", color = PremiumColors.Gray400) },
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
fun AssistantQuickActions(onAction: (String) -> Unit) {
    val actions = listOf(
        "Today's Specials 🌟",
        "Family Combos 👨‍👩‍👧‍👦",
        "Recommend burgers 🍔",
        "Dinner suggestions 🌃",
        "Healthy salads 🥗",
        "Cold beverages 🥤",
        "Track my order 📍",
        "Any coupons? 🏷️"
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
fun PremiumProcessingBubble(status: String) {
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
            text = status,
            fontSize = 13.sp,
            color = PremiumColors.Gray500,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AssistantMessageBubble(message: AssistantChatMessage, viewModel: AssistantViewModel) {
    val isUser = message.isFromUser
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        when (val content = message.content) {
            is AssistantUiState.TextResponse -> {
                PremiumTextBubble(text = content.text, isFromUser = isUser)
            }
            is AssistantUiState.MenuSearch -> {
                MenuHorizontalList(items = content.items, viewModel = viewModel)
            }
            is AssistantUiState.Recommendations -> {
                MenuHorizontalList(items = content.items, viewModel = viewModel)
            }
            is AssistantUiState.MenuDetails -> {
                MenuDetailCard(item = content.item, viewModel = viewModel)
            }
            is AssistantUiState.CartUpdate -> {
                CartUpdateCard(item = content.item, quantity = content.quantity, message = content.message)
            }
            is AssistantUiState.CartView -> {
                CartCard(items = content.items, total = content.total, message = content.message)
            }
            is AssistantUiState.BookingResult -> {
                BookingCard(message = content.message, date = content.date, time = content.time, guests = content.guests)
            }
            is AssistantUiState.FeedbackResult -> {
                FeedbackCard(message = content.message, rating = content.rating)
            }
            is AssistantUiState.OrderStatus -> {
                OrderStatusCard(message = content.message, status = content.status, eta = content.eta, progress = content.progress)
            }
            is AssistantUiState.RatingRequest -> {
                RatingRequestCard(orderId = content.orderId, message = content.message, viewModel = viewModel)
            }
            is AssistantUiState.CheckoutSummary -> {
                CheckoutCard(items = content.items, total = content.total, message = content.message, viewModel = viewModel)
            }
            is AssistantUiState.CouponList -> {
                CouponListCard(coupons = content.coupons, message = content.message)
            }
            is AssistantUiState.InfoCard -> {
                InfoDisplayCard(title = content.title, content = content.content, icon = content.icon)
            }
            is AssistantUiState.Error -> {
                PremiumTextBubble(text = content.message, isFromUser = false, isError = true)
            }
            else -> {}
        }
    }
}

@Composable
fun PremiumTextBubble(text: String, isFromUser: Boolean, isError: Boolean = false) {
    if (text.isBlank()) return

    Surface(
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isFromUser) 16.dp else 4.dp,
            bottomEnd = if (isFromUser) 4.dp else 16.dp
        ),
        color = when {
            isFromUser -> PremiumColors.Accent
            isError -> MaterialTheme.colorScheme.errorContainer
            else -> PremiumColors.Gray100
        },
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = when {
                isFromUser -> Color.White
                isError -> MaterialTheme.colorScheme.onErrorContainer
                else -> PremiumColors.Gray900
            },
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
    }
}
