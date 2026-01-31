package ua.riven.app.ui.envelopes

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
import androidx.compose.material.icons.filled.Edit
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
import ua.riven.app.ui.theme.GradientCard
import ua.riven.app.ui.theme.RivenColors
import ua.riven.app.ui.theme.RivenCard
import ua.riven.app.ui.components.NeumorphicInput

data class EnvelopeDetailsScreen(val envelopeId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<EnvelopeDetailsViewModel> { parametersOf(envelopeId) }
        val state by viewModel.state.collectAsState()
        val haptic = LocalHapticFeedback.current
        
        var showUpdateDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = RivenColors.Background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(state.envelope?.name ?: "ENVELOPE", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showUpdateDialog = true 
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Limit", tint = Color.White)
                        }
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteDialog = true 
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Envelope", tint = RivenColors.Red)
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
                // 1. Budget Card
                state.envelope?.let { envelope ->
                    GradientCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MONTHLY BUDGET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = envelope.icon,
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "₴ ${formatDecimal(envelope.monthlyLimit)}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val progress = (envelope.currentSpent / envelope.monthlyLimit).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = if (progress >= 1f) RivenColors.Red else Color.White,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Spent ₴ ${formatDecimal(envelope.currentSpent)} of ₴ ${formatDecimal(envelope.monthlyLimit)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // 2. History
                Text(
                    text = "ENVELOPE USAGE",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                )

                if (state.transactions.isEmpty()) {
                    // Empty State for Envelope Usage
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💸",
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
                            text = "Transactions linked to this envelope will appear here",
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
            
            if (showUpdateDialog && state.envelope != null) {
                UpdateLimitDialog(
                    currentLimit = state.envelope!!.monthlyLimit,
                    onDismiss = { showUpdateDialog = false },
                    onSave = { 
                        viewModel.onUpdateLimit(it)
                        showUpdateDialog = false
                    }
                )
            }

            if (showDeleteDialog) {
                DeleteEnvelopeDialog(
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.onDeleteEnvelope()
                        showDeleteDialog = false
                        navigator.pop()
                    }
                )
            }
        }
    }

    @Composable
    private fun DeleteEnvelopeDialog(
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = RivenColors.Surface,
            title = { Text("DELETE ENVELOPE", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure? Transactions linked to this envelope will remain but will be unlinked.", color = RivenColors.TextSecondary) },
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
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = RivenColors.Red,
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
                    text = "₴${formatDecimal(transaction.amount.let { if(it < 0) -it else it })}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = RivenColors.Red,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }

    @Composable
    private fun UpdateLimitDialog(
        currentLimit: Double,
        onDismiss: () -> Unit,
        onSave: (Double) -> Unit
    ) {
        var limit by remember { mutableStateOf(currentLimit.toString()) }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = RivenColors.Surface,
            title = { Text("UPDATE LIMIT", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeumorphicInput(
                        value = limit, 
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) limit = it }, 
                        placeholder = "New Monthly Limit"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val limitVal = limit.toDoubleOrNull() ?: currentLimit
                    onSave(limitVal)
                }) {
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

    private fun formatDecimal(value: Double): String {
        return ((value * 100).toInt() / 100.0).toString()
    }
}
