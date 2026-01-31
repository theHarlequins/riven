package ua.riven.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ua.riven.app.db.Wallet
import ua.riven.app.ui.theme.RivenColors

@Composable
fun NeumorphicPieChart(
    wallets: List<Wallet>,
    totalNetWorth: Double,
    usdRate: Double,
    modifier: Modifier = Modifier
) {
    if (wallets.isEmpty() || totalNetWorth <= 0.0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available", color = RivenColors.TextSecondary)
        }
        return
    }

    val chartColors = listOf(
        RivenColors.Accent,
        RivenColors.Blue,
        RivenColors.Green,
        Color(0xFFFFD54F),
        Color(0xFFE91E63)
    )

    val segments = wallets.mapIndexed { index, wallet ->
        val weight = when (wallet.currency) {
            "UAH" -> wallet.balance
            "USD" -> wallet.balance * usdRate
            "EUR" -> wallet.balance * (usdRate * 1.07)
            "BTC" -> wallet.balance * 3804200.0
            else -> wallet.balance
        }
        val proportion = (weight / totalNetWorth).toFloat()
        PieSegment(
            name = wallet.name,
            proportion = proportion,
            color = chartColors[index % chartColors.size]
        )
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background circle
        Canvas(modifier = Modifier.size(200.dp)) {
            drawCircle(
                color = RivenColors.Surface,
                radius = size.minDimension / 2,
                style = Stroke(width = 30.dp.toPx())
            )
        }

        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            val strokeWidth = 30.dp.toPx()

            segments.forEach { segment ->
                val sweepAngle = segment.proportion * 360f
                if (sweepAngle > 0) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f, // Small gap
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                startAngle += sweepAngle
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "₴ ${formatDecimal(totalNetWorth)}",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = RivenColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            )
            Text(
                text = "TOTAL ASSETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = RivenColors.TextSecondary,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

private data class PieSegment(
    val name: String,
    val proportion: Float,
    val color: Color
)

private fun formatDecimal(value: Double): String {
    return ((value * 100).toInt() / 100.0).toString()
}
