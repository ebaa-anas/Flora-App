package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.model.CareGuide
import com.example.myapplication.model.Plant
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- 1. DATA LOGIC ---

suspend fun fetchCareGuide(plantId: Long): CareGuide? {
    return withContext(Dispatchers.IO) {
        try {
            // FIX: Ensure 'supabase' is imported or accessible here
            supabase.from("care_guides")
                .select {
                    filter {
                        eq("plant_id", plantId)
                    }
                }
                .decodeSingleOrNull<CareGuide>()
        } catch (e: Exception) {
            Log.e("SupabaseError", "CareGuide Fetch Failed: ${e.message}")
            null
        }
    }
}

// --- 2. THE UI SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailsScreen(
    plant: Plant,
    onBack: () -> Unit,
    onAddToCart: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedPot by remember { mutableStateOf<String?>(null) }
    var careData by remember { mutableStateOf<CareGuide?>(null) }

    // FETCH CARE DATA
    LaunchedEffect(plant.id) {
        val id = plant.id ?: 0L
        if (id != 0L) {
            careData = fetchCareGuide(id)
        }
    }

    val potPrice = 15.0
    val totalWithPot = if (selectedPot != null) plant.price + potPrice else plant.price

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(0.7f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // HERO IMAGE
            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                AsyncImage(
                    model = plant.imageUrl,
                    contentDescription = plant.name,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    onClick = {
                        val sceneViewerIntent = Intent(Intent.ACTION_VIEW)
                        val intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                            .appendQueryParameter("file", "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/PotPlant/glTF-Binary/PotPlant.glb")
                            .appendQueryParameter("mode", "ar_only")
                            .appendQueryParameter("title", plant.name)
                            .build()
                        sceneViewerIntent.setData(intentUri)
                        // FIX: Added 'google' to the package name for compatibility
                        sceneViewerIntent.setPackage("com.google.android.googlequicksearchbox")
                        context.startActivity(sceneViewerIntent)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ViewInAr, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View in AR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                // NAME AND PRICE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(plant.name, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(plant.category, color = Color.Gray, fontSize = 16.sp)
                    }
                    Text("$${plant.price}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // CARE INFO
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Match these with your Supabase column names
                    CareInfoItem(Icons.Rounded.WbSunny, "Light", careData?.light_requirement ?: "Bright")
                    CareInfoItem(Icons.Rounded.WaterDrop, "Water", careData?.watering_frequency ?: "Weekly")
                    CareInfoItem(Icons.Rounded.Thermostat, "Temp", careData?.temperature_range ?: "22°C")
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("About", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(plant.description, color = Color.Gray, lineHeight = 22.sp)

                Spacer(modifier = Modifier.height(32.dp))

                // POT SELECTION
                Text("Upgrade your vibe", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                LazyRow(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listOf("Ceramic Minimalist", "Terracotta Classic")) { pot ->
                        val isSelected = selectedPot == pot
                        val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)

                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { selectedPot = if (isSelected) null else pot }
                                .border(2.dp, borderColor, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (pot.contains("Ceramic")) "⚪" else "🟠", fontSize = 40.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(pot, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("+$potPrice", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onAddToCart(selectedPot) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.LocalMall, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add Bundle • $$totalWithPot",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun CareInfoItem(icon: ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}