package io.mz.payments.core

import io.mz.payments.api.SdkLogger
import io.mz.payments.api.SdkPluginDependencies
import io.mz.payments.method.api.PaymentMethod
import io.mz.payments.method.api.PaymentMethodDescriptor
import io.mz.payments.method.api.PaymentMethodId
import io.mz.payments.method.api.PaymentMethodPluginFactory
import io.mz.payments.method.api.PaymentRequest
import io.mz.payments.method.api.PaymentResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class ManifestPaymentPluginDiscoveryTest {
    @Test
    fun `ignores metadata outside the payment plugin namespace`() {
        val discovery = discovery(
            metadata = listOf(PluginMetadata("another.library.key", "Factory")),
        )

        assertEquals(emptyList<PaymentMethodPluginFactory>(), discovery.discover())
    }

    @Test
    fun `loads factories in deterministic metadata key order`() {
        val cardFactory = TestFactory("card")
        val walletFactory = TestFactory("wallet")
        val discovery = discovery(
            metadata = listOf(
                PluginMetadata("io.mz.payments.plugin.wallet", "WalletFactory"),
                PluginMetadata("io.mz.payments.plugin.card", "CardFactory"),
            ),
            instances = mapOf(
                "CardFactory" to cardFactory,
                "WalletFactory" to walletFactory,
            ),
        )

        assertEquals(listOf(cardFactory, walletFactory), discovery.discover())
    }

    @Test
    fun `caches metadata and instantiated factories`() {
        var metadataReads = 0
        var instantiations = 0
        val factory = TestFactory("card")
        val discovery = ManifestPaymentPluginDiscovery(
            metadataProvider = ApplicationMetadataProvider {
                metadataReads += 1
                listOf(
                    PluginMetadata("io.mz.payments.plugin.card", "CardFactory"),
                )
            },
            instantiator = FactoryInstantiator {
                instantiations += 1
                factory
            },
            logger = SdkLogger.None,
        )

        val first = discovery.discover()
        val second = discovery.discover()

        assertSame(first, second)
        assertEquals(1, metadataReads)
        assertEquals(1, instantiations)
    }

    @Test
    fun `fails when metadata does not contain a class name`() {
        val discovery = discovery(
            metadata = listOf(
                PluginMetadata("io.mz.payments.plugin.card", 42),
            ),
        )

        assertThrows(PaymentPluginDiscoveryException::class.java) {
            discovery.discover()
        }
    }

    @Test
    fun `fails when discovered class has the wrong type`() {
        val discovery = discovery(
            metadata = listOf(
                PluginMetadata("io.mz.payments.plugin.card", "NotAFactory"),
            ),
            instances = mapOf("NotAFactory" to Any()),
        )

        assertThrows(PaymentPluginDiscoveryException::class.java) {
            discovery.discover()
        }
    }

    @Test
    fun `wraps reflective construction failures`() {
        val discovery = ManifestPaymentPluginDiscovery(
            metadataProvider = ApplicationMetadataProvider {
                listOf(
                    PluginMetadata("io.mz.payments.plugin.card", "MissingFactory"),
                )
            },
            instantiator = FactoryInstantiator {
                throw ClassNotFoundException(it)
            },
            logger = SdkLogger.None,
        )

        assertThrows(PaymentPluginDiscoveryException::class.java) {
            discovery.discover()
        }
    }

    private fun discovery(
        metadata: List<PluginMetadata>,
        instances: Map<String, Any> = emptyMap(),
    ) = ManifestPaymentPluginDiscovery(
        metadataProvider = ApplicationMetadataProvider { metadata },
        instantiator = FactoryInstantiator { className ->
            instances.getValue(className)
        },
        logger = SdkLogger.None,
    )
}

private class TestFactory(
    private val id: String,
) : PaymentMethodPluginFactory {
    override fun create(dependencies: SdkPluginDependencies): PaymentMethod =
        object : PaymentMethod {
            override val descriptor = PaymentMethodDescriptor(
                id = PaymentMethodId(id),
                displayName = id,
            )

            override suspend fun pay(request: PaymentRequest): PaymentResult =
                PaymentResult.Success(descriptor.id, request.reference)
        }
}
