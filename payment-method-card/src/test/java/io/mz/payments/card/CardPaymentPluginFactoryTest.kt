package io.mz.payments.card

import io.mz.payments.api.SdkLogger
import io.mz.payments.api.SdkPluginDependencies
import io.mz.payments.method.api.Money
import io.mz.payments.method.api.PaymentMethodId
import io.mz.payments.method.api.PaymentRequest
import io.mz.payments.method.api.PaymentResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPaymentPluginFactoryTest {
    @Test
    fun `factory builds card method with core dependencies`() = runTest {
        val messages = mutableListOf<String>()
        val method = CardPaymentPluginFactory().create(
            dependencies = dependencies(SdkLogger(messages::add)),
        )
        val request = PaymentRequest("order-42", Money(1_250, "EUR"))

        val result = method.pay(request)

        assertEquals(PaymentMethodId("card"), method.descriptor.id)
        assertEquals(
            PaymentResult.Success(PaymentMethodId("card"), "card-order-42"),
            result,
        )
        assertTrue(messages.single().contains("order-42"))
    }

    private fun dependencies(logger: SdkLogger) =
        object : SdkPluginDependencies {
            override fun logger(): SdkLogger = logger
        }
}
