package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.example.myapplication.auth.LoginScreen
import com.example.myapplication.auth.SignUpScreen
import com.example.myapplication.model.OrderItemRequest
import com.example.myapplication.model.OrderRequest
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

import kotlinx.serialization.Serializable

// 1. Credentials
private const val SUPABASE_URL = "https://mckpeuvojctibneakuje.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_BV7u7ShKZg6ozTBRf74EbQ_5aA4CKXu"

val supabase = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
    install(Postgrest)
    install(Auth)
}

@Serializable
data class UserProfile(
    val id: String,
    val full_name: String?,
    val phone_number: String?,
    val email: String?,
    val address: String? = null,
    val notifications_enabled: Boolean? = true
)

// NEW FUNCTION: Save Address
suspend fun saveAddressToProfile(newAddress: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext false
            supabase.from("profiles").update({
                set("address", newAddress)
            }) { filter { eq("id", user.id) } }
            true
        } catch (e: Exception) { false }
    }
}

// NEW FUNCTION: Toggle Notifications
suspend fun updateNotificationPreference(enabled: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext false
            supabase.from("profiles").update({
                set("notifications_enabled", enabled)
            }) { filter { eq("id", user.id) } }
            true
        } catch (e: Exception) { false }
    }
}

// --- GLOBAL BACKEND LOGIC ---
suspend fun fetchPlants(): List<Plant> = withContext(Dispatchers.IO) {
    try { supabase.from("plants").select().decodeList<Plant>() }
    catch (e: Exception) {
        Log.e("SupabaseError", "Fetch Plants Failed: ${e.message}")
        emptyList()
    }
}

data class PromoItem(val tag: String, val title: String, val bgColor: Color, val imageUrl: String)

// 1. THE LOGIN LOGIC
suspend fun loginProfessional(email: String, pass: String): String? {
    return try {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = pass
        }
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        val profile = supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserProfile>()
        profile?.full_name ?: "Plant Lover"
    } catch (e: Exception) {
        Log.e("AuthError", "Login crashed: ${e.message}")
        null
    }
}

