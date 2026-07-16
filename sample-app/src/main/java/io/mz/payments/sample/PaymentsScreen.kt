package io.mz.payments.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mz.payments.method.api.PaymentMethodDescriptor
import io.mz.payments.method.api.PaymentMethodId

private val BrandGreen = Color(0xFF176B4D)
private val Ink = Color(0xFF17211D)
private val SoftGray = Color(0xFFF3F5F4)

@Composable
fun PaymentsSampleApp() {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = BrandGreen,
            onPrimary = Color.White,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = SoftGray,
        ),
    ) {
        val viewModel: PaymentsViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        PaymentsScreen(
            state = state,
            onSelect = viewModel::select,
            onPay = viewModel::pay,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentsScreen(
    state: PaymentsUiState,
    onSelect: (PaymentMethodId) -> Unit,
    onPay: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Payments SDK") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
        containerColor = SoftGray,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Installed payment methods",
                style = MaterialTheme.typography.titleLarge,
            )

            if (state.methods.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "No payment methods discovered",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.methods.forEach { method ->
                        PaymentMethodRow(
                            method = method,
                            selected = state.selectedMethodId == method.id,
                            onSelect = { onSelect(method.id) },
                        )
                    }
                }

                Button(
                    onClick = onPay,
                    enabled = state.selectedMethodId != null && !state.processing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.processing) "Processing" else "Pay EUR 12.50")
                }
            }

            state.resultMessage?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = BrandGreen,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Build profile: ${BuildConfig.FLAVOR}",
                style = MaterialTheme.typography.labelMedium,
                color = Ink.copy(alpha = 0.66f),
            )
        }
    }
}

@Composable
private fun PaymentMethodRow(
    method: PaymentMethodDescriptor,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (selected) BorderStroke(2.dp, BrandGreen) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RadioButton(selected = selected, onClick = null)
            Column {
                Text(
                    text = method.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = method.id.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.copy(alpha = 0.62f),
                )
            }
        }
    }
}
