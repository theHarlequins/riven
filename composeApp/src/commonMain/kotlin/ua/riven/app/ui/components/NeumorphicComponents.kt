package ua.riven.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import ua.riven.app.ui.theme.RivenColors

@Composable
fun NeumorphicInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RivenColors.Surface)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = TextStyle(
                    color = RivenColors.TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = RivenColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(RivenColors.Accent),
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NeumorphicCurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    currencies: List<String> = listOf("₴", "$", "€"),
    modifier: Modifier = Modifier,
    contentDescription: String = "Select currency"
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(RivenColors.Surface)
            .padding(4.dp)
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        currencies.forEach { currency ->
            val isSelected = currency == selectedCurrency
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                label = "currency_scale"
            )
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) RivenColors.Accent else Color.Transparent)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onCurrencySelected(currency) }
                    )
                    .padding(vertical = 12.dp)
                    .semantics {
                        this.contentDescription = "Currency $currency"
                        role = Role.Button
                    }
            ) {
                Text(
                    text = currency,
                    color = if (isSelected) Color.White else RivenColors.TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}
