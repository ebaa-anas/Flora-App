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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import com.example.myapplication.model.Plant
import com.example.myapplication.ui.theme.MyApplicationTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 1. Credentials
private const val SUPABASE_URL = "https://mckpeuvojctibneakuje.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_BV7u7ShKZg6ozTBRf74EbQ_5aA4CKXu"

val supabase = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
    install(Postgrest)
    install(Auth)
}

data class PromoItem(val tag: String, val title: String, val bgColor: Color, val imageUrl: String)

// 2. Global Logic
suspend fun fetchPlants(): List<Plant> = withContext(Dispatchers.IO) {
    try { supabase.from("plants").select().decodeList<Plant>() }
    catch (e: Exception) { emptyList() }
}

suspend fun loginProfessional(email: String, pass: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            val userId = supabase.auth.currentUserOrNull()?.id
            val profile = supabase.from("profiles")
                .select { filter { eq("id", userId!!) } }
                .decodeSingleOrNull<Map<String, String>>()
            profile?.get("full_name")?.replace("\"", "")
        } catch (e: Exception) { null }
    }
}

suspend fun signUpProfessional(email: String, pass: String, name: String, phone: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            supabase.auth.signUpWith(Email) { this.email = email; this.password = pass }
            val userId = supabase.auth.currentUserOrNull()?.id
            if (userId != null) {
                supabase.from("profiles").insert(
                    mapOf("id" to userId, "full_name" to name, "phone_number" to phone, "email" to email)
                )
            }
            true
        } catch (e: Exception) { false }
    }
}

