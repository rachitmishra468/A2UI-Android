package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val cartItems = viewModel.getCartItems()
    val total = viewModel.getCartTotal()
    var selectedPayment by remember { mutableStateOf("UPI") }
    var selectedType by remember { mutableStateOf("Delivery") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F8F8))
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Order Summary
                item {
                    SectionTitle("Order Summary")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            cartItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${item.quantity}x ${item.menuItem.name}")
                                    Text("₹${item.menuItem.price.amount * item.quantity}")
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", fontWeight = FontWeight.Bold)
                                Text("₹$total", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 2. Delivery / Dine In
                item {
                    SectionTitle("Delivery / Dine In")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeButton("Delivery", selectedType == "Delivery", { selectedType = "Delivery" }, Modifier.weight(1f))
                        TypeButton("Dine In", selectedType == "Dine In", { selectedType = "Dine In" }, Modifier.weight(1f))
                    }
                }

                // 3. Payment Method
                item {
                    SectionTitle("Payment Method")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column {
                            PaymentOption("UPI", selectedPayment == "UPI", { selectedPayment = "UPI" })
                            PaymentOption("Credit/Debit Card", selectedPayment == "Card", { selectedPayment = "Card" })
                            PaymentOption("Cash on Delivery", selectedPayment == "Cash", { selectedPayment = "Cash" })
                        }
                    }
                }

                // 4. Promo Code
                item {
                    SectionTitle("Promo Code")
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter promo code") },
                        trailingIcon = { TextButton(onClick = {}) { Text("Apply") } },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Place Order Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = {
                        val order = viewModel.checkout()
                        if (order != null) {
                            onOrderPlaced(order.id.value)
                        }
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Place Order - ₹${(total * 1.05).toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun TypeButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Text(label)
    }
}

@Composable
fun PaymentOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
