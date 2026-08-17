package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel
import com.example.a2ui_sample.presentation.theme.PremiumColors
import com.example.a2ui_sample.presentation.components.PremiumCard
import com.example.a2ui_sample.presentation.components.PremiumButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal = cartItems.sumOf { it.quantity * it.menuItem.price.amount }
    val tax = (subtotal * 0.05).toInt()
    val total = subtotal + tax
    
    var selectedPayment by remember { mutableStateOf("UPI") }
    var selectedType by remember { mutableStateOf("Delivery") }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Bold) },
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Service Type Segmented Control
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PremiumColors.Gray100, CircleShape)
                            .padding(4.dp)
                    ) {
                        ServiceTypeItem("Delivery", selectedType == "Delivery", { selectedType = "Delivery" }, Modifier.weight(1f))
                        ServiceTypeItem("Dine In", selectedType == "Dine In", { selectedType = "Dine In" }, Modifier.weight(1f))
                    }
                }

                // Payment Methods
                item {
                    SectionHeader("Payment Method")
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PremiumPaymentOption("UPI / GPay", Icons.Outlined.AccountBalanceWallet, selectedPayment == "UPI", { selectedPayment = "UPI" })
                            HorizontalDivider(color = PremiumColors.Gray50)
                            PremiumPaymentOption("Credit / Debit Card", Icons.Outlined.CreditCard, selectedPayment == "Card", { selectedPayment = "Card" })
                            HorizontalDivider(color = PremiumColors.Gray50)
                            PremiumPaymentOption("Cash on Delivery", Icons.Outlined.Payments, selectedPayment == "Cash", { selectedPayment = "Cash" })
                        }
                    }
                }

                // Summary
                item {
                    SectionHeader("Order Summary")
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.quantity}x ${item.menuItem.name}", color = PremiumColors.Gray700, fontSize = 14.sp)
                                Text("₹${item.menuItem.price.amount * item.quantity}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = PremiumColors.Gray100)
                        DetailRowItem("Subtotal", "₹$subtotal")
                        DetailRowItem("Service Tax (5%)", "₹$tax")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("₹$total", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PremiumColors.Accent)
                        }
                    }
                }
            }

            // Sticky Bottom Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                PremiumButton(
                    text = "Place Order • ₹$total",
                    onClick = {
                        scope.launch {
                            val order = viewModel.checkout()
                            if (order != null) {
                                onOrderPlaced(order.id.value)
                            }
                        }
                    },
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    containerColor = PremiumColors.Primary
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ServiceTypeItem(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(40.dp),
        onClick = onClick,
        color = if (isSelected) Color.White else Color.Transparent,
        shape = CircleShape,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected) PremiumColors.Primary else PremiumColors.Gray500
            )
        }
    }
}

@Composable
fun PremiumPaymentOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) PremiumColors.Accent else PremiumColors.Gray400, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 15.sp)
            RadioButton(
                selected = isSelected, 
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PremiumColors.Accent)
            )
        }
    }
}

@Composable
fun DetailRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = PremiumColors.Gray500, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
