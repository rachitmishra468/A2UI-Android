package com.example.a2ui_sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.a2ui.compose.rendering.A2UIRenderer
import org.a2ui.compose.service.A2UISurface
import org.a2ui.compose.service.A2UIRendererState

/**
 * AiRestaurantScreen
 * Purpose: The main UI for the AI Restaurant Assistant.
 * Demonstrates the flow: User Input -> ViewModel -> Agent (ADK) -> A2UI Renderer -> Dynamic UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRestaurantScreen(viewModel: RestaurantViewModel = viewModel()) {
    Scaffold(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("AI Food Assistant", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            ChatInputSection(
                onSendMessage = { query ->
                    viewModel.sendMessage(query)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(viewModel.uiMessages, key = { it.id }) { message ->
                    ChatBubble(message, viewModel)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: UiMessage, viewModel: RestaurantViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (!message.isFromAgent) Alignment.End else Alignment.Start
    ) {
        if (message.isA2UI) {
            // ==========================================
            // A2UI DYNAMIC RENDERING LAYER
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    // Unique renderer for this message to prevent state bleeding
                    val localRenderer = remember { A2UIRenderer() }
                    
                    // Initialize the local renderer with this message's specific content
                    LaunchedEffect(message.a2uiPayloads) {
                        message.a2uiPayloads.forEach { payload ->
                            localRenderer.processMessage(payload)
                        }
                    }

                    A2UISurface(
                        surfaceId = "restaurant_surface",
                        rendererState = A2UIRendererState(localRenderer),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // TRADITIONAL COMPOSE UI
            Surface(
                color = if (!message.isFromAgent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (!message.isFromAgent) 16.dp else 0.dp,
                    bottomEnd = if (!message.isFromAgent) 0.dp else 16.dp
                )
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (!message.isFromAgent) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun ChatInputSection(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Search food (e.g. 'veg burger')...") },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}