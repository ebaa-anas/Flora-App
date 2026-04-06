package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(
    savedAddress: String,
    savedPhone: String,
    onBack: () -> Unit,
    onNext: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State Variables
    var street by remember { mutableStateOf(savedAddress) }
    var building by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(savedPhone) }

    // GPS Location State
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    // --- RELIABILITY LOGIC ---
    val isPhoneValid = phone.length >= 10
    val isAddressValid = street.length > 5
    val canProceed = isPhoneValid && isAddressValid

    // --- GPS PERMISSION LAUNCHER ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                isFetchingLocation = true
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            // Translate GPS coordinates to a real street address (Reverse Geocoding)
                            val geocoder = Geocoder(context, Locale.getDefault())
                            try {
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    // Auto-fill the text field with the discovered street!
                                    street = address.thoroughfare ?: address.subLocality ?: address.getAddressLine(0) ?: "Unknown Location"
                                    Toast.makeText(context, "Location found!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not read address.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Make sure your GPS is turned on", Toast.LENGTH_LONG).show()
                        }
                        isFetchingLocation = false
                    }.addOnFailureListener {
                        isFetchingLocation = false
                        Toast.makeText(context, "Failed to get location", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: SecurityException) {
                    isFetchingLocation = false
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Delivery Details", fontWeight = FontWeight.Black) },
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
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Where to?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text("Provide your location for fast delivery.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // --- SMART GPS BUTTON ---
                Card(
                    onClick = {
                        // Check if we already have permission, if not, ask for it
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                                // We have permission, fire the launcher manually to run the success logic
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            else -> {
                                // Ask for permission
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Locating you...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Rounded.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Use Current Location", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(0.5f))
                    Text("  OR ENTER MANUALLY  ", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(0.5f))
                }

                // --- STRUCTURED LOGISTICS FIELDS ---
                PremiumAddressField(
                    value = street,
                    onValueChange = { street = it },
                    label = "Street / District",
                    icon = Icons.Rounded.LocationOn,
                    isError = street.isNotBlank() && !isAddressValid
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumAddressField(building, { building = it }, "Bld / Apt No.", Icons.Rounded.Home)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumAddressField(floor, { floor = it }, "Floor", Icons.Rounded.Layers)
                    }
                }

                PremiumAddressField(
                    value = phone,
                    onValueChange = { if (it.length <= 15) phone = it },
                    label = "Phone Number",
                    icon = Icons.Rounded.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = phone.isNotBlank() && !isPhoneValid,
                    supportingText = if (phone.isNotBlank() && !isPhoneValid) "Min 10 digits required for courier" else null
                )
            }

            // --- SMART CHECKOUT BUTTON ---
            Button(
                onClick = {
                    val formattedBuilding = if (building.isNotBlank()) ", Bldg $building" else ""
                    val formattedFloor = if (floor.isNotBlank()) ", Flr $floor" else ""
                    val fullAddress = "$street$formattedBuilding$formattedFloor"

                    onNext(fullAddress, phone)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(64.dp),
                shape = RoundedCornerShape(22.dp),
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Text("Proceed to Payment", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumAddressField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
        keyboardOptions = keyboardOptions,
        isError = isError,
        supportingText = supportingText?.let { { Text(it, fontSize = 10.sp) } },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true
    )
}