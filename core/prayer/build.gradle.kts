plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.salah.prayer"
    compileSdk = 35
    defaultConfig { minSdk = 23 }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(libs.adhan)
    implementation(libs.room.runtime)
    implementation(libs.okhttp)
    implementation(libs.moshi)
    implementation(libs.coroutines.android)
    ksp(libs.room.compiler)
    ksp(libs.moshi.codegen)
    coreLibraryDesugaring(libs.desugar)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
}

tasks.withType<Test>().configureEach {
    val runComparison = providers.gradleProperty("comparePrayerReference").isPresent
    if (!runComparison) exclude("**/GoogleReferenceComparison.class")
    testLogging.showStandardStreams = runComparison
}
