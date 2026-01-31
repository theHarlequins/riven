package ua.riven.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ua.riven.app.db.Wallet
import ua.riven.app.ui.components.ExpandableFab
import ua.riven.app.ui.components.NeumorphicPieChart
import ua.riven.app.ui.theme.GradientCard
import ua.riven.app.ui.theme.RivenColors
import ua.riven.app.ui.transactions.TransactionScreen
import ua.riven.app.ui.exchange.ExchangeScreen
import ua.riven.app.ui.transactions.TransactionType
import kotlin.math.absoluteValue

class DashboardScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<DashboardViewModel>()
        val state by viewModel.state.collectAsState()
        val haptic = LocalHapticFeedback.current

        Scaffold(
            containerColor = RivenColors.Background,
            topBar = {
                FintechHeader(
                    onAvatarClick = { navigator.push(MoreScreen()) }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Massive Net Worth Section
                NetWorthSection(
                    amount = state.totalNetWorth,
                    usdRate = state.currentUsdRate,
                    runway = state.runwayMonths,
                    wallets = state.wallets,
                    onBurnRateClick = { viewModel.onBurnRateClicked() },
                    onInjectDemo = { viewModel.onInjectDemoData() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 2. Wallets Carousel (Monobank Style) OR Empty State
                if (state.wallets.isNotEmpty()) {
                    WalletsCarousel(
                        wallets = state.wallets,
                        onWalletClick = { id -> 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigator.push(WalletDetailsScreen(id)) 
                        }
                    )
                } else {
                    EmptyDashboardState(
                        onAddFirstWallet = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onAddWalletClicked() 
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. Action Grid
                ActionGrid(
                    onTopUp = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onAddWalletClicked() 
                    },
                    onTransfer = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.push(TransactionScreen(initialType = TransactionType.TRANSFER)) 
                    },
                    onExchange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.push(ExchangeScreen()) 
                    },
                    onMore = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.push(MoreScreen()) 
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Recent Transactions (Simplified Bottom List)
                RecentTransactionsList(
                    transactions = state.recentTransactions,
                    wallets = state.wallets,
                    modifier = Modifier.weight(1f),
                    onAddFirstTransaction = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.push(TransactionScreen())
                    }
                )
            }

            // Keep the Super FAB as the ultimate action point
            ExpandableFab(
                onTransactionClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navigator.push(TransactionScreen()) 
                },
                onWalletClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onAddWalletClicked() 
                },
                onEnvelopeClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navigator.push(TransactionScreen(initialType = TransactionType.EXPENSE)) 
                }
            )

            if (state.isAddWalletSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.onAddWalletDismissed() },
                    containerColor = RivenColors.Surface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = RivenColors.TextSecondary) }
                ) {
                    AddWalletSheet(
                        onSave = { name, curr, bal, color -> 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onSaveWallet(name, curr, bal, color) 
                        },
                        onDismiss = { viewModel.onAddWalletDismissed() }
                    )
                }
            }

            if (state.isAddEnvelopeSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.onAddEnvelopeDismissed() },
                    containerColor = RivenColors.Surface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = RivenColors.TextSecondary) }
                ) {
                    AddEnvelopeSheet(
                        onSave = { name, target -> 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onSaveEnvelope(name, target) 
                        },
                        onDismiss = { viewModel.onAddEnvelopeDismissed() }
                    )
                }
            }

            if (state.isBurnRateDialogVisible) {
                BurnRateDialog(
                    currentRate = state.monthlyBurnRate,
                    onDismiss = { viewModel.onBurnRateDismissed() },
                    onSave = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onSaveBurnRate(it) 
                    }
                )
            }
        }
    }

    /**
     * Empty Dashboard State - Phase 1 Fix
     * Rationale: Zero-data states should provide a clear path forward with 
     * domain-relevant iconography and a high-visibility CTA.
     */
    @Composable
    private fun EmptyDashboardState(
        onAddFirstWallet: () -> Unit
    ) {
        val haptic = LocalHapticFeedback.current
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(RivenColors.Surface)
                .padding(vertical = 48.dp, horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Vault Icon
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(RivenColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = RivenColors.Accent,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Your vault is empty",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add your first wallet to start tracking your finances",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RivenColors.TextSecondary
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // High-visibility CTA Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddFirstWallet()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RivenColors.Accent
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddCard,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add First Wallet",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }

    @Composable
    private fun BurnRateDialog(
        currentRate: Double,
        onDismiss: () -> Unit,
        onSave: (Double) -> Unit
    ) {
        var rate by remember { mutableStateOf(currentRate.toString()) }
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = RivenColors.Surface,
            title = { Text("MONTHLY BURN RATE", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This is used to calculate your financial runway.", color = RivenColors.TextSecondary, fontSize = 12.sp)
                    ua.riven.app.ui.components.NeumorphicInput(
                        value = rate,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) rate = it },
                        placeholder = "Amount (₴)"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSave(rate.toDoubleOrNull() ?: currentRate) }) {
                    Text("SAVE", color = RivenColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = RivenColors.TextSecondary)
                }
            }
        )
    }

    @Composable
    private fun FintechHeader(onAvatarClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RivenColors.Surface)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = RivenColors.TextSecondary, modifier = Modifier.size(20.dp))
            }

            // Logo
            Text(
                text = "RIVΞN",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    color = Color.White
                )
            )

            // Notifications
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RivenColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    private fun NetWorthSection(
        amount: Double,
        usdRate: Double,
        runway: Double,
        wallets: List<Wallet>,
        onBurnRateClick: () -> Unit,
        onInjectDemo: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onInjectDemo
                )
        ) {
            NeumorphicPieChart(
                wallets = wallets,
                totalNetWorth = amount,
                usdRate = usdRate,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "₴ ${formatCurrency(amount)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            val approxUsd = if (usdRate > 0) amount / usdRate else 0.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "≈ $ ${formatCurrency(approxUsd)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = RivenColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
                
                if (runway > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        color = if (runway < 3) RivenColors.Red.copy(alpha = 0.2f) else RivenColors.Accent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onBurnRateClick() }
                    ) {
                        Text(
                            text = "${runway.toInt()} MONS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (runway < 3) RivenColors.Red else RivenColors.Accent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun WalletsCarousel(
        wallets: List<Wallet>,
        onWalletClick: (Long) -> Unit
    ) {
        val pagerState = rememberPagerState(pageCount = { wallets.size })

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val wallet = wallets[page]
            
            // Animation for scaling
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue

            CardItem(
                wallet = wallet,
                modifier = Modifier
                    .graphicsLayer {
                        val scale = lerp(0.9f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                    }
                    .clickable { onWalletClick(wallet.id) }
            )
        }
    }

    @Composable
    private fun CardItem(wallet: Wallet, modifier: Modifier = Modifier) {
        val gradient = when(wallet.currency) {
            "USD" -> Brush.linearGradient(listOf(Color(0xFF2E3192), Color(0xFF1BFFFF)))
            "BTC" -> Brush.linearGradient(listOf(Color(0xFFF7931A), Color(0xFFFFAB40)))
            else -> Brush.linearGradient(listOf(RivenColors.GradientStart, RivenColors.GradientEnd))
        }

        GradientCard(
            gradient = gradient,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Name Top-Left
                Text(
                    text = wallet.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Balance Middle-Left
                val symbol = when(wallet.currency) {
                    "UAH" -> "₴"
                    "USD" -> "$"
                    "EUR" -> "€"
                    "BTC" -> "₿"
                    else -> wallet.currency
                }
                Text(
                    text = "$symbol ${formatCurrency(wallet.balance)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // Type Bottom-Right
                Icon(
                    imageVector = when(wallet.type) {
                        "Cash" -> Icons.Default.Money
                        "Crypto" -> Icons.Default.CurrencyBitcoin
                        else -> Icons.Default.CreditCard
                    },
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }

    @Composable
    private fun ActionGrid(
        onTopUp: () -> Unit,
        onTransfer: () -> Unit,
        onExchange: () -> Unit,
        onMore: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ActionCircle(label = "Top Up", icon = Icons.Default.Add, color = Color(0xFF32D74B), onClick = onTopUp)
            ActionCircle(label = "Transfer", icon = Icons.Default.SwapHoriz, color = Color(0xFF0A84FF), onClick = onTransfer)
            ActionCircle(label = "Exchange", icon = Icons.Default.CurrencyExchange, color = Color(0xFFFFD60A), onClick = onExchange)
            ActionCircle(label = "More", icon = Icons.Default.MoreHoriz, color = Color(0xFF8E8E93), onClick = onMore)
        }
    }

    @Composable
    private fun ActionCircle(
        label: String,
        icon: ImageVector,
        color: Color,
        onClick: () -> Unit
    ) {
        val haptic = LocalHapticFeedback.current
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(RivenColors.Surface)
                    .clickable { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = RivenColors.TextSecondary))
        }
    }

    @Composable
    private fun RecentTransactionsList(
        transactions: List<ua.riven.app.db.TransactionRecord>,
        wallets: List<Wallet>,
        modifier: Modifier = Modifier,
        onAddFirstTransaction: () -> Unit = {}
    ) {
        val haptic = LocalHapticFeedback.current
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(RivenColors.Surface)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                // Enhanced Empty State for Transactions
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = RivenColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No transactions yet",
                        color = RivenColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddFirstTransaction()
                        }
                    ) {
                        Text(
                            "Add your first transaction",
                            color = RivenColors.Accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(transactions.size) { index ->
                        val tx = transactions[index]
                        val wallet = wallets.find { it.id == tx.walletId }
                        TransactionRow(tx, wallet)
                    }
                }
            }
        }
    }

    @Composable
    private fun TransactionRow(tx: ua.riven.app.db.TransactionRecord, wallet: Wallet?) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RivenColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (tx.amount > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = if (tx.amount > 0) RivenColors.Green else RivenColors.Red,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = tx.category, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    val date = Instant.fromEpochMilliseconds(tx.date).toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        text = "${date.dayOfMonth} ${date.month.name.take(3)} • ${wallet?.name ?: "Unknown"}", 
                        style = MaterialTheme.typography.labelSmall.copy(color = RivenColors.TextSecondary)
                    )
                }
            }

            Text(
                text = "${if (tx.amount > 0) "+" else ""} ₴${formatDecimal(tx.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (tx.amount > 0) RivenColors.Green else RivenColors.Red,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }

    private fun formatCurrency(value: Double): String = formatDecimal(value)

    private fun formatDecimal(value: Double): String {
        return ((value * 100).toInt() / 100.0).toString()
    }
}
