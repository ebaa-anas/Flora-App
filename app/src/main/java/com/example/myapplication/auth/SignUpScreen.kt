package com.example.myapplication.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onSignUpComplete: () -> Unit, onGoToLogin: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // THE WAVE FIX: Using cubicTo for an asymmetrical organic S-curve
    val curveShape = GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height - 140f)

        // This math creates the "Wave" instead of the "U-shape"
        cubicTo(
            size.width * 0.7f, size.height,
            size.width * 0.3f, size.height - 180f,
            0f, size.height - 60f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // TOP SECTION: The Organic S-Wave Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(curveShape) // Clips the image into the wave shape
        ) {
            Image(
                painter = painterResource(id = R.drawable.welcome2_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Subtle darkening to help the "Register" text stand out if it overlaps
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.15f)))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Register",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1B4332), // Deep Forest Green
                letterSpacing = (-1).sp
            )
            Text("Create your new account", color = Color.Gray.copy(0.7f))

            Spacer(modifier = Modifier.height(32.dp))

            // Professional Input Fields
            AuthTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)

            Spacer(modifier = Modifier.height(48.dp))

            // The Primary Action Button
            Button(
                onClick = onSignUpComplete,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
            ) {
                Text("Register", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onGoToLogin) {
                Text("Already have an account? Log in", color = Color(0xFF52B788))
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// HELPER FUNCTION: This must be in the same file to fix your "Unresolved" error
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Color(0xFF2D6A4F)) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF1F8F5), // Soft Mint from reference
            focusedBorderColor = Color(0xFF52B788),
            unfocusedBorderColor = Color.Transparent
        )
    )
}