suspend fun saveProfileChanges(newName: String, newEmail: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                supabase.from("profiles").update({
                    set("full_name", newName)
                    set("email", newEmail)
                }) { filter { eq("id", user.id) } }
                true
            } else false
        } catch (e: Exception) { false }
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("flora_prefs", android.content.Context.MODE_PRIVATE)
        val isFirstTime = sharedPref.getBoolean("is_first_time", true)

        setContent {
            var isGift by remember { mutableStateOf(false) }
            var giftNote by remember { mutableStateOf("") }
            var currentScreen by remember { mutableStateOf("loading") }
            var userName by remember { mutableStateOf("User") }
            var userEmail by remember { mutableStateOf("") }
            var isDarkMode by remember { mutableStateOf(false) }
            var selectedPlant by remember { mutableStateOf<Plant?>(null) }

            // 1. UPDATED DATA TYPE: Triple(Plant, PotType, Quantity)
            val cartItems = remember { mutableStateListOf<Triple<Plant, String?, Int>>() }

            var showEditProfile by remember { mutableStateOf(false) }
            var currentOrderAddress by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()

            // 2. CALCULATE TOTAL (Needed for PaymentScreen)
            val subtotal = cartItems.sumOf { (plant, pot, qty) ->
                val potFee = if (pot == "Ceramic") 15.0 else 0.0
                (plant.price + potFee) * qty
            }
            val deliveryFee = if (subtotal >= 75.0 || cartItems.isEmpty()) 0.0 else 15.0
            val finalTotal = subtotal + deliveryFee

            LaunchedEffect(Unit) {
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    val user = session.user
                    userEmail = user?.email ?: ""
                    try {
                        val profile = supabase.from("profiles")
                            .select { filter { eq("id", user?.id ?: "") } }
                            .decodeSingle<Map<String, String>>()
                        userName = profile["full_name"] ?: "User"
                    } catch (e: Exception) {
                        userName = user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User"
                    }
                    currentScreen = "home"
                } else {
                    currentScreen = if (isFirstTime) "onboarding" else "welcome"
                }
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        currentScreen == "onboarding" -> OnboardingScreen {
                            sharedPref.edit().putBoolean("is_first_time", false).apply()
                            currentScreen = "welcome"
                        }
                        currentScreen == "welcome" -> WelcomeScreen { currentScreen = "login" }
                        currentScreen == "login" -> LoginScreen(
                            onLoginSuccess = { name -> userName = name; currentScreen = "home" },
                            onGoToSignup = { currentScreen = "signup" },
                            loginLogic = { e, p -> loginProfessional(e, p) }
                        )
                        currentScreen == "signup" -> SignUpScreen(
                            onSignUpComplete = { name -> userName = name; currentScreen = "home" },
                            onGoToLogin = { currentScreen = "login" },
                            signUpLogic = { e, p, n, ph -> signUpProfessional(e, p, n, ph) }
                        )

                        // 3. MAIN NAV GROUP (Updated cartItems type)
                        currentScreen == "home" || currentScreen == "settings" || currentScreen == "my_plants" || currentScreen == "expert_chat" || currentScreen == "cart" -> {
                            FloraMainContainer(
                                userName = userName,
                                userEmail = userEmail,
                                cartItems = cartItems,
                                onPlantClick = { selectedPlant = it; currentScreen = "details" },
                                currentNav = currentScreen,
                                isDarkMode = isDarkMode,
                                onToggleDark = { isDarkMode = it },
                                onEditProfileRequested = { showEditProfile = true },
                                onNavChange = { screen -> currentScreen = screen },
                                isGift = isGift,
                                onIsGiftChange = { isGift = it },
                                giftNote = giftNote,
                                onGiftNoteChange = { giftNote = it }
                            )}


                        currentScreen == "details" -> {
                            if (selectedPlant != null) {
                                PlantDetailsScreen(
                                    plant = selectedPlant!!,
                                    onBack = { currentScreen = "home"; selectedPlant = null },
                                    onAddToCart = { pot ->
                                        cartItems.add(Triple(selectedPlant!!, pot, 1))
                                        currentScreen = "cart"
                                        selectedPlant = null
                                    }
                                )
                            }
                        }

                        currentScreen == "address" -> AddressScreen(
                            onBack = { currentScreen = "cart" },
                            onNext = { addressData ->
                                currentOrderAddress = addressData
                                currentScreen = "payment"
                            }
                        )

                        currentScreen == "payment" -> {
                            PaymentScreen(
                                total = finalTotal,
                                onBack = { currentScreen = "address" },
                                onPaymentSuccess = {
                                    scope.launch {
                                        val userId = supabase.auth.currentUserOrNull()?.id ?: ""
                                        val orderId = completePurchase(
                                            userId = userId,
                                            address = currentOrderAddress,
                                            total = finalTotal,
                                            isGift = isGift,
                                            giftNote = giftNote,
                                            // 4. FIX: pass Triple list to updated function
                                            cartItems = cartItems
                                        )
                                        if (orderId != null) {
                                            cartItems.clear()
                                            currentScreen = "order_success"
                                        }
                                    }
                                }
                            )
                        }

                        currentScreen == "order_success" -> {
                            // Placeholder for your QR screen
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Order Success! QR coming soon.")
                            }
                        }
                    }

                    if (showEditProfile) {
                        EditProfileDialog(
                            currentName = userName,
                            currentEmail = userEmail,
                            onDismiss = { showEditProfile = false },
                            onSaveSuccess = { newName, newEmail ->
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
        userName: String,
        userEmail: String,
        cartItems: SnapshotStateList<Triple<Plant, String?, Int>>, // FIXED TYPE
        onPlantClick: (Plant) -> Unit,
        isGift: Boolean,
        onIsGiftChange: (Boolean) -> Unit,
        giftNote: String,
        onGiftNoteChange: (String) -> Unit,
        currentNav: String,
        onNavChange: (String) -> Unit,
        isDarkMode: Boolean,
        onToggleDark: (Boolean) -> Unit,
        onEditProfileRequested: () -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Data Fetching
        var supabasePlants by remember { mutableStateOf<List<Plant>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var showFilterSheet by remember { mutableStateOf(false) }
        var activePetFilter by remember { mutableStateOf("All") }
        var activeLightFilter by remember { mutableStateOf("All") }

        LaunchedEffect(Unit) {
            supabasePlants = fetchPlants()
            isLoading = false
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ProfessionalSidebar(
                    userName = userName,
                    userEmail = userEmail,
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
                                "Flora",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    "Menu",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { onNavChange("cart") }) {
                                BadgedBox(badge = { if (cartItems.size > 0) Badge { Text(cartItems.size.toString()) } }) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        "Cart",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    // Navigation Logic
                    when (currentNav.lowercase().replace(" ", "_")) {
                        "home" -> {
                            if (isLoading) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                FloraHomeScreenContent(
                                    padding = PaddingValues(0.dp),
                                    plants = supabasePlants,
                                    onPlantClick = onPlantClick,
                                    onFilterClick = { showFilterSheet = true },
                                    appliedPetSafety = activePetFilter,
                                    appliedLight = activeLightFilter
                                )
                            }
                        }

                        "my_plants" -> MyGreenhouseScreen(
                            padding = PaddingValues(0.dp),
                            userName = userName
                        )

                        "expert_chat" -> ExpertChatScreen(onBack = { onNavChange("home") })

                        "settings" -> {
                            SettingsScreen(
                                padding = PaddingValues(0.dp),
                                onBack = { onNavChange("home") },
                                onLogout = { onNavChange("login") },
                                isDark = isDarkMode,
                                onToggleDark = onToggleDark,
                                onEditProfile = onEditProfileRequested
                            )
                        }
                        "cart" -> { CartScreen(
                            cartItems = cartItems,
                            isGift = isGift,
                            onIsGiftChange = { onIsGiftChange(it) }, // These need to be passed in
                            giftNote = giftNote,
                            onGiftNoteChange = { onGiftNoteChange(it) },
                            onBack = { onNavChange("home") },
                            onCheckout = { onNavChange("address") },
                            onViewItem = { plant -> onPlantClick(plant) }
                        )}
                        "address" -> AddressScreen(
                            onBack = { onNavChange("cart") },
                            onNext = { onNavChange("PaymentScreen") }
                        )


                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Screen $currentNav not found.")
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloraHomeScreenContent(
    padding: PaddingValues,
    plants: List<Plant>,
    onPlantClick: (Plant) -> Unit,
    onFilterClick: () -> Unit,
    appliedPetSafety: String = "All",
    appliedLight: String = "All"
) {
    // 1. LOCAL SEARCH STATE
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // 2. FILTER LOGIC
    val filteredPlants = plants.filter { plant ->
        val matchesSearch = plant.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || plant.category == selectedCategory

        // Note: For Pet Safety and Light to work, these columns must exist in your Plant model/DB
        // If they don't exist yet, this part will just show all plants
        val matchesPet = appliedPetSafety == "All" || (appliedPetSafety.contains("Pet Friendly") && plant.isPetSafe == true)
        val matchesLight = appliedLight == "All" || plant.light == appliedLight

        matchesSearch && matchesCategory && matchesPet && matchesLight
    }

    val promoBanners = listOf(
        PromoItem("Best Seller", "Monstera Deliciosa", Color(0xFF2D6A4F), "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=800"),
        PromoItem("Summer Sale", "Up to 50% Off", Color(0xFF1B4332), "https://images.unsplash.com/photo-1466781783364-391eaf8942ad?q=80&w=800"),
        PromoItem("Best Seller", "Monstera Deliciosa", Color(0xFF2D6A4F), "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=800")
    )
    val pagerState = rememberPagerState(pageCount = { promoBanners.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 16.dp
            ) { page ->
                PromoBannerCard(promoBanners[page])
            }

            // Pager Indicators
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(promoBanners.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 10.dp else 6.dp)
                    )
                }
            }
        }
        // 3. SEARCH & HEADER SECTION (Now Functional)
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Text("Discover your\nperfect greenery",
                style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 35.sp,
                color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))

                        // ADDED: Real TextField for Searching
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) Text("Search for plants...", color = Color.Gray)
                                innerTextField()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                ) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Filter", tint = Color.White)
                }
            }
        }

        // 4. CATEGORY CHIPS (Now Functional)
        LazyRow( contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)) {
            items(listOf("All", "Indoor", "Outdoor", "Seeds")) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    onClick = { selectedCategory = category }, // ADDED: Click logic
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(0.3f)) else null
                ) {
                    Text(text = category,  modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. DATA GRID (Now uses filteredPlants)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            filteredPlants.chunked(2).forEach { plantPair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    plantPair.forEach { plant ->
                        ModernPlantCard(plant = plant, onClick = onPlantClick, modifier = Modifier.weight(1f))
                    }
                    if (plantPair.size == 1) Spacer(modifier = Modifier.weight(1f))
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
    onSaveSuccess: (String, String) -> Unit // Pass updated data back to UI
) {
    var tempName by remember { mutableStateOf(currentName) }
    var tempEmail by remember { mutableStateOf(currentEmail) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {if (!isUpdating) onDismiss()}, // Block dismiss while loading
        confirmButton = {
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = {
                    isUpdating = true
                    scope.launch {
                        val success = saveProfileChanges(tempName, tempEmail)
                        if (success) {
                            onSaveSuccess(tempName, tempEmail)
                            onDismiss()
                        }
                        isUpdating = false
                    }
                }) {
                    Text("Save Changes", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isUpdating) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
            }
        },
        title = { Text("Edit Profile", fontWeight = FontWeight.Black, color = Color(0xFF1B4332)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdating
                )
                OutlinedTextField(
                    value = tempEmail,
                    onValueChange = { tempEmail = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUpdating
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}// 6. UPDATE PURCHASE FUNCTION
suspend fun completePurchase(
    userId: String,
    address: String,
    total: Double,
    isGift: Boolean,
    giftNote: String,
    cartItems: List<Triple<Plant, String?, Int>> // FIXED TYPE
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val orderResponse = supabase.from("orders").insert(
                mapOf(
                    "user_id" to userId,
                    "total_amount" to total,
                    "address" to address,
                    "is_gift" to isGift,
                    "gift_note" to giftNote,
                    "status" to "Processing"
                )
            ) { select() }.decodeSingle<Map<String, Any>>()

            val orderId = orderResponse["id"].toString()

            cartItems.forEach { (plant, pot, qty) ->
                supabase.from("order_items").insert(
                    mapOf(
                        "order_id" to orderId,
                        "plant_id" to plant.id,
                        "quantity" to qty,
                        "pot_type" to (pot ?: "Standard")
                    )
                )
            }
            orderId
        } catch (e: Exception) { null }
    }
}