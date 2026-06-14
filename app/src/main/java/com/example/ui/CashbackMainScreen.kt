package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Bank
import com.example.data.CashbackWithBank
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke

data class PopularBankPreset(val name: String, val colorHex: String, val emoji: String)


// Utility helper to match categories to relevant descriptive emojis
fun getEmojiForCategory(category: String, presetIcon: String = ""): String {
    val norm = category.lowercase().trim()
    return when {
        norm.contains("супермаркет") || norm.contains("магазин") || norm.contains("продукт") || presetIcon == "shopping_cart" -> "🛒"
        norm.contains("такси") || norm.contains("транспорт") || norm.contains("метро") || presetIcon == "local_taxi" -> "🚕"
        norm.contains("ресторан") || norm.contains("кафе") || norm.contains("еда") || norm.contains("бургер") || presetIcon == "restaurant" -> "🍔"
        norm.contains("аптек") || norm.contains("здоров") || norm.contains("лекарств") || presetIcon == "local_pharmacy" -> "💊"
        norm.contains("одежд") || norm.contains("обувь") || norm.contains("плать") || presetIcon == "checkroom" -> "👕"
        norm.contains("электроник") || norm.contains("техник") || norm.contains("телефон") || presetIcon == "devices" -> "💻"
        norm.contains("топлив") || norm.contains("бензин") || norm.contains("азс") || presetIcon == "local_gas_station" -> "⛽️"
        norm.contains("развлечен") || norm.contains("кино") || norm.contains("театр") || norm.contains("билет") || presetIcon == "theater_comedy" -> "🎟"
        norm.contains("красот") || norm.contains("салон") || norm.contains("спа") || norm.contains("косметик") || presetIcon == "spa" -> "💅"
        norm.contains("дом") || norm.contains("ремонт") || norm.contains("мебель") || presetIcon == "construction" -> "🔨"
        norm.contains("все покупки") || norm.contains("любые") || presetIcon == "percent" -> "💳"
        else -> "💰"
    }
}

