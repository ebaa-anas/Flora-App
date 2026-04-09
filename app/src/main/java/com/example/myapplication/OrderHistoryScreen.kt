package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.Plant
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// --- DATA MODELS ---
data class OrderData(
    val fullId: String,
    val displayId: String,
    val status: String,
    val price: String,
    val date: String
)

data class OrderItemDisplay(
    val plantName: String,
    val potType: String,
    val quantity: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    onTrackOrder: (String) -> Unit
) {
    var orders by remember { mutableStateOf<List<OrderData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrderForDetails by remember { mutableStateOf<OrderData?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val response = supabase.from("orders")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

                    orders = response.map { map ->
                        val rawId = map["id"]?.toString()?.replace("\"", "") ?: ""
                        val rawTotal = map["total_amount"]?.toString()?.replace("\"", "")?.toDoubleOrNull() ?: 0.0
                        val rawDate = map["created_at"]?.toString()?.replace("\"", "") ?: ""
                        val formattedDate = if (rawDate.length >= 10) rawDate.take(10) else "Recent"

                        // --- AUTO-PROGRESSION TIME ENGINE FOR HISTORY ---
                        val cleanDate = if (rawDate.contains(".")) rawDate.substringBefore(".") else rawDate
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")

                        val timeMilli = try {
                            sdf.parse(cleanDate)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }

                        val nowMillis = System.currentTimeMillis()
                        val diffMinutes = ((nowMillis - timeMilli) / (1000 * 60)).toInt()

                        val calculatedStatus = when {
                            diffMinutes >= 45 -> "Delivered"
                            diffMinutes >= 30 -> "Out for Delivery"
                            diffMinutes >= 15 -> "Prepared"
                            else -> "Processing"
                        }

                        val dbStatus = map["status"]?.toString()?.replace("\"", "") ?: "Processing"
                        val finalStatus = if (dbStatus == "Delivered") "Delivered" else calculatedStatus

                        OrderData(
                            fullId = rawId,
                            displayId = rawId.take(8).uppercase(),
                            status = finalStatus, // Now uses the smart time-based status
                            price = "₺${String.format(Locale.US, "%.2f", rawTotal)}",
                            date = formattedDate
                        )
                    }.reversed()
                }
            } catch (e: Exception) {
                Log.e("OrderHistory", "Failed to fetch orders: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Orders", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            } else if (orders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.ShoppingBag, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No orders yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Your greenery history will appear here.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(orders) { orderItem ->
                        PremiumOrderHistoryCard(
                            order = orderItem,
                            onTrackOrder = onTrackOrder,
                            onViewDetails = { selectedOrderForDetails = orderItem }
                        )
                    }
                }
            }
        }
    }

    selectedOrderForDetails?.let { order ->
        OrderDetailsDialog(
            order = order,
            onDismiss = { selectedOrderForDetails = null }
        )
    }
}

@Composable
fun PremiumOrderHistoryCard(
    order: OrderData,
    onTrackOrder: (String) -> Unit,
    onViewDetails: () -> Unit
) {
    // THEME COLORS (Strictly earthy/plant tones - No Blue)
    val statusColor = when (order.status) {
        "Delivered" -> Color(0xFF1B4332)       // Deep Forest Green
        "Out for Delivery" -> Color(0xFFE76F51) // Terracotta Orange
        "Prepared" -> Color(0xFF74C69D)        // Soft Mint
        "Processing" -> Color(0xFF52B788)      // Sage Green
        else -> Color.Gray
    }

    val statusIcon = when (order.status) {
        "Delivered" -> Icons.Rounded.CheckCircle
        "Out for Delivery" -> Icons.Rounded.LocalShipping
        "Prepared" -> Icons.Rounded.Inventory2
        else -> Icons.Rounded.Eco
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // HEADER
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(26.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Order #${order.displayId}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(order.date, color = Color.Gray, fontSize = 13.sp)
                }

                Text(order.price, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATUS BADGE
            Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "• ${order.status}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.05f))

            // ACTIONS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Button(
                    onClick = { onTrackOrder(order.fullId) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Track", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
    }
}

// PREMIUM DIGITAL RECEIPT & ITEMS DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsDialog(
    order: OrderData,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(order.fullId) { generateQRCode(order.fullId) }
    var orderItems by remember { mutableStateOf<List<OrderItemDisplay>>(emptyList()) }
    var itemsLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // FETCH ORDER ITEMS DYNAMICALLY
    LaunchedEffect(order.fullId) {
        scope.launch {
            try {
                val itemsResponse = supabase.from("order_items")
                    .select { filter { eq("order_id", order.fullId) } }
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

                val fetchedItems = mutableListOf<OrderItemDisplay>()
                for (item in itemsResponse) {
                    val pId = item["plant_id"]?.toString()?.toLongOrNull() ?: continue
                    val qty = item["quantity"]?.toString()?.toIntOrNull() ?: 1
                    val pot = item["pot_type"]?.toString()?.replace("\"", "") ?: "Standard"

                    val plantData = supabase.from("plants").select { filter { eq("id", pId) } }.decodeSingleOrNull<Plant>()
                    fetchedItems.add(OrderItemDisplay(plantName = plantData?.name ?: "Flora Plant", potType = pot, quantity = qty))
                }
                orderItems = fetchedItems
            } catch (e: Exception) {
                Log.e("OrderDetails", "Failed to fetch items: ${e.message}")
            } finally {
                itemsLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f),
        containerColor = Color.White,
        shape = RoundedCornerShape(32.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ORDER DETAILS", fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 2.sp, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(24.dp))

                // DYNAMIC QR CODE OR DELIVERED STATUS
                if (order.status == "Delivered") {
                    Box(
                        modifier = Modifier.size(120.dp).background(Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.TaskAlt, null, modifier = Modifier.size(60.dp), tint = Color(0xFF2D6A4F))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Order Completed", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D6A4F))
                    Text("Your greenery has arrived safely.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                } else {
                    Box(
                        modifier = Modifier.size(160.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)).padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrBitmap?.let {
                            Image(bitmap = it.asImageBitmap(), contentDescription = "Order QR", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                        } ?: Text("Loading QR...", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Show this QR to the courier", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("- - - - - - - - - - - - - - - - - - - - - - - -", color = Color.LightGray, maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                // ITEMS LIST
                if (itemsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                } else {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        orderItems.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${item.quantity}x", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(item.plantName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Pot: ${item.potType}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("- - - - - - - - - - - - - - - - - - - - - - - -", color = Color.LightGray, maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                // SUMMARY
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order ID", color = Color.Gray, fontSize = 13.sp)
                    Text(order.displayId, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid", color = Color.Gray, fontSize = 14.sp)
                    Text(order.price, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

