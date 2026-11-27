package com.shikkha.app.auth.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shikkha.app.R

/**
 * Restricted screen shown to accounts whose registration is not approved (Spec 2.2).
 * Pending users cannot reach any learning, social, exam, or competition feature:
 * the navigation graph never offers a route, and RLS blocks data access anyway.
 */
@Composable
fun PendingApprovalScreen(
    rejected: Boolean,
    checking: Boolean,
    onCheckStatus: () -> Unit,
    onSignOut: () -> Unit,
) {
    AuthScaffold(title = stringResource(R.string.pending_title)) {
        Text(
            text = if (rejected) {
                stringResource(R.string.pending_status_rejected)
            } else {
                stringResource(R.string.pending_message)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCheckStatus,
            enabled = !checking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (checking) stringResource(R.string.loading)
                else stringResource(R.string.pending_check_again)
            )
        }
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.logout))
        }
    }
}
