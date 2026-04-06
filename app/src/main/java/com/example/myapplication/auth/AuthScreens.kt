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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.supabase
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit, // Passes back the user's name
    onGoToSignup: () -> Unit,
    // Inject the professional login function from MainActivity
    loginLogic: suspend (String, String) -> String?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp).clip(loginWave)) {
            Image(
                painter = painterResource(id = R.drawable.welcome2_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.15f)))
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Welcome Back", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
            Text("Login to your account", color = Color.Gray)

            Spacer(modifier = Modifier.height(30.dp))

            AuthTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Email)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)

            if (errorText.isNotEmpty()) {
                Text(errorText, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Text("Forgot Password?", modifier = Modifier.align(Alignment.End).padding(top = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))

            Spacer(modifier = Modifier.height(18.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF2D6A4F))
            } else {
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty()) {
                            errorText = "Please fill in all fields"
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val name = loginLogic(email, password)
                            isLoading = false
                            if (name != null) onLoginSuccess(name) else errorText = "Invalid credentials"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
                ) {
                    Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("Or continue with", color = Color.Gray, fontSize = 12.sp)

            // GOOGLE LOGIN BUTTON
            IconButton(
                onClick = {
                    scope.launch {
                        try {
                            supabase.auth.signInWith(Google)
                            val user = supabase.auth.currentUserOrNull()
                            if (user != null) {
                                onLoginSuccess(user.userMetadata?.get("full_name").toString())
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

            TextButton(onClick = onGoToSignup) {
                Text("Don't have an account? Sign up", color = Color(0xFF52B788))
            }
        }
    }
}