// 2. THE SIGNUP LOGIC
suspend fun signUpProfessional(email: String, pass: String, name: String, phone: String): Boolean {
    return try {
        val authUser = supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = pass
        }
        val userId = authUser?.id ?: return false
        val newProfile = UserProfile(
            id = userId,
            full_name = name,
            phone_number = phone,
            email = email
        )
        supabase.from("profiles").insert(newProfile)
        true
    } catch (e: Exception) {
        Log.e("AuthError", "Signup crashed: ${e.message}")
        false
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
        } catch (e: Exception) {
            Log.e("SupabaseError", "Update Profile Failed: ${e.message}")
            false
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("flora_prefs", MODE_PRIVATE)
        val isFirstTime = sharedPref.getBoolean("is_first_time", true)

        setContent {
            val context = LocalContext.current

            // --- ALL YOUR HOISTED STATES ---
            var currentScreen by remember { mutableStateOf("home") }
            var userName by remember { mutableStateOf("User") }
            var userEmail by remember { mutableStateOf("") }
            var isDarkMode by remember { mutableStateOf(false) }
            var selectedPlant by remember { mutableStateOf<Plant?>(null) }
            var lastOrderId by remember { mutableStateOf("") }
            var currentOrderPhone by remember { mutableStateOf("") }
            var currentOrderAddress by remember { mutableStateOf("") }
            var isGift by remember { mutableStateOf(false) }
            var giftNote by remember { mutableStateOf("") }
            var showEditProfile by remember { mutableStateOf(false) }
            var lastOrderStatus by remember { mutableStateOf("Processing") }

            var successOrderTotal by remember { mutableDoubleStateOf(0.0) }
            var successOrderAddress by remember { mutableStateOf("") }

            var userAddress by remember { mutableStateOf("") }
            var notificationsEnabled by remember { mutableStateOf(true) }
            var showEditAddress by remember { mutableStateOf(false) }

            val cartItems = remember { mutableStateListOf<Triple<Plant, String?, Int>>() }
            val scope = rememberCoroutineScope()

            // BULLETPROOF CART MATH
            val subtotal = cartItems.sumOf { (plant, pot, qty) ->
                val potFee = if (pot?.contains("Ceramic") == true) 15.0 else 0.0
                (plant.price + potFee) * qty
            }
            val deliveryFee = if (subtotal >= 75.0 || cartItems.isEmpty()) 0.0 else 15.0
            val finalTotal = subtotal + deliveryFee

            // --- STARTUP LOGIC ---
            LaunchedEffect(Unit) {
                val session = supabase.auth.currentSessionOrNull()
                if (session != null) {
                    val user = session.user
                    userEmail = user?.email ?: ""
                    try {
                        val profile = supabase.from("profiles")
                            .select { filter { eq("id", user?.id ?: "") } }
                            .decodeSingleOrNull<UserProfile>()

                        userName = profile?.full_name ?: user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User"
                        userAddress = profile?.address ?: ""
                        notificationsEnabled = profile?.notifications_enabled ?: true
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
                    when (currentScreen) {
                        "onboarding" -> OnboardingScreen {
                            sharedPref.edit { putBoolean("is_first_time", false) }
                            currentScreen = "welcome"
                        }
                        "welcome" -> WelcomeScreen { currentScreen = "login" }
                        "login" -> LoginScreen(
                            onLoginSuccess = { name ->
                                userName = name
                                scope.launch {
                                    userEmail = supabase.auth.currentUserOrNull()?.email ?: ""
                                    val user = supabase.auth.currentUserOrNull()
                                    val profile = supabase.from("profiles").select { filter { eq("id", user?.id ?: "") } }.decodeSingleOrNull<UserProfile>()
                                    userAddress = profile?.address ?: ""
                                    notificationsEnabled = profile?.notifications_enabled ?: true
                                }
                                currentScreen = "home"
                            },
                            onGoToSignup = { currentScreen = "signup" },
                            loginLogic = { e, p -> loginProfessional(e, p) }
                        )
                        "signup" -> SignUpScreen(
                            onSignUpComplete = { name ->
                                userName = name
                                scope.launch { userEmail = supabase.auth.currentUserOrNull()?.email ?: "" }
                                currentScreen = "home"
                            },
                            onGoToLogin = { currentScreen = "login" },
                            signUpLogic = { e, p, n, ph -> signUpProfessional(e, p, n, ph) }
                        )

                        "home", "settings", "my_plants", "expert_chat", "cart", "my_orders" -> {
                            FloraMainContainer(
                                userName = userName,
                                userEmail = userEmail,
                                cartItems = cartItems,
                                isGift = isGift,
                                onIsGiftChange = { isGift = it },
                                giftNote = giftNote,
                                onGiftNoteChange = { giftNote = it },
                                onPlantClick = { selectedPlant = it; currentScreen = "details" },
                                currentNav = currentScreen,
                                onNavChange = { screen -> currentScreen = screen },
                                isDarkMode = isDarkMode,
                                onToggleDark = { isDarkMode = it },
                                onEditProfileRequested = { showEditProfile = true },
                                // THE BRIDGE FIX: Successfully passes the ID back from the nested component!
                                onTrackOrderRequest = { orderIdToTrack ->
                                    lastOrderId = orderIdToTrack
                                    currentScreen = "tracking"
                                }
                            )
                        }

                        "details" -> {
                            selectedPlant?.let { plant ->
                                PlantDetailsScreen(
                                    plant = plant,
                                    onBack = { currentScreen = "home"; selectedPlant = null },
                                    onAddToCart = { pot ->
                                        cartItems.add(Triple(plant, pot, 1))
                                        // Stays on page logic preserved
                                    }
                                )
                            }
                        }

                        "address" -> AddressScreen(
                            savedAddress = userAddress,
                            savedPhone = "",
                            onBack = { currentScreen = "cart" },
                            onNext = { addressData, phoneData ->
                                currentOrderAddress = addressData
                                currentOrderPhone = phoneData
                                currentScreen = "payment"
                            }
                        )

                        "payment" -> PaymentScreen(
                            total = finalTotal,
                            onBack = { currentScreen = "address" },
                            onPaymentSuccess = { method, saveCard, cardNumber, cardHolder ->
                                scope.launch {
                                    val user = supabase.auth.currentUserOrNull()
                                    if (user == null) {
                                        Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                                        currentScreen = "login"
                                        return@launch
                                    }

                                    val orderId = completePurchase(
                                        userId = user.id,
                                        address = currentOrderAddress,
                                        phone = currentOrderPhone,
                                        paymentMethod = method,
                                        total = finalTotal,
                                        isGift = isGift,
                                        giftNote = giftNote,
                                        cartItems = cartItems.toList()
                                    )

                                    if (orderId != null) {
                                        // Silent Card Saving
                                        if (saveCard && method == "card") {
                                            try {
                                                val last4 = if (cardNumber.length >= 4) cardNumber.takeLast(4) else ""
                                                supabase.from("profiles").update({
                                                    set("last_payment_method", "card")
                                                    set("saved_card_holder", cardHolder)
                                                    set("saved_card_last4", last4)
                                                }) { filter { eq("id", user.id) } }
                                            } catch (e: Exception) { Log.e("SupabaseAuth", "Save card failed") }
                                        }

                                        lastOrderId = orderId
                                        lastOrderStatus = "Processing"
                                        successOrderTotal = finalTotal
                                        successOrderAddress = currentOrderAddress
                                        cartItems.clear()
                                        currentScreen = "order_success"
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Purchase failed. Check logs.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )

                        "order_success" -> {
                            OrderSuccessScreen(
                                orderId = lastOrderId,
                                total = successOrderTotal,
                                address = successOrderAddress,
                                onTrackOrder = { currentScreen = "tracking" },
                                onGoHome = { currentScreen = "home" }
                            )
                        }
                        "tracking" -> {
                            TrackingScreen(
                                orderId = lastOrderId,
                                onBack = { currentScreen = "home" }
                            )
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

// -----------------------------------------------------------------------------------------
// --- FLORA MAIN CONTAINER (Updated Signature to accept onTrackOrderRequest) ---
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloraMainContainer(
    userName: String,
    userEmail: String,
    cartItems: SnapshotStateList<Triple<Plant, String?, Int>>,
    onPlantClick: (Plant) -> Unit,
    isGift: Boolean,
    onIsGiftChange: (Boolean) -> Unit,
    giftNote: String,
    onGiftNoteChange: (String) -> Unit,
    currentNav: String,
    onNavChange: (String) -> Unit,
    isDarkMode: Boolean,
    onToggleDark: (Boolean) -> Unit,
    onEditProfileRequested: () -> Unit,
    onTrackOrderRequest: (String) -> Unit // THE BRIDGE FIX
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var supabasePlants by remember { mutableStateOf<List<Plant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var activePetFilter by remember { mutableStateOf("All") }
    var activeLightFilter by remember { mutableStateOf("All") }

    var userAddress by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showEditAddress by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supabasePlants = fetchPlants()
        val user = supabase.auth.currentUserOrNull()
        if(user != null){
            try {
                val profile = supabase.from("profiles").select { filter { eq("id", user.id) } }.decodeSingleOrNull<UserProfile>()
                userAddress = profile?.address ?: ""
                notificationsEnabled = profile?.notifications_enabled ?: true
            } catch (e: Exception) {}
        }
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
                    title = { Text("Flora", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavChange("cart") }) {
                            BadgedBox(badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } }) {
                                Icon(Icons.Default.ShoppingCart, "Cart", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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

                    "my_plants" -> MyGreenhouseScreen(padding = PaddingValues(0.dp), userName = userName)
                    "expert_chat" -> ExpertChatScreen(onBack = { onNavChange("home") })

                    "my_orders" -> OrderHistoryScreen(
                        onBack = { onNavChange("home") },
                        onTrackOrder = onTrackOrderRequest // Routes securely back to MainActivity
                    )

                    "settings" -> {
                        SettingsScreen(
                            padding = PaddingValues(0.dp),
                            userName = userName,
                            userEmail = userEmail,
                            userAddress = userAddress,
                            notificationsEnabled = notificationsEnabled,
                            onBack = { onNavChange("home") },
                            onLogout = {
                                scope.launch { supabase.auth.signOut(); onNavChange("login") }
                            },
                            isDark = isDarkMode,
                            onToggleDark = onToggleDark,
                            onEditProfile = onEditProfileRequested,
                            onEditAddress = { showEditAddress = true },
                            onToggleNotifications = { isEnabled ->
                                notificationsEnabled = isEnabled
                                scope.launch { updateNotificationPreference(isEnabled) }
                            }
                        )
                    }
                    "cart" -> { CartScreen(
                        cartItems = cartItems,
                        isGift = isGift,
                        onIsGiftChange = onIsGiftChange,
                        giftNote = giftNote,
                        onGiftNoteChange = onGiftNoteChange,
                        onBack = { onNavChange("home") },
                        onCheckout = { onNavChange("address") },
                        onViewItem = onPlantClick
                    )}

                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Screen $currentNav not found.") }
                }
            }
        }
    }

    if (showEditAddress) {
        EditAddressDialog(
            currentAddress = userAddress,
            onDismiss = { showEditAddress = false },
            onSaveSuccess = { newAddress ->
                userAddress = newAddress
                showEditAddress = false
            }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismiss = { showFilterSheet = false },
            onApply = { pet, light ->
                activePetFilter = pet
                activeLightFilter = light
                showFilterSheet = false
            }
        )
    }
}

// ... ALL THE REMAINING HELPER COMPOSABLES (FloraHomeScreenContent, PromoBannerCard, Sidebar, Dialogs, completePurchase) REMAIN EXACTLY THE SAME BELOW THIS LINE ...
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredPlants = plants.filter { plant ->
        val matchesSearch = plant.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || plant.category == selectedCategory

        val matchesPet = appliedPetSafety == "All" || (appliedPetSafety.contains("Pet Friendly") && plant.isPetSafe)
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

        LazyRow( contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)) {
            items(listOf("All", "Indoor", "Outdoor", "Seeds")) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    onClick = { selectedCategory = category },
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
fun ProfessionalSidebar(userName: String, userEmail: String, currentNav: String, onNavChange: (String) -> Unit) {
    ModalDrawerSheet(
        modifier = Modifier.width(310.dp).fillMaxHeight(),
        drawerContainerColor = Color(0xFF081C15),
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SidebarItem(Icons.Default.Home, "Home", currentNav == "home") { onNavChange("home") }
                SidebarItem(Icons.Default.Star, "My Greenhouse", currentNav == "my_plants") { onNavChange("my_plants") }
                SidebarItem(Icons.Default.Call, "Expert Support", currentNav == "expert_chat") { onNavChange("expert_chat") }
                SidebarItem(Icons.AutoMirrored.Filled.List, "My Orders", currentNav == "my_orders") { onNavChange("my_orders") }
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
    onSaveSuccess: (String, String) -> Unit
) {
    var tempName by remember { mutableStateOf(currentName) }
    var tempEmail by remember { mutableStateOf(currentEmail) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {if (!isUpdating) onDismiss()},
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
}

@Composable
fun EditAddressDialog(
    currentAddress: String,
    onDismiss: () -> Unit,
    onSaveSuccess: (String) -> Unit
) {
    var tempAddress by remember { mutableStateOf(currentAddress) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        confirmButton = {
            if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            else TextButton(onClick = {
                isUpdating = true
                scope.launch {
                    if (saveAddressToProfile(tempAddress)) {
                        onSaveSuccess(tempAddress)
                        onDismiss()
                    }
                    isUpdating = false
                }
            }) { Text("Save Address", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { if (!isUpdating) TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } },
        title = { Text("Shipping Address", fontWeight = FontWeight.Black, color = Color(0xFF1B4332)) },
        text = {
            OutlinedTextField(
                value = tempAddress,
                onValueChange = { tempAddress = it },
                label = { Text("Full Address (Istanbul)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isUpdating
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

suspend fun completePurchase(
    userId: String,
    address: String,
    phone: String,
    paymentMethod: String,
    total: Double,
    isGift: Boolean,
    giftNote: String,
    cartItems: List<Triple<Plant, String?, Int>>
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val orderData = OrderRequest(
                user_id = userId,
                total_amount = total,
                address = address,
                phone_number = phone,
                payment_method = paymentMethod,
                is_gift = isGift,
                gift_note = giftNote
            )

            val orderResponse = supabase.from("orders")
                .insert(orderData) {
                    select()
                }
                .decodeSingle<Map<String, kotlinx.serialization.json.JsonElement>>()

            val rawOrderId = orderResponse["id"]?.toString() ?: return@withContext null
            val orderId = rawOrderId.replace("\"", "")

            cartItems.forEach { (plant, pot, qty) ->
                val itemData = OrderItemRequest(
                    order_id = orderId,
                    plant_id = plant.id ?: 0L,
                    quantity = qty,
                    pot_type = pot ?: "Standard"
                )
                supabase.from("order_items").insert(itemData)
            }

            Log.d("SupabaseSuccess", "Order successfully stored with ID: $orderId")
            orderId

        } catch (e: Exception) {
            Log.e("SupabaseError", "Detailed Failure: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}