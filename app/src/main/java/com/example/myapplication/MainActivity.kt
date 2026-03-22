package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.auth.LoginScreen
import com.example.myapplication.auth.SignUpScreen
import com.example.myapplication.SettingsScreen
import com.example.myapplication.model.Plant
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

data class PromoItem(val tag: String, val title: String, val bgColor: Color, val imageUrl: String)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedPref = getSharedPreferences("flora_prefs", android.content.Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("is_first_time", true)

            var currentScreen by remember {
                mutableStateOf(if (isFirstTime) "onboarding" else "welcome")
            }
            var isDarkMode by remember { mutableStateOf(false) }
            var userName by remember { mutableStateOf("Eiba Anas") }
            var userEmail by remember { mutableStateOf("eiba.anas@email.com") }
            var userPhone by remember { mutableStateOf("+90 5XX XXX XX XX") }
            var showEditProfile by remember { mutableStateOf(false) }
            var selectedPlant by remember { mutableStateOf<Plant?>(null) }
            var activeOrderId by remember { mutableStateOf("") }
            val cartItems = remember { mutableStateListOf<Pair<Plant, String?>>() }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        currentScreen == "onboarding" -> OnboardingScreen(onFinish = {
                            sharedPref.edit().putBoolean("is_first_time", false).apply()
                            currentScreen = "welcome"
                        })
                        currentScreen == "welcome" -> WelcomeScreen(onStartClick = { currentScreen = "login" })
                        currentScreen == "login" -> LoginScreen(onLoginSuccess = { currentScreen = "home" }, onGoToSignup = { currentScreen = "signup" })
                        currentScreen == "signup" -> SignUpScreen(onSignUpComplete = { currentScreen = "home" }, onGoToLogin = { currentScreen = "login" })

                        selectedPlant != null && currentScreen == "home" -> {
                            PlantDetailsScreen(
                                plant = selectedPlant!!,
                                onBack = { selectedPlant = null },
                                onAddToCart = { pot ->
                                    cartItems.add(selectedPlant!! to pot)
                                    selectedPlant = null
                                }
                            )
                        }

                        currentScreen == "cart" -> CartScreen(
                            cartItems = cartItems,
                            onBack = { currentScreen = "home" },
                            onCheckout = { currentScreen = "address" },
                            onViewItem = { plant ->
                                selectedPlant = plant
                                currentScreen = "home"
                            }
                        )

                        currentScreen == "address" -> AddressScreen(onBack = { currentScreen = "cart" }, onNext = { currentScreen = "payment" })
                        currentScreen == "payment" -> {
                            val total = cartItems.sumOf { it.first.price }
                            PaymentScreen(total = total, onBack = { currentScreen = "address" }, onPaymentSuccess = {
                                activeOrderId = "FL-${(1000..9999).random()}"
                                currentScreen = "success"
                            })
                        }
                        currentScreen == "success" -> OrderSuccessScreen(
                            orderId = activeOrderId,
                            onTrackOrder = { cartItems.clear(); currentScreen = "tracking" },
                            onGoHome = { currentScreen = "home" }
                        )
                        currentScreen == "tracking" -> TrackingScreen(
                            orderId = activeOrderId,
                            onBack = { currentScreen = "home" }
                        )
                        // In MainActivity.kt
                        currentScreen == "my_orders" -> OrderHistoryScreen(
                            onBack = { currentScreen = "home" },
                            onTrackOrder = { id ->
                                activeOrderId = id
                                currentScreen = "tracking"
                            }
                        )

                        // CORE APP CONTAINER
                        currentScreen == "home" || currentScreen == "settings" || currentScreen == "expert_chat" || currentScreen == "my_plants" -> {
                            FloraMainContainer(
                                userName = userName, // ADDED THIS
                                userEmail = userEmail, // ADDED THIS
                                cartItemsSize = cartItems.size,
                                onPlantClick = { selectedPlant = it },
                                currentNav = currentScreen,
                                onNavChange = { screen -> currentScreen = screen },
                                isDarkMode = isDarkMode,
                                onToggleDark = { newValue -> isDarkMode = newValue },
                                onEditProfileRequested = { showEditProfile = true } // ADDED THIS
                            )
                        }
                    }

                    // DIALOG LOGIC
                    if (showEditProfile) {
                        EditProfileDialog(
                            currentName = userName,
                            currentEmail = userEmail,
                            onDismiss = { showEditProfile = false },
                            onSave = { newName, newEmail ->
                                userName = newName
                                userEmail = newEmail
                                showEditProfile = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloraMainContainer(
    userName: String, // ADDED
    userEmail: String, // ADDED
    cartItemsSize: Int,
    onPlantClick: (Plant) -> Unit,
    currentNav: String,
    onNavChange: (String) -> Unit,
    isDarkMode: Boolean,
    onToggleDark: (Boolean) -> Unit,
    onEditProfileRequested: () -> Unit // ADDED
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showFilters by remember { mutableStateOf(false) }

    if (showFilters) {
        FilterBottomSheet(
            onDismiss = { showFilters = false },
            onApply = { _, _ -> showFilters = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfessionalSidebar(
                userName = userName, // CONNECTED
                userEmail = userEmail, // CONNECTED
                currentNav = currentNav,
                onNavChange = { screen ->
                    onNavChange(screen)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentNav) {
                                "home" -> "Flora"
                                "my_plants" -> "My Greenhouse"
                                "settings" -> "Settings"
                                else -> "Flora"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavChange("cart") }) {
                            BadgedBox(badge = { if (cartItemsSize > 0) Badge { Text(cartItemsSize.toString()) } }) {
                                Icon(Icons.Default.ShoppingCart, "Cart", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            when (currentNav.lowercase()) {
                "home" -> FloraHomeScreenContent(padding, onPlantClick, onFilterClick = { showFilters = true })
                "my_plants" -> MyGreenhouseScreen(padding = padding, userName = userName) // FIXED ERROR HERE
                "settings" -> SettingsScreen(
                    padding = padding,
                    onBack = { onNavChange("home") },
                    onLogout = { onNavChange("login") },
                    isDark = isDarkMode,
                    onToggleDark = onToggleDark,
                    onEditProfile = onEditProfileRequested // FIXED ERROR HERE
                )
                "expert_chat" -> ExpertChatScreen(onBack = { onNavChange("home") })
                else -> FloraHomeScreenContent(padding, onPlantClick, onFilterClick = { showFilters = true })
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloraHomeScreenContent(padding: PaddingValues, onPlantClick: (Plant) -> Unit, onFilterClick: () -> Unit) {
    val plants = listOf(
        Plant(1, "Monstera", "Indoor", 25.0, "Classic", "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=500", 4.9),
        Plant(2, "Calathea", "Indoor", 18.0, "Beautiful", "https://images.unsplash.com/photo-1559563458-527698bf5295?q=80&w=500", 4.7),
        Plant(3, "Snake Plant", "Hardy", 20.0, "Air Purifier", "https://images.unsplash.com/photo-1596547609652-9cf5d8d76921?q=80&w=500", 4.8),
        Plant(4, "Spider Plant", "Easy", 12.0, "Beginner", "https://images.unsplash.com/photo-1470509037663-253afd7f0f51?q=80&w=500", 4.5)
    )

    val promoBanners = listOf(
        PromoItem("Summer Sale", "Up to 50% Off", Color(0xFF1B4332), "https://images.unsplash.com/photo-1466781783364-391eaf8942ad?q=80&w=800"),
        PromoItem("Best Seller", "Monstera Deliciosa", Color(0xFF2D6A4F), "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=800")
    )

    val pagerState = rememberPagerState(pageCount = { promoBanners.size })

    Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        Column {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 20.dp), contentPadding = PaddingValues(horizontal = 24.dp), pageSpacing = 16.dp) { page ->
                PromoBannerCard(promoBanners[page])
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
                repeat(promoBanners.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
                    Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(if (pagerState.currentPage == iteration) 12.dp else 6.dp))
                }
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text("Discover your\nperfect greenery", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, lineHeight = 36.sp))
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.Search, null, tint = Color.Gray); Spacer(modifier = Modifier.width(12.dp)); Text("Search for plants...", color = Color.LightGray)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onFilterClick, modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))) { Icon(Icons.Rounded.Menu, null, tint = Color.White) }
            }
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(listOf("All", "Indoor", "Outdoor", "Seeds", "Tools")) { cat ->
                val isSelected = cat == "All"
                Surface(shape = RoundedCornerShape(16.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(0.3f)) else null) {
                    Text(text = cat, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
            plants.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { plant -> ModernPlantCard(plant, onPlantClick, Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PromoBannerCard(item: PromoItem) {
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = item.bgColor)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.6f)
            Column(modifier = Modifier.padding(20.dp).align(Alignment.CenterStart)) {
                Text(item.tag, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(item.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ModernPlantCard(plant: Plant, onClick: (Plant) -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.clickable { onClick(plant) },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(model = plant.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.padding(12.dp)) {
                Text(plant.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$${plant.price}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun OwnedPlantItem(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) { Text("🌿") }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                LinearProgressIndicator(progress = 0.4f, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
                Text("Next water in 3 days", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
// ... [Remaining functions ProfessionalSidebar, SidebarItem, MyPlantsScreen stay the same but use MaterialTheme colors]

@Composable
fun MyPlantsScreen(padding: PaddingValues) {
    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEC)), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, null, tint = Color(0xFFE67E22))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Care Alert", fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
                    Text("Your Monstera needs water today!", fontSize = 14.sp)
                }
            }
        }
        Text("Active Collection", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
        Spacer(modifier = Modifier.height(16.dp))
        listOf("Monstera Deliciosa", "Calathea Medallion").forEach { plantName -> OwnedPlantItem(plantName); Spacer(modifier = Modifier.height(12.dp)) }
        Button(onClick = { }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52B788)), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Search, null); Spacer(modifier = Modifier.width(8.dp)); Text("AI Plant Diagnosis")
        }
    }
}@Composable
fun ProfessionalSidebar(userName: String, userEmail: String, currentNav: String, onNavChange: (String) -> Unit) {
    ModalDrawerSheet(
        modifier = Modifier.width(310.dp).fillMaxHeight(), // Forces full height
        drawerContainerColor = Color(0xFF081C15),
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Fills the drawer area
                .padding(24.dp)
        ) {
            // 1. Profile Header
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF52B788)),
                contentAlignment = Alignment.Center
            ) {
                Text(userName.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(userEmail, fontSize = 14.sp, color = Color.White.copy(0.6f))

            Spacer(modifier = Modifier.height(40.dp))

            // 2. The List (Back to Normal)
            // Use a Column to stack them vertically
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SidebarItem(Icons.Default.Home, "Home", currentNav == "home") { onNavChange("home") }
                SidebarItem(Icons.Default.Star, "My Greenhouse", currentNav == "my_plants") { onNavChange("my_plants") }
                SidebarItem(Icons.Default.Call, "Expert Support", currentNav == "expert_chat") { onNavChange("expert_chat") }
                SidebarItem(Icons.Default.List, "My Orders", currentNav == "my_orders") { onNavChange("my_orders") }
                SidebarItem(Icons.Default.ShoppingCart, "My Cart", currentNav == "cart") { onNavChange("cart") }
                SidebarItem(Icons.Default.Settings, "Settings", currentNav == "settings") { onNavChange("settings") }
            }
        }
    }
}
@Composable
fun SidebarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) Color(0xFF2D6A4F) else Color.Transparent).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White); Spacer(modifier = Modifier.width(16.dp)); Text(label, color = Color.White)
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var tempName by remember { mutableStateOf(currentName) }
    var tempEmail by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(tempName, tempEmail) }) {
                Text("Save Changes", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        title = { Text("Edit Profile", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Full Name") },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tempEmail,
                    onValueChange = { tempEmail = it },
                    label = { Text("Email Address") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}