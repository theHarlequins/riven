package ua.riven.app.ui.envelopes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ua.riven.app.db.Envelope
import ua.riven.app.ui.envelopes.EnvelopeDetailsScreen
import ua.riven.app.ui.components.NeumorphicInput
import ua.riven.app.ui.theme.BigActionButton
import ua.riven.app.ui.theme.RivenColors
import ua.riven.app.ui.theme.RivenCard

class EnvelopesScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<EnvelopesViewModel>()
        val state by viewModel.state.collectAsState()
        
        var showAddDialog by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = RivenColors.Background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("BUDGET ENVELOPES", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RivenColors.Background)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = RivenColors.Accent,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Envelope")
                }
            }
        ) { paddingValues ->
            if (state.envelopes.isEmpty()) {
                // Empty State - Phase 1 Fix
                EmptyEnvelopesState(
                    onAddFirst = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.envelopes) { envelope ->
                        EnvelopeListItem(envelope) {
                            navigator.push(EnvelopeDetailsScreen(envelope.id))
                        }
                    }
                }
            }
            
            if (showAddDialog) {
                AddEnvelopeDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { name, limit, icon, color ->
                        viewModel.onAddEnvelope(name, limit, icon, color)
                        showAddDialog = false
                    }
                )
            }
        }
    }

    @Composable
    private fun AddEnvelopeDialog(
        onDismiss: () -> Unit,
        onSave: (String, Double, String, String) -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var limit by remember { mutableStateOf("") }
        var icon by remember { mutableStateOf("🛍️") }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = RivenColors.Surface,
            title = { Text("NEW ENVELOPE", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeumorphicInput(value = name, onValueChange = { name = it }, placeholder = "Name (e.g. Food)")
                    NeumorphicInput(value = limit, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) limit = it }, placeholder = "Monthly Limit (₴)")
                    NeumorphicInput(value = icon, onValueChange = { icon = it }, placeholder = "Icon (Emoji)")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val limitVal = limit.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) onSave(name, limitVal, icon, "#7F5AF0")
                }) {
                    Text("CREATE", color = RivenColors.Accent, fontWeight = FontWeight.Bold)
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
    private fun EnvelopeListItem(envelope: Envelope, onClick: () -> Unit) {
        val progress = if (envelope.monthlyLimit > 0) (envelope.currentSpent / envelope.monthlyLimit).toFloat() else 0f
        
        RivenCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RivenColors.Background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(envelope.icon, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = envelope.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "₴ ${formatDecimal(envelope.currentSpent)} of ₴ ${formatDecimal(envelope.monthlyLimit)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = RivenColors.TextSecondary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Modern Flat Progress Bar
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (progress > 1f) RivenColors.Red else RivenColors.Accent,
                    trackColor = RivenColors.Background
                )
            }
        }
    }

    /**
     * Empty Envelopes State - Phase 1 UX Fix
     * Rationale: Zero-data states should provide a clear path forward with
     * domain-relevant iconography and a high-visibility CTA.
     */
    @Composable
    private fun EmptyEnvelopesState(
        onAddFirst: () -> Unit
    ) {
        val haptic = LocalHapticFeedback.current
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Envelope/Box Icon
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
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(RivenColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = RivenColors.Accent,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "No envelopes yet",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Create budget envelopes to track spending by category and stay within your limits",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RivenColors.TextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // High-visibility CTA Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddFirst()
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
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create First Envelope",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }
    }

    private fun formatDecimal(value: Double): String {
        return ((value * 100).toInt() / 100.0).toString()
    }
}
