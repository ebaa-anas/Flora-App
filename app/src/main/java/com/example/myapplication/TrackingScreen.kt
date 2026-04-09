package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// 1. DATA MODEL
data class TimelineStep(val title: String, val time: String, val isCompleted: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    orderId: String,
    onBack: () -> Unit
) {
    var currentStatus by remember { mutableStateOf("Processing") }
    var orderTimeDisplay by remember { mutableStateOf("--:--") }
    var orderTimeMillis by remember { mutableLongStateOf(0L) }

    // 2. THE TIME-TRAVEL POLLING ENGINE (Now with Date + Time)
    LaunchedEffect(orderId) {
        while (true) {
            try {
                val response = supabase.from("orders")
                    .select { filter { eq("id", orderId) } }
                    .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

                // Extract exact time from Supabase UTC timestamp
                val rawDate = response["created_at"]?.toString()?.replace("\"", "") ?: ""
                val cleanDate = if (rawDate.contains(".")) rawDate.substringBefore(".") else rawDate

                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")

                val timeMilli = try {
                    sdf.parse(cleanDate)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                orderTimeMillis = timeMilli

                // Format to Local Time WITH the Date (e.g. "Apr 08, 14:30")
                val localSdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
                localSdf.timeZone = TimeZone.getDefault()
                orderTimeDisplay = localSdf.format(Date(timeMilli))

                // --- THE AUTO-PROGRESSION MATH ---
                val nowMillis = System.currentTimeMillis()
                val diffMinutes = ((nowMillis - timeMilli) / (1000 * 60)).toInt()

                val calculatedStatus = when {
                    diffMinutes >= 45 -> "Delivered"
                    diffMinutes >= 30 -> "Out for Delivery"
                    diffMinutes >= 15 -> "Prepared"
                    else -> "Processing"
                }

                // If Supabase manually says "Delivered", respect it. Otherwise, use the smart timer!
                val dbStatus = response["status"]?.toString()?.replace("\"", "") ?: "Processing"
                currentStatus = if (dbStatus == "Delivered") "Delivered" else calculatedStatus

            } catch (e: Exception) {
                Log.e("TrackingError", "Failed to fetch status: ${e.message}")
            }
            delay(5000) // Re-checks every 5 seconds
        }
    }

    // HELPER: Adds realistic time gaps mathematically and returns Date + Time
    fun formatOffset(baseMillis: Long, minutesToAdd: Int): String {
        if (baseMillis == 0L) return "Pending"
        val newMillis = baseMillis + (minutesToAdd * 60 * 1000L)
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(newMillis))
    }

    // 3. DYNAMIC STEP LOGIC
    val isPrepared = currentStatus in listOf("Prepared", "Out for Delivery", "Delivered")
    val isOut = currentStatus in listOf("Out for Delivery", "Delivered")
    val isDelivered = currentStatus == "Delivered"

    val timePlaced = orderTimeDisplay
    val timePrepared = if (isPrepared) formatOffset(orderTimeMillis, 15) else "Pending"
    val timeOut = if (isOut) formatOffset(orderTimeMillis, 30) else "Pending"
    val timeDelivered = if (isDelivered) formatOffset(orderTimeMillis, 45) else "Pending"

    val estimatedArrival = if (orderTimeMillis != 0L) formatOffset(orderTimeMillis, 45) else "--:--"

    val steps = listOf(
        TimelineStep("Order Placed", timePlaced, true),
        TimelineStep("Prepared", timePrepared, isPrepared),
        TimelineStep("Out for Delivery", timeOut, isOut),
        TimelineStep("Delivered", timeDelivered, isDelivered)
    )

    Scaffold(
        containerColor = Color(0xFFF9FBF9),
        topBar = {
            TopAppBar(
                title = { Text("Track Order", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9FBF9))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // LIVE MAP VISUAL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Map, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Status: $currentStatus", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    val arrivalText = if (isDelivered) "Delivered on $timeDelivered" else "Est. Arrival: $estimatedArrival"
                    Text(arrivalText, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                // HEADER
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Order #${orderId.take(8).uppercase()}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                        Text("Real-time Tracking Active", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = { /* Help logic */ }, modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)) {
                        Icon(Icons.Rounded.HeadsetMic, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // COURIER INFO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(54.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ElectricScooter, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ahmed K.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Flora Delivery Expert", fontSize = 13.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { /* Call */ }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(40.dp)) {
                            Icon(Icons.Rounded.Phone, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text("Delivery Progress", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(24.dp))

                // THE SMART TIMELINE
                TrackingTimeline(steps = steps)
            }
        }
    }
}

@Composable
fun TrackingTimeline(steps: List<TimelineStep>) {
    Column {
        steps.forEachIndexed { index, step ->
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(
                                width = if (step.isCompleted) 0.dp else 2.dp,
                                color = if (step.isCompleted) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .background(
                                color = if (step.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (step.isCompleted) {
                            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .weight(1f)
                                .background(
                                    if (step.isCompleted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.padding(bottom = 36.dp)) {
                    Text(
                        text = step.title,
                        fontWeight = if (step.isCompleted) FontWeight.Black else FontWeight.Bold,
                        color = if (step.isCompleted) MaterialTheme.colorScheme.onBackground else Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.time,
                        fontSize = 13.sp,
                        color = if (step.isCompleted) MaterialTheme.colorScheme.primary else Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}