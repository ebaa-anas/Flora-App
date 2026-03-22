package com.example.myapplication

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(total: Double, onBack: () -> Unit, onPaymentSuccess: () -> Unit) {
    var selectedMethod by remember { mutableStateOf("card") }
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var saveCard by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {

            CheckoutStepper(currentStep = 1)

            Text(
                "Choose Payment Method",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Using the helper component we fixed earlier
                PaymentTypeTab(isSelected = selectedMethod == "card", label = "Card", icon = Icons.Rounded.CreditCard) { selectedMethod = "card" }
                PaymentTypeTab(isSelected = selectedMethod == "cash", label = "Cash", icon = Icons.Rounded.Payments) { selectedMethod = "cash" }
            }

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                AnimatedVisibility(
                    visible = selectedMethod == "card",
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        // VIRTUAL CARD: Professional Gradient Look
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Icon(Icons.Rounded.FilterVintage, null, tint = Color.White.copy(0.7f))
                                    Text("PREMIUM FLORA CARD", color = Color.White.copy(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Text(
                                    text = if (cardNumber.isEmpty()) "**** **** **** ****"
                                    else cardNumber.chunked(4).joinToString(" "),
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("CARD HOLDER", color = Color.White.copy(0.6f), fontSize = 10.sp)
                                        Text(if (cardHolder.isEmpty()) "NAME HERE" else cardHolder.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("EXPIRY", color = Color.White.copy(0.6f), fontSize = 10.sp)
                                        Text(if (expiry.isEmpty()) "MM/YY" else expiry, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // INPUT FIELDS (Using the fixed PremiumAddressField logic)
                        AddressTextField(cardHolder, { cardHolder = it }, "Card Holder Name", Icons.Rounded.Person)
                        AddressTextField(cardNumber, { if (it.length <= 16) cardNumber = it }, "Card Number", Icons.Rounded.CreditCard)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                AddressTextField(expiry, { if (it.length <= 4) expiry = it }, "Expiry (MMYY)", Icons.Rounded.DateRange)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = cvv, onValueChange = { if (it.length <= 3) cvv = it },
                                    label = { Text("CVV") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }

                        Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = saveCard,
                                onCheckedChange = { saveCard = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("Save card info for future payments", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                // CASH PAYMENT INFO
                AnimatedVisibility(visible = selectedMethod == "cash", enter = fadeIn() + expandVertically()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Pay with cash upon delivery. Ensure you have the exact amount ready.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total Amount", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 16.sp)
                Text("$${String.format("%.2f", total)}", fontWeight = FontWeight.Black, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = onPaymentSuccess,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).height(64.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White // Fix: No more blue text!
                ),
                enabled = selectedMethod == "cash" || (cardNumber.length == 16 && cvv.length == 3 && cardHolder.isNotEmpty())
            ) {
                Text(if (selectedMethod == "cash") "Confirm Order" else "Pay Now", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}