package com.debloat.hyperos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.debloat.hyperos.R
import com.debloat.hyperos.ui.theme.AccentOrange
import com.debloat.hyperos.viewmodel.FilterTab

@Composable
fun BottomActionBar(
    filterTab: FilterTab,
    selectedCount: Int,
    batchInProgress: Boolean,
    batchLabel: String,
    batchCompleted: Int,
    batchTotal: Int,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseAction = if (filterTab == FilterTab.REMOVED) {
        stringResource(R.string.action_restore)
    } else {
        stringResource(R.string.action_uninstall)
    }

    val buttonText = if (selectedCount > 0) "$baseAction ($selectedCount)" else baseAction
    val enabled = selectedCount > 0 && !batchInProgress

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (batchInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentOrange
                    )
                    Text(
                        text = "  $batchLabel ($batchCompleted/$batchTotal)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Button(
                onClick = onActionClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.Black,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}