import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

// Release signing is driven entirely by environment variables so the keystore
// never lives in the repository. When they are absent (local dev, PR CI) the
// release build simply stays unsigned; the GitHub Release workflow provides
// them from repository secrets.
val keystoreFile: String? = System.getenv("KEYSTORE_FILE")

// versionCode must grow with every published build. The release workflow derives
// a monotonic value from the semantic version (major * 1000000 + minor * 1000 +
// patch), which keeps every release strictly above the last without depending on
// a CI run counter that resets when a workflow is renamed.
val versionCodeBase = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "com.dchernykh.serpent"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dchernykh.serpent"
        // Wear OS 3 (API 30) is the oldest platform Google still supports; older
        // watches run the pre-3 RPC-based platform, which this app does not target.
        minSdk = 30
        targetSdk = 36

        versionCode = versionCodeBase
        versionName = System.getenv("VERSION_NAME") ?: rootProject.extra["releasedVersion"] as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // A debug build carries its own applicationId, so it installs beside
            // a release one instead of colliding with it. Without the suffix,
            // pushing a debug build to a watch that already has the published app
            // fails outright on the signature mismatch, and the only way through
            // is to uninstall the app - and with it the high scores that are the
            // most interesting thing to test against.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Shrink and obfuscate with R8, and strip unused resources. APK size
            // matters more on a watch than on a phone: watches have far less
            // storage, and the APK is pushed over Bluetooth when a paired phone
            // installs it.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Fail the build on lint errors; warnings stay non-fatal for now and can
        // be promoted to errors once the codebase stabilises. Android Lint ships
        // the Wear OS checks (standalone flag, unsupported APIs, tile and
        // complication misuse), so this is the gate that catches watch-specific
        // manifest and API mistakes.
        abortOnError = true
        warningsAsErrors = false
        // lintDebug in CI covers analysis; skip the duplicate release lint pass.
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

ktlint {
    android.set(true)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

kover {
    reports {
        filters {
            excludes {
                // Generated code is not meaningful to cover.
                classes("*.BuildConfig", "*.R", "*.R$*", "*ComposableSingletons*")
                // The activity is the one thing here a JVM test cannot reach; the
                // instrumented test covers it by launching it. Whatever else ends
                // up out of a unit test's reach earns its own exclusion when it
                // is written, not before.
                classes("com.dchernykh.serpent.MainActivity*")
            }
        }
        verify {
            rule {
                // Nothing but the scaffold exists yet, so the bound stays at 0 to
                // keep it green. It is raised to 80 in the same change that lands
                // the game: the rule set, the pacing, the record decision and the
                // round-screen geometry are all plain Kotlin with no excuse for
                // being uncovered.
                minBound(0)
            }
        }
    }
}

// Pin transitive dependency versions for reproducible builds. Only the shipped
// and unit-test runtime classpaths are locked (Android's internal configurations
// are intentionally left out). Regenerate wear/gradle.lockfile with the
// "Update lockfiles" workflow or `./gradlew :wear:dependencies --write-locks`.
listOf(
    "debugRuntimeClasspath",
    "releaseRuntimeClasspath",
    "debugUnitTestRuntimeClasspath",
    "releaseUnitTestRuntimeClasspath",
).forEach { configurationName ->
    configurations.matching { it.name == configurationName }.configureEach {
        resolutionStrategy.activateDependencyLocking()
    }
}

dependencies {
    // Only what the scaffold itself uses. The game brings its own - a view
    // model, coroutines for the tick loop, a DataStore for the records - along
    // with the code that needs them, so nothing here is a dependency the APK
    // carries and no line of source justifies.
    implementation(libs.androidx.activity.compose)
    // Box, fillMaxSize and background are imported by name from
    // androidx.compose.foundation, so it is declared rather than left to arrive
    // through Wear Compose - which is free to stop bringing it in any release.
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // The instrumented test writes `org.junit.Test` and `org.junit.Assert`, so
    // JUnit is declared here rather than left to arrive transitively through
    // androidx.test.ext:junit - a transitive that is free to drop it in any
    // release, taking the test source set with it.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
