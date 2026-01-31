package ua.riven.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.riven.app.ui.theme.RivenColors

@Composable
fun ExpandableFab(
    onTransactionClick: () -> Unit,
    onWalletClick: () -> Unit,
    onEnvelopeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Dimmed Overlay
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { expanded = false }
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.padding(16.dp)
        ) {
            // Mini Buttons
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    FabSubItem(
                        label = "Transaction",
                        icon = Icons.Default.SwapHoriz,
                        iconColor = Color.White,
                        bgColor = RivenColors.Accent,
                        onClick = {
                            expanded = false
                            onTransactionClick()
                        }
                    )
                    FabSubItem(
                        label = "Wallet",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = Color.White,
                        bgColor = RivenColors.Surface,
                        onClick = {
                            expanded = false
                            onWalletClick()
                        }
                    )
                    FabSubItem(
                        label = "Envelope",
                        icon = Icons.Default.MailOutline,
                        iconColor = Color.White,
                        bgColor = RivenColors.Surface,
                        onClick = {
                            expanded = false
                            onEnvelopeClick()
                        }
                    )
                }
            }

            // Main FAB
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = RivenColors.Accent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun FabSubItem(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            color = RivenColors.Surface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
