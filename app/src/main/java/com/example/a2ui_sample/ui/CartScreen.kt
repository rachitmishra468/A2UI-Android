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
fun CartScreen(navController: NavController, viewModel: RestaurantViewModel = viewModel()) {
    Scaffold(topBar = { TopAppBar(title = { Text("Your Cart") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            val cart = viewModel.getCartItems()
            if (cart.isEmpty()) {
                Text("Your cart is empty")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart) { ci ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(ci.menuItem.name, style = MaterialTheme.typography.titleMedium)
                                    Text("₹${ci.menuItem.price} x ${ci.quantity} = ₹${ci.menuItem.price * ci.quantity}")
                                }
                                Column {
                                    Row {
                                        Button(onClick = { viewModel.updateCartQuantity(ci.menuItem.id, ci.quantity + 1) }) { Text("+") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { viewModel.updateCartQuantity(ci.menuItem.id, ci.quantity - 1) }) { Text("-") }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { viewModel.removeCartItem(ci.menuItem.id) }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }

                Text("Total: ₹${viewModel.repository.getCartTotal()}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.checkoutCart(); navController.navigate("home") }) { Text("Checkout") }
                    Button(onClick = { navController.navigate("ai") }) { Text("Back to AI Agent") }
                }
            }
        }
    }
}

