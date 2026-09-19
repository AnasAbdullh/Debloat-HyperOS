package com.debloat.hyperos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.debloat.hyperos.ui.theme.AccentOrange
import com.debloat.hyperos.viewmodel.CategoryGroup

@Composable
fun CategorySectionHeader(
    group: CategoryGroup,
    allSelected: Boolean,
    onSelectAllToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(AccentOrange, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = group.category,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.width(8.dp))

        CountPill(removed = group.removedCount, total = group.totalCount)

        Spacer(modifier = Modifier.weight(1f))

        Switch(
            checked = allSelected,
            onCheckedChange = onSelectAllToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentOrange,
                checkedTrackColor = AccentOrange.copy(alpha = 0.35f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun CountPill(removed: Int, total: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = "$removed/$total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}