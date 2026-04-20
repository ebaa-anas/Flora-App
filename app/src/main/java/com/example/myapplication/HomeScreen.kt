package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.model.Plant
@Composable
fun HomeScreen(
    padding: PaddingValues,
    plants: List<Plant>,
    onPlantClick: (Plant) -> Unit
) {
    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        if (plants.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plants) { plant ->
                    // Use a clickable card to trigger your Details screen
                    PlantCard(plant, onClick = { onPlantClick(plant) })
                }
            }
        }
    }
}
@Composable
fun PlantCard(plant: Plant, onClick: () -> Unit) { // Added onClick parameter here
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = plant.imageUrl,
                contentDescription = plant.name,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(plant.name, style = MaterialTheme.typography.titleLarge)
                Text("${plant.price} USD", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}