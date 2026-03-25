package com.example.myapplication

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(onBack: () -> Unit, onNext: (String, String) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // --- RELIABILITY LOGIC ---
    // Ensures the data follows basic rules before allowing the user to save it to Supabase
    val isPhoneValid = phone.length >= 10
    val isAddressValid = street.length > 5 && building.isNotBlank()
    val isNameValid = fullName.split(" ").size >= 2

    val canProceed = isPhoneValid && isAddressValid && isNameValid

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Checkout", fontWeight = FontWeight.Black) },
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
                "Shipping Address",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumAddressField(
                    fullName, { fullName = it }, "Full Name", Icons.Rounded.Person,
                    isError = fullName.isNotBlank() && !isNameValid,
                    supportingText = if (fullName.isNotBlank() && !isNameValid) "Enter first and last name" else null
                )

                PremiumAddressField(
                    street, { street = it }, "Street / Neighborhood", Icons.Rounded.LocationOn,
                    isError = street.isNotBlank() && street.length <= 5
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumAddressField(building, { building = it }, "Bld No", Icons.Rounded.Home)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PremiumAddressField(floor, { floor = it }, "Floor/Flat", Icons.Rounded.Info)
                    }
                }

                PremiumAddressField(
                    value = phone,
                    onValueChange = { if (it.length <= 15) phone = it }, // Limits length
                    label = "Phone Number",
                    icon = Icons.Rounded.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = phone.isNotBlank() && !isPhoneValid,
                    supportingText = if (phone.isNotBlank() && !isPhoneValid) "Min 10 digits required" else null
                )
            }

            Button(
                onClick = {
                    val fullAddress = "$street, Bldg $building, Floor $floor"
                    onNext(fullAddress, phone)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(22.dp),
                // --- RELIABLE DATA CHECK ---
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Proceed to Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
