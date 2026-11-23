package com.salah.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun SalahCard(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    containerColor: Color? = null,
    border: BorderStroke? = null,
    shape: Shape? = null,
    inset: Dp = 16.dp,
    gap: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val cardShape = shape ?: if (emphasized) RoundedCornerShape(20.dp) else RoundedCornerShape(16.dp)
    // In dark mode: use surfaceContainer for clean tonal depth without noisy borders
    val cardBg = containerColor ?: if (emphasized) colors.surfaceContainer else colors.surface
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = colors.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(inset),
            verticalArrangement = Arrangement.spacedBy(gap),
            content = content,
        )
    }
}
