package ua.riven.app.ui.exchange

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import ua.riven.app.db.Wallet
import ua.riven.app.ui.theme.BigActionButton
import ua.riven.app.ui.theme.RivenColors

class ExchangeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ExchangeViewModel>()
        val state by viewModel.state.collectAsState()
        val haptic = LocalHapticFeedback.current

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) {
                navigator.pop()
            }
        }

        Scaffold(
            containerColor = RivenColors.Background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("EXCHANGE", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RivenColors.Background)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Amount Comparison section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // From Section
                        ExchangeWalletRow(
                            wallet = state.fromWallet,
                            amount = state.fromAmount,
                            isSource = true,
                            onClick = { /* Bottom sheet normally */ }
                        )

                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = RivenColors.Accent,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // To Section
                        ExchangeWalletRow(
                            wallet = state.toWallet,
                            amount = formatDecimal(state.toAmount),
                            isSource = false,
                            onClick = { /* Bottom sheet normally */ }
                        )
                        
                        Text(
                            text = "Rate: 1 ${state.fromWallet?.currency ?: ""} = ${formatDecimal(state.rate)} ${state.toWallet?.currency ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RivenColors.TextSecondary,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                // 2. Keypad
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
                                        if (key == "BKSP") viewModel.onBackspaceClick() 
                                        else viewModel.onNumberClick(key)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ConfirmButton(
                        isEnabled = (state.fromAmount.toDoubleOrNull() ?: 0.0) > 0.0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onExchange()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun ExchangeWalletRow(
        wallet: Wallet?,
        amount: String,
        isSource: Boolean,
        onClick: () -> Unit
    ) {
        val symbol = when(wallet?.currency) {
            "UAH" -> "₴"
            "USD" -> "$"
            "EUR" -> "€"
            "BTC" -> "₿"
            else -> wallet?.currency ?: ""
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSource) RivenColors.Surface else RivenColors.Background)
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = wallet?.name ?: "Select Wallet",
                    style = MaterialTheme.typography.labelSmall,
                    color = RivenColors.TextSecondary
                )
                Text(
                    text = if (isSource) "$symbol $amount" else "$symbol $amount",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RivenColors.TextSecondary)
        }
    }

    @Composable
    private fun KeypadButton(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        Box(
            modifier = modifier
                .heightIn(min = 64.dp) // Accessibility: Minimum 60dp touch target + padding
                .aspectRatio(1.5f)
                .clip(CircleShape)
                .background(if (isPressed) RivenColors.SurfaceVariant else RivenColors.Surface)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
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

    @Composable
    private fun ConfirmButton(
        isEnabled: Boolean,
        onClick: () -> Unit
    ) {
        BigActionButton(
            text = "EXCHANGE FUNDS",
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = RivenColors.Accent
        )
    }

    private fun formatDecimal(value: Double): String {
        return ((value * 100).toInt() / 100.0).toString()
    }
}
