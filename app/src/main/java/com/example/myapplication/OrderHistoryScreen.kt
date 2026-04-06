package com.example.myapplication

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.ShoppingCart
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
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.util.Locale

// 1. DATA MODEL
data class OrderData(
    val fullId: String,
    val displayId: String,
    val status: String,
    val price: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBack: () -> Unit,
    onTrackOrder: (String) -> Unit
) {
    var orders by remember { mutableStateOf<List<OrderData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // STATE FOR THE QR RECEIPT DIALOG
    var selectedOrderForReceipt by remember { mutableStateOf<OrderData?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val response = supabase.from("orders")
                        .select {
                            filter { eq("user_id", userId) }
                        }.decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()

                    orders = response.map { map ->
                        val rawId = map["id"]?.toString()?.replace("\"", "") ?: ""
                        val rawStatus = map["status"]?.toString()?.replace("\"", "") ?: "Processing"
                        val rawTotal = map["total_amount"]?.toString()?.replace("\"", "")?.toDoubleOrNull() ?: 0.0
                        val rawDate = map["created_at"]?.toString()?.replace("\"", "") ?: ""

                        val formattedDate = if (rawDate.length >= 10) rawDate.take(10) else "Recent"

                        OrderData(
                            fullId = rawId,
                            displayId = rawId.take(8).uppercase(),
                            status = rawStatus,
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
                    Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No orders yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Your purchase history will appear here.", color = Color.Gray, fontSize = 14.sp)
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
                            onViewReceipt = { selectedOrderForReceipt = orderItem }
                        )
                    }
                }
            }
        }
    }

    // SHOW THE QR RECEIPT DIALOG WHEN AN ORDER IS SELECTED
    selectedOrderForReceipt?.let { order ->
        DigitalReceiptDialog(
            order = order,
            onDismiss = { selectedOrderForReceipt = null }
        )
    }
}

@Composable
fun PremiumOrderHistoryCard(
    order: OrderData,
    onTrackOrder: (String) -> Unit,
    onViewReceipt: () -> Unit
) {
    val statusColor = when (order.status) {
        "Delivered" -> MaterialTheme.colorScheme.primary
        "Out for Delivery" -> Color(0xFFFFB703)
        "Prepared" -> Color(0xFF8ECAE6)
        "Processing" -> Color(0xFF3A86FF)
        else -> Color.Gray
    }

    val statusIcon = when (order.status) {
        "Delivered" -> Icons.Rounded.Check
        "Out for Delivery" -> Icons.Rounded.ShoppingCart
        else -> Icons.Rounded.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // HEADER: Icon, Order ID, Date, Price
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(26.dp)
                    )
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
            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "• ${order.status}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.05f))

            // ACTIONS: View Receipt & Track Order
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onViewReceipt,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Receipt", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

// PREMIUM DIGITAL RECEIPT DIALOG
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalReceiptDialog(
    order: OrderData,
    onDismiss: () -> Unit
) {
    // Uses the function from OrderSuccessScreen.kt
    val qrBitmap = remember(order.fullId) { generateQRCode(order.fullId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.85f),
        containerColor = Color.White,
        shape = RoundedCornerShape(32.dp),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DIGITAL RECEIPT", fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 2.sp, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(24.dp))

                // THE QR CODE
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Order QR",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        )
                    } ?: Text("Loading QR...", fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Show this QR to the courier", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Text("to receive your delivery safely.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(24.dp))
                Text("- - - - - - - - - - - - - - - - - - - - - - - -", color = Color.LightGray, maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                // ORDER SUMMARY
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order ID", color = Color.Gray, fontSize = 14.sp)
                    Text(order.displayId, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Date", color = Color.Gray, fontSize = 14.sp)
                    Text(order.date, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", color = Color.Gray, fontSize = 14.sp)
                    Text(order.price, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
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