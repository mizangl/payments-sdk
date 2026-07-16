package io.mz.payments.core

import android.content.Context
import io.mz.payments.api.SdkLogger
import io.mz.payments.method.api.PaymentMethodDescriptor
import io.mz.payments.method.api.PaymentMethodId
import io.mz.payments.method.api.PaymentRequest
import io.mz.payments.method.api.PaymentResult

/** Public entry point for discovering and executing installed payment methods. */
interface PaymentsSdk {
    val availablePaymentMethods: List<PaymentMethodDescriptor>

    suspend fun pay(
        methodId: PaymentMethodId,
        request: PaymentRequest,
    ): PaymentResult

    /** Builds an independent SDK instance without process-global state. */
    class Builder internal constructor(context: Context) {
        private val applicationContext = context.applicationContext
        private var logger: SdkLogger = SdkLogger.None

        fun logger(logger: SdkLogger): Builder = apply {
            this.logger = logger
        }

        fun build(): PaymentsSdk = PaymentsSdkAssembler.build(
            context = applicationContext,
            logger = logger,
        )
    }

    companion object {
        @JvmStatic
        fun builder(context: Context): Builder = Builder(context)
    }
}

class PaymentMethodNotFoundException(methodId: PaymentMethodId) :
    IllegalArgumentException("Payment method '$methodId' is not installed.")

class PaymentPluginDiscoveryException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

class PaymentPluginInitializationException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

private object PaymentsSdkAssembler {
    fun build(
        context: Context,
        logger: SdkLogger,
    ): PaymentsSdk {
        val component = DaggerCoreComponent.factory().create(logger)
        val discovery = ManifestPaymentPluginDiscovery.create(context, logger)
        val registry = PaymentMethodRegistry.create(
            factories = discovery.discover(),
            dependencies = component,
        )

        return DefaultPaymentsSdk(registry)
    }
}

private class DefaultPaymentsSdk(
    private val registry: PaymentMethodRegistry,
) : PaymentsSdk {
    override val availablePaymentMethods: List<PaymentMethodDescriptor>
        get() = registry.descriptors

    override suspend fun pay(
        methodId: PaymentMethodId,
        request: PaymentRequest,
    ): PaymentResult = registry.pay(methodId, request)
}
