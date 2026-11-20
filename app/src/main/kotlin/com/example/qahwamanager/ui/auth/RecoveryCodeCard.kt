package com.example.qahwamanager.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.theme.QahwaTheme

/** Shows a recovery code once, with the warning to write it down, and a single way out. */
@Composable
fun RecoveryCodeCard(code: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val qColors = QahwaTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.recovery_code_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = qColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        QahwaCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = code,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp),
                fontWeight = FontWeight.Bold,
                color = qColors.primaryAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp, horizontal = 16.dp),
            )
        }
        Text(
            text = stringResource(R.string.recovery_code_message),
            style = MaterialTheme.typography.bodyMedium,
            color = qColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        QahwaPrimaryButton(
            text = stringResource(R.string.recovery_code_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
