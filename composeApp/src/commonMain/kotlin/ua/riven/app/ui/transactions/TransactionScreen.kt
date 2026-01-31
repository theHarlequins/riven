package ua.riven.app.ui.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.core.parameter.parametersOf
import ua.riven.app.ui.theme.BigActionButton
import ua.riven.app.ui.theme.RivenColors

data class TransactionScreen(
    val walletId: Long? = null,
    val initialType: TransactionType? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<TransactionViewModel> { parametersOf(walletId, initialType) }
        val state by viewModel.state.collectAsState()
        val haptic = LocalHapticFeedback.current

        // Collect one-time effects
        LaunchedEffect(Unit) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is TransactionEffect.NavigateBack -> navigator.pop()
                    is TransactionEffect.ShowError -> {
                        // Could show a Snackbar here
                    }
                }
            }
        }

        Scaffold(
            containerColor = RivenColors.Background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        TransactionTypeToggle(
                            selectedType = state.type,
                            onTypeSelected = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onTypeChange(it) 
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                navigator.pop() 
                            },
                            modifier = Modifier.size(48.dp) // Material minimum touch target
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = RivenColors.Background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Massive Amount Section (Top 40%)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₴",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = RivenColors.TextSecondary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
                            )
                            Text(
                                text = state.amountStr,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Transaction Flow Row
                        var showWalletSheet by remember { mutableStateOf(false) }
                        var showTargetSheet by remember { mutableStateOf(false) }

                        FlowSelector(
                            source = state.selectedWallet?.name ?: "Select Wallet",
                            target = if (state.type == TransactionType.TRANSFER) 
                                (state.selectedToWallet?.name ?: "Select Target") 
                                else (state.selectedEnvelope?.name ?: "General"),
                            onClickSource = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showWalletSheet = true 
                            },
                            onClickTarget = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showTargetSheet = true 
                            }
                        )

                        if (showWalletSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showWalletSheet = false },
                                containerColor = RivenColors.Surface,
                                dragHandle = { BottomSheetDefaults.DragHandle(color = RivenColors.TextSecondary) }
                            ) {
                                SelectionList(
                                    title = "SELECT WALLET",
                                    items = state.wallets,
                                    selectedItem = state.selectedWallet,
                                    itemContent = { wallet ->
                                        Text(wallet.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${wallet.currency} ${formatDecimal(wallet.balance)}", color = RivenColors.TextSecondary, fontSize = 12.sp)
                                    },
                                    onSelect = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.onWalletSelect(it)
                                        showWalletSheet = false
                                    }
                                )
                            }
                        }

                        if (showTargetSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showTargetSheet = false },
                                containerColor = RivenColors.Surface,
                                dragHandle = { BottomSheetDefaults.DragHandle(color = RivenColors.TextSecondary) }
                            ) {
                                if (state.type == TransactionType.TRANSFER) {
                                    SelectionList(
                                        title = "SELECT DESTINATION WALLET",
                                        items = state.wallets.filter { it.id != state.selectedWallet?.id },
                                        selectedItem = state.selectedToWallet,
                                        itemContent = { wallet ->
                                            Text(wallet.name, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("${wallet.currency} ${formatDecimal(wallet.balance)}", color = RivenColors.TextSecondary, fontSize = 12.sp)
                                        },
                                        onSelect = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onToWalletSelect(it)
                                            showTargetSheet = false
                                        }
                                    )
                                } else {
                                    SelectionList(
                                        title = "SELECT ENVELOPE",
                                        items = state.envelopes,
                                        selectedItem = state.selectedEnvelope,
                                        itemContent = { envelope ->
                                            Text("${envelope.icon} ${envelope.name}", color = Color.White, fontWeight = FontWeight.Bold)
                                            val progress = (envelope.currentSpent / envelope.monthlyLimit * 100).toInt()
                                            Text("$progress% of ₴${formatDecimal(envelope.monthlyLimit)} limit", color = RivenColors.TextSecondary, fontSize = 12.sp)
                                        },
                                        onSelect = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onEnvelopeSelect(it)
                                            showTargetSheet = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Custom Keypad with enhanced touch targets
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf(".", "0", "BKSP")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    text = if (key == "BKSP") "" else key,
                                    icon = if (key == "BKSP") Icons.AutoMirrored.Filled.Backspace else null,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (key == "BKSP") viewModel.onBackspace() 
                                        else viewModel.onDigitPress(key)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Confirm Button - Sticky at bottom in Thumb Zone
                    ConfirmButton(
                        type = state.type,
                        amount = state.amountStr,
                        isEnabled = (state.amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onSave()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun TransactionTypeToggle(
        selectedType: TransactionType,
        onTypeSelected: (TransactionType) -> Unit
    ) {
        val haptic = LocalHapticFeedback.current
        
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(RivenColors.Surface)
                .padding(4.dp)
        ) {
            TransactionType.values().forEach { type ->
                val isSelected = type == selectedType
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) RivenColors.Accent else Color.Transparent)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTypeSelected(type) 
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.name.take(1) + type.name.drop(1).lowercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else RivenColors.TextSecondary
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun FlowSelector(
        source: String,
        target: String,
        onClickSource: () -> Unit,
        onClickTarget: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(RivenColors.Surface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClickSource() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(source, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RivenColors.TextSecondary, modifier = Modifier.size(16.dp))
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClickTarget() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(target, color = RivenColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    @Composable
    private fun <T> SelectionList(
        title: String,
        items: List<T>,
        selectedItem: T?,
        itemContent: @Composable (T) -> Unit,
        onSelect: (T) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = RivenColors.TextSecondary,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (items.isEmpty()) {
                // Empty state for selection lists
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No items available",
                        color = RivenColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items.size) { index ->
                        val item = items[index]
                        val isSelected = item == selectedItem
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) RivenColors.SurfaceVariant else Color.Transparent)
                                .clickable { onSelect(item) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                itemContent(item)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = RivenColors.Accent)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    /**
     * Keypad Button - Phase 2 Fix
     * Rationale: Minimum 60dp touch target for accessibility compliance
     * with visual feedback for press states.
     */
    @Composable
    private fun KeypadButton(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val haptic = LocalHapticFeedback.current

        Box(
            modifier = modifier
                .heightIn(min = 64.dp) // Minimum 60dp touch target + padding
                .aspectRatio(1.5f)
                .clip(CircleShape)
                .background(if (isPressed) RivenColors.SurfaceVariant else RivenColors.Surface)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                )
            }
        }
    }

    /**
     * Confirm Button - Phase 2 Fix
     * Rationale: Save button must be in Thumb Zone (bottom of screen)
     * with high visibility and haptic confirmation on success.
     */
    @Composable
    private fun ConfirmButton(
        type: TransactionType,
        amount: String,
        isEnabled: Boolean,
        onClick: () -> Unit
    ) {
        val haptic = LocalHapticFeedback.current
        
        val label = when(type) {
            TransactionType.EXPENSE -> "Spend ₴$amount"
            TransactionType.INCOME -> "Add ₴$amount"
            TransactionType.TRANSFER -> "Transfer ₴$amount"
        }
        
        val color = when(type) {
            TransactionType.EXPENSE -> RivenColors.Red
            TransactionType.INCOME -> RivenColors.Green
            TransactionType.TRANSFER -> RivenColors.Accent
        }

        BigActionButton(
            text = label.uppercase(),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp) // Thumb Zone compliant
                .graphicsLayer { alpha = if (isEnabled) 1f else 0.4f }
                .then(if (isEnabled) Modifier else Modifier.clickable(enabled = false) {}),
            backgroundColor = color
        )
    }

    private fun formatDecimal(value: Double): String {
        return ((value * 100).toInt() / 100.0).toString()
    }
}
