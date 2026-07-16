package io.mz.payments.core

import io.mz.payments.api.SdkPluginDependencies
import io.mz.payments.method.api.PaymentMethod
import io.mz.payments.method.api.PaymentMethodDescriptor
import io.mz.payments.method.api.PaymentMethodId
import io.mz.payments.method.api.PaymentMethodPluginFactory
import io.mz.payments.method.api.PaymentRequest
import io.mz.payments.method.api.PaymentResult

internal class PaymentMethodRegistry private constructor(
    private val methods: Map<PaymentMethodId, PaymentMethod>,
) {
    val descriptors: List<PaymentMethodDescriptor> =
        methods.values.map(PaymentMethod::descriptor)

    suspend fun pay(
        methodId: PaymentMethodId,
        request: PaymentRequest,
    ): PaymentResult = methods[methodId]?.pay(request)
        ?: throw PaymentMethodNotFoundException(methodId)

    companion object {
        fun create(
            factories: List<PaymentMethodPluginFactory>,
            dependencies: SdkPluginDependencies,
        ): PaymentMethodRegistry {
            val methods = linkedMapOf<PaymentMethodId, PaymentMethod>()

            factories.forEach { factory ->
                val method = try {
                    factory.create(dependencies)
                } catch (error: Exception) {
                    throw PaymentPluginInitializationException(
                        "Payment plugin '${factory.javaClass.name}' failed to initialize.",
                        error,
                    )
                }

                val id = method.descriptor.id
                if (methods.put(id, method) != null) {
                    throw PaymentPluginInitializationException(
                        "Duplicate payment method ID '$id'.",
                    )
                }
            }

            return PaymentMethodRegistry(
                methods = methods.toSortedMap(compareBy(PaymentMethodId::value)),
            )
        }
    }
}
