package ua.riven.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.parameter.parametersOf
import ua.riven.app.db.TransactionRecord
import ua.riven.app.ui.components.NeumorphicInput
import ua.riven.app.ui.transactions.TransactionScreen
import ua.riven.app.ui.transactions.TransactionType
import ua.riven.app.ui.theme.BigActionButton
import ua.riven.app.ui.theme.GradientCard
import ua.riven.app.ui.theme.RivenColors
import ua.riven.app.ui.theme.RivenCard

data class WalletDetailsScreen(val walletId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<WalletDetailsViewModel> { parametersOf(walletId) }
        val state by viewModel.state.collectAsState()
        val haptic = LocalHapticFeedback.current
        
        var showDeleteDialog by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = RivenColors.Background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(state.wallet?.name ?: "DETAILS", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteDialog = true 
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Wallet", tint = RivenColors.Red)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RivenColors.Background)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Balance Card
                GradientCard(modifier = Modifier.fillMaxWidth()) {
                    val wallet = state.wallet
                    Column {
                        Text(
                            text = "AVAILABLE BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val currency = when (wallet?.currency) {
                            "UAH" -> "₴"
                            "USD" -> "$"
                            "EUR" -> "€"
                            "BTC" -> "₿"
                            else -> wallet?.currency ?: ""
                        }
                        Text(
                            text = "$currency ${formatDecimal(wallet?.balance ?: 0.0)}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    }
                }

                // 2. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BigActionButton(
                        text = "REFILL",
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigator.push(TransactionScreen(walletId, TransactionType.INCOME)) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                    BigActionButton(
                        text = "PAY",
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigator.push(TransactionScreen(walletId, TransactionType.EXPENSE)) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. History
                Text(
                    text = "TRANSACTION HISTORY",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                )

                if (state.transactions.isEmpty()) {
                    // Empty State for Transaction History
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💳",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your wallet history will appear here after making transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RivenColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.transactions) { transaction ->
                            DetailTransactionItem(transaction)
                        }
                    }
                }
            }
            
            if (showDeleteDialog) {
                DeleteConfirmationDialog(
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.onDeleteWallet()
                        showDeleteDialog = false
                        navigator.pop()
                    }
                )
            }
        }
    }

    @Composable
    private fun DeleteConfirmationDialog(
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = RivenColors.Surface,
            title = { Text("DELETE ASSET", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure? This will delete all associated transaction history.", color = RivenColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("DELETE", color = RivenColors.Red, fontWeight = FontWeight.Bold)
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
    private fun DetailTransactionItem(transaction: TransactionRecord) {
        val isIncome = transaction.amount > 0
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
            color = RivenColors.Surface
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(RivenColors.Background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isIncome) RivenColors.Green else RivenColors.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        val date = Instant.fromEpochMilliseconds(transaction.date)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        Text(
                            text = "${date.dayOfMonth} ${date.month.name.take(3)} • ${date.hour}:${date.minute.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RivenColors.TextSecondary
                        )
                    }
                }
                Text(
                    text = "${if(isIncome) "+" else ""} ₴${formatDecimal(transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = if (isIncome) RivenColors.Green else RivenColors.Red,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }

    private fun formatDecimal(value: Double): String {
        return ((value * 100).toInt() / 100.0).toString()
    }
}
