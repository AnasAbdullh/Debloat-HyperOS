package com.debloat.hyperos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.debloat.hyperos.R
import com.debloat.hyperos.data.entity.DebloatAppEntity
import com.debloat.hyperos.ui.theme.AccentOrange
import com.debloat.hyperos.ui.theme.RemovedRed

@Composable
fun AppCard(
    app: DebloatAppEntity,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val titleDecoration = remember(app.isInstalled) {
        if (!app.isInstalled) TextDecoration.LineThrough else TextDecoration.None
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        // تثبيت توزيع عناصر الكرت ليبقى الـ Checkbox في جهة والبيانات واضحة
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Checkbox(
                    checked = app.isSelected,
                    onCheckedChange = onSelectedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentOrange,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        checkmarkColor = MaterialTheme.colorScheme.surface
                    )
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = app.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            color = if (!app.isInstalled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = titleDecoration
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        SystemBadge(isSystemApp = app.isSystemApp)
                    }

                    Text(
                        text = app.packageName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textDirection = TextDirection.Ltr
                        )
                    )
                }

                if (!app.isInstalled) {
                    RemovedChip()
                }
            }
        }
    }
}

@Composable
private fun SystemBadge(isSystemApp: Boolean) {
    Surface(
        color = if (isSystemApp) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        } else {
            AccentOrange.copy(alpha = 0.2f)
        },
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = stringResource(if (isSystemApp) R.string.badge_sys else R.string.badge_user),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = if (isSystemApp) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                AccentOrange
            },
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
        )
    }
}

@Composable
private fun RemovedChip() {
    Surface(
        color = RemovedRed.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(R.string.chip_removed),
            style = MaterialTheme.typography.labelSmall.copy(color = RemovedRed),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}