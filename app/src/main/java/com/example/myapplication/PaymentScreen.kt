package com.example.myapplication

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    total: Double,
    onBack: () -> Unit,
    onPaymentSuccess: (String, Boolean, String, String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("card") }
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var saveCard by remember { mutableStateOf(false) }

    val isCardValid = cardNumber.length == 16 && cvv.length >= 3 && cardHolder.isNotBlank() && expiry.length == 4
    val canSubmit = selectedMethod == "cash" || isCardValid

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Secure Payment", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {

            Text("Choose Payment Method", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))

            // PAYMENT METHOD TABS
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentTypeTab(isSelected = selectedMethod == "card", label = "Card", icon = Icons.Rounded.CreditCard) { selectedMethod = "card" }
                PaymentTypeTab(isSelected = selectedMethod == "cash", label = "Cash", icon = Icons.Rounded.Payments) { selectedMethod = "cash" }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                AnimatedVisibility(visible = selectedMethod == "card") {
                    Column {
                        // VIRTUAL CARD DESIGN
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp)
                                .background(brush = Brush.linearGradient(listOf(Color(0xFF1B4332), Color(0xFF52B788))), shape = RoundedCornerShape(24.dp))
                        ) {
                            Column(modifier = Modifier.padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Rounded.FilterVintage, null, tint = Color.White.copy(0.7f))
                                    Text("PREMIUM FLORA", color = Color.White.copy(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(
                                    text = if (cardNumber.isEmpty()) "**** **** **** ****" else cardNumber.chunked(4).joinToString(" "),
                                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("CARD HOLDER", color = Color.White.copy(0.6f), fontSize = 10.sp)
                                        Text(if (cardHolder.isEmpty()) "NAME HERE" else cardHolder.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("EXPIRY", color = Color.White.copy(0.6f), fontSize = 10.sp)
                                        val formattedExpiry = if (expiry.length > 2) expiry.substring(0, 2) + "/" + expiry.substring(2) else expiry
                                        Text(if (expiry.isEmpty()) "MM/YY" else formattedExpiry, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // SECURE INPUTS
                        AddressTextField(cardHolder, { cardHolder = it }, "Card Holder Name", Icons.Rounded.Person)
                        AddressTextField(
                            value = cardNumber,
                            onValueChange = { if (it.length <= 16 && it.all { char -> char.isDigit() }) cardNumber = it },
                            label = "Card Number",
                            icon = Icons.Rounded.CreditCard,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                AddressTextField(
                                    value = expiry,
                                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) expiry = it },
                                    label = "Expiry (MMYY)",
                                    icon = Icons.Rounded.DateRange,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) cvv = it },
                                    label = { Text("CVV") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = saveCard, onCheckedChange = { saveCard = it })
                            Text("Save card details for future plant shopping", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                AnimatedVisibility(visible = selectedMethod == "cash") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Cash on Delivery: Please ensure you have the exact amount ready in cash.", fontSize = 14.sp)
                        }
                    }
                }
            }

            // FINAL SUMMARY & ACTION
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total to Pay", color = Color.Gray, fontSize = 16.sp)
                Text("₺${String.format(Locale.US, "%.2f", total)}", fontWeight = FontWeight.Black, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = {
                    onPaymentSuccess(selectedMethod, saveCard, cardNumber, cardHolder)
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).height(64.dp),
                shape = RoundedCornerShape(22.dp),
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Text(if (selectedMethod == "cash") "Confirm Order" else "Pay Now", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

// FIX: Added RowScope to give it access to the weight modifier!
@Composable
fun RowScope.PaymentTypeTab(isSelected: Boolean, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(50.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(0.2f)) else null
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (isSelected) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}