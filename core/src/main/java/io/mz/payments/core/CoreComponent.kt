package io.mz.payments.core

import dagger.BindsInstance
import dagger.Component
import io.mz.payments.api.SdkLogger
import io.mz.payments.api.SdkPluginDependencies
import javax.inject.Singleton

@Singleton
@Component
internal interface CoreComponent : SdkPluginDependencies {
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance logger: SdkLogger,
        ): CoreComponent
    }
}
