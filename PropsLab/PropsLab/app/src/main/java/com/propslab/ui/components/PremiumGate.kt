package com.propslab.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.propslab.billing.Feature
import com.propslab.brand.Brand
import com.propslab.ui.theme.Palette

/**
 * The free build's placeholder for a paid surface.
 *
 * It is a placeholder on purpose: no feed is read, no request is spent and no sample data is
 * invented to make the locked screen look populated. It says what the feature is, what the
 * free build has instead, and where the unlock lives.
 */
@Composable
fun PremiumGate(
    feature: Feature,
    title: String,
    body: String,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, tone = Palette.Court) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(feature.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.Faint)
            Spacer(Modifier.weight(1f))
            Badge("PREMIUM", Palette.Caution, filled = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Palette.Chalk)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.Dust)
        Spacer(Modifier.height(8.dp))
        Text(
            "Free build: ${feature.freeVersion}.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
        Spacer(Modifier.height(14.dp))
        ActionButton(
            "See what Premium adds",
            onOpenPremium,
            tone = Palette.Gain,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "One plan, ${Brand.PRICE_LABEL} a ${Brand.PRICE_PERIOD}, with a seven-day trial. All " +
                "analysis stays free.",
            style = MaterialTheme.typography.bodySmall, color = Palette.Faint,
        )
    }
}
