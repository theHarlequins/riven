package ua.riven.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ua.riven.app.ui.envelopes.EnvelopesScreen
import ua.riven.app.ui.theme.ActionItem
import ua.riven.app.ui.theme.RivenColors

class MoreScreen : Screen {
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
                CenterAlignedTopAppBar(
                    title = { Text("MORE", fontWeight = FontWeight.ExtraBold) },
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
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Core Tools
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FINANCIAL TOOLS", style = MaterialTheme.typography.labelSmall, color = RivenColors.TextSecondary)
                    ActionItem(
                        title = "Budget Envelopes",
                        subtitle = "Manage your monthly limits",
                        icon = Icons.Default.MailOutline,
                        onClick = { navigator.push(EnvelopesScreen()) }
                    )
                    ActionItem(
                        title = "Transaction Export",
                        subtitle = "CSV, PDF (Coming Soon)",
                        icon = Icons.Default.IosShare,
                        iconColor = RivenColors.TextSecondary,
                        onClick = { /* TODO */ }
                    )
                }

                // 2. Simulation (Crisis Mode)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(RivenColors.Surface)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CRISIS SIMULATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = RivenColors.Red
                            )
                            Text(
                                "How would your portfolio look if...",
                                style = MaterialTheme.typography.bodySmall,
                                color = RivenColors.TextSecondary
                            )
                        }
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RivenColors.Red)
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("USD/UAH Rate", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("₴ ${formatDecimal(state.currentUsdRate)}", color = RivenColors.Accent, fontWeight = FontWeight.ExtraBold)
                        }
                        Slider(
                            value = state.currentUsdRate.toFloat(),
                            onValueChange = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onRateSliderChanged(it.toDouble()) 
                            },
                            valueRange = 40f..150f,
                            colors = SliderDefaults.colors(
                                thumbColor = RivenColors.Accent,
                                activeTrackColor = RivenColors.Accent,
                                inactiveTrackColor = RivenColors.Background
                            )
                        )
                        Text(
                            "Slide to simulate currency devaluation and check your runway.",
                            style = MaterialTheme.typography.labelSmall,
                            color = RivenColors.TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // 3. App Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("RIVΞN v1.0.0", color = RivenColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("Built for Financial Sovereignty", color = RivenColors.TextSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    private fun formatDecimal(value: Double): String {
        return ((value * 100).toInt() / 100.0).toString()
    }
}
