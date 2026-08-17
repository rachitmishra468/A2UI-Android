package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.a2ui_sample.domain.model.MenuItem
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.components.PremiumCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("All", "Burgers", "Sides", "Beverages", "Desserts", "Combos")
    var selectedCategory by remember { mutableStateOf("All") }

    val allItems = remember { viewModel.getAllMenuItems() }
    val cartItems by viewModel.cartItems.collectAsState()
    
    val filteredItems = remember(searchQuery, selectedCategory, allItems) {
        allItems.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) || 
                               item.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        BadgedBox(
                            badge = {
                                val cartCount = cartItems.sumOf { it.quantity }
                                if (cartCount > 0) {
                                    Badge(containerColor = PremiumColors.Accent) { Text(cartCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.LocalMall, contentDescription = "Cart")
                        }
                    }
                }
            )
        },
        bottomBar = {
            val cartCount = cartItems.sumOf { it.quantity }
            val cartTotal = cartItems.sumOf { it.quantity * it.menuItem.price.amount }
            
            if (cartCount > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = PremiumColors.Primary,
                    shadowElevation = 8.dp,
                    onClick = onNavigateToCart
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("$cartCount items added", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text("₹$cartTotal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Cart", color = Color.White, fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Premium Search
            PremiumSearchBar(searchQuery) { searchQuery = it }

            // Category Scroll
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        modifier = Modifier.clip(CircleShape),
                        color = if (isSelected) PremiumColors.Accent else PremiumColors.Gray100,
                        onClick = { selectedCategory = category }
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color = if (isSelected) Color.White else PremiumColors.Gray600,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Menu List
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredItems) { item ->
                    val cartItem = cartItems.find { it.menuItem.id == item.id }
                    val quantity = cartItem?.quantity ?: 0
                    
                    PremiumMenuItemCard(
                        item = item, 
                        quantity = quantity,
                        onAdd = { viewModel.addToCart(item.id) },
                        onUpdateQuantity = { newQty -> viewModel.updateCartQuantity(item.id, newQty) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        placeholder = { Text("Search our menu", color = PremiumColors.Gray400) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PremiumColors.Gray500) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = PremiumColors.Gray100,
            focusedContainerColor = PremiumColors.Gray100
        ),
        singleLine = true
    )
}

@Composable
fun PremiumMenuItemCard(
    item: MenuItem, 
    quantity: Int,
    onAdd: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = PremiumColors.Gray900
                )
                Text(
                    text = item.description,
                    color = PremiumColors.Gray500,
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "₹${item.price.amount}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = PremiumColors.Accent
                    )
                    
                    if (quantity > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(PremiumColors.Gray100, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(onClick = { onUpdateQuantity(quantity - 1) }, modifier = Modifier.size(28.dp)) { 
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp)) 
                            }
                            Text(
                                quantity.toString(), 
                                modifier = Modifier.padding(horizontal = 12.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { onUpdateQuantity(quantity + 1) }, modifier = Modifier.size(28.dp)) { 
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) 
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier
                                .size(32.dp)
                                .background(PremiumColors.Primary, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
