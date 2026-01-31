package ua.riven.app.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.riven.app.ui.components.NeumorphicCurrencySelector
import ua.riven.app.ui.components.NeumorphicInput
import ua.riven.app.ui.theme.BigActionButton
import ua.riven.app.ui.theme.RivenColors

@Composable
fun AddWalletSheet(
    onSave: (name: String, currency: String, balance: Double, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("₴") }
    var balance by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#2196F3") }

    val colorOptions = listOf(
        "#2196F3" to "Blue",
        "#4CAF50" to "Green",
        "#FF9800" to "Orange",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple",
        "#00BCD4" to "Cyan",
        "#FF5722" to "Deep Orange",
        "#607D8B" to "Blue Grey"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RivenColors.Surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "ADD ASSET",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
        )

        NeumorphicInput(
            value = name,
            onValueChange = { name = it },
            placeholder = "Wallet Name (e.g. Monobank)",
            contentDescription = "Wallet name input field",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "CURRENCY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = RivenColors.TextSecondary
                ),
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            NeumorphicCurrencySelector(
                selectedCurrency = currency,
                onCurrencySelected = { currency = it },
                currencies = listOf("₴", "$", "€", "₿"),
                modifier = Modifier.fillMaxWidth()
            )
        }

        NeumorphicInput(
            value = balance,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) balance = it },
            placeholder = "Current Balance",
            contentDescription = "Current balance input field",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "COLOR",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = RivenColors.TextSecondary
                ),
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(colorOptions) { (colorHex, colorName) ->
                    val isSelected = colorHex == selectedColor
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.9f else 1f,
                        label = "color_scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(colorHex)))
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.dp, Color.White, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { selectedColor = colorHex }
                            )
                            .semantics {
                                this.contentDescription = "Select $colorName color"
                                role = Role.Button
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val isFormValid = name.isNotBlank()
        
        BigActionButton(
            text = "SAVE WALLET",
            onClick = {
                val balanceValue = balance.toDoubleOrNull() ?: 0.0
                onSave(name, currency, balanceValue, selectedColor)
            },
            enabled = isFormValid
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
