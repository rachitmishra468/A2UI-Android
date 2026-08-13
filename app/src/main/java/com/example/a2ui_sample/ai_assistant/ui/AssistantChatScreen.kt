package com.example.a2ui_sample.ai_assistant.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.ai_assistant.ui.components.*
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantChatMessage
import com.example.a2ui_sample.ai_assistant.ui.model.AssistantUiState
import com.example.a2ui_sample.ai_assistant.viewmodel.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantChatScreen(
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val messages = viewModel.messages
    val listState = rememberLazyListState()
    var textState by remember { mutableStateOf("") }
    val totalCartQuantity = viewModel.cartItems.sumOf { it.quantity }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!spokenText.isNullOrEmpty()) {
                textState = spokenText
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant (ADK)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                    }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = onNavigateToCart) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    AssistantMessageBubble(message, viewModel)
                }
                
                if (viewModel.isTyping) {
                    item {
                        Text(
                            "Assistant is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            color = Color.Gray
                        )
                    }
                }
            }

            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "How can I help?")
                        }
                        voiceLauncher.launch(intent)
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                    }

                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Menu Assistant...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (textState.isNotBlank()) {
                                viewModel.sendMessage(textState)
                                textState = ""
                            }
                        },
                        enabled = textState.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantMessageBubble(message: AssistantChatMessage, viewModel: AssistantViewModel) {
    val contentName = message.content.javaClass.simpleName
    android.util.Log.d("AssistantFlow", "📱 Rendering Bubble: type=$contentName, isFromUser=${message.isFromUser}")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        when (val content = message.content) {
            is AssistantUiState.TextResponse -> {
                TextBubble(text = content.text, isFromUser = message.isFromUser)
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
                OrderStatusCard(message = content.message, status = content.status, eta = content.eta)
            }
            is AssistantUiState.Error -> {
                TextBubble(text = content.message, isFromUser = false, isError = true)
            }
            else -> {}
        }
    }
}

@Composable
fun TextBubble(text: String, isFromUser: Boolean, isError: Boolean = false) {
    val bgColor = when {
        isFromUser -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when {
        isFromUser -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isFromUser) 16.dp else 0.dp,
            bottomEnd = if (isFromUser) 0.dp else 16.dp
        ),
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
