package com.example.myapplication.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import io.github.jan.supabase.gotrue.providers.Google
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.supabase
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onSignUpComplete: (String) -> Unit,
    onGoToLogin: () -> Unit,
    signUpLogic: suspend (String, String, String, String) -> Boolean // Name, Email, Phone, Pass
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") } // Added to handle Google errors gracefully
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val curveShape = GenericShape { size, _ ->
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height - 140f)
        cubicTo(size.width * 0.7f, size.height, size.width * 0.3f, size.height - 180f, 0f, size.height - 60f)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(curveShape)) {
            Image(painter = painterResource(id = R.drawable.welcome2_bg), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.15f)))
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Register", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
            Text("Create your professional account", color = Color.Gray.copy(0.7f))

            Spacer(modifier = Modifier.height(24.dp))

            AuthTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address", icon = Icons.Default.Email)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", icon = Icons.Default.Phone)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)

            if (errorText.isNotEmpty()) {
                Text(errorText, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF2D6A4F))
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val success = signUpLogic(email, password, fullName, phone)
                                isLoading = false
                                if (success) {
                                    android.widget.Toast.makeText(context, "Account Created! Welcome, $fullName", android.widget.Toast.LENGTH_LONG).show()
                                    onSignUpComplete(fullName)
                                } else {
                                    errorText = "Sign up failed. Check details."
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorText = "Error: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
                ) {
                    Text("Register", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("Or continue with", color = Color.Gray, fontSize = 12.sp)

            // GOOGLE SIGN UP BUTTON (Added exactly matching LoginScreen)
            IconButton(
                onClick = {
                    scope.launch {
                        try {
                            supabase.auth.signInWith(Google)
                            val user = supabase.auth.currentUserOrNull()
                            if (user != null) {
                                val googleName = user.userMetadata?.get("full_name").toString()
                                android.widget.Toast.makeText(context, "Welcome, $googleName", android.widget.Toast.LENGTH_LONG).show()
                                onSignUpComplete(googleName)
                            }
                        } catch (e: Exception) {
                            errorText = "Google Error: ${e.localizedMessage}"
                        }
                    }
                },
                modifier = Modifier.padding(16.dp).size(50.dp).background(Color(0xFFF1F8F5), CircleShape)
            ) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Google", tint = Color(0xFF2D6A4F))
            }

            TextButton(onClick = onGoToLogin) {
                Text("Already have an account? Log in", color = Color(0xFF52B788))
            }
        }
    }
}

// HELPER FUNCTION (Keep this at the bottom of the file)
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
            unfocusedContainerColor = Color(0xFFF1F8F5),
            focusedBorderColor = Color(0xFF52B788),
            unfocusedBorderColor = Color.Transparent
        )
    )
}