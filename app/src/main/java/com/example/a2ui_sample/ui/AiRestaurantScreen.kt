package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Home
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.a2ui.compose.rendering.A2UIRenderer
import org.a2ui.compose.rendering.ActionHandler
import org.a2ui.compose.service.A2UISurface
import org.a2ui.compose.service.A2UIRendererState
import com.example.a2ui_sample.domain.model.MenuItem
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextOverflow
import com.example.a2ui_sample.presentation.viewmodel.RestaurantViewModel
import com.example.a2ui_sample.presentation.viewmodel.UiMessage
import com.example.a2ui_sample.presentation.theme.*
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRestaurantScreen(navController: NavController? = null, viewModel: RestaurantViewModel = viewModel()) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages are added
    LaunchedEffect(viewModel.uiMessages.size) {
        if (viewModel.uiMessages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.uiMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RestaurantPrimary,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "💬 AI Assistant",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController?.navigate("home") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(modifier = Modifier.size(40.dp)) {
                        IconButton(
                            onClick = { navController?.navigate("cart") },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        val cartCount = viewModel.getCartItems().size
                        if (cartCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset((-4).dp, 4.dp)
                                    .background(RestaurantSecondary, RoundedCornerShape(50))
                                    .size(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cartCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear Chat",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Chat messages area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
        ) {
            items(viewModel.uiMessages, key = { it.id }) { message ->
                ChatBubble(message, viewModel)
            }
        }

        // Input area
        ChatInputSection(
            onSendMessage = { query ->
                viewModel.sendMessage(query)
            }
        )
    }
}

@Composable
fun ChatBubble(message: UiMessage, viewModel: RestaurantViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (!message.isFromAgent) Alignment.End else Alignment.Start
    ) {
        if (message.isA2UI) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (!message.isFromAgent) 16.dp else 0.dp,
                    bottomEnd = if (!message.isFromAgent) 0.dp else 16.dp
                )
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    val localRenderer = remember(message.id) { A2UIRenderer() }

                    // Wire up "Add to Cart" (and other) button taps in this chat card to the ViewModel
                    LaunchedEffect(localRenderer) {
                        localRenderer.setActionHandler(object : ActionHandler {
                            override fun onAction(surfaceId: String, actionName: String, context: Map<String, Any>) {
                                if (actionName == "addToCart" || actionName == "add_to_cart") {
                                    val itemId = when (val id = context["itemId"]) {
                                        is Number -> id.toInt()
                                        is String -> id.toIntOrNull()
                                        else -> null
                                    }
                                    if (itemId != null) {
                                        viewModel.addItemToCartById(itemId)
                                    }
                                }
                            }
                            override fun openUrl(url: String) {}
                            override fun showToast(message: String) {}
                        })
                    }

                    LaunchedEffect(message.a2uiPayloads, message.id) {
                        message.a2uiPayloads.forEach { payload ->
                            localRenderer.processMessage(payload)
                        }
                    }

                    A2UISurface(
                        surfaceId = "restaurant_surface",
                        rendererState = remember(localRenderer) { A2UIRendererState(localRenderer) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Surface(
                color = if (!message.isFromAgent) RestaurantSecondary else BackgroundCard,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (!message.isFromAgent) 16.dp else 0.dp,
                    bottomEnd = if (!message.isFromAgent) 0.dp else 16.dp
                ),
                modifier = Modifier.fillMaxWidth(0.85f),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    color = if (!message.isFromAgent) Color.White else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ChatInputSection(onSendMessage: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        "Try: 'Show veg items' or 'Book table'",
                        color = TextHint,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = BackgroundGray,
                    unfocusedContainerColor = BackgroundGray
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = RestaurantSecondary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}