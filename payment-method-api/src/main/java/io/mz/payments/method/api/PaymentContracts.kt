package io.mz.payments.method.api

import io.mz.payments.api.SdkPluginDependencies

/** Stable identifier contributed by a payment-method plugin. */
@JvmInline
value class PaymentMethodId(val value: String) {
    init {
        require(value.isNotBlank()) { "Payment method ID cannot be blank." }
    }

    override fun toString(): String = value
}

/** Monetary value represented in minor units to avoid floating-point rounding. */
data class Money(
    val amountMinor: Long,
    val currencyCode: String,
) {
    init {
        require(amountMinor > 0) { "Amount must be greater than zero." }
        require(currencyCode.matches(Regex("[A-Z]{3}"))) {
            "Currency must be a three-letter uppercase code."
        }
    }
}

/** Input shared by every payment-method implementation. */
data class PaymentRequest(
    val reference: String,
    val amount: Money,
) {
    init {
        require(reference.isNotBlank()) { "Payment reference cannot be blank." }
    }
}

/** User-facing information needed to list an installed payment method. */
data class PaymentMethodDescriptor(
    val id: PaymentMethodId,
    val displayName: String,
) {
    init {
        require(displayName.isNotBlank()) { "Display name cannot be blank." }
    }
}

/** Expected result of a payment attempt. */
sealed interface PaymentResult {
    data class Success(
        val methodId: PaymentMethodId,
        val transactionId: String,
    ) : PaymentResult

    data class Failure(
        val methodId: PaymentMethodId,
        val code: String,
        val message: String,
    ) : PaymentResult
}

/** Runtime payment behavior supplied by an optional plugin AAR. */
interface PaymentMethod {
    val descriptor: PaymentMethodDescriptor

    suspend fun pay(request: PaymentRequest): PaymentResult
}

/**
 * Manifest-discovered entry point implemented by each payment-method AAR.
 *
 * Implementations must expose a public no-argument constructor.
 */
fun interface PaymentMethodPluginFactory {
    fun create(dependencies: SdkPluginDependencies): PaymentMethod
}
