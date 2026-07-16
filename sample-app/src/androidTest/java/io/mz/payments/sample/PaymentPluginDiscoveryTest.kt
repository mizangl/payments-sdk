package io.mz.payments.sample

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mz.payments.core.PaymentsSdk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentPluginDiscoveryTest {
    @Test
    fun discoversExactlyThePaymentMethodsIncludedByTheFlavor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sdk = PaymentsSdk.builder(context).build()
        val expectedIds = BuildConfig.EXPECTED_PAYMENT_METHOD_IDS
            .split(',')
            .filter(String::isNotBlank)

        assertEquals(
            expectedIds,
            sdk.availablePaymentMethods.map { it.id.value },
        )
    }
}
