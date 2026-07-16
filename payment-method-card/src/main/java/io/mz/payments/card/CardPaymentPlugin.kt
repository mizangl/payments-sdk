package io.mz.payments.card

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
internal annotation class CardPaymentScope

@Module
internal interface CardPaymentModule {
    @Binds
    fun bindPaymentMethod(implementation: CardPaymentMethod): PaymentMethod
}

@CardPaymentScope
@Component(
    dependencies = [SdkPluginDependencies::class],
    modules = [CardPaymentModule::class],
)
internal interface CardPaymentComponent {
    fun paymentMethod(): PaymentMethod

    @Component.Factory
    interface Factory {
        fun create(
            dependencies: SdkPluginDependencies,
        ): CardPaymentComponent
    }
}

@Keep
class CardPaymentPluginFactory : PaymentMethodPluginFactory {
    override fun create(dependencies: SdkPluginDependencies): PaymentMethod =
        DaggerCardPaymentComponent.factory()
            .create(dependencies)
            .paymentMethod()
}

internal class CardPaymentMethod @Inject constructor(
    private val logger: SdkLogger,
) : PaymentMethod {
    override val descriptor = PaymentMethodDescriptor(
        id = PaymentMethodId("card"),
        displayName = "Card payment",
    )

    override suspend fun pay(request: PaymentRequest): PaymentResult {
        logger.log("Processing ${request.reference} with card payment")
        return PaymentResult.Success(
            methodId = descriptor.id,
            transactionId = "card-${request.reference}",
        )
    }
}
