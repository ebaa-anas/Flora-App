package com.example.myapplication

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.myapplication.model.Plant
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlin.math.absoluteValue

// --- DATA MODEL FOR THE SMART DASHBOARD ---
data class GreenhousePlant(
    val plant: Plant,
    val waterLevel: Float, // 0.0 to 1.0
    val needsWatering: Boolean
)

@Composable
fun MyGreenhouseScreen(
    padding: PaddingValues,
    userName: String
) {
    var myPlants by remember { mutableStateOf<List<GreenhousePlant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // --- FETCH DATA & CALCULATE SMART STATS ---
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId != null) {
                    // 1. Get all orders for this user
                    val ordersResponse = supabase.from("orders")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<Map<String, JsonElement>>()

                    val orderIds = ordersResponse.mapNotNull { it["id"]?.toString()?.replace("\"", "") }

                    if (orderIds.isNotEmpty()) {
                        // 2. Get all items from those orders
                        val itemsResponse = supabase.from("order_items").select().decodeList<Map<String, JsonElement>>()
                        val myPlantIds = itemsResponse
                            .filter { it["order_id"]?.toString()?.replace("\"", "") in orderIds }
                            .mapNotNull { it["plant_id"]?.toString()?.toLongOrNull() }
                            .distinct() // Remove duplicates so we just have one of each plant type

                        if (myPlantIds.isNotEmpty()) {
                            // 3. Fetch the actual plant details
                            val plantsResponse = supabase.from("plants").select().decodeList<Plant>()
                            val boughtPlants = plantsResponse.filter { it.id in myPlantIds }

                            // 4. Generate the "Smart" Water Levels (Stable pseudo-random based on name for demo)
                            myPlants = boughtPlants.map { plant ->
                                val hash = plant.name.hashCode().absoluteValue
                                val level = ((hash % 80) + 10) / 100f // Generates a level between 0.10 and 0.90
                                GreenhousePlant(
                                    plant = plant,
                                    waterLevel = level,
                                    needsWatering = level < 0.35f
                                )
                            }.sortedBy { it.waterLevel } // Put thirsty plants at the top!
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Greenhouse", "Failed to load greenhouse: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // --- CALCULATE DYNAMIC OVERVIEW ALERTS ---
    val thirstyPlants = myPlants.filter { it.needsWatering }
    val averageHealth = if (myPlants.isNotEmpty()) (myPlants.map { it.waterLevel }.average() * 100).toInt() else 0
    val healthDisplay = if (averageHealth == 0) "--" else "$averageHealth%"

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Welcome back, $userName", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)

        // 1. PROFESSIONAL USER HEADER
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D6A4F)), // Forest Green
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userName.isNotBlank()) userName.take(1).uppercase() else "U",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                Text("Senior Plant Parent", color = Color(0xFF52B788), fontSize = 14.sp, fontWeight = FontWeight.Bold) // Sage
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. HEALTH OVERVIEW STATS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Total Plants", myPlants.size.toString(), Color(0xFF2D6A4F), Modifier.weight(1f))
            StatCard("Avg Hydration", healthDisplay, Color(0xFF52B788), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. DYNAMIC URGENT CARE ALERTS
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2D6A4F))
            }
        } else if (thirstyPlants.isNotEmpty()) {
            Text("Active Tasks", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))

            val thirstyNames = thirstyPlants.take(2).map { it.plant.name }.joinToString(" & ")
            val extraCount = if (thirstyPlants.size > 2) " +${thirstyPlants.size - 2} more" else ""

            CareAlertCard(
                title = "Watering Needed",
                subtitle = "$thirstyNames$extraCount are thirsty",
                icon = Icons.Rounded.WaterDrop,
                accentColor = Color(0xFFE76F51) // Terracotta Orange for urgency
            )
            Spacer(modifier = Modifier.height(32.dp))
        } else if (myPlants.isNotEmpty()) {
            Text("Active Tasks", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            CareAlertCard(
                title = "All Good!",
                subtitle = "Your greenhouse is fully hydrated.",
                icon = Icons.Rounded.TaskAlt,
                accentColor = Color(0xFF52B788) // Sage green for good status
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 4. COLLECTION LIST
        Text("My Collection", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            // Waiting for DB...
        } else if (myPlants.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Eco, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your greenhouse is empty.", fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Visit the shop to start your collection!", fontSize = 13.sp, color = Color.LightGray)
            }
        } else {
            myPlants.forEach { greenhouseData ->
                GreenhouseItem(data = greenhouseData)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun CareAlertCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, color = accentColor)
                Text(subtitle, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun GreenhouseItem(data: GreenhousePlant) {
    // Dynamic styling based on water level
    val barColor = if (data.needsWatering) Color(0xFFE76F51) else Color(0xFF52B788)
    val statusText = if (data.needsWatering) "Needs Water" else "Hydrated"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(54.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🪴", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(data.plant.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(statusText, fontSize = 11.sp, color = barColor, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { data.waterLevel },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = barColor,
                    trackColor = barColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}