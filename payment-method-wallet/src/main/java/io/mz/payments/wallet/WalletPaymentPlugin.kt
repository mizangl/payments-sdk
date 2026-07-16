package io.mz.payments.wallet

import androidx.annotation.Keep
import dagger.Binds
import dagger.Component
import dagger.Module
import io.mz.payments.api.SdkLogger
import io.mz.payments.api.SdkPluginDependencies
import io.mz.payments.method.api.PaymentMethod
import io.mz.payments.method.api.PaymentMethodDescriptor
import io.mz.payments.method.api.PaymentMethodId
import io.mz.payments.method.api.PaymentMethodPluginFactory
import io.mz.payments.method.api.PaymentRequest
import io.mz.payments.method.api.PaymentResult
import javax.inject.Inject
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class WalletPaymentScope

@Module
internal interface WalletPaymentModule {
    @Binds
    fun bindPaymentMethod(implementation: WalletPaymentMethod): PaymentMethod
}

@WalletPaymentScope
@Component(
    dependencies = [SdkPluginDependencies::class],
    modules = [WalletPaymentModule::class],
)
internal interface WalletPaymentComponent {
    fun paymentMethod(): PaymentMethod

    @Component.Factory
    interface Factory {
        fun create(
            dependencies: SdkPluginDependencies,
        ): WalletPaymentComponent
    }
}

@Keep
class WalletPaymentPluginFactory : PaymentMethodPluginFactory {
    override fun create(dependencies: SdkPluginDependencies): PaymentMethod =
        DaggerWalletPaymentComponent.factory()
            .create(dependencies)
            .paymentMethod()
}

internal class WalletPaymentMethod @Inject constructor(
    private val logger: SdkLogger,
) : PaymentMethod {
    override val descriptor = PaymentMethodDescriptor(
        id = PaymentMethodId("wallet"),
        displayName = "Digital wallet",
    )

    override suspend fun pay(request: PaymentRequest): PaymentResult {
        logger.log("Processing ${request.reference} with digital wallet")
        return PaymentResult.Success(
            methodId = descriptor.id,
            transactionId = "wallet-${request.reference}",
        )
    }
}
