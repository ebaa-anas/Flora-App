package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

@Composable
fun OrderSuccessScreen(
    orderId: String,
    total: Double,
    address: String,
    onTrackOrder: () -> Unit,
    onGoHome: () -> Unit
) {
    // GENERATE THE REAL QR BITMAP
    val qrBitmap = remember(orderId) { generateQRCode(orderId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // EXIT ICON
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(
                onClick = onGoHome,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Rounded.Close, "Exit", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(110.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text("Order Placed!", fontSize = 32.sp, fontWeight = FontWeight.Black)
            Text("Your greenery is being prepared", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 16.sp)

            Spacer(modifier = Modifier.height(30.dp))

            // RECEIPT CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DIGITAL RECEIPT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.3f), letterSpacing = 3.sp, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR CODE DISPLAY
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Order QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Text("QR Error", fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // DASHED LINE SEPARATOR
                    Text("- - - - - - - - - - - - - - - - - - - - - - - -", color = Color.LightGray, maxLines = 1, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    // DATA ROWS
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order ID", color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        Text(orderId.take(8).uppercase(), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Paid", color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        Text("₺${String.format(Locale.US, "%.2f", total)}", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Delivery Address", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                        Text(address, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                    }
                }
            }
        }

        // NAVIGATION BUTTON
        Button(
            onClick = onTrackOrder,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
        ) {
            Text("Track My Plant", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// QR GENERATOR LOGIC
fun generateQRCode(text: String): Bitmap? {
    return try {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}