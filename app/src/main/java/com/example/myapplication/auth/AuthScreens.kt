package com.example.myapplication.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape // Required for the wave
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Required to "cut" the image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoToSignup: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // THE WAVE LOGIC: Flipped for Login to match your style
    val loginWave = GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height - 60f)
        cubicTo(
            size.width * 0.7f, size.height - 180f,
            size.width * 0.3f, size.height,
            0f, size.height - 140f
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // FIXED HEADER: Updated with the asymmetrical wave
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(loginWave) // Clips image to the wave
        ) {
            Image(
                painter = painterResource(id = R.drawable.welcome2_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Subtle darkening for premium look
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.15f)))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Welcome Back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332)
            )
            Text("Login to your account", color = Color.Gray)

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF2D6A4F)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF1F8F5),
                    focusedBorderColor = Color(0xFF52B788)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF2D6A4F)) },
                trailingIcon = { Icon(Icons.Default.FavoriteBorder, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF1F8F5),
                    focusedBorderColor = Color(0xFF52B788)
                )
            )

            Text(
                "Forgot Password?",
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onLoginSuccess,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
            ) {
                Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Or continue with", color = Color.Gray, fontSize = 12.sp)

            Row(modifier = Modifier.padding(16.dp)) {
                // Social icons...
            }

            TextButton(onClick = onGoToSignup) {
                Text("Don't have an account? Sign up", color = Color(0xFF52B788))
            }
        }
    }
}