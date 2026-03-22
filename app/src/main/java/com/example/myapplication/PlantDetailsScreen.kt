package com.example.myapplication

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.model.Plant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailsScreen(
    plant: Plant,
    onBack: () -> Unit,
    onAddToCart: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedPot by remember { mutableStateOf<String?>(null) }
    val potPrice = 15.0
    val totalWithPot = if (selectedPot != null) plant.price + potPrice else plant.price

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("") }, // Empty title for clean look
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.7f), CircleShape)
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
        ) {
            // 1. HERO IMAGE WITH GRADIENT OVERLAY
            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                AsyncImage(
                    model = plant.imageUrl,
                    contentDescription = plant.name,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)),
                    contentScale = ContentScale.Crop
                )

                // AR Trigger: Floating Glass Style
                Surface(
                    onClick = {
                        val sceneViewerIntent = Intent(Intent.ACTION_VIEW)
                        val intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                            .appendQueryParameter("file", "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/PotPlant/glTF-Binary/PotPlant.glb")
                            .appendQueryParameter("mode", "ar_only")
                            .appendQueryParameter("title", plant.name)
                            .build()
                        sceneViewerIntent.setData(intentUri)
                        sceneViewerIntent.setPackage("com.google.android.googlequicksearchbox")
                        context.startActivity(sceneViewerIntent)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ViewInAr, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View in AR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                // 2. NAME AND PRICE
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(plant.name, fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                        Text(plant.category, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 16.sp)
                    }
                    Text("$${plant.price}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. PROFESSIONAL CARE STATS (The Luxury "Value Add")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CareInfoItem(Icons.Rounded.WbSunny, "Light", "Indirect")
                    CareInfoItem(Icons.Rounded.WaterDrop, "Water", "Weekly")
                    CareInfoItem(Icons.Rounded.Thermostat, "Temp", "22°C")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. BUNDLE BUILDER: POT SELECTION
                Text("Upgrade your vibe", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("Select a designer pot for your $${potPrice}", color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 14.sp)

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
                                    modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)),
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

                // 5. PREMIUM ADD TO CART BUTTON
                Button(
                    onClick = { onAddToCart(selectedPot) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Rounded.LocalMall, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add Bundle • $$totalWithPot",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun CareInfoItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}