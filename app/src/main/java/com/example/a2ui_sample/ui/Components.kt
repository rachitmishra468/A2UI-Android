package com.example.a2ui_sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.a2ui_sample.data.CartItem
import com.example.a2ui_sample.data.MenuItem

/**
 * File: Components.kt
 * Purpose: Contains reusable UI components for the Restaurant Assistant.
 */

@Composable
fun MenuCard(item: MenuItem) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = item.image,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(text = "₹${item.price}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MenuCardVertical(item: MenuItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.image,
                contentDescription = item.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "${item.type} • ${item.category}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "₹${item.price}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RecommendationCard(item: MenuItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = item.image,
                contentDescription = item.name,
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = item.name, fontWeight = FontWeight.Bold)
                Text(text = "Chef's Special • ₹${item.price}", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CartSummaryCard(cartItems: List<CartItem>, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Cart Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            cartItems.forEach {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "${it.menuItem.name} x ${it.quantity}")
                    Text(text = "₹${it.menuItem.price * it.quantity}")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Total Amount", fontWeight = FontWeight.Bold)
                Text(text = "₹$total", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MenuList(items: List<MenuItem>) {
    LazyRow {
        items(items) { item ->
            MenuCard(item)
        }
    }
}

@Composable
fun MenuListVertical(items: List<MenuItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items) { item ->
            MenuCardVertical(item)
        }
    }
}

