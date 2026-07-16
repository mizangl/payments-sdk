package io.mz.payments.sample

import android.content.Context
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.mz.payments.api.SdkLogger
import io.mz.payments.core.PaymentsSdk
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SdkModule {
    @Provides
    @Singleton
    fun providePaymentsSdk(
        @ApplicationContext context: Context,
    ): PaymentsSdk = PaymentsSdk.builder(context)
        .logger(SdkLogger { message -> Log.d("PaymentsSdk", message) })
        .build()
}
