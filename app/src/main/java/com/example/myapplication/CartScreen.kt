package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
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
import coil.compose.AsyncImage
import com.example.myapplication.model.Plant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartItems: MutableList<Pair<Plant, String?>>,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onViewItem: (Plant) -> Unit
) {
    var isGift by remember { mutableStateOf(false) }
    var giftNote by remember { mutableStateOf("") }

    // 1. DYNAMIC QUANTITY TRACKER
    // We use a Map to track quantities for each index in the cart
    val itemQuantities = remember { mutableStateMapOf<Int, Int>() }

    // Initialize quantities to 1 if not already set
    cartItems.indices.forEach { index ->
        if (!itemQuantities.containsKey(index)) itemQuantities[index] = 1
    }

    // 2. DYNAMIC TOTALS (Now recalculates when quantities change)
    val subtotal by remember(cartItems, itemQuantities.toMap()) {
        derivedStateOf {
            cartItems.indices.sumOf { index ->
                val plant = cartItems[index].first
                val qty = itemQuantities[index] ?: 1
                plant.price * qty
            }
        }
    }

    val freeDeliveryThreshold = 75.0 // Set to your desired goal
    val shippingFee = if (subtotal >= freeDeliveryThreshold || cartItems.isEmpty()) 0.0 else 15.0
    val finalTotal = subtotal + shippingFee

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Shopping Cart", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // 3. THE LIVE PROGRESS BAR
            if (cartItems.isNotEmpty()) {
                val progress = (subtotal / freeDeliveryThreshold).coerceIn(0.0, 1.0).toFloat()
                val remaining = freeDeliveryThreshold - subtotal

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (subtotal >= freeDeliveryThreshold) "Free Delivery Unlocked! 🌿"
                            else "Add $${String.format("%.2f", remaining)} for Free Delivery",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (subtotal >= freeDeliveryThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(cartItems) { index, item ->
                    val currentQty = itemQuantities[index] ?: 1

                    CartItemCard(
                        plant = item.first,
                        quantity = currentQty,
                        onQuantityChange = { newQty -> itemQuantities[index] = newQty },
                        onRemove = {
                            cartItems.removeAt(index)
                            itemQuantities.remove(index)
                        },
                        onViewDetail = { onViewItem(item.first) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // 4. STICKY SUMMARY
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 20.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Gift Wrap Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CardGiftcard, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add gift wrap & personal note", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = isGift, onCheckedChange = { isGift = it })
                    }

                    if (isGift) {
                        OutlinedTextField(
                            value = giftNote, onValueChange = { giftNote = it },
                            placeholder = { Text("Enter your message...") },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.05f))

                    PriceRow(label = "Subtotal", amount = subtotal)
                    PriceRow(label = "Delivery Fee", amount = shippingFee, isFree = subtotal >= freeDeliveryThreshold)

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("$${String.format("%.2f", finalTotal)}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = onCheckout,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(64.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                    ) {
                        Text("Secure Checkout", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    plant: Plant,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onViewDetail() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = plant.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(plant.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("$${plant.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)

                Spacer(modifier = Modifier.height(12.dp))

                // QUANTITY PICKER
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)).padding(2.dp)
                ) {
                    IconButton(onClick = { if (quantity > 1) onQuantityChange(quantity - 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Remove, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("$quantity", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { onQuantityChange(quantity + 1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.background(Color(0xFFFFEBEE).copy(alpha = 0.1f), CircleShape)) {
                Icon(Icons.Rounded.DeleteOutline, "Remove", tint = Color(0xFFE63946))
            }
        }
    }
}

@Composable
fun PriceRow(label: String, amount: Double, isFree: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        Text(
            text = if (isFree && label == "Delivery Fee") "FREE" else "$${String.format("%.2f", amount)}",
            fontWeight = FontWeight.Bold,
            color = if (isFree && label == "Delivery Fee") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}