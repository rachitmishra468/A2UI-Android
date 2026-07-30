package com.example.a2ui_sample.presentation.a2ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.a2ui_sample.presentation.viewmodel.RestaurantViewModel
import org.a2ui.compose.rendering.ActionHandler

/**
 * MenuA2UISection
 * A section that renders the restaurant menu using A2UI.
 */
@Composable
fun MenuA2UISection(viewModel: RestaurantViewModel) {
    val builder = remember { MenuA2UIBuilder() }
    val renderer = viewModel.renderer
    val surfaceId = builder.getSurfaceId()

    // Initialize the surface and schema once
    LaunchedEffect(Unit) {
        renderer.processMessage(builder.createSurfaceJson())
        renderer.processMessage(builder.buildMenuSchema())
    }

    // Update the data model when the menu items change in the ViewModel
    val items = viewModel.getMenuItems()
    LaunchedEffect(items) {
        android.util.Log.d("A2UI_DEBUG", "Sending ${items.size} items to A2UI surface: $surfaceId")
        renderer.processMessage(builder.buildMenuData(items))
    }

    // Set up the action handler for A2UI events (like "add_to_cart")
    LaunchedEffect(renderer) {
        renderer.setActionHandler(object : ActionHandler {
            override fun onAction(surfaceId: String, actionName: String, context: Map<String, Any>) {
                if (actionName == "add_to_cart") {
                    // Extract itemId from the action context
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
            override fun openUrl(url: String) {
                // Handle URL opening if needed
            }
            override fun showToast(message: String) {
                // Handle toast display if needed
            }
        })
    }

    // Render the A2UI surface within a Box with proper padding
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        renderer.renderSurface(surfaceId).invoke()
    }
}
