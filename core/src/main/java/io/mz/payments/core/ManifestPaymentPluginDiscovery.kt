package io.mz.payments.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import io.mz.payments.api.SdkLogger
import io.mz.payments.method.api.PaymentMethodPluginFactory

internal data class PluginMetadata(
    val key: String,
    val value: Any?,
)

internal fun interface ApplicationMetadataProvider {
    fun entries(): List<PluginMetadata>
}

internal fun interface FactoryInstantiator {
    fun instantiate(className: String): Any
}

internal class ManifestPaymentPluginDiscovery(
    private val metadataProvider: ApplicationMetadataProvider,
    private val instantiator: FactoryInstantiator,
    private val logger: SdkLogger,
) {
    private val factories by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadFactories()
    }

    fun discover(): List<PaymentMethodPluginFactory> = factories

    private fun loadFactories(): List<PaymentMethodPluginFactory> =
        metadataProvider.entries()
            .asSequence()
            .filter { it.key.startsWith(PLUGIN_PREFIX) }
            .sortedBy(PluginMetadata::key)
            .map(::instantiateFactory)
            .toList()

    private fun instantiateFactory(
        metadata: PluginMetadata,
    ): PaymentMethodPluginFactory {
        val className = metadata.value as? String
            ?: throw PaymentPluginDiscoveryException(
                "Plugin metadata '${metadata.key}' must contain a factory class name.",
            )

        val instance = try {
            instantiator.instantiate(className)
        } catch (error: ReflectiveOperationException) {
            throw PaymentPluginDiscoveryException(
                "Cannot instantiate payment plugin factory '$className'.",
                error,
            )
        }

        val factory = instance as? PaymentMethodPluginFactory
            ?: throw PaymentPluginDiscoveryException(
                "Class '$className' does not implement PaymentMethodPluginFactory.",
            )

        logger.log("Discovered payment plugin factory: $className")
        return factory
    }

    companion object {
        private const val PLUGIN_PREFIX = "io.mz.payments.plugin."

        fun create(
            context: Context,
            logger: SdkLogger,
        ): ManifestPaymentPluginDiscovery = ManifestPaymentPluginDiscovery(
            metadataProvider = AndroidApplicationMetadataProvider(context),
            instantiator = ReflectionFactoryInstantiator(context.classLoader),
            logger = logger,
        )
    }
}

private class AndroidApplicationMetadataProvider(
    context: Context,
) : ApplicationMetadataProvider {
    private val packageManager = context.packageManager
    private val packageName = context.packageName

    @Suppress("DEPRECATION")
    override fun entries(): List<PluginMetadata> {
        val metadata = applicationInfo().metaData ?: return emptyList()
        return metadata.keySet().map { key ->
            PluginMetadata(key = key, value = metadata.get(key))
        }
    }

    @Suppress("DEPRECATION")
    private fun applicationInfo(): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(
                    PackageManager.GET_META_DATA.toLong(),
                ),
            )
        } else {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA,
            )
        }
}

private class ReflectionFactoryInstantiator(
    private val classLoader: ClassLoader,
) : FactoryInstantiator {
    override fun instantiate(className: String): Any =
        Class.forName(className, true, classLoader)
            .getDeclaredConstructor()
            .newInstance()
}
