package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertChatScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var userName by remember { mutableStateOf("Plant Lover") }
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    val messages = remember { mutableStateListOf<ChatMessage>() }

    // FETCH USER NAME
    LaunchedEffect(Unit) {
        try {
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                val profile = supabase.from("profiles").select { filter { eq("id", user.id) } }.decodeSingleOrNull<UserProfile>()
                userName = profile?.full_name?.split(" ")?.firstOrNull() ?: "there"
            }
        } catch (e: Exception) {
            Log.e("ChatScreen", "Failed to fetch name: ${e.message}")
        }

        messages.add(ChatMessage("Hello $userName! 🌱 I'm Dr. Flora. You can ask me about watering, lighting, repotting, pests, or pet safety!", false))
    }

    // --- THE GUARDRAIL AI BRAIN ---
    fun generateBotanistReply(input: String): String {
        val lowerInput = input.lowercase()
        return when {
            lowerInput.contains("yellow") ->
                "Yellow leaves are usually a sign of overwatering! Check the soil. If it's soggy, let it dry out completely. Want to know how to check soil moisture? 🥀"
            lowerInput.contains("water") || lowerInput.contains("often") || lowerInput.contains("moisture") ->
                "Most indoor plants prefer to dry out between waterings. Stick your finger 2 inches into the soil. If it feels dry, it's time to water! 💧 Do you have questions about lighting next?"
            lowerInput.contains("light") || lowerInput.contains("dark") || lowerInput.contains("sun") ->
                "Low light doesn't mean NO light! Snake plants and ZZ plants are fantastic for dimmer corners. ☀️ Let me know if you need help with repotting or fertilizer!"
            lowerInput.contains("brown") || lowerInput.contains("crispy") ->
                "Brown, crispy edges usually indicate a lack of humidity or underwatering. Try misting your plant's leaves in the morning! 🌫️"
            lowerInput.contains("bug") || lowerInput.contains("pest") || lowerInput.contains("gnat") || lowerInput.contains("mite") ->
                "Uh oh, pests! 🐛 For fungus gnats, let the soil dry out. For spider mites, wipe the leaves with a gentle mix of water and neem oil. Keep the infected plant isolated!"
            lowerInput.contains("repot") || lowerInput.contains("pot") ->
                "You should generally repot plants every 1-2 years in the spring! 🪴 You'll know it's time if roots are poking out the drainage holes."
            lowerInput.contains("fertiliz") || lowerInput.contains("food") || lowerInput.contains("feed") ->
                "Feed your plants during their active growing season (Spring and Summer) every 2-4 weeks. Pause fertilizing in the winter! 🧪"
            lowerInput.contains("droop") || lowerInput.contains("wilt") ->
                "Drooping leaves can be tricky! If the soil is bone dry, it's just thirsty. If the soil is wet, it might be overwatered. Always check the soil first! 🥀"
            lowerInput.contains("cat") || lowerInput.contains("dog") || lowerInput.contains("pet") || lowerInput.contains("toxic") ->
                "Great question for pet parents! 🐾 Monsteras, Pothos, and ZZ plants are toxic. Stick to safe options like Spider Plants, Calatheas, and Boston Ferns!"

            // THE GRACEFUL FALLBACK (If they ask something random)
            else ->
                "That's an interesting question! While I'm still learning about that, I am currently a certified expert in **watering schedules, lighting needs, repotting, pest control, and pet safety**. Try asking me about one of those topics!"
        }
    }

    fun sendMessageToMockAI(text: String) {
        if (text.isBlank()) return

        messages.add(ChatMessage(text, isUser = true))
        messageText = ""
        isTyping = true

        scope.launch { listState.animateScrollToItem(messages.size - 1) }

        scope.launch {
            delay(1500)
            val reply = generateBotanistReply(text)
            messages.add(ChatMessage(reply, isUser = false))
            isTyping = false
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Spa, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Dr. Flora", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(if (isTyping) "Typing..." else "Online • Botanist", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.navigationBarsPadding().padding(16.dp)) {

                    // PERMANENT QUICK QUESTIONS: The AnimatedVisibility is gone. This stays forever.
                    Column {
                        Text("Suggested Topics:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val quickQuestions = listOf(
                                "Why are my leaves yellow?",
                                "When should I repot?",
                                "Is Monstera safe for cats?",
                                "How to get rid of bugs?",
                                "Best low light plants?"
                            )
                            items(quickQuestions) { tag ->
                                Surface(
                                    onClick = { sendMessageToMockAI(tag) },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f))
                                ) {
                                    Text(tag, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Ask Dr. Flora...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = { sendMessageToMockAI(messageText) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp),
                            elevation = FloatingActionButtonDefaults.elevation(2.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isTyping) {
                item {
                    Text("Dr. Flora is typing...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}