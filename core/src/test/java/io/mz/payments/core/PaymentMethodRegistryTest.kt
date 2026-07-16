package io.mz.payments.core

import io.mz.payments.api.SdkLogger
import io.mz.payments.api.SdkPluginDependencies
import io.mz.payments.method.api.Money
import io.mz.payments.method.api.PaymentMethod
import io.mz.payments.method.api.PaymentMethodDescriptor
import io.mz.payments.method.api.PaymentMethodId
import io.mz.payments.method.api.PaymentMethodPluginFactory
import io.mz.payments.method.api.PaymentRequest
import io.mz.payments.method.api.PaymentResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PaymentMethodRegistryTest {
    private val dependencies = object : SdkPluginDependencies {
        override fun logger(): SdkLogger = SdkLogger.None
    }

    @Test
    fun `empty factory list creates an empty registry`() {
        val registry = PaymentMethodRegistry.create(emptyList(), dependencies)

        assertEquals(emptyList<PaymentMethodDescriptor>(), registry.descriptors)
    }

    @Test
    fun `descriptors are sorted by payment method id`() {
        val registry = PaymentMethodRegistry.create(
            factories = listOf(factory("wallet"), factory("card")),
            dependencies = dependencies,
        )

        assertEquals(
            listOf("card", "wallet"),
            registry.descriptors.map { it.id.value },
        )
    }

    @Test
    fun `duplicate payment method ids fail initialization`() {
        assertThrows(PaymentPluginInitializationException::class.java) {
            PaymentMethodRegistry.create(
                factories = listOf(factory("card"), factory("card")),
                dependencies = dependencies,
            )
        }
    }

    @Test
    fun `factory failures are wrapped with plugin context`() {
        val failingFactory = PaymentMethodPluginFactory {
            error("Cannot create plugin")
        }

        assertThrows(PaymentPluginInitializationException::class.java) {
            PaymentMethodRegistry.create(
                factories = listOf(failingFactory),
                dependencies = dependencies,
            )
        }
    }

    @Test
    fun `pay delegates to the selected method`() = runTest {
        val registry = PaymentMethodRegistry.create(
            factories = listOf(factory("card")),
            dependencies = dependencies,
        )
        val request = PaymentRequest(
            reference = "order-42",
            amount = Money(1_250, "EUR"),
        )

        val result = registry.pay(PaymentMethodId("card"), request)

        assertEquals(
            PaymentResult.Success(PaymentMethodId("card"), "card-order-42"),
            result,
        )
    }

    @Test
    fun `pay fails for an unavailable method`() {
        val registry = PaymentMethodRegistry.create(emptyList(), dependencies)

        assertThrows(PaymentMethodNotFoundException::class.java) {
            kotlinx.coroutines.test.runTest {
                registry.pay(
                    PaymentMethodId("card"),
                    PaymentRequest("order-42", Money(1_250, "EUR")),
                )
            }
        }
    }

    private fun factory(id: String): PaymentMethodPluginFactory =
        PaymentMethodPluginFactory {
            object : PaymentMethod {
                override val descriptor = PaymentMethodDescriptor(
                    id = PaymentMethodId(id),
                    displayName = id,
                )

                override suspend fun pay(request: PaymentRequest): PaymentResult =
                    PaymentResult.Success(
                        methodId = descriptor.id,
                        transactionId = "$id-${request.reference}",
                    )
            }
        }
}