// Convert Color Hex string back safely
fun parseColorHex(hex: String, default: Color = Color(0xFF673AB7)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashbackMainScreen(
    viewModel: CashbackViewModel,
    modifier: Modifier = Modifier
) {
    val banks by viewModel.allBanks.collectAsStateWithLifecycle()
    val cashbacks by viewModel.allCashbacks.collectAsStateWithLifecycle()
    val filteredCashbacks by viewModel.filteredCashbacks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBankFilter by viewModel.selectedBankFilter.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("best_deals") } // "best_deals" or "my_cards"
    var showAddBankDialog by remember { mutableStateOf(false) }
    var showAddCashbackDialog by remember { mutableStateOf(false) }
    var preselectedBankId by remember { mutableStateOf<Long?>(null) }
    var editingCashback by remember { mutableStateOf<CashbackWithBank?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("cashback_prefs", android.content.Context.MODE_PRIVATE) }
    var tutorialStep by remember { mutableIntStateOf(prefs.getInt("tutorial_step_key", 1)) }
    
    val focusManager = LocalFocusManager.current

    // Pulsing scale for highlighting the FAB during the second step of onboarding
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_pulse")
    val fabScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (banks.isNotEmpty() && cashbacks.isEmpty() && tutorialStep > 0) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_scale"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            // Hide FAB when overlays are shown to prevent overlapping "Save"/"Create" buttons!
            if (!showAddBankDialog && !showAddCashbackDialog) {
                if (activeTab == "my_cards" && banks.isNotEmpty() || (activeTab == "best_deals" && banks.isNotEmpty())) {
                    ExtendedFloatingActionButton(
                        text = { Text("Внести кешбэк", fontWeight = FontWeight.Bold, color = Color.White) },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White) },
                        onClick = { 
                            preselectedBankId = null
                            showAddCashbackDialog = true 
                        },
                        containerColor = Color(0xFF2E7D32), // High visibility success green FAB color requested by user
                        modifier = Modifier
                            .testTag("add_cashback_fab")
                            .padding(bottom = 16.dp)
                            .scale(fabScale)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header Title Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Кешбэк Помощник",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = "Сравнивай проценты и выбирай лучший банк",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }
                    
                    // Quick Stat Counter Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${banks.size} 🏦 • ${cashbacks.size} 🏷️",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Custom Tab Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(12.dp))

                    val activeTabColor = MaterialTheme.colorScheme.surface
                    val activeTextColor = MaterialTheme.colorScheme.primary

                    // Best Deals tab
                    Box(
                        modifier = tabModifier
                            .testTag("tab_best_deals")
                            .background(if (activeTab == "best_deals") activeTabColor else Color.Transparent)
                            .clickable { activeTab = "best_deals" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star, 
                                contentDescription = null, 
                                tint = if (activeTab == "best_deals") activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Где выгоднее?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (activeTab == "best_deals") activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // My Cards/Banks tab
                    val myCardsHighlight = banks.isEmpty() && activeTab == "best_deals" && tutorialStep > 0
                    val myCardsBorderModifier = if (myCardsHighlight) {
                        Modifier
                            .scale(fabScale)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = tabModifier
                            .then(myCardsBorderModifier)
                            .testTag("tab_my_cards")
                            .background(if (activeTab == "my_cards") activeTabColor else Color.Transparent)
                            .clickable { activeTab = "my_cards" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Home, 
                                contentDescription = null, 
                                tint = if (activeTab == "my_cards") activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Мои Банки",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (activeTab == "my_cards") activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Render Active Panel
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    modifier = Modifier.weight(1f),
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        "best_deals" -> {
                            BestDealsTabContent(
                                viewModel = viewModel,
                                filteredCashbacks = filteredCashbacks,
                                searchQuery = searchQuery,
                                banksAvailable = banks.isNotEmpty(),
                                onSelectCategoryPreset = { preset ->
                                    viewModel.searchQuery.value = preset
                                }
                            )
                        }
                        "my_cards" -> {
                            MyCardsTabContent(
                                viewModel = viewModel,
                                banks = banks,
                                cashbacks = cashbacks,
                                onAddBankClick = { showAddBankDialog = true },
                                onAddCashbackClick = { bankId ->
                                    preselectedBankId = bankId
                                    editingCashback = null
                                    showAddCashbackDialog = true
                                },
                                onEditCashbackClick = { cb ->
                                    editingCashback = cb
                                    showAddCashbackDialog = true
                                },
                                isTutorialHighlight = banks.isEmpty() && activeTab == "my_cards" && tutorialStep > 0,
                                tutorialPulseScale = fabScale
                            )
                        }
                    }
                }
            }

            // 💡 Interactive Onboarding Tutorial Assistant Box
            if (tutorialStep > 0) {
                // Determine text description dynamically based on actual app usage state
                val tutorialText = when {
                    banks.isEmpty() && activeTab == "best_deals" -> {
                        "👋 Добро пожаловать в Кешбэк Помощник! Давайте научимся пользоваться приложением.\n\n👉 Шаг 1 из 3: Перейдите во вкладку 'Мои Банки' (вверху), чтобы добавить ваш первый банк."
                    }
                    banks.isEmpty() && activeTab == "my_cards" -> {
                        "🎯 Шаг 1 из 3: Отлично! Теперь нажмите кнопку 'Новый Банк' вверху экрана, чтобы добавить ваш банк."
                    }
                    banks.isNotEmpty() && cashbacks.isEmpty() -> {
                        "💸 Шаг 2 из 3: Супер, ваш первый банк успешно добавлен! Теперь внесите процент кешбэка по любой категории. Нажмите круглую зелёную кнопку 'Внести кешбэк' внизу справа."
                    }
                    banks.isNotEmpty() && cashbacks.isNotEmpty() -> {
                        "🎉 Шаг 3 из 3: Всё настроено! Теперь откройте вкладку 'Где выгоднее?' (сверху). Начните вводить в поиск категорию (например, такси) или кликните готовые чипсы — мы автоматически выберем банк с максимальным возвратом!"
                    }
                    else -> {
                        "📈 Всё готово! Добавляйте новые банки и проводите поиск лучших предложений."
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (banks.isNotEmpty()) 80.dp else 0.dp) // Offset to prevent blocking the FAB if showing (Step 2 and Step 3)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 Интерактивное обучение",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = { 
                                        tutorialStep = 0
                                        prefs.edit().putInt("tutorial_step_key", 0).apply()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Skip tutorial",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = tutorialText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (banks.isNotEmpty() && cashbacks.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            tutorialStep = 0
                                            prefs.edit().putInt("tutorial_step_key", 0).apply()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Завершить обучение 🌟", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            tutorialStep = 0
                                            prefs.edit().putInt("tutorial_step_key", 0).apply()
                                        }
                                    ) {
                                        Text("Пропустить обучение", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Slide Up dialog for Adding Bank
            AnimatedVisibility(
                visible = showAddBankDialog,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                AddBankOverlay(
                    onDismiss = { showAddBankDialog = false },
                    onSave = { name, colorHex, emoji ->
                        viewModel.addBank(name, colorHex, emoji)
                        showAddBankDialog = false
                    }
                )
            }

            // Slide Up dialog for Adding / Editing Cashback
            AnimatedVisibility(
                visible = showAddCashbackDialog,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                AddCashbackOverlay(
                    banks = banks,
                    presets = viewModel.presetCategories,
                    cashbacks = cashbacks,
                    preselectedBankId = preselectedBankId,
                    editingCashback = editingCashback,
                    onDismiss = { 
                        showAddCashbackDialog = false
                        editingCashback = null
                    },
                    onSave = { bankId, category, percent, icon ->
                        if (editingCashback != null) {
                            viewModel.updateCashback(editingCashback!!.id, bankId, category, percent, icon)
                        } else {
                            viewModel.addCashback(bankId, category, percent, icon)
                        }
                        showAddCashbackDialog = false
                        editingCashback = null
                    },
                    onAddBankClick = {
                        showAddCashbackDialog = false
                        showAddBankDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun BestDealsTabContent(
    viewModel: CashbackViewModel,
    filteredCashbacks: List<CashbackWithBank>,
    searchQuery: String,
    banksAvailable: Boolean,
    onSelectCategoryPreset: (String) -> Unit
) {
    var cashbackToDelete by remember { mutableStateOf<CashbackWithBank?>(null) }

    if (cashbackToDelete != null) {
        AlertDialog(
            onDismissRequest = { cashbackToDelete = null },
            title = { Text("Удалить категорию?") },
            text = { Text("Вы действительно хотите удалить категорию '${cashbackToDelete?.category}' из банка '${cashbackToDelete?.bankName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        cashbackToDelete?.let { viewModel.removeCashback(it.id) }
                        cashbackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Удалить", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { cashbackToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_cashback_field"),
            placeholder = { Text("Искать категорию: например, такси") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Hot Categories Preset Row for instant clicks
        val fastCategories = listOf(
            "Все" to "💰",
            "Супермаркеты" to "🛒",
            "Такси и Транспорт" to "🚕",
            "Рестораны и Кафе" to "🍔",
            "Аптеки" to "💊",
            "Одежда" to "👕",
            "Электроника" to "💻",
            "Топливо" to "⛽️",
            "Развлечения" to "🎟"
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 12.dp)
        ) {
            items(fastCategories, key = { it.first }) { pair ->
                val (title, emoji) = pair
                val isSelected = (title == "Все" && searchQuery.isEmpty()) || 
                                 (title != "Все" && searchQuery.equals(title, ignoreCase = true))

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectCategoryPreset(if (title == "Все") "" else title)
                    },
                    label = { Text("$emoji $title", fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.height(38.dp)
                )
            }
        }

        if (!banksAvailable) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🏦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "У вас пока нет добавленных банков",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Перейдите во вкладку 'Мои Банки', чтобы добавить свой первый банк и внести туда активные категории.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        } else if (filteredCashbacks.isEmpty()) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Полученные кешбэки не найдены",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (searchQuery.isNotEmpty()) "Попробуйте ввести другой поисковый запрос или обнулить фильтр"
                            else "Добавьте категории кешбэка с помощью кнопки '+'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        } else {
            // Cashback comparison list
            Text(
                text = "Лучшие предложения по вашему запросу:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
            ) {
                items(filteredCashbacks, key = { cb -> cb.id }) { cb ->
                    CashbackRankItem(cb, onDeleteClick = { cashbackToDelete = cb })
                }
            }
        }
    }
}

@Composable
fun CashbackRankItem(
    cb: CashbackWithBank,
    onDeleteClick: () -> Unit
) {
    val bankColor = remember(cb.bankColorHex) { parseColorHex(cb.bankColorHex) }
    val categoryEmoji = remember(cb.category, cb.iconName) { getEmojiForCategory(cb.category, cb.iconName) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Badge with Circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(bankColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryEmoji,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main info details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cb.category,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Bank Indicator Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(bankColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${cb.bankIconEmoji} ${cb.bankName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Percentage Returns Badge & Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // High contrast percent label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(bankColor, bankColor.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Determine readable text color based on brightness
                    val lightText = Color.White
                    val darkText = Color(0xFF1E293B)
                    // High-contrast simple check for yellow-like colors
                    val textColor = if (cb.bankColorHex.lowercase() == "#ffdd2d" || cb.bankColorHex.lowercase() == "#fcd91f") darkText else lightText
                    
                    Text(
                        text = "${cb.percentage}%",
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Cashback",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MyCardsTabContent(
    viewModel: CashbackViewModel,
    banks: List<Bank>,
    cashbacks: List<com.example.data.CashbackWithBank>,
    onAddBankClick: () -> Unit,
    onAddCashbackClick: (Long) -> Unit,
    onEditCashbackClick: (com.example.data.CashbackWithBank) -> Unit = {},
    isTutorialHighlight: Boolean = false,
    tutorialPulseScale: Float = 1f
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cashback_prefs", android.content.Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", false)) }

    var bankToDelete by remember { mutableStateOf<Bank?>(null) }
    var runningExitAnimBankId by remember { mutableStateOf<Long?>(null) }

    var cashbackToDeleteInBank by remember { mutableStateOf<com.example.data.CashbackWithBank?>(null) }
    var runningExitAnimCid by remember { mutableStateOf<Long?>(null) }

    // Dialog for bank deletion confirmation
    if (bankToDelete != null) {
        AlertDialog(
            onDismissRequest = { bankToDelete = null },
            title = { Text("Удалить банк?") },
            text = { Text("Вы действительно хотите удалить банк '${bankToDelete?.name}' и все связанные с ним категории?") },
            confirmButton = {
                Button(
                    onClick = {
                        val idToExit = bankToDelete?.id
                        if (idToExit != null) {
                            runningExitAnimBankId = idToExit
                        }
                        bankToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Удалить", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { bankToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog for category deletion confirmation inside a bank
    if (cashbackToDeleteInBank != null) {
        AlertDialog(
            onDismissRequest = { cashbackToDeleteInBank = null },
            title = { Text("Удалить категорию?") },
            text = { Text("Вы действительно хотите удалить категорию '${cashbackToDeleteInBank?.category}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        val cidToExit = cashbackToDeleteInBank?.id
                        if (cidToExit != null) {
                            runningExitAnimCid = cidToExit
                        }
                        cashbackToDeleteInBank = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Удалить", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { cashbackToDeleteInBank = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ваши банки",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Add Bank Shortcut
                Button(
                    onClick = onAddBankClick,
                    modifier = Modifier
                        .height(34.dp)
                        .scale(if (isTutorialHighlight) tutorialPulseScale else 1f)
                        .testTag("add_bank_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Новый Банк", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (banks.isEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗳️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Список пуст",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Создайте банк с помощью кнопки 'Новый Банк'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        } else {
            items(banks, key = { it.id }) { bank ->
                val isVisible = runningExitAnimBankId != bank.id
                LaunchedEffect(isVisible) {
                    if (!isVisible) {
                        delay(250)
                        viewModel.removeBank(bank.id)
                        runningExitAnimBankId = null
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
                    exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
                ) {
                    val bankColor = remember(bank.colorHex) { parseColorHex(bank.colorHex) }
                    val bankCashbacks = remember(cashbacks, bank.id) { cashbacks.filter { it.bankId == bank.id } }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(18.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column {
                            // Header block styled with actual brand color
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(bankColor, bankColor.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                val lightText = Color.White
                                val darkText = Color(0xFF1E293B)
                                val headerTextColor = if (bank.colorHex.lowercase() == "#ffdd2d" || bank.colorHex.lowercase() == "#fcd91f") darkText else lightText

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = bank.iconEmoji,
                                            fontSize = 22.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = bank.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = headerTextColor
                                            )
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Quick add button inside card header and preselected this bank
                                        IconButton(
                                            onClick = { onAddCashbackClick(bank.id) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add Cashback to bank",
                                                tint = headerTextColor
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(2.dp))

                                        // Delete bank button
                                        IconButton(
                                            onClick = { bankToDelete = bank },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove Bank",
                                                tint = headerTextColor.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Categories listed of that bank
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (bankCashbacks.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Кешбэк ещё не внесён",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                } else {
                                    bankCashbacks.forEach { cb ->
                                        val isCbVisible = runningExitAnimCid != cb.id
                                        LaunchedEffect(isCbVisible) {
                                            if (!isCbVisible) {
                                                delay(250)
                                                viewModel.removeCashback(cb.id)
                                                runningExitAnimCid = null
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = isCbVisible,
                                            enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
                                            exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(animationSpec = tween(250))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                    .clickable { onEditCashbackClick(cb) }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    val categoryEmoji = remember(cb.category, cb.iconName) { getEmojiForCategory(cb.category, cb.iconName) }
                                                    Text(
                                                        text = categoryEmoji,
                                                        fontSize = 20.sp,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    Text(
                                                        text = cb.category,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${cb.percentage}%",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = bankColor
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = { cashbackToDeleteInBank = cb },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close, 
                                                            contentDescription = "Delete", 
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reminders & Notifications Control Card
        item {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Runtime permission launcher for API 33+
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    com.example.notification.CashbackNotificationReceiver.scheduleMonthlyAlarm(context)
                    prefs.edit().putBoolean("notifications_enabled", true).apply()
                    notificationsEnabled = true
                    Toast.makeText(context, "Уведомления включены! Мы напомним вам 28-го числа каждого месяца.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Разрешение на уведомления отклонено.", Toast.LENGTH_SHORT).show()
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔔", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                  text = "Напоминания о кешбэке",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Конец каждого месяца (28 число)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "В конце месяца банки обновляют любимые категории. Мы напомним вам открыть приложение и внести новые данные, чтобы всегда платить банком с максимальным процентом возврата!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Activate toggle btn
                        Button(
                            onClick = {
                                if (notificationsEnabled) {
                                    com.example.notification.CashbackNotificationReceiver.cancelMonthlyAlarm(context)
                                    prefs.edit().putBoolean("notifications_enabled", false).apply()
                                    notificationsEnabled = false
                                    Toast.makeText(context, "Уведомления выключены.", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        com.example.notification.CashbackNotificationReceiver.scheduleMonthlyAlarm(context)
                                        prefs.edit().putBoolean("notifications_enabled", true).apply()
                                        notificationsEnabled = true
                                        Toast.makeText(context, "Уведомления успешно включены!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (notificationsEnabled) MaterialTheme.colorScheme.error.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (notificationsEnabled) "Выкл. уведомления" else "Вкл. уведомления",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Test btn
                        OutlinedButton(
                            onClick = {
                                com.example.notification.CashbackNotificationReceiver.sendImmediateTestNotification(context)
                                Toast.makeText(context, "Тестовое уведомление отправлено!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Тест 🚀", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // GitHub Repository Card
        item {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://github.com/Tixii-Don/Cashback-helper") }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔗", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Проект на GitHub",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "https://github.com/Tixii-Don/Cashback-helper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text("😺", fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
fun AddBankOverlay(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var selectedEmojiIndex by remember { mutableIntStateOf(0) }

    val bankColorsList = listOf(
        "#FFDD2D" to "Т-Банк",
        "#EF3124" to "Альфа",
        "#005BFF" to "Ozon",
        "#21A038" to "Сбер",
        "#FCD91F" to "Яндекс",
        "#0E58A4" to "ВТБ",
        "#8B5CF6" to "Пурпур",
        "#EC4899" to "Розовый",
        "#FF5722" to "Оранж",
        "#111827" to "Графит"
    )

    val bankEmojisList = listOf("🟡", "🔴", "🔵", "🟢", "⭐️", "💎", "💳", "🏦", "🟣", "⚫️", "✅", "🔥")

    val popularPresetBanks = listOf(
        PopularBankPreset("Т-Банк", "#FFDD2D", "🟡"),
        PopularBankPreset("Альфа-Банк", "#EF3124", "🔴"),
        PopularBankPreset("СберБанк", "#21A038", "🟢"),
        PopularBankPreset("Ozon Банк", "#005BFF", "🔵"),
        PopularBankPreset("Яндекс Банк", "#FCD91F", "⭐️"),
        PopularBankPreset("ВТБ", "#0E58A4", "💎"),
        PopularBankPreset("Свой вариант", "#8B5CF6", "💳")
    )
    var selectedPresetIndex by remember { mutableIntStateOf(-1) }

    val localFocusManager = LocalFocusManager.current

    // Full screen blur blocker overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    localFocusManager.clearFocus()
                    onDismiss()
                }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Intercept and consume clicks inside dialog card
                ),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .size(40.dp, 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    "Добавить банк",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Popular banks presets carousel list
                Text(
                    "ВЫБЕРИТЕ ИЗ ПОПУЛЯРНЫХ (В ОДИН КЛИК):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                val bankPresetsScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .horizontalScroll(bankPresetsScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularPresetBanks.forEachIndexed { index, preset ->
                        val isSelected = selectedPresetIndex == index
                        val pColor = parseColorHex(preset.colorHex)
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) pColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) pColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.clickable {
                                selectedPresetIndex = index
                                if (preset.name == "Свой вариант") {
                                    name = ""
                                    selectedColorIndex = 6 // Purple default color for manual
                                    selectedEmojiIndex = 6 // Card icon emoji
                                } else {
                                    name = preset.name
                                    // Match colorHex to core index
                                    val matchCIdx = bankColorsList.indexOfFirst { it.first.lowercase() == preset.colorHex.lowercase() }
                                    if (matchCIdx != -1) selectedColorIndex = matchCIdx
                                    // Match emoji to core index
                                    val matchEIdx = bankEmojisList.indexOfFirst { it == preset.emoji }
                                    if (matchEIdx != -1) selectedEmojiIndex = matchEIdx
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    "НАЗВАНИЕ КАРТЫ / БАНКА ИЛИ СВОЙ ВАРИАНТ:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Название банка (например, Т-Банк, Альфа-Банк)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bank_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Emoji Icon choice selector
                Text(
                    "ВЫБЕРИТЕ ЛОГОТИП / ЭМОДЗИ:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bankEmojisList.size) { index ->
                        val emoji = bankEmojisList[index]
                        val isSelected = selectedEmojiIndex == index
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable { selectedEmojiIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Color choice selector
                Text(
                    "ФИРМЕННЫЙ ЦВЕТ КАРТЫ:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bankColorsList.size) { index ->
                        val (hex, label) = bankColorsList[index]
                        val isSelected = selectedColorIndex == index
                        this@Column.AnimatedVisibility(visible = true) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(parseColorHex(hex))
                                    .border(3.dp, if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent, CircleShape)
                                    .clickable { selectedColorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Done, 
                                        contentDescription = null, 
                                        tint = if (hex == "#FFDD2D" || hex == "#FCD91F") Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Отмена", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val (hexColor, _) = bankColorsList[selectedColorIndex]
                                val emoji = bankEmojisList[selectedEmojiIndex]
                                onSave(name.trim(), hexColor, emoji)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("submit_bank_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Создать", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCashbackOverlay(
    banks: List<Bank>,
    presets: List<CategoryPreset>,
    cashbacks: List<com.example.data.CashbackWithBank>,
    preselectedBankId: Long? = null,
    editingCashback: com.example.data.CashbackWithBank? = null,
    onDismiss: () -> Unit,
    onSave: (Long, String, Double, String) -> Unit,
    onAddBankClick: () -> Unit
) {
    var selectedBankIndex by remember(preselectedBankId, editingCashback, banks) {
        val targetBankId = editingCashback?.bankId ?: preselectedBankId
        val foundIdx = banks.indexOfFirst { it.id == targetBankId }
        mutableIntStateOf(if (foundIdx != -1) foundIdx else 0)
    }
    var customCategoryName by remember(editingCashback) {
        mutableStateOf(if (editingCashback != null && presets.none { it.name == editingCashback.category }) editingCashback.category else "")
    }
    var selectedPresetIndex by remember(editingCashback, presets) {
        mutableIntStateOf(
            if (editingCashback == null) 0
            else {
                val matchedPresetIdx = presets.indexOfFirst { it.name == editingCashback.category }
                if (matchedPresetIdx != -1) matchedPresetIdx + 1 else 0
            }
        )
    }
    var percentageString by remember(editingCashback) {
        mutableStateOf(
            if (editingCashback != null) {
                val pct = editingCashback.percentage
                if (pct % 1.0 == 0.0) pct.toInt().toString() else pct.toString()
            } else "5"
        )
    }

    val localFocusManager = LocalFocusManager.current

    // Dialog layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    localFocusManager.clearFocus()
                    onDismiss()
                }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Intercept clicks inside card
                ),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .size(40.dp, 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (editingCashback != null) "Редактировать кешбэк" else "Добавить процент кешбэка",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (banks.isEmpty()) {
                    // Empty banks helper interface action trigger
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("⚠️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Сначала добавьте хотя бы один банк, чтобы внести для него кешбэк.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddBankClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Добавить Банк", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Core Content Section - Scrollable to ensure low-height screens handle perfectly
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight = 1f, fill = false)
                            .verticalScroll(scrollState)
                    ) {
                        // 1. Bank selection Segment
                        Text(
                            "ВЫБЕРИТЕ БАНК:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val bankSelectionScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Using standard horizontal scroll for zero lag load on sheet transitions
                            Row(
                                modifier = Modifier.horizontalScroll(bankSelectionScrollState),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                banks.forEachIndexed { idx, bank ->
                                    val isSelected = selectedBankIndex == idx
                                    val color = parseColorHex(bank.colorHex)
                                    
                                    val lightText = Color.White
                                    val darkText = Color(0xFF1E293B)
                                    val textColor = if (bank.colorHex.lowercase() == "#ffdd2d" || bank.colorHex.lowercase() == "#fcd91f") darkText else lightText

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) color else color.copy(alpha = 0.15f))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else color.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { selectedBankIndex = idx }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(bank.iconEmoji, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bank.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) textColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.Done,
                                                    contentDescription = null,
                                                    tint = textColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Preset category selector grid
                        Text(
                            "КАТЕГОРИЯ ТРАТ:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Visual grid render
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val itemsToRender = listOf(CategoryPreset("Своя", "edit")) + presets
                            
                            itemsToRender.chunked(3).forEachIndexed { rowIndex, rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEachIndexed { colIndex, item ->
                                        val index = rowIndex * 3 + colIndex
                                        val isSelected = selectedPresetIndex == index
                                        val emoji = if (item.iconName == "edit") "✍️" else getEmojiForCategory(item.name, item.iconName)
                                        val displayName = when (item.name) {
                                            "Красота и Спа" -> "Красота"
                                            "Дом и Ремонт" -> "Ремонт"
                                            "Все покупки" -> "Все трат."
                                            "Рестораны и Кафе" -> "Кафе"
                                            "Такси и Транспорт" -> "Такси"
                                            "Аптеки и Здоровье" -> "Аптеки"
                                            "Одежда и Обувь" -> "Одежда"
                                            "Топливо и АЗС" -> "Топливо"
                                            "Супермаркеты" -> "Продукты"
                                            "Развлечения" -> "Развлеч."
                                            else -> item.name
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(16.dp)
                                                )
                                                .clickable { 
                                                    selectedPresetIndex = index
                                                    if (index != 0) {
                                                        customCategoryName = "" // clear custom field
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(emoji, fontSize = 20.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = displayName,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    // Pad incomplete rows to keep grid structure aligned
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) {
                                            Box(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // If select "✍️ Custom Category", show descriptive clean text input
                        if (selectedPresetIndex == 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = customCategoryName,
                                onValueChange = { customCategoryName = it },
                                placeholder = { Text("Название вашей категории (например, Авиабилеты)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Luxurious Stepper Card with focusable input for Percentage returns
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "РАЗМЕР КЕШБЭКА (%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            letterSpacing = 1.2.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = percentageString,
                                        onValueChange = { percent ->
                                            if (percent.isEmpty() || percent.toDoubleOrNull() != null || percent.endsWith(".")) {
                                                if (percent.length <= 5) {
                                                    percentageString = percent
                                                }
                                            }
                                        },
                                        placeholder = { Text("5") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_cashback_percentage"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Interactive Steppers Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Minus button
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .shadow(1.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .clickable {
                                                val doubleVal = percentageString.toDoubleOrNull() ?: 0.0
                                                if (doubleVal > 0.5) {
                                                    val newVal = doubleVal - if (doubleVal % 1.0 == 0.5) 0.5 else 1.0
                                                    percentageString = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else newVal.toString()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }

                                    // Plus button
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .shadow(1.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .clickable {
                                                val doubleVal = percentageString.toDoubleOrNull() ?: 0.0
                                                val newVal = doubleVal + 1.0
                                                percentageString = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else newVal.toString()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Controls
                    val finalCategoryName = if (selectedPresetIndex == 0) customCategoryName else presets[selectedPresetIndex - 1].name
                    val selectedPreset = if (selectedPresetIndex > 0) presets[selectedPresetIndex - 1] else null
                    val finalIconName = selectedPreset?.iconName ?: "shopping_bag"

                    val isCategoryAlreadyExists = selectedBankIndex in banks.indices && finalCategoryName.trim().isNotEmpty() && cashbacks.any { cb ->
                        cb.bankId == banks[selectedBankIndex].id && 
                        cb.category.trim().lowercase() == finalCategoryName.trim().lowercase() &&
                        cb.id != (editingCashback?.id ?: -1L)
                    }

                    if (isCategoryAlreadyExists) {
                        Text(
                            text = "⚠️ Эта категория уже добавлена для выбранного банка",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    val isFormValid = finalCategoryName.isNotBlank() && (percentageString.toDoubleOrNull() ?: 0.0) > 0.0 && !isCategoryAlreadyExists

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Отмена", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (isFormValid) {
                                    val currentBank = banks[selectedBankIndex]
                                    val valPct = percentageString.toDoubleOrNull() ?: 0.0
                                    onSave(currentBank.id, finalCategoryName.trim(), valPct, finalIconName)
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("submit_cashback_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Сохранить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

