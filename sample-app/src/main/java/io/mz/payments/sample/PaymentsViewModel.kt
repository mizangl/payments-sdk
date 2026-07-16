package io.mz.payments.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.mz.payments.core.PaymentsSdk
import io.mz.payments.method.api.Money
import io.mz.payments.method.api.PaymentMethodDescriptor
import io.mz.payments.method.api.PaymentMethodId
import io.mz.payments.method.api.PaymentRequest
import io.mz.payments.method.api.PaymentResult
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentsUiState(
    val methods: List<PaymentMethodDescriptor>,
    val selectedMethodId: PaymentMethodId?,
    val processing: Boolean = false,
    val resultMessage: String? = null,
)

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val paymentsSdk: PaymentsSdk,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        PaymentsUiState(
            methods = paymentsSdk.availablePaymentMethods,
            selectedMethodId = paymentsSdk.availablePaymentMethods.firstOrNull()?.id,
        ),
    )
    val uiState: StateFlow<PaymentsUiState> = _uiState.asStateFlow()

    fun select(methodId: PaymentMethodId) {
        _uiState.update {
            it.copy(selectedMethodId = methodId, resultMessage = null)
        }
    }

    fun pay() {
        val methodId = _uiState.value.selectedMethodId ?: return
        if (_uiState.value.processing) return

        viewModelScope.launch {
            _uiState.update { it.copy(processing = true, resultMessage = null) }

            try {
                val result = paymentsSdk.pay(
                    methodId = methodId,
                    request = PaymentRequest(
                        reference = "sample-order-42",
                        amount = Money(amountMinor = 1_250, currencyCode = "EUR"),
                    ),
                )
                _uiState.update {
                    it.copy(
                        processing = false,
                        resultMessage = result.toMessage(),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        processing = false,
                        resultMessage = error.message ?: "Payment failed",
                    )
                }
            }
        }
    }
}

private fun PaymentResult.toMessage(): String = when (this) {
    is PaymentResult.Success -> "Approved: $transactionId"
    is PaymentResult.Failure -> "Declined ($code): $message"
}
