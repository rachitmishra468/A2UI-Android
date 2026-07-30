package com.example.a2ui_sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController, viewModel: RestaurantViewModel = viewModel()) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Restaurant Home") })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text("Menu", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            val items = viewModel.getMenuItems()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("₹${item.price}")
                            }
                            Column {
                                Button(onClick = { viewModel.addItemToCartById(item.id) }) { Text("Add (Manual)") }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(onClick = { navController.navigate("ai") }) { Text("Open AI Agent") }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { navController.navigate("cart") }) { Text("View Cart") }
                Button(onClick = { navController.navigate("ai") }) { Text("AI Agent Chat") }
            }
        }
    }
}

