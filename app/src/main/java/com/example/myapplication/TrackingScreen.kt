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

// 1. DATA MODEL
data class TimelineStep(val title: String, val time: String, val isCompleted: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    orderId: String,
    onBack: () -> Unit
) {
    // 2. STATE FOR REAL-TIME UPDATES
    var currentStatus by remember { mutableStateOf("Processing") }

    // 3. REAL-TIME POLLING LOGIC
    // This fetches the latest status from Supabase every 5 seconds
    LaunchedEffect(orderId) {
        while (true) {
            try {
                val response = supabase.from("orders")
                    .select {
                        filter { eq("id", orderId) }
                    }
                    .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

                // Clean the string (Supabase returns "Status" with quotes)
                val statusFromDb = response["status"]?.toString()?.replace("\"", "") ?: "Processing"
                currentStatus = statusFromDb
            } catch (e: Exception) {
                Log.e("TrackingError", "Failed to fetch status: ${e.message}")
            }
            delay(5000) // Wait 5 seconds before the next check
        }
    }

    // 4. DYNAMIC STEPS LOGIC (Reacts to currentStatus)
    val steps = listOf(
        TimelineStep("Order Placed", "10:00 AM", true),
        TimelineStep("Prepared", "11:30 AM", currentStatus == "Prepared" || currentStatus == "Out for Delivery" || currentStatus == "Delivered"),
        TimelineStep("Out for Delivery", "01:15 PM", currentStatus == "Out for Delivery" || currentStatus == "Delivered"),
        TimelineStep("Delivered", "Arriving soon", currentStatus == "Delivered")
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Track Order", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Map, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(0.3f))
                    // Displays the actual status from the DB
                    Text("Current Status: $currentStatus", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Text("Estimated Arrival: 14:30", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                // HEADER
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Order ID: #${orderId.take(8).uppercase()}", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Real-time Tracking Active", color = Color.Gray)
                    }
                    IconButton(onClick = { /* Help logic */ }) {
                        Icon(Icons.Rounded.HelpCenter, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // COURIER INFO
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(54.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ElectricBike, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ahmed K.", fontWeight = FontWeight.Bold)
                            Text("Flora Delivery Expert", fontSize = 13.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { /* Call */ }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(40.dp)) {
                            Icon(Icons.Rounded.Phone, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text("Delivery Progress", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(24.dp))

                // THE TIMELINE (Now using dynamic steps)
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
                            .size(24.dp)
                            .border(
                                width = if (step.isCompleted) 0.dp else 2.dp,
                                color = if (step.isCompleted) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(0.1f),
                                shape = CircleShape
                            )
                            .background(
                                color = if (step.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (step.isCompleted) {
                            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                        }
                    }
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(
                                    if (step.isCompleted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(0.1f)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        text = step.title,
                        fontWeight = if (step.isCompleted) FontWeight.Bold else FontWeight.Medium,
                        color = if (step.isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                    )
                    Text(
                        text = step.time,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                }
            }
        }
    }
}