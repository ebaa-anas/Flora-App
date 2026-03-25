package com.example.myapplication

import androidx.compose.foundation.background
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
    cartItems: MutableList<Triple<Plant, String?, Int>>, // Ensure this is a MutableList from MainActivity
    isGift: Boolean,
    onIsGiftChange: (Boolean) -> Unit,
    giftNote: String,
    onGiftNoteChange: (String) -> Unit,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onViewItem: (Plant) -> Unit
) {
    // 1. DYNAMIC TOTALS - Recalculates every time an item is added or removed
    val subtotal by remember(cartItems.size, cartItems.map { it.third + (if(it.second == "Ceramic") 1 else 0) }) {
        derivedStateOf {
            cartItems.sumOf { (plant, potType, qty) ->
                val potFee = if (potType == "Ceramic") 15.0 else 0.0
                (plant.price + potFee) * qty
            }
        }
    }

    val freeDeliveryThreshold = 75.0
    val shippingFee = if (subtotal >= freeDeliveryThreshold || cartItems.isEmpty()) 0.0 else 15.0
    val finalTotal = subtotal + shippingFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Progress Bar for Free Delivery
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
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (subtotal >= freeDeliveryThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // List of Items
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Use itemsIndexed to safely modify the MutableList
                itemsIndexed(cartItems) { index, item ->
                    val (plant, potType, qty) = item

                    CartItemCard(
                        plant = plant,
                        quantity = qty,
                        selectedPot = potType,
                        onPotChange = { newPot ->
                            cartItems[index] = Triple(plant, newPot, qty)
                        },
                        onQuantityChange = { newQty ->
                            cartItems[index] = Triple(plant, potType, newQty)
                        },
                        onRemove = {
                            cartItems.removeAt(index)
                        },
                        onViewDetail = { onViewItem(plant) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Bottom Summary Section
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 20.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CardGiftcard, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Add gift wrap & note", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = isGift, onCheckedChange = onIsGiftChange)
                    }
                    if (isGift) {
                        OutlinedTextField(
                            value = giftNote,
                            onValueChange = onGiftNoteChange,
                            placeholder = { Text("Enter your message...") },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
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
                        enabled = cartItems.isNotEmpty() // Prevents checkout with empty cart
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
    selectedPot: String?,
    onPotChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onViewDetail() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = plant.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // --- ADD THE CODE HERE ---
            Column(modifier = Modifier.weight(1f)) {
                // 1. Calculate the real-time price for THIS specific row
                val potFee = if (selectedPot == "Ceramic") 15.0 else 0.0
                val unitPrice = plant.price + potFee
                val itemTotal = unitPrice * quantity

                Text(plant.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)

                // 2. Display the calculated Total (Plant + Pot * Qty)
                Text(
                    text = "$${String.format("%.2f", itemTotal)}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // POT SELECTOR (Displays the preference)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pot: ", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = selectedPot ?: "Standard",
                        modifier = Modifier.clickable {
                            onPotChange(if (selectedPot == "Ceramic") "Standard" else "Ceramic")
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // QUANTITY PICKER
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (quantity > 1) onQuantityChange(quantity - 1) }) {
                        Icon(Icons.Rounded.Remove, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("$quantity", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            // --- END OF ADDED CODE ---

            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.Red)
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