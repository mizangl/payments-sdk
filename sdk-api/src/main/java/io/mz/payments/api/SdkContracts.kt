package io.mz.payments.api

/** Receives diagnostic messages from the SDK without imposing a logging framework. */
fun interface SdkLogger {
    fun log(message: String)

    companion object {
        /** Default logger used when the host does not provide one. */
        val None: SdkLogger = SdkLogger {
            println(it)
        }
    }
}

/** Narrow dependency surface exposed by the core graph to optional plugins. */
interface SdkPluginDependencies {
    fun logger(): SdkLogger
}
