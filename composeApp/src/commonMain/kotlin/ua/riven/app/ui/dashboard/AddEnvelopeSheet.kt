package ua.riven.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.riven.app.ui.components.NeumorphicInput
import ua.riven.app.ui.theme.RivenButton
import ua.riven.app.ui.theme.RivenColors

@Composable
fun AddEnvelopeSheet(
    onSave: (name: String, targetAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RivenColors.Background)
            .padding(24.dp)
            .semantics { contentDescription = "Create new budget envelope form" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "NEW BUDGET ENVELOPE",
            style = MaterialTheme.typography.titleMedium.copy(
                color = RivenColors.TextSecondary,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
        )

        NeumorphicInput(
            value = name,
            onValueChange = { name = it },
            placeholder = "Envelope Name (e.g. Rent)",
            contentDescription = "Envelope name input field"
        )

        NeumorphicInput(
            value = targetAmount,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) targetAmount = it },
            placeholder = "Target Amount (₴)",
            contentDescription = "Target amount input field"
        )

        Spacer(modifier = Modifier.height(16.dp))

        val isFormValid = name.isNotBlank()
        
        RivenButton(
            text = "CREATE ENVELOPE",
            onClick = {
                val amountValue = targetAmount.toDoubleOrNull() ?: 0.0
                onSave(name, amountValue)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
