// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = "io.mz.payments"
    version = "1.0.0-SNAPSHOT"
}

val publishedModules = listOf(
    ":sdk-api",
    ":payment-method-api",
    ":core",
    ":payment-method-card",
    ":payment-method-wallet",
)

tasks.register("publishSdkToProjectRepository") {
    group = "publishing"
    description = "Publishes all SDK AARs to build/local-maven."
    dependsOn(
        publishedModules.map {
            "$it:publishReleasePublicationToLocalSdkRepository"
        },
    )
}

tasks.register("publishSdkToMavenLocal") {
    group = "publishing"
    description = "Publishes all SDK AARs to Maven local."
    dependsOn(
        publishedModules.map {
            "$it:publishReleasePublicationToMavenLocal"
        },
    )
}

tasks.register("verifyReleasePluginFactories") {
    group = "verification"
    description = "Builds minified AABs and verifies reflective plugin factories are not renamed."
    dependsOn(
        ":sample-app:bundleCardRelease",
        ":sample-app:bundleWalletRelease",
        ":sample-app:bundleAllRelease",
    )
    inputs.files(
        layout.projectDirectory.file(
            "sample-app/build/outputs/mapping/cardRelease/mapping.txt",
        ),
        layout.projectDirectory.file(
            "sample-app/build/outputs/mapping/walletRelease/mapping.txt",
        ),
        layout.projectDirectory.file(
            "sample-app/build/outputs/mapping/allRelease/mapping.txt",
        ),
    )

    doLast {
        val expectedFactories = mapOf(
            "cardRelease" to listOf(
                "io.mz.payments.card.CardPaymentPluginFactory",
            ),
            "walletRelease" to listOf(
                "io.mz.payments.wallet.WalletPaymentPluginFactory",
            ),
            "allRelease" to listOf(
                "io.mz.payments.card.CardPaymentPluginFactory",
                "io.mz.payments.wallet.WalletPaymentPluginFactory",
            ),
        )

        val mappingsByVariant = inputs.files.files.associateBy {
            it.parentFile.name
        }

        expectedFactories.forEach { (variant, classNames) ->
            val mappingFile = checkNotNull(mappingsByVariant[variant]) {
                "Missing declared R8 mapping input for $variant."
            }
            check(mappingFile.isFile) {
                "Missing R8 mapping for $variant at ${mappingFile.absolutePath}."
            }
            val mapping = mappingFile.readText()

            classNames.forEach { className ->
                check("$className -> $className:" in mapping) {
                    "R8 renamed or removed manifest factory '$className' in $variant."
                }
            }
        }
    }
}
