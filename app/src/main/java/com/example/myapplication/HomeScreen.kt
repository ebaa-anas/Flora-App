package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.model.Plant


@Composable
fun HomeScreen(onFetch: suspend () -> List<Plant>) {
    // State: The "Brain" of the screen
    var plants by remember { mutableStateOf<List<Plant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Logic: Fetch data once when screen opens
    LaunchedEffect(Unit) {
        try {
            plants = onFetch()
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    // UI: What the user sees
    Scaffold(
        topBar = { Text("Flora Istanbul", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp)) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text("Error: $errorMessage", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(plants) { plant ->
                        PlantCard(plant)
                    }
                }
            }
        }
    }
}

@Composable
fun PlantCard(plant: Plant) {
    Card(modifier = Modifier.fillMaxWidth()) {
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