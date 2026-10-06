buildscript {
    repositories { google(); mavenCentral(); maven("https://developer.huawei.com/repo/") }
    dependencies { classpath("com.huawei.agconnect:agcp:1.9.6.300") }
}

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
