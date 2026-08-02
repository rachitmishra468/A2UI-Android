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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.a2ui_sample.presentation.viewmodel.RestaurantViewModel
import com.example.a2ui_sample.presentation.viewmodel.UiMessage
import org.a2ui.compose.rendering.A2UIRenderer
import org.a2ui.compose.rendering.ActionHandler
import org.a2ui.compose.service.A2UIRendererState
import org.a2ui.compose.service.A2UISurface

private val WhatsAppBackground = Color(0xFFECE5DD)
private val WhatsAppHeader = Color(0xFF075E54)
private val OutgoingBubble = Color(0xFFDCF8C6)
private val IncomingBubble = Color.White
private val SendButton = Color(0xFF25D366)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRestaurantScreen(
    navController: NavController? = null,
    viewModel: RestaurantViewModel = viewModel()
) {
    val listState = rememberLazyListState()
    var inputBarHeightPx by remember { mutableIntStateOf(0) }
    val inputBarHeightDp = with(LocalDensity.current) { inputBarHeightPx.toDp() }

    LaunchedEffect(viewModel.uiMessages.size) {
        if (viewModel.uiMessages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.uiMessages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                cartCount = viewModel.getCartItems().size,
                onHomeClick = { navController?.navigate("home") },
                onCartClick = { navController?.navigate("cart") },
                onClearClick = viewModel::clearChat
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp,
                    top = 10.dp,
                    bottom = inputBarHeightDp + 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(viewModel.uiMessages, key = { it.id }) { message ->
                    MessageRow(message = message, viewModel = viewModel)
                }
            }
        }

        // Transparent IME container keeps background static and lifts only the input UI.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
        ) {
            ChatInputBar(
                modifier = Modifier.onSizeChanged { inputBarHeightPx = it.height },
                onSendMessage = viewModel::sendMessage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    cartCount: Int,
    onHomeClick: () -> Unit,
    onCartClick: () -> Unit,
    onClearClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "AI Assistant",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        },
        actions = {
            IconButton(onClick = onHomeClick) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
            }
            Box {
                IconButton(onClick = onCartClick) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                }
                if (cartCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp),
                        color = Color(0xFFFF3B30),
                        shape = CircleShape
                    ) {
                        Text(
                            text = cartCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onClearClick) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = WhatsAppHeader,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
private fun MessageRow(message: UiMessage, viewModel: RestaurantViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromAgent) Arrangement.Start else Arrangement.End
    ) {
        if (message.isA2UI) {
            A2UIBubble(message = message, viewModel = viewModel)
        } else {
            TextBubble(message = message)
        }
    }
}

@Composable
private fun A2UIBubble(message: UiMessage, viewModel: RestaurantViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(0.92f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IncomingBubble),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            val renderer = remember(message.id) { A2UIRenderer() }

            LaunchedEffect(renderer) {
                renderer.setActionHandler(createActionHandler(viewModel))
            }

            LaunchedEffect(message.id, message.a2uiPayloads) {
                message.a2uiPayloads.forEach(renderer::processMessage)
            }

            A2UISurface(
                surfaceId = "restaurant_surface",
                rendererState = remember(renderer) { A2UIRendererState(renderer) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TextBubble(message: UiMessage) {
    val isAgent = message.isFromAgent
    Surface(
        modifier = Modifier.fillMaxWidth(0.86f),
        color = if (isAgent) IncomingBubble else OutgoingBubble,
        shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 14.dp,
            bottomStart = if (isAgent) 4.dp else 14.dp,
            bottomEnd = if (isAgent) 14.dp else 4.dp
        ),
        shadowElevation = 1.dp
    ) {
        Text(
            text = message.content,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1F1F1F)
        )
    }
}

private fun createActionHandler(viewModel: RestaurantViewModel): ActionHandler {
    return object : ActionHandler {
        override fun onAction(surfaceId: String, actionName: String, context: Map<String, Any>) {
            if (actionName != "addToCart" && actionName != "add_to_cart") return
            val itemId = extractItemId(context) ?: return
            viewModel.addItemToCartById(itemId)
        }

        override fun openUrl(url: String) {
            // No-op: external URL opening is intentionally disabled for this screen.
        }

        override fun showToast(message: String) {
            // No-op: toast behavior is intentionally delegated to app-level UX.
        }
    }
}

private fun extractItemId(context: Map<String, Any>): Int? {
    val raw = context["itemId"] ?: return null
    return when (raw) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull()
        else -> null
    }
}

@Composable
private fun ChatInputBar(
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    placeholder = {
                        Text("Type a message", color = Color(0xFF8E8E93), fontSize = 14.sp)
                    },
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = WhatsAppHeader
                    )
                )
            }

            IconButton(
                onClick = {
                    val message = input.trim()
                    if (message.isNotEmpty()) {
                        onSendMessage(message)
                        input = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(SendButton, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}
