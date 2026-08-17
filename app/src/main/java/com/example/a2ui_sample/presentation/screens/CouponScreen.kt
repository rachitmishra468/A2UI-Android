package com.example.a2ui_sample.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.a2ui_sample.presentation.components.PremiumCouponCard
import com.example.a2ui_sample.presentation.viewmodel.RestaurantMainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    onBack: () -> Unit,
    viewModel: RestaurantMainViewModel = hiltViewModel()
) {
    // In a real app, you'd fetch this from a separate CouponViewModel
    // For now, we'll use a mocked list or fetch from repository via main VM if available
    val coupons by viewModel.availableCoupons.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Coupons", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (coupons.isEmpty()) {
                item {
                    Text("No coupons available at the moment.", modifier = Modifier.padding(16.dp))
                }
            } else {
                items(coupons) { coupon ->
                    PremiumCouponCard(
                        coupon = coupon,
                        onCopy = { /* Logic to copy to clipboard */ }
                    )
                }
            }
        }
    }
}
