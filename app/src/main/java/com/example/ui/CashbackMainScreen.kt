package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import com.example.data.MccDirectory
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.example.data.Bank
import com.example.data.CashbackWithBank
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke

data class PopularBankPreset(val name: String, val colorHex: String, val emoji: String)


// Cache for colors and emojis to avoid allocations and lag during scroll
private val colorCache = HashMap<String, Color>()
fun parseColorHex(hex: String, default: Color = Color(0xFF673AB7)): Color {
    return colorCache.getOrPut(hex) {
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            default
        }
    }
}

private val emojiCache = HashMap<String, String>()
fun getEmojiForCategory(category: String, presetIcon: String = ""): String {
    val cacheKey = "$category|$presetIcon"
    return emojiCache.getOrPut(cacheKey) {
        val norm = category.lowercase().trim()
        when {
            norm.contains("супермаркет") || norm.contains("магазин") || norm.contains("продукт") || presetIcon == "shopping_cart" -> "🛒"
            norm.contains("автоуслуг") || norm.contains("автосервис") || norm.contains("мойк") || norm.contains("шин") || norm.contains("сто ") || presetIcon == "directions_car" -> "🚗"
            norm.contains("такси") || norm.contains("транспорт") || norm.contains("метро") || presetIcon == "local_taxi" -> "🚕"
            norm.contains("ресторан") || norm.contains("кафе") || norm.contains("еда") || norm.contains("бургер") || presetIcon == "restaurant" -> "🍔"
            norm.contains("аптек") || norm.contains("здоров") || norm.contains("лекарств") || presetIcon == "local_pharmacy" -> "💊"
            norm.contains("одежд") || norm.contains("обувь") || norm.contains("плать") || presetIcon == "checkroom" -> "👕"
            norm.contains("электроник") || norm.contains("техник") || norm.contains("телефон") || presetIcon == "devices" -> "💻"
            norm.contains("топлив") || norm.contains("бензин") || norm.contains("азс") || presetIcon == "local_gas_station" -> "⛽️"
            norm.contains("развлечен") || norm.contains("театр") || (norm.contains("билет") && !norm.contains("авиа")) || presetIcon == "theater_comedy" -> "🎟"
            norm.contains("кино") || norm.contains("музе") || norm.contains("искусств") || presetIcon == "movie" -> "🎬"
            norm.contains("красот") || norm.contains("салон") || norm.contains("спа") || norm.contains("косметик") || presetIcon == "spa" -> "💅"
            norm.contains("дом") || norm.contains("ремонт") || norm.contains("мебель") || presetIcon == "construction" -> "🔨"
            norm.contains("книг") || norm.contains("хобби") || presetIcon == "menu_book" -> "📚"
            norm.contains("авиа") || norm.contains("самолет") || norm.contains("путешеств") || presetIcon == "flight" -> "✈️"
            norm.contains("спорт") || norm.contains("фитне") || presetIcon == "sports_soccer" -> "⚽️"
            norm.contains("зоо") || norm.contains("животн") || norm.contains("корм") || presetIcon == "pets" -> "🐾"
            norm.contains("цвет") || norm.contains("флорист") || presetIcon == "redeem" -> "💐"
            norm.contains("все покупки") || norm.contains("любые") || presetIcon == "percent" -> "💳"
            else -> "💰"
        }
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

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val activeTab = if (selectedTabIndex == 0) "best_deals" else "my_cards"
    var showAddBankDialog by remember { mutableStateOf(false) }
    var showAddCashbackDialog by remember { mutableStateOf(false) }
    var activeMccCashback by remember { mutableStateOf<CashbackWithBank?>(null) }
    var preselectedBankId by remember { mutableStateOf<Long?>(null) }
    var editingCashback by remember { mutableStateOf<CashbackWithBank?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("cashback_prefs", android.content.Context.MODE_PRIVATE) }
    var tutorialStep by remember { mutableIntStateOf(prefs.getInt("tutorial_step_key", 1)) }
    
    val focusManager = LocalFocusManager.current

    // Back button handler to close any active overlay dialog or MCC sheet
    if (showAddBankDialog || showAddCashbackDialog || activeMccCashback != null) {
        BackHandler {
            if (activeMccCashback != null) {
                activeMccCashback = null
            } else if (showAddBankDialog) {
                showAddBankDialog = false
            } else if (showAddCashbackDialog) {
                showAddCashbackDialog = false
                editingCashback = null
            }
        }
    }

    // Pulsing scale for highlighting the FAB only during onboarding (idle when tutorial is complete to save CPU/battery)
    val shouldPulseTutorial = tutorialStep > 0

    val fabScaleState = if (shouldPulseTutorial && banks.isNotEmpty() && cashbacks.isEmpty()) {
        val infiniteTransition = rememberInfiniteTransition(label = "onboarding_pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fab_pulse_scale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            // Hide FAB when overlays are shown to prevent overlapping "Save"/"Create" buttons!
            if (!showAddBankDialog && !showAddCashbackDialog) {
                if (activeTab == "my_cards" && banks.isNotEmpty() || (activeTab == "best_deals" && banks.isNotEmpty())) {
                    val fabHighlight = banks.isNotEmpty() && cashbacks.isEmpty() && tutorialStep > 0
                    val fabBorderModifier = if (fabHighlight) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                    } else {
                        Modifier
                    }

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
                            .graphicsLayer {
                                val s = fabScaleState.value
                                scaleX = s
                                scaleY = s
                            }
                            .then(fabBorderModifier)
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
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Title Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Кешбэк Помощник",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Сравнивай проценты и выбирай лучший банк",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Quick Stat Counter Badge with unbreakable horizontal alignment
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${banks.size}",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text("🏦", fontSize = 11.sp)
                            Text(
                                "•",
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${cashbacks.size}",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text("🏷️", fontSize = 11.sp)
                        }
                    }
                }

                // Highlight triggers based on selected tab state
                val findProfitableHighlight = banks.isNotEmpty() && cashbacks.isNotEmpty() && activeTab == "my_cards" && tutorialStep > 0
                val myCardsHighlight = banks.isEmpty() && activeTab == "best_deals" && tutorialStep > 0
                val isMyCardsTutorialHighlight = banks.isEmpty() && activeTab == "my_cards" && tutorialStep > 0

                // Back gesture handler on "Банки" page to smoothly scroll back to "Категории"
                BackHandler(enabled = selectedTabIndex == 1 && !showAddBankDialog && !showAddCashbackDialog && activeMccCashback == null) {
                    selectedTabIndex = 0
                }

                // Custom Tab Segmented Control with isolated glowing borders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Categories tab (formerly Где выгоднее)
                    GlowingTabItem(
                        title = "Категории",
                        icon = Icons.Default.Star,
                        isSelected = selectedTabIndex == 0,
                        isHighlighted = findProfitableHighlight,
                        testTag = "tab_best_deals",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTabIndex = 0
                            if (banks.isNotEmpty() && cashbacks.isNotEmpty() && tutorialStep > 0) {
                                tutorialStep = 0
                                prefs.edit().putInt("tutorial_step_key", 0).apply()
                                Toast.makeText(context, "Обучение завершено! 🎉 Удачи в поиске максимального кешбэка!", Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Banks tab (formerly Мои Банки)
                    GlowingTabItem(
                        title = "Банки",
                        icon = Icons.Default.Home,
                        isSelected = selectedTabIndex == 1,
                        isHighlighted = myCardsHighlight,
                        testTag = "tab_my_cards",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedTabIndex = 1
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LaunchedEffect(selectedTabIndex) {
                    if (selectedTabIndex == 0 && banks.isNotEmpty() && cashbacks.isNotEmpty() && tutorialStep > 0) {
                        tutorialStep = 0
                        prefs.edit().putInt("tutorial_step_key", 0).apply()
                        Toast.makeText(context, "Обучение завершено! 🎉 Удачи в поиске максимального кешбэка!", Toast.LENGTH_LONG).show()
                    }
                }

                // High-performance AnimatedContent tab container with smooth horizontal swipe navigation
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(selectedTabIndex) {
                            var dragDistance = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragDistance = 0f },
                                onDragEnd = {
                                    val swipeThreshold = 45.dp.toPx()
                                    if (dragDistance < -swipeThreshold && selectedTabIndex == 0) {
                                        selectedTabIndex = 1
                                    } else if (dragDistance > swipeThreshold && selectedTabIndex == 1) {
                                        selectedTabIndex = 0
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragDistance += dragAmount
                                }
                            )
                        },
                    label = "tab_animated_content"
                ) { page ->
                    when (page) {
                        0 -> {
                            BestDealsTabContent(
                                viewModel = viewModel,
                                filteredCashbacks = filteredCashbacks,
                                allCashbacks = cashbacks,
                                searchQuery = searchQuery,
                                banksAvailable = banks.isNotEmpty(),
                                onSelectCategoryPreset = { preset ->
                                    viewModel.searchQuery.value = preset
                                },
                                onAddBankClick = { showAddBankDialog = true },
                                onCashbackClick = { cb -> activeMccCashback = cb }
                            )
                        }
                        1 -> {
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
                                onMccClick = { cb -> activeMccCashback = cb },
                                isTutorialHighlight = isMyCardsTutorialHighlight,
                                isTutorialActive = tutorialStep > 0
                            )
                        }
                    }
                }
            }

            // 💡 Interactive Onboarding Tutorial Assistant Box
            if (tutorialStep > 0) {
                // Determine text description dynamically based on actual selected app state
                val tutorialText = when {
                    banks.isEmpty() && activeTab == "best_deals" -> {
                        "👋 Добро пожаловать в Кешбэк Помощник! Давайте научимся пользоваться приложением.\n\n👉 Шаг 1 из 3: Перейдите во вкладку 'Банки' (вверху), чтобы добавить ваш первый банк."
                    }
                    banks.isEmpty() && activeTab == "my_cards" -> {
                        "🎯 Шаг 1 из 3: Отлично! Теперь нажмите кнопку 'Новый Банк' вверху экрана, чтобы добавить ваш банк."
                    }
                    banks.isNotEmpty() && cashbacks.isEmpty() -> {
                        "💸 Шаг 2 из 3: Супер, ваш первый банк успешно добавлен! Теперь внесите процент кешбэка по любой категории. Нажмите круглую зелёную кнопку 'Внести кешбэк' внизу справа."
                    }
                    banks.isNotEmpty() && cashbacks.isNotEmpty() -> {
                        "🎉 Шаг 3 из 3: Всё настроено! Теперь откройте вкладку 'Категории' (сверху). Начните вводить в поиск категорию (например, такси) или кликните готовые чипсы — мы автоматически выберем банк с максимальным возвратом!"
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
                    existingBanks = banks,
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

            // MCC Information Modal Dialog
            if (activeMccCashback != null) {
                MccInfoDialog(
                    cashback = activeMccCashback,
                    onDismiss = { activeMccCashback = null },
                    onEditCashback = { cb ->
                        activeMccCashback = null
                        editingCashback = cb
                        showAddCashbackDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun GlowingTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isHighlighted: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val glowAlpha = if (isHighlighted) {
        val transition = rememberInfiniteTransition(label = "tab_glow")
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "tab_glow_alpha"
        ).value
    } else {
        0f
    }

    val activeTabColor = MaterialTheme.colorScheme.surface
    val activeTextColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isHighlighted) {
                    Modifier
                        .border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                } else Modifier
            )
            .background(if (isSelected) activeTabColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) activeTextColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddBankButton(
    onClick: () -> Unit,
    isTutorialHighlight: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = if (isTutorialHighlight) {
        val transition = rememberInfiniteTransition(label = "new_bank_zoom")
        transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "new_bank_scale"
        ).value
    } else {
        1.0f
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = false
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.85f, 0.5f)
            }
            .height(36.dp)
            .testTag("add_bank_button"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isTutorialHighlight) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isTutorialHighlight) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = if (isTutorialHighlight) ButtonDefaults.buttonElevation(defaultElevation = 4.dp) else ButtonDefaults.buttonElevation()
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Новый Банк", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedCategoryChip(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chip_scale"
    )

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        interactionSource = interactionSource,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    title,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                )
            }
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            borderWidth = if (isSelected) 1.5.dp else 1.dp
        ),
        modifier = modifier
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
fun BestDealsTabContent(
    viewModel: CashbackViewModel,
    filteredCashbacks: List<CashbackWithBank>,
    allCashbacks: List<CashbackWithBank>,
    searchQuery: String,
    banksAvailable: Boolean,
    onSelectCategoryPreset: (String) -> Unit,
    onAddBankClick: () -> Unit,
    onCashbackClick: (CashbackWithBank) -> Unit = {}
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

    val availableCategories: List<Pair<String, String>> = remember(allCashbacks, searchQuery) {
        val uniqueCategories = if (allCashbacks.isNotEmpty()) {
            allCashbacks.map { it.category }.distinct()
        } else {
            viewModel.presetCategories.map { it.name }
        }
        val fullList = mutableListOf(Pair("Все", "🌟"))
        uniqueCategories.forEach { cat ->
            val emoji = getEmojiForCategory(cat)
            fullList.add(Pair(cat, emoji))
        }
        if (searchQuery.isNotBlank()) {
            fullList.filter { (cat, _) ->
                cat == "Все" || cat.contains(searchQuery, ignoreCase = true)
            }
        } else {
            fullList
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_cashback_field"),
            placeholder = { Text("Название категории") },
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

        // Display category filter chips naturally edge-to-edge
        if (availableCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(availableCategories, key = { it.first }) { pair ->
                    val (title, emoji) = pair
                    val isSelected = (title == "Все" && searchQuery.isEmpty()) || 
                                     (title != "Все" && searchQuery.equals(title, ignoreCase = true))

                    AnimatedCategoryChip(
                        title = title,
                        emoji = emoji,
                        isSelected = isSelected,
                        onClick = {
                            onSelectCategoryPreset(if (title == "Все") "" else title)
                        },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(250),
                            fadeOutSpec = tween(250),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    )
                }
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
                        .padding(horizontal = 16.dp)
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddBankClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("add_bank_from_best_deals")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Добавить банк", fontWeight = FontWeight.Bold)
                        }
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
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val noCategoriesAtAll = allCashbacks.isEmpty()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(if (noCategoriesAtAll) "💳" else "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (noCategoriesAtAll) "Нет добавленных категорий кешбэка"
                                   else "Категории не найдены",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (noCategoriesAtAll) "Добавьте категории кешбэка для ваших карт с помощью кнопки '+'"
                                   else "Попробуйте ввести другой поисковый запрос или выбрать другой банк",
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
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 20.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 4.dp)
            ) {
                items(filteredCashbacks, key = { cb -> cb.id }) { cb ->
                    CashbackRankItem(
                        cb = cb,
                        onMccClick = { onCashbackClick(cb) },
                        onDeleteClick = { cashbackToDelete = cb },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(300),
                            fadeOutSpec = tween(300),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CashbackRankItem(
    cb: CashbackWithBank,
    onMccClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bankColor = remember(cb.bankColorHex) { parseColorHex(cb.bankColorHex) }
    val categoryEmoji = remember(cb.category, cb.iconName) { getEmojiForCategory(cb.category, cb.iconName) }
    val percentGradient = remember(bankColor) {
        Brush.horizontalGradient(
            colors = listOf(bankColor, bankColor.copy(alpha = 0.85f))
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onMccClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // MCC indicator on its own dedicated row below the bank name
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "МСС 🔍",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                        .background(percentGradient)
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
    onMccClick: (com.example.data.CashbackWithBank) -> Unit = {},
    isTutorialHighlight: Boolean = false,
    isTutorialActive: Boolean = false
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cashback_prefs", android.content.Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", false)) }

    var bankToDelete by remember { mutableStateOf<Bank?>(null) }
    var cashbackToDeleteInBank by remember { mutableStateOf<com.example.data.CashbackWithBank?>(null) }

    // Dialog for bank deletion confirmation
    if (bankToDelete != null) {
        AlertDialog(
            onDismissRequest = { bankToDelete = null },
            title = { Text("Удалить банк?") },
            text = { Text("Вы действительно хотите удалить банк '${bankToDelete?.name}' и все связанные с ним категории?") },
            confirmButton = {
                Button(
                    onClick = {
                        bankToDelete?.let { viewModel.removeBank(it.id) }
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
                        cashbackToDeleteInBank?.let { viewModel.removeCashback(it.id) }
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 4.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp, end = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ваши банки",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Add Bank Shortcut with restored scale/zoom animation and safe margins
                AddBankButton(
                    onClick = onAddBankClick,
                    isTutorialHighlight = isTutorialHighlight
                )
            }
        }

        if (banks.isEmpty()) {
            item(key = "empty_banks_placeholder") {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(350)),
                    exit = fadeOut(animationSpec = tween(200))
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
                val bankColor = remember(bank.colorHex) { parseColorHex(bank.colorHex) }
                val bankCashbacks = remember(cashbacks, bank.id) { cashbacks.filter { it.bankId == bank.id } }
                val headerGradient = remember(bankColor) {
                    Brush.horizontalGradient(
                        colors = listOf(bankColor, bankColor.copy(alpha = 0.85f))
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
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
                                .background(headerGradient)
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
                                    val categoryEmoji = remember(cb.category, cb.iconName) { getEmojiForCategory(cb.category, cb.iconName) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .clickable { onMccClick(cb) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = categoryEmoji,
                                                fontSize = 20.sp,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = cb.category,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "МСС-коды 🔍",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        ),
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${cb.percentage}%",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = bankColor
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onEditCashbackClick(cb) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit, 
                                                    contentDescription = "Edit", 
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
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

        // Only display Reminders and GitHub cards once the onboarding tutorial has been completed
        if (!isTutorialActive) {
            // Reminders & Notifications Control Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Permission launcher for scheduled monthly reminder
                val monthlyPermissionLauncher = rememberLauncherForActivityResult(
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

                // Separate permission launcher specifically for the Test button — DOES NOT enable monthly notifications!
                val testPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        com.example.notification.CashbackNotificationReceiver.sendImmediateTestNotification(context)
                        Toast.makeText(context, "Тестовое уведомление отправлено!", Toast.LENGTH_SHORT).show()
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
                                            monthlyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (!isGranted) {
                                            Toast.makeText(context, "Разрешите уведомления для проверки теста", Toast.LENGTH_SHORT).show()
                                            testPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@OutlinedButton
                                        }
                                    }
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
}

@Composable
fun AddBankOverlay(
    existingBanks: List<Bank>,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val isNameDuplicate = remember(name, existingBanks) {
        val trimmed = name.trim()
        existingBanks.any { it.name.trim().equals(trimmed, ignoreCase = true) }
    }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var selectedEmojiIndex by remember { mutableIntStateOf(0) }

    val bankColorsList = listOf(
        "#FFDD2D" to "Т-Банк",
        "#EF3124" to "Альфа-Банк",
        "#005BFF" to "Ozon Банк",
        "#21A038" to "СберБанк",
        "#FCD91F" to "Яндекс Банк",
        "#0E58A4" to "ВТБ",
        "#FFE000" to "Райффазен",
        "#1062AC" to "Газпромбанк",
        "#FF4F12" to "Совкомбанк",
        "#E1251B" to "МТС Банк",
        "#8B5CF6" to "Пурпур",
        "#EC4899" to "Розовый",
        "#FF5722" to "Оранж",
        "#111827" to "Графит"
    )

    val bankEmojisList = listOf("🟡", "🔴", "🔵", "🟢", "⭐️", "💎", "💳", "🏦", "🌾", "Ⓜ️", "🟠", "🥚", "🟣", "⚫️", "✅", "🔥")

    val popularPresetBanks = listOf(
        PopularBankPreset("Т-Банк", "#FFDD2D", "🟡"),
        PopularBankPreset("Альфа-Банк", "#EF3124", "🔴"),
        PopularBankPreset("СберБанк", "#21A038", "🟢"),
        PopularBankPreset("Ozon Банк", "#005BFF", "🔵"),
        PopularBankPreset("Яндекс Банк", "#FCD91F", "⭐️"),
        PopularBankPreset("ВТБ", "#0E58A4", "💎"),
        PopularBankPreset("Райффайзен", "#FFE000", "🌾"),
        PopularBankPreset("Газпромбанк", "#1062AC", "Ⓜ️"),
        PopularBankPreset("Совкомбанк", "#FF4F12", "🟠"),
        PopularBankPreset("МТС Банк", "#E1251B", "🥚"),
        PopularBankPreset("Свой вариант", "#8B5CF6", "💳")
    )
    var selectedPresetIndex by remember { mutableIntStateOf(-1) }

    val localFocusManager = LocalFocusManager.current

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffset"
    )

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
                .offset { IntOffset(0, animatedDragOffset.roundToInt().coerceAtLeast(0)) }
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
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header drag-capture area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtLeast(0f)
                                },
                                onDragEnd = {
                                    if (dragOffsetY > 180f) {
                                        onDismiss()
                                    } else {
                                        dragOffsetY = 0f
                                    }
                                },
                                onDragCancel = {
                                    dragOffsetY = 0f
                                }
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Drag handle bar
                    Box(
                        modifier = Modifier
                            .size(46.dp, 6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                    isError = isNameDuplicate,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isNameDuplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isNameDuplicate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )

                if (isNameDuplicate) {
                    Text(
                        text = "Банк с таким названием уже добавлен!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

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
                            if (name.isNotBlank() && !isNameDuplicate) {
                                val (hexColor, _) = bankColorsList[selectedColorIndex]
                                val emoji = bankEmojisList[selectedEmojiIndex]
                                onSave(name.trim(), hexColor, emoji)
                            }
                        },
                        enabled = name.isNotBlank() && !isNameDuplicate,
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
            if (editingCashback == null) 0 // Initially select "Все покупки" (index 0)
            else {
                val matchedPresetIdx = presets.indexOfFirst { it.name == editingCashback.category }
                if (matchedPresetIdx != -1) matchedPresetIdx else presets.size
            }
        )
    }
    var percentageString by remember(editingCashback) {
        mutableStateOf(
            if (editingCashback != null) {
                val pct = editingCashback.percentage
                if (pct % 1.0 == 0.0) pct.toInt().toString() else pct.toString()
            } else "1" // Default is 1% for "Все покупки"
        )
    }

    val localFocusManager = LocalFocusManager.current

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffset"
    )

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
                .offset { IntOffset(0, animatedDragOffset.roundToInt().coerceAtLeast(0)) }
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
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header drag-capture area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtLeast(0f)
                                },
                                onDragEnd = {
                                    if (dragOffsetY > 180f) {
                                        onDismiss()
                                    } else {
                                        dragOffsetY = 0f
                                    }
                                },
                                onDragCancel = {
                                    dragOffsetY = 0f
                                }
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Drag handle bar
                    Box(
                        modifier = Modifier
                            .size(46.dp, 6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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

                        // Visual grid render: presets first, "Своя" at the very bottom
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val itemsToRender = presets + listOf(CategoryPreset("Своя", "edit"))
                            
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
                                            "Дом и Ремонт", "Дом и ремонт" -> "Дом и ремонт"
                                            "Все покупки" -> "Все покупки"
                                            "Рестораны" -> "Рестораны"
                                            "Автоуслуги" -> "Автоуслуги"
                                            "Такси", "Такси и Транспорт" -> "Такси"
                                            "Аптеки", "Аптеки и Здоровье" -> "Аптеки"
                                            "Одежда и Обувь" -> "Одежда"
                                            "Топливо и АЗС" -> "Топливо"
                                            "Супермаркеты" -> "Супермаркеты"
                                            "Развлечения" -> "Развлечения"
                                            "Авиабилеты" -> "Авиабилеты"
                                            "Цветы" -> "Цветы"
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
                                                    if (index < presets.size) {
                                                        customCategoryName = "" // clear custom field
                                                        val selectedPreset = presets[index]
                                                        if (selectedPreset.name == "Все покупки") {
                                                            percentageString = "1"
                                                        } else {
                                                            percentageString = "5"
                                                        }
                                                    } else {
                                                        percentageString = "5"
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(emoji, fontSize = 20.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = displayName,
                                                    fontSize = 10.sp,
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

                        // If select "✍️ Custom Category" (at index presets.size)
                        if (selectedPresetIndex == presets.size) {
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
                    val finalCategoryName = if (selectedPresetIndex == presets.size) customCategoryName else (presets.getOrNull(selectedPresetIndex)?.name ?: "")
                    val selectedPreset = if (selectedPresetIndex < presets.size) presets.getOrNull(selectedPresetIndex) else null
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

@Composable
fun MccInfoDialog(
    cashback: CashbackWithBank? = null,
    categoryName: String? = null,
    onDismiss: () -> Unit,
    onEditCashback: ((CashbackWithBank) -> Unit)? = null
) {
    val catName = cashback?.category ?: categoryName ?: return
    val mccDetails = remember(catName) { MccDirectory.getMccForCategory(catName) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Понятно", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (cashback != null && onEditCashback != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditCashback(cashback)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Изменить %")
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(mccDetails.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MCC: ${mccDetails.categoryName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (cashback != null) {
                        Text(
                            text = "${cashback.bankIconEmoji} ${cashback.bankName} • ${cashback.percentage}% кешбэк",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = mccDetails.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "СПИСОК КОДОВ:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    )

                    // Quick copy all codes
                    TextButton(
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        onClick = {
                            val codesString = mccDetails.codes.joinToString(", ") { it.code }
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("MCC Codes", codesString)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Коды скопированы: $codesString", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("📋 Копировать все", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mccDetails.codes.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = item.code,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (item.examples.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Примеры: ${item.examples}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Helpful tip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Уточнить MCC перед покупкой можно в чеке или деталях операции мобильного банка.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

