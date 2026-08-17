package com.example.a2ui_sample.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a2ui_sample.domain.model.Coupon
import com.example.a2ui_sample.presentation.theme.PremiumColors

@Composable
fun PremiumCouponCard(
    coupon: Coupon,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(PremiumColors.Accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, tint = PremiumColors.Accent)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coupon.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PremiumColors.Accent
                )
                Text(
                    text = coupon.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PremiumColors.Gray500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = PremiumColors.Success.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${coupon.discountPercentage}% OFF",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = PremiumColors.Success,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = { onCopy(coupon.code) },
                modifier = Modifier.background(PremiumColors.Gray100, CircleShape)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
            }
        }
    }
